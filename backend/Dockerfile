FROM python:3.11-slim-bookworm

WORKDIR /app

# Instalace systémových závislostí pro python-pptx a image processing
RUN apt-get update && apt-get install -y \
    libgl1-mesa-glx \
    libglib2.0-0 \
    tesseract-ocr \
    tesseract-ocr-ces \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Zbytek kódu mapujeme přes volume v docker-compose, 
# takže tady COPY . . není nutné pro dev, ale pro prod ano.
COPY . .

# Spuštění
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000", "--reload"]