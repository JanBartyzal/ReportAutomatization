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
from userauth import get_azure_user, get_current_user, PROD_MODE


# Inicializace DB (v produkci řešeno přes migrace/Alembic)
#models.Base.metadata.create_all(bind=engine)

router = APIRouter()
logger = logging.getLogger("uvicorn")
API_ENV = os.getenv("API_ENV", "production")
db = SessionLocal()

@router.get("/all-stats")
async def admin_stats(user: User = Depends(verify_admin)):
    return {"message": "Vítej admine, vidíš vše."}

@router.get("/items")
async def read_items(
    user: User = Depends(get_azure_user) if PROD_MODE else Depends(get_current_user)
):
    return {"user": user}