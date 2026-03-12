"""gRPC service implementing ``AiGatewayServiceServicer``."""

from __future__ import annotations

import logging
from typing import Any, TYPE_CHECKING

import grpc

from atomizer.v1 import ai_pb2, ai_pb2_grpc  # type: ignore[import-untyped]

from src.atomizers.ai.client.dapr_client import DaprStateClient
from src.atomizers.ai.client.litellm_client import LiteLLMClient
from src.atomizers.ai.service.ai_service import AiService
from src.atomizers.ai.service.prompt_service import PromptService
from src.atomizers.ai.service.quota_service import QuotaService
from src.atomizers.ai.service.rate_limiter import RateLimiter
from src.common.context import extract_context_from_grpc

if TYPE_CHECKING:
    from src.common.config import Settings

logger = logging.getLogger(__name__)


class AiGatewayGrpcService(ai_pb2_grpc.AiGatewayServiceServicer):
    """Async gRPC servicer for the AI Gateway."""

    def __init__(
        self,
        settings: "Settings | None" = None,
        ai_service: AiService | None = None,
        quota_service: QuotaService | None = None,
    ) -> None:
        if settings is None:
            from src.common.config import Settings
            settings = Settings()

        if ai_service is None:
            llm_client = LiteLLMClient(
                base_url=settings.litellm_base_url,
                api_key=settings.litellm_api_key,
            )
            dapr_client = DaprStateClient(
                host=settings.dapr_host,
                port=settings.dapr_http_port,
                store_name=settings.dapr_statestore_name,
            )
            prompt_service = PromptService(config_path=settings.prompts_config_path)
            quota_svc = quota_service or QuotaService(
                state_client=dapr_client,
                monthly_token_quota=settings.default_monthly_token_quota,
            )
            rate_limiter = RateLimiter(max_concurrent=settings.max_concurrent_per_org)

            ai_service = AiService(
                llm_client=llm_client,
                prompt_service=prompt_service,
                quota_service=quota_svc,
                rate_limiter=rate_limiter,
                model_semantic=settings.model_semantic,
                model_embedding=settings.model_embedding,
            )

        self._ai = ai_service
        self._quota = quota_service or QuotaService(
            monthly_token_quota=settings.default_monthly_token_quota,
        )

    async def _check_quota(self, org_id: str, context: grpc.aio.ServicerContext) -> bool:
        """Check quota and abort with RESOURCE_EXHAUSTED if exceeded."""
        if await self._quota.is_exceeded(org_id):
            await context.abort(
                grpc.StatusCode.RESOURCE_EXHAUSTED,
                f"Monthly token quota exceeded for org={org_id}. tokens_remaining: 0",
            )
            return False
        return True

    async def AnalyzeSemantic(
        self,
        request: Any,
        context: grpc.aio.ServicerContext,
    ) -> ai_pb2.SemanticResponse:
        """Semantic analysis of extracted text."""
        req_ctx = extract_context_from_grpc(context)
        org_id = request.context.org_id or req_ctx.org_id
        user_id = request.context.user_id or req_ctx.user_id

        logger.info(
            "[%s] AnalyzeSemantic org=%s type=%s",
            req_ctx.correlation_id, org_id, request.analysis_type,
        )

        await self._check_quota(org_id, context)

        params = dict(request.parameters) if request.parameters else {}
        result = await self._ai.analyze_semantic(
            org_id=org_id,
            user_id=user_id,
            text=request.text,
            analysis_type=request.analysis_type,
            parameters=params,
        )

        return ai_pb2.SemanticResponse(
            result=result.result,
            entities=result.entities,
            classification=result.classification,
            tokens_used=result.tokens_used,
            tokens_remaining=result.tokens_remaining,
        )

    async def GenerateEmbeddings(
        self,
        request: Any,
        context: grpc.aio.ServicerContext,
    ) -> ai_pb2.EmbeddingResponse:
        """Generate vector embeddings for a document."""
        req_ctx = extract_context_from_grpc(context)
        org_id = request.context.org_id or req_ctx.org_id
        user_id = request.context.user_id or req_ctx.user_id

        logger.info(
            "[%s] GenerateEmbeddings org=%s doc=%s",
            req_ctx.correlation_id, org_id, request.document_id,
        )

        await self._check_quota(org_id, context)

        result = await self._ai.generate_embeddings(
            org_id=org_id,
            user_id=user_id,
            document_id=request.document_id,
            text=request.text,
        )

        return ai_pb2.EmbeddingResponse(
            document_id=result.document_id,
            embedding=result.embedding,
            tokens_used=result.tokens_used,
        )

    async def SuggestCleaning(
        self,
        request: Any,
        context: grpc.aio.ServicerContext,
    ) -> ai_pb2.CleaningResponse:
        """Suggest column name normalization and type detection."""
        req_ctx = extract_context_from_grpc(context)
        org_id = request.context.org_id or req_ctx.org_id
        user_id = request.context.user_id or req_ctx.user_id

        logger.info(
            "[%s] SuggestCleaning org=%s headers=%d",
            req_ctx.correlation_id, org_id, len(request.headers),
        )

        await self._check_quota(org_id, context)

        sample_rows = [[cell for cell in row.cells] for row in request.sample_rows]

        result = await self._ai.suggest_cleaning(
            org_id=org_id,
            user_id=user_id,
            headers=list(request.headers),
            sample_rows=sample_rows,
        )

        suggestions_pb = [
            ai_pb2.ColumnSuggestion(
                col_index=s.col_index,
                original_name=s.original_name,
                suggested_name=s.suggested_name,
                suggested_type=s.suggested_type,
                confidence=s.confidence,
            )
            for s in result.suggestions
        ]

        return ai_pb2.CleaningResponse(
            suggestions=suggestions_pb,
            tokens_used=result.tokens_used,
        )
