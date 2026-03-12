"""MCP Server setup – registers tools and handles authentication.

Implements the Model Context Protocol (MCP) server with tool definitions
for querying OPEX data, searching documents, checking report status,
and comparing periods.
"""

from __future__ import annotations

import json
import logging
from typing import Any

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from src.auth.obo_flow import TokenClaims, validate_token
from src.client.ai_client import AiClient
from src.db.connection import DatabasePool
from src.tools.compare_periods import compare_periods
from src.tools.query_opex import query_opex_data
from src.tools.report_status import get_report_status
from src.tools.search_documents import search_documents

logger = logging.getLogger(__name__)

# Tool definitions following MCP specification
TOOL_DEFINITIONS = [
    {
        "name": "query_opex_data",
        "description": "Query aggregated OPEX data for the user's organization. "
                       "Returns structured financial data filtered by period and metric.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "period": {
                    "type": "string",
                    "description": "Period filter (e.g., '2024-Q1', '2024-01')",
                },
                "metric": {
                    "type": "string",
                    "description": "Metric to aggregate (e.g., 'amount_czk', 'it_costs')",
                },
            },
        },
    },
    {
        "name": "search_documents",
        "description": "Search documents using text or semantic (vector) search. "
                       "Always scoped to the user's organization.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "query": {
                    "type": "string",
                    "description": "Search query text",
                },
                "semantic": {
                    "type": "boolean",
                    "description": "Use semantic (vector) search if true, text search if false",
                    "default": True,
                },
            },
            "required": ["query"],
        },
    },
    {
        "name": "get_report_status",
        "description": "Get the submission status matrix for a reporting period. "
                       "Shows which forms have been submitted and when.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "period_id": {
                    "type": "string",
                    "description": "Period identifier to check status for",
                },
            },
            "required": ["period_id"],
        },
    },
    {
        "name": "compare_periods",
        "description": "Compare OPEX data between two reporting periods. "
                       "Returns deltas and percentage changes.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "period_a": {
                    "type": "string",
                    "description": "First period identifier",
                },
                "period_b": {
                    "type": "string",
                    "description": "Second period identifier",
                },
                "metric": {
                    "type": "string",
                    "description": "Optional metric to compare",
                },
            },
            "required": ["period_a", "period_b"],
        },
    },
]


def _extract_bearer_token(request: Request) -> str | None:
    """Extract bearer token from Authorization header."""
    auth_header = request.headers.get("authorization", "")
    if auth_header.startswith("Bearer "):
        return auth_header[7:]
    return None


def create_mcp_app(db: DatabasePool) -> FastAPI:
    """Create the MCP sub-application with tool endpoints.

    Args:
        db: Database connection pool for tool queries.

    Returns:
        FastAPI app implementing MCP protocol endpoints.
    """
    mcp_app = FastAPI()
    ai_client = AiClient()

    @mcp_app.get("/tools")
    async def list_tools() -> JSONResponse:
        """List available MCP tools."""
        return JSONResponse({"tools": TOOL_DEFINITIONS})

    @mcp_app.post("/tools/{tool_name}")
    async def call_tool(tool_name: str, request: Request) -> JSONResponse:
        """Execute an MCP tool with authentication and RLS enforcement."""
        # Authenticate
        token = _extract_bearer_token(request)
        if not token:
            return JSONResponse(
                {"error": "Missing authorization token"},
                status_code=401,
            )

        try:
            claims: TokenClaims = validate_token(token)
        except ValueError as e:
            return JSONResponse({"error": str(e)}, status_code=401)

        org_id = claims.org_id
        user_id = claims.user_id

        # Parse request body
        try:
            body = await request.json()
        except Exception:
            body = {}

        arguments = body.get("arguments", body)

        # Dispatch to tool
        result = await _dispatch_tool(
            tool_name, db, ai_client, org_id, user_id, arguments,
        )

        return JSONResponse(result)

    return mcp_app


async def _dispatch_tool(
    tool_name: str,
    db: DatabasePool,
    ai_client: AiClient,
    org_id: str,
    user_id: str,
    arguments: dict[str, Any],
) -> dict[str, Any]:
    """Route a tool call to the correct handler."""
    match tool_name:
        case "query_opex_data":
            return await query_opex_data(
                db=db,
                org_id=org_id,
                period=arguments.get("period"),
                metric=arguments.get("metric"),
            )
        case "search_documents":
            return await search_documents(
                db=db,
                ai_client=ai_client,
                org_id=org_id,
                user_id=user_id,
                query=arguments.get("query", ""),
                semantic=arguments.get("semantic", True),
            )
        case "get_report_status":
            return await get_report_status(
                db=db,
                org_id=org_id,
                period_id=arguments.get("period_id", ""),
            )
        case "compare_periods":
            return await compare_periods(
                db=db,
                org_id=org_id,
                period_a=arguments.get("period_a", ""),
                period_b=arguments.get("period_b", ""),
                metric=arguments.get("metric"),
            )
        case _:
            return {"error": f"Unknown tool: {tool_name}"}
