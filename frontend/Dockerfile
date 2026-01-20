# --- Stage 1: Build ---
# ZMĚNA: Upgrade z node:18 na node:22-alpine kvůli Vite v7+
FROM node:22-alpine AS builder

WORKDIR /app

# Kopírování závislostí
COPY package.json package-lock.json* ./

# Instalace (používáme npm install pro jistotu)
RUN npm install

# Kopírování celého zdrojového kódu (VČETNĚ index.html)
COPY . .

# Build
RUN npm run build

EXPOSE 5173
CMD ["npm", "run", "dev"]