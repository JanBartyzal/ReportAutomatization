#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROTO_DIR="${SCRIPT_DIR}/../packages/protos"

echo "=== Proto Generation ==="
echo "Proto directory: ${PROTO_DIR}"

# Check buf is installed
if ! command -v buf &> /dev/null; then
  echo "ERROR: 'buf' CLI not found. Install it from https://buf.build/docs/installation"
  exit 1
fi

# Clean previously generated code
rm -rf "${PROTO_DIR}/gen"

# Run buf lint
echo "[1/3] Linting protos..."
cd "${PROTO_DIR}"
buf lint

# Run buf generate
echo "[2/3] Generating stubs..."
buf generate

# Verify output
echo "[3/3] Verifying generated code..."
if [ -d "${PROTO_DIR}/gen/java" ] && [ -d "${PROTO_DIR}/gen/python" ]; then
  TOTAL=$(find "${PROTO_DIR}/gen" -type f | wc -l)
  echo "SUCCESS: Generated ${TOTAL} files"
  echo "  Java stubs:   gen/java/"
  echo "  Python stubs: gen/python/"
else
  echo "ERROR: Expected gen/java/ and gen/python/ directories"
  exit 1
fi
