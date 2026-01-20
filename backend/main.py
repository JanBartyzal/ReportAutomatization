import os
import shutil
import logging
import requests 
from sqlalchemy.orm import Session
from database import SessionLocal, engine
from typing import List, Optional
from fastapi import FastAPI, UploadFile, File, Form, Depends, HTTPException, Body
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

app = FastAPI(title="Parser Service API")
logger = logging.getLogger("uvicorn")
API_ENV = os.getenv("API_ENV", "production")
db = SessionLocal()


@app.get("/api/health")
async def health_check():
    # Public endpoint (např. pro load balancer)
    return {"status": "ok"}

@app.post("/api/generate-report")
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

@app.get("/api/my-reports")
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

@app.get("/api/admin/all-stats")
async def admin_stats(user: User = Depends(verify_admin)):
    return {"message": "Vítej admine, vidíš vše."}

@app.post("/api/upload")
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
    
    file_path = os.path.join("uploads", filename)
    
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
@app.get("/api/get-list-regions")
async def get_list_regions(user: User = Depends(get_current_user)):
    regions = db.query("SELECT * FROM regions").all()
    return regions


@app.get("/api/get-available-regions")
async def get_available_regions(user: User = Depends(get_current_user)):
    region = user.region
    if Depends(verify_admin):
        region = "all"
    return region

@app.get("/api/get-list-uploaded-files")
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

@app.post("/api/process-and-vectorize")
async def process_slide(file_data: dict, user: User = Depends(get_current_user)):
    # 1. Extrakce dat (jako dřív)
    raw_json = extract_table_logic(...) 
    
    # 2. Příprava pro RAG
    md_text = json_to_markdown(raw_json)
    vector = get_embedding(md_text)
    
    # 3. Uložení do DB (Data + Vektor)
    query = """
    INSERT INTO document_chunks (content, metadata, embedding)
    VALUES (:content, :meta, :emb)
    """
    await database.execute(query, {
        "content": md_text,
        "meta": json.dumps({"source": file_data['filename'], "owner": user.oid}),
        "emb": str(vector) # pgvector bere pole jako string "[0.1, 0.2...]"
    })
    
    return {"status": "Indexed for AI search"}

@app.post("/api/chat-with-data")
async def chat_rag(query: str, user: User = Depends(get_current_user)):
    """
    Nejdůležitější endpoint: RAG vyhledávání
    """
    # A. Vytvoř vektor z dotazu uživatele
    query_vec = get_embedding(query)
    
    # B. Sémantické hledání v Postgresu (najdi 5 nejpodobnějších tabulek)
    # Operátor <=> znamená "cosine distance"
    search_sql = """
    SELECT content, metadata 
    FROM document_chunks 
    ORDER BY embedding <=> :q_vec 
    LIMIT 5
    """
    results = await database.fetch_all(search_sql, {"q_vec": str(query_vec)})
    
    # C. Sestavení kontextu pro LLM
    context_str = "\n\n".join([f"Source: {r['metadata']}\nData:\n{r['content']}" for r in results])
    
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
        "sources": [r['metadata'] for r in results] # Přiznáme zdroje
    }