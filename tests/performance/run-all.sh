#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# run-all.sh - Execute all k6 performance tests sequentially and collect
#              JSON results into the results/ directory.
# ---------------------------------------------------------------------------
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RESULTS_DIR="${SCRIPT_DIR}/results"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Ensure results directory exists
mkdir -p "${RESULTS_DIR}"

echo "============================================="
echo "  k6 Performance Test Suite"
echo "  $(date)"
echo "============================================="
echo ""

PASS=0
FAIL=0

run_test() {
  local name="$1"
  local script="$2"
  local output="${RESULTS_DIR}/${name}_${TIMESTAMP}.json"

  echo "---------------------------------------------"
  echo "  Running: ${name}"
  echo "  Script:  ${script}"
  echo "  Output:  ${output}"
  echo "---------------------------------------------"

  if k6 run --out json="${output}" "${SCRIPT_DIR}/scripts/${script}"; then
    echo "[PASS] ${name}"
    PASS=$((PASS + 1))
  else
    echo "[FAIL] ${name}"
    FAIL=$((FAIL + 1))
  fi
  echo ""
}

# Run each test
run_test "upload-throughput"        "upload-throughput.js"
run_test "query-latency"            "query-latency.js"
run_test "dashboard-aggregation"    "dashboard-aggregation.js"

# Print summary
echo "============================================="
echo "  Summary"
echo "============================================="
echo "  Passed: ${PASS}"
echo "  Failed: ${FAIL}"
echo "  Results saved in: ${RESULTS_DIR}"
echo "============================================="

# Exit with failure if any test failed
if [ "${FAIL}" -gt 0 ]; then
  exit 1
fi
