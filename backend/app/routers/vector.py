"""
Vector operations and RAG (Retrieval Augmented Generation) endpoints.

This module handles document vectorization and chat-with-data functionality
using embeddings and semantic search with pgvector.
"""

import json
import logging
from typing import List, Dict, Any, Optional
from sqlalchemy.orm import Session
from app.db.session import get_db
from fastapi import Depends, HTTPException
from fastapi.routing import APIRouter
from app.schemas.user import User
from app.core.security import get_current_user
from app.db.models import Document_chunks as DBDocument_chunks
from litellm import completion
from app.services.rag_service import get_embedding, json_to_markdown
from app.core.config import settings


router = APIRouter()
logger = logging.getLogger(__name__)


@router.post("/vectorize_json")
async def process_slide(
    json_data: dict,
    report_id: int,
    slide_index: int,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> Dict[str, str]:
    """
    Vectorize slide data and store in vector database.
    
    Converts JSON table data to markdown format, creates vector embedding,
    and stores for RAG semantic search.
    
    Args:
        json_data: Slide data dictionary
        report_id: Parent report ID
        slide_index: Slide number
        user: Authenticated user
        db: Database session
        
    Returns:
        Status confirmation dictionary
    """
    # Extract raw JSON data
    raw_json = json_data
    
    # Convert to RAG-optimized format
    md_text = json_to_markdown(raw_json)
    vector = get_embedding(md_text)
    
    # Store chunk with metadata
    chunk = DBDocument_chunks(
        content=md_text,
        mdata=json.dumps({"report_id": report_id, "slide_index": slide_index}),
        embedding=str(vector)
    )

    db.add(chunk)
    db.commit()
   
    logger.info(f"Vectorized slide {slide_index} from report {report_id}")
    return {"status": "Indexed for AI search"}


@router.post("/chat-with-data")
async def chat_rag(
    query: str,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> Dict[str, Any]:
    """
    RAG-powered chat: semantic search + LLM generation.
    
    Workflow:
    1. Create embedding from user query
    2. Semantic search in PostgreSQL (pgvector cosine distance)
    3. Retrieve top 5 most similar document chunks
    4. Build context prompt with retrieved data
    5. Generate answer using LLM
    
    Args:
        query: User's question
        user: Authenticated user
        db: Database session
        
    Returns:
        Dictionary with:
            - answer: LLM-generated response
            - sources: List of source metadata for citations
    """
    # Step A: Create query vector
    query_vec = get_embedding(query)
    
    # Step B: Semantic search in PostgreSQL
    # Operator <=> means "cosine distance"
    # Note: This uses raw SQL for pgvector operators
    search_sql = """
    SELECT content, mdata 
    FROM document_chunks 
    ORDER BY embedding <=> :q_vec 
    LIMIT 5
    """
    
    # Execute semantic search
    results = db.execute(search_sql, {"q_vec": str(query_vec)}).fetchall()
    
    # Step C: Build context for LLM
    context_str = "\n\n".join([
        f"Source: {r['mdata']}\nData:\n{r['content']}" 
        for r in results
    ])
    
    system_prompt = f"""
    You are a financial analyst. Answer ONLY based on the data provided below.
    If the data does not contain the answer, state that clearly.
    
    Available data:
    {context_str}
    """
    
    # Step D: Call LLM for answer generation
    response = completion(
        model=settings.model_name,
        messages=[
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": query}
        ]
    )
    
    return {
        "answer": response.choices[0].message.content,
        "sources": [r['mdata'] for r in results]  # Cite sources
    }