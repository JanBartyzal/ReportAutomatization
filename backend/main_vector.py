import os
import shutil
import logging
import requests 
from sqlalchemy.orm import Session
from database import SessionLocal, engine
from typing import List, Optional
from fastapi import FastAPI, UploadFile, File, Form, Depends, HTTPException, Body
from fastapi.routing import APIRouter
from pydantic import BaseModel
from models import User
from userauth import get_current_user, verify_admin
from redis_cache import cache
from dbmodels import UploadFile as DBUploadFile,Report as DBReport,SlideData as DBSlideData, Document_chunks as DBDocument_chunks
from litellm import embedding, completion
from pgvector.sqlalchemy import Vector
from rag import get_embedding, json_to_markdown


# Inicializace DB (v produkci řešeno přes migrace/Alembic)
#models.Base.metadata.create_all(bind=engine)

router = APIRouter()
logger = logging.getLogger("uvicorn")
API_ENV = os.getenv("API_ENV", "production")
db = SessionLocal()


@router.post("/vectorize_json")
async def process_slide(json_data: dict, report_id: int, slide_index: int):
    # 1. Extrakce dat (jako dřív)
    raw_json = json_data 
    
    # 2. Příprava pro RAG
    md_text = json_to_markdown(raw_json)
    vector = get_embedding(md_text)
    

    chunk=DBDocument_chunks(
        content=md_text,
        mdata=json.dumps({"report_id": report_id, "slide_index": slide_index}),
        embedding=str(vector)
    )

    db.add(chunk)
    db.commit()
   
    
    return {"status": "Indexed for AI search"}

@router.post("/chat-with-data")
async def chat_rag(query: str, user: User = Depends(get_current_user)):
    """
    Nejdůležitější endpoint: RAG vyhledávání
    """
    # A. Vytvoř vektor z dotazu uživatele
    query_vec = get_embedding(query)
    
    # B. Sémantické hledání v Postgresu (najdi 5 nejpodobnějších tabulek)
    # Operátor <=> znamená "cosine distance"
    search_sql = """
    SELECT content, mdata 
    FROM document_chunks 
    ORDER BY embedding <=> :q_vec 
    LIMIT 5
    """
    results = await database.fetch_all(search_sql, {"q_vec": str(query_vec)})
    
    # C. Sestavení kontextu pro LLM
    context_str = "\n\n".join([f"Source: {r['mdata']}\nData:\n{r['content']}" for r in results])
    
    system_prompt = f"""
    Jsi finanční analytik. Odpovídej POUZE na základě níže uvedených dat.
    Pokud data neobsahují odpověď, řekni to.
    
    Dostupná data:
    {context_str}
    """
    
    # D. Volání LLM (Generování odpovědi)
    response = completion(
        model=os.getenv("MODEL_NAME"), # gpt-4-vision nebo llama3
        messages=[
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": query}
        ]
    )
    
    return {
        "answer": response.choices[0].message.content,
        "sources": [r['mdata'] for r in results] # Přiznáme zdroje
    }