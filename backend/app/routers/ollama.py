"""
Ollama proxy router.

This module provides a proxy endpoint to the Ollama service to allow 
the frontend to communicate with Ollama through the backend.
"""

from fastapi import APIRouter, Request, Response, HTTPException
import httpx
import json
from app.core.config import settings
import logging

router = APIRouter()
logger = logging.getLogger("uvicorn")

@router.post("/generate")
async def generate_proxy(request: Request):
    """
    Proxy request to Ollama generate endpoint.
    
    Args:
        request: FastAPI request object
        
    Returns:
        Ollama service response
    """
    url = f"{settings.ollama_base_url}/chat"
    
    print("Ollama URL: ", url)
    print("Ollama Request: ", request)

    try:
        # Get request body
        body = await request.json()
        
        async with httpx.AsyncClient(timeout=settings.ollama_timeout) as client:
            response = await client.post(url, json=body)
            
            # Forward the response back to caller
            return Response(
                content=response.content,
                status_code=response.status_code,
                headers=dict(response.headers)
            )
            
    except json.JSONDecodeError:
        raise HTTPException(status_code=400, detail="Invalid JSON body")
    except httpx.RequestError as exc:
        logger.error(f"Error proxying request to Ollama: {exc}")
        raise HTTPException(status_code=503, detail=f"Ollama service unavailable: {str(exc)}")
    except Exception as e:
        logger.error(f"Unexpected error in Ollama proxy: {e}")
        raise HTTPException(status_code=500, detail=str(e))
