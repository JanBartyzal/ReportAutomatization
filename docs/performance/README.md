# Performance Test Results

## Baseline

| Test | Date | VUs | Duration | p50 (ms) | p95 (ms) | p99 (ms) | Error Rate |
|------|------|-----|----------|----------|----------|----------|------------|
| upload-throughput | _TBD_ | 10 | 2m 45s | - | - | - | - |
| query-latency | _TBD_ | 20 | 3m 45s | - | - | - | - |
| dashboard-aggregation | _TBD_ | 15 | 2m 45s | - | - | - | - |

## SLA Thresholds

Values are defined in `tests/performance/config/thresholds.json`.

| Endpoint | p50 | p95 | p99 | Max Error Rate |
|----------|-----|-----|-----|----------------|
| Upload | 1 000 ms | 5 000 ms | 10 000 ms | 1 % |
| Query | 200 ms | 1 000 ms | 2 000 ms | 1 % |
| Dashboard | 500 ms | 3 000 ms | 5 000 ms | 1 % |

## Prerequisites

- [k6](https://k6.io/docs/get-started/installation/) must be installed.
- The target environment must be running and seeded with test data (report IDs, dashboard IDs).

## Running Tests

```bash
# From the repository root
cd tests/performance

# Run a single test
k6 run scripts/query-latency.js

# Smoke test (1 VU, 10 s) to verify connectivity
npm run test:smoke

# Run individual suites
npm run test:upload
npm run test:query
npm run test:dashboard

# Run all tests and collect JSON results
npm run test:all
```

### Overriding Configuration

Pass environment variables with the `-e` flag:

```bash
k6 run -e BASE_URL=https://staging.example.com \
       -e TEST_AUTH_TOKEN=eyJ... \
       -e TEST_REPORT_IDS=101,102,103 \
       scripts/query-latency.js
```

## Interpreting Results

k6 prints a summary table at the end of each run. Key columns:

- **http_req_duration** -- end-to-end request time. Compare p50/p95/p99 against the SLA thresholds above.
- **http_req_failed** -- ratio of non-2xx responses. Should stay below the max error rate.
- **Custom metrics** (e.g. `upload_duration`, `query_latency_p50`, `aggregation_duration`) track the same timings scoped to specific endpoint groups.

A test **passes** if all configured thresholds are met. k6 exits with code 99 when any threshold is breached.

## Comparing With Baseline

1. Run the full suite: `npm run test:all`.
2. Open the JSON files in `results/` (named `<test>_<timestamp>.json`).
3. Extract `metrics.http_req_duration` percentiles and compare them row-by-row with the baseline table above.
4. Update the baseline table when a new release is accepted.
