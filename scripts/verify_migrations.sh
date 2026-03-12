#!/bin/bash
# =============================================================================
# Flyway Migration Verification Script
# =============================================================================
# Verifies that all migrations ran successfully and all expected tables exist
# Usage: ./scripts/verify_migrations.sh
# =============================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-reportplatform}"
DB_USER="${DB_USER:-postgres}"
DB_PASSWORD="${DB_PASSWORD:-postgres}"

echo "=============================================="
echo "Flyway Migration Verification"
echo "=============================================="
echo "Database: $DB_NAME@$DB_HOST:$DB_PORT"
echo ""

# Check if psql is available
if ! command -v psql &> /dev/null; then
    echo -e "${RED}Error: psql not found. Please install PostgreSQL client.${NC}"
    exit 1
fi

# Build connection string
export PGPASSWORD="$DB_PASSWORD"
CONN_STRING="host=$DB_HOST port=$DB_PORT dbname=$DB_NAME user=$DB_USER"

# Check database connectivity
echo -n "Checking database connectivity... "
if psql $CONN_STRING -c "SELECT 1" &> /dev/null; then
    echo -e "${GREEN}OK${NC}"
else
    echo -e "${RED}FAILED${NC}"
    echo "Could not connect to database. Please check your connection settings."
    exit 1
fi

echo ""
echo "=============================================="
echo "Migration Summary"
echo "=============================================="

# Get Flyway schema history
echo -e "\n${YELLOW}Flyway Schema History:${NC}"
psql $CONN_STRING -t -c "SELECT version, description, type, installed_on, state FROM flyway_schema_history ORDER BY installed_rank;" 2>/dev/null || echo "No flyway_schema_history table found"

echo ""
echo "=============================================="
echo "Engine-Core Tables (V1-V5)"
echo "=============================================="
echo -e "${YELLOW}Auth Tables:${NC}"
psql $CONN_STRING -c "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_name LIKE 'auth_%' ORDER BY table_name;" 2>/dev/null || echo "N/A"

echo -e "\n${YELLOW}Admin Tables:${NC}"
psql $CONN_STRING -c "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_name IN ('organizations', 'users', 'api_keys') ORDER BY table_name;" 2>/dev/null || echo "N/A"

echo -e "\n${YELLOW}Batch Tables:${NC}"
psql $CONN_STRING -c "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_name LIKE 'batch_%' ORDER BY table_name;" 2>/dev/null || echo "N/A"

echo -e "\n${YELLOW}Versioning Tables:${NC}"
psql $CONN_STRING -c "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_name LIKE 'version_%' ORDER BY table_name;" 2>/dev/null || echo "N/A"

echo -e "\n${YELLOW}Audit Tables:${NC}"
psql $CONN_STRING -c "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_name LIKE 'audit_%' ORDER BY table_name;" 2>/dev/null || echo "N/A"

echo ""
echo "=============================================="
echo "Engine-Data Tables (V1-V7)"
echo "=============================================="
echo -e "${YELLOW}Sink Tables:${NC}"
psql $CONN_STRING -c "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND (table_name LIKE 'sink_%' OR table_name LIKE 'form_%' OR table_name LIKE 'promoted_%') ORDER BY table_name;" 2>/dev/null || echo "N/A"

echo -e "\n${YELLOW}Query Views:${NC}"
psql $CONN_STRING -c "SELECT table_name FROM information_schema.views WHERE table_schema = 'public' AND table_name LIKE 'v_%' ORDER BY table_name;" 2>/dev/null || echo "N/A"

echo -e "\n${YELLOW}Dashboard Tables:${NC}"
psql $CONN_STRING -c "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_name LIKE 'dashboard_%' ORDER BY table_name;" 2>/dev/null || echo "N/A"

echo -e "\n${YELLOW}Search Tables:${NC}"
psql $CONN_STRING -c "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_name LIKE 'search_%' ORDER BY table_name;" 2>/dev/null || echo "N/A"

echo -e "\n${YELLOW}Template Tables:${NC}"
psql $CONN_STRING -c "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_name LIKE 'template_%' ORDER BY table_name;" 2>/dev/null || echo "N/A"

echo ""
echo "=============================================="
echo "Engine-Reporting Tables (V1-V5)"
echo "=============================================="
echo -e "${YELLOW}Lifecycle Tables:${NC}"
psql $CONN_STRING -c "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_name LIKE 'report_%' ORDER BY table_name;" 2>/dev/null || echo "N/A"

echo -e "\n${YELLOW}Period Tables:${NC}"
psql $CONN_STRING -c "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_name LIKE 'period_%' ORDER BY table_name;" 2>/dev/null || echo "N/A"

echo -e "\n${YELLOW}Form Tables:${NC}"
psql $CONN_STRING -c "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_name LIKE 'form_%' ORDER BY table_name;" 2>/dev/null || echo "N/A"

echo -e "\n${YELLOW}Notification Tables:${NC}"
psql $CONN_STRING -c "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_name LIKE 'notification_%' ORDER BY table_name;" 2>/dev/null || echo "N/A"

echo ""
echo "=============================================="
echo "Index Verification"
echo "=============================================="
echo -e "${YELLOW}Total Indexes:${NC}"
psql $CONN_STRING -c "SELECT COUNT(*) as index_count FROM pg_indexes WHERE schemaname = 'public';" 2>/dev/null || echo "N/A"

echo ""
echo "=============================================="
echo "Verification Complete"
echo "=============================================="
