import logging
from fastapi import FastAPI
import models
import dbmodels
from database import engine

# Import routers
import main_admin
import main_import
import main_opex
import main_report
import main_vector
from fastapi.middleware.cors import CORSMiddleware

# Init logger
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("ReportAutomation")

# Create tables
dbmodels.Base.metadata.create_all(bind=engine)

app = FastAPI(title="Report Automation API")

# Definujte povolené zdroje
origins = [
    "http://localhost:5173",
    "http://127.0.0.1:5173",
]

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=True,
    allow_methods=["*"],  # Povolí GET, POST, atd.
    allow_headers=["*"],  # Povolí všechny hlavičky včetně Authorization
)


@app.get("/api/health")
def health_check():
    return {"status": "ok", "service": "report-automation"}

# --- Include Routers ---
app.include_router(main_admin.router, prefix="/api/admin", tags=["admin"])
app.include_router(main_import.router, prefix="/api/import", tags=["import"])
app.include_router(main_opex.router, prefix="/api/opex", tags=["opex"])
app.include_router(main_report.router, prefix="/api/report", tags=["report"])
app.include_router(main_vector.router, prefix="/api/vector", tags=["vector"])


logger.info("Report Automation started with modular structure.")