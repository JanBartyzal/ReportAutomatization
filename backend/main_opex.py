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
from opex import OpexManager


# Inicializace DB (v produkci řešeno přes migrace/Alembic)
#models.Base.metadata.create_all(bind=engine)

router = APIRouter()
logger = logging.getLogger("uvicorn")
API_ENV = os.getenv("API_ENV", "production")
db = SessionLocal()



@router.get("/data")
async def opex_data(
    opexId: str, 
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

@router.get("/run_opex_secure")
async def run_opex_secure(
    file_id: str, 
    user: User = Depends(get_current_user) # <--- TADY JE ZABEZPEČENÍ
):
    opex_manager = OpexManager()
    opex_manager.process_opex(file_id)
    return {"message": "Processing started", "user": user.oid}

@router.get("/run_opex")
async def run_opex(file_id: str):
    opex_manager = OpexManager()
    opex_manager.process_opex(file_id)
    return {"message": "Processing started"}

@router.get("/get_file_header")
async def get_file_header(file_id: str):
    opex_manager = OpexManager()
    result = opex_manager.get_presetation_header(file_id)
    return result

@router.get("/get_slide_data")
async def get_slide_data(file_id: str, slide_id: int):
    opex_manager = OpexManager()
    result = opex_manager.get_slide_data(file_id, slide_id)
    return result