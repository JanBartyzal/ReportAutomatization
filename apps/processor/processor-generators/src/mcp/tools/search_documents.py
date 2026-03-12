"""MCP tool: search_documents -- search documents with optional semantic search.

Supports both text search and semantic search (via pgVector similarity).
"""

from __future__ import annotations

import logging
from typing import Any

from src.mcp.client.ai_client import AiClient
from src.mcp.db.connection import DatabasePool

logger = logging.getLogger(__name__)


async def search_documents(
    db: DatabasePool,
    ai_client: AiClient,
    org_id: str,
    user_id: str,
    query: str,
    semantic: bool = True,
) -> dict[str, Any]:
    """Search documents scoped to the user's organization.

    Args:
        db: Database connection pool with RLS enforcement.
        ai_client: AI client for embedding generation.
        org_id: Organization ID (from authenticated user).
        user_id: User ID for quota tracking.
        query: Search query text.
        semantic: If true, use vector similarity search. Otherwise text search.

    Returns:
        Dict with search results and metadata.
    """
    try:
        if semantic:
            return await _semantic_search(db, ai_client, org_id, user_id, query)
        else:
            return await _text_search(db, org_id, query)
    except Exception as e:
        logger.error("search_documents failed: %s", e)
        return {"status": "error", "message": str(e), "results": []}


async def _semantic_search(
    db: DatabasePool,
    ai_client: AiClient,
    org_id: str,
    user_id: str,
    query: str,
) -> dict[str, Any]:
    """Perform semantic search using pgVector similarity."""
    # Generate embedding for the search query
    embedding = await ai_client.generate_embedding(query, org_id, user_id)

    if not embedding:
        logger.warning("Empty embedding returned, falling back to text search")
        return await _text_search(db, org_id, query)

    # Convert to pgVector format
    embedding_str = "[" + ",".join(str(v) for v in embedding) + "]"

    sql = """
        SELECT
            document_id,
            content,
            metadata,
            1 - (embedding <=> $2::vector) AS similarity
        FROM documents
        WHERE org_id = $1
        ORDER BY embedding <=> $2::vector
        LIMIT 10
    """

    rows = await db.execute_with_rls(org_id, sql, [org_id, embedding_str])

    results = []
    for row in rows:
        record = dict(row)
        record["similarity"] = float(record.get("similarity", 0))
        results.append(record)

    return {
        "status": "success",
        "search_type": "semantic",
        "count": len(results),
        "results": results,
    }


async def _text_search(
    db: DatabasePool,
    org_id: str,
    query: str,
) -> dict[str, Any]:
    """Perform plain text search on document content."""
    sql = """
        SELECT
            document_id,
            content,
            metadata
        FROM documents
        WHERE org_id = $1
          AND content ILIKE '%' || $2 || '%'
        LIMIT 20
    """

    rows = await db.execute_with_rls(org_id, sql, [org_id, query])

    return {
        "status": "success",
        "search_type": "text",
        "count": len(rows),
        "results": [dict(row) for row in rows],
    }
