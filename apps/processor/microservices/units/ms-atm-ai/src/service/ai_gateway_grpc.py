"""gRPC service implementing ``AiGatewayServiceServicer``.

Each RPC validates the request context, checks quota, enforces rate limits,
delegates to AiService, and builds the proto response.
"""

from __future__ import annotations

import logging
from typing import Any

import grpc

from atomizer.v1 import ai_pb2, ai_pb2_grpc  # type: ignore[import-untyped]

from src.models.context import extract_context_from_grpc
from src.service.ai_service import AiService
from src.service.quota_service import QuotaService

logger = logging.getLogger(__name__)


class AiGatewayGrpcService(ai_pb2_grpc.AiGatewayServiceServicer):
    """Async gRPC servicer for the AI Gateway.

    Provides three RPCs:
    - ``AnalyzeSemantic`` – text classification, summarization, entity extraction.
    - ``GenerateEmbeddings`` – vector embeddings for pgVector storage.
    - ``SuggestCleaning`` – column name normalization suggestions.
    """

    def __init__(
        self,
        ai_service: AiService | None = None,
        quota_service: QuotaService | None = None,
    ) -> None:
        self._ai = ai_service or AiService()
        self._quota = quota_service or QuotaService()

    async def _check_quota(self, org_id: str, context: grpc.aio.ServicerContext) -> bool:
        """Check quota and abort with RESOURCE_EXHAUSTED if exceeded."""
        if await self._quota.is_exceeded(org_id):
            await context.abort(
                grpc.StatusCode.RESOURCE_EXHAUSTED,
                f"Monthly token quota exceeded for org={org_id}. tokens_remaining: 0",
            )
            return False
        return True

    # ------------------------------------------------------------------
    # AnalyzeSemantic
    # ------------------------------------------------------------------

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

    # ------------------------------------------------------------------
    # GenerateEmbeddings
    # ------------------------------------------------------------------

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

    # ------------------------------------------------------------------
    # SuggestCleaning
    # ------------------------------------------------------------------

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

        # Convert proto SheetRow objects to plain lists
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
