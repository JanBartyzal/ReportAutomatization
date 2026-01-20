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
from dbmodels import UploadFile as DBUploadFile,Report as DBReport,SlideData as DBSlideData
from litellm import embedding, completion
from pgvector.sqlalchemy import Vector
from rag import get_embedding, json_to_markdown


# Inicializace DB (v produkci řešeno přes migrace/Alembic)
#models.Base.metadata.create_all(bind=engine)

router = APIRouter()
logger = logging.getLogger("uvicorn")
API_ENV = os.getenv("API_ENV", "production")
db = SessionLocal()

@router.post("/api/generate-report")
async def generate_report(
    file_data: dict, 
    user: User = Depends(get_current_user) # <--- TADY JE ZABEZPEČENÍ
):
    """
    Tento endpoint zavolá jen přihlášený uživatel.
    """
    print(f"Report generuje uživatel: {user.email} (ID: {user.oid})")
    
    # --- 3. ROW LEVEL SECURITY (LOGIKA) ---
    # Do databáze ukládáme ID uživatele
    # db.execute("INSERT INTO reports (user_id, data) VALUES (:uid, :data)", 
    #            {"uid": user.oid, "data": file_data})
    
    return {"message": "Processing started", "user": user.oid}

@router.get("/api/my-reports")
async def get_my_reports(user: User = Depends(get_current_user)):
    """
    Vrátí data JEN pro konkrétního uživatele.
    """
    # --- 3. ROW LEVEL SECURITY (LOGIKA) ---
    # query = "SELECT * FROM reports WHERE user_id = :uid"
    # results = db.fetch_all(query, {"uid": user.oid})
    
    return [
        {"id": 1, "title": "Můj tajný report", "owner": user.oid},
        # {"id": 2, "title": "Cizí report"} <-- Tohle sem nepatří
    ]

