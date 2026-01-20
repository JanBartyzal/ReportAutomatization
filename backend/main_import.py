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
import hashlib


# Inicializace DB (v produkci řešeno přes migrace/Alembic)
#models.Base.metadata.create_all(bind=engine)

router = APIRouter()
logger = logging.getLogger("uvicorn")
API_ENV = os.getenv("API_ENV", "production")
db = SessionLocal()


@router.post("/upload")
async def upload_file(
    file: UploadFile = File(...),
    user: User = Depends(get_current_user)
):
    """
    Tento endpoint zavolá jen přihlášený uživatel.
    """
    print(f"File uploaded by user: {user.email} (ID: {user.oid})")
    
    # --- 3. ROW LEVEL SECURITY (LOGIKA) ---
    # Do databáze ukládáme ID uživatele

    file_data = await file.read()
    
    filename = file.filename
    filename = filename.replace(" ", "_")
    prefixFilename = filename.split(".")[0]
    extensionFilename = filename.split(".")[1]
    md5hash = hashlib.md5(file_data).hexdigest()
    filename = f"{prefixFilename}_{md5hash}.{extensionFilename}"
    
    file_path = os.path.join("local_data/uploads", filename)
    
    with open(file_path, "wb") as f:
        f.write(file_data)

    upload_file = DBUploadFile(
        oid=user.oid,
        filename=filename,
        md5hash=md5hash
    )    
    
    db.add(upload_file)
    db.commit()    

    # db.execute("INSERT INTO reports (user_id, data) VALUES (:uid, :data)", 
    #            {"uid": user.oid, "data": file_data})
    
    return {"message": "File uploaded successfully", "user": user.oid}



@router.post("/uploadopex")
async def upload_opex_file(
    file: UploadFile = File(...),
    user: User = Depends(get_current_user)
):
    """
    Tento endpoint zavolá jen přihlášený uživatel.
    """
    print(f"File uploaded by user: {user} )")
    
    # --- 3. ROW LEVEL SECURITY (LOGIKA) ---
    # Do databáze ukládáme ID uživatele

    file_data = await file.read()
    
    filename = file.filename
    filename = filename.replace(" ", "_")
    prefixFilename = filename.split(".")[0]
    extensionFilename = filename.split(".")[1]
    md5hash = hashlib.md5(file_data).hexdigest()
    filename = f"{prefixFilename}_{md5hash}.{extensionFilename}"
    
    file_path = os.path.join("local_data/uploads", filename)
    
    with open(file_path, "wb") as f:
        f.write(file_data)

    upload_file = DBUploadFile(
        oid=user.oid,
        filename=filename,
        md5hash=md5hash
    )    
    
    db.add(upload_file)
    db.commit()    

    # db.execute("INSERT INTO reports (user_id, data) VALUES (:uid, :data)", 
    #            {"uid": user.oid, "data": file_data})
    
    return {"message": "File uploaded successfully", "user": user}


@router.get("/get-list-uploaded-files")
async def get_list_uploaded_files(user: User = Depends(get_current_user)):
    region = user.region
    if Depends(verify_admin):
        region = "all"

    if region is None or region == "" or region == "all":
        query = "SELECT * FROM upload_files"
        results = db.fetch_all(query)
    else:
        query = "SELECT * FROM upload_files WHERE region = :region"
        results = db.fetch_all(query, {"region": region})
    
    return results
