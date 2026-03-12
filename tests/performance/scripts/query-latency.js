import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { getAuthHeaders } from './helpers/auth.js';
import { BASE_URL, TEST_REPORT_IDS, DEFAULT_PAGE_SIZE } from './helpers/config.js';

// ---------------------------------------------------------------------------
// Custom metrics
// ---------------------------------------------------------------------------
const queryLatencyP50 = new Trend('query_latency_p50', true);
const queryLatencyP95 = new Trend('query_latency_p95', true);
const querySuccessRate = new Rate('query_success_rate');

// ---------------------------------------------------------------------------
// k6 options
// ---------------------------------------------------------------------------
export const options = {
  stages: [
    { duration: '30s', target: 20 },  // ramp up to 20 VUs
    { duration: '3m', target: 20 },   // sustain 20 VUs for 3 minutes
    { duration: '15s', target: 0 },   // ramp down
  ],
  thresholds: {
    http_req_duration: ['p(50)<200', 'p(95)<1000', 'p(99)<2000'],
    http_req_failed: ['rate<0.1'],
    query_latency_p50: ['p(50)<200'],
    query_latency_p95: ['p(95)<1000'],
    query_success_rate: ['rate>0.99'],
  },
  tags: {
    test_name: 'query-latency',
  },
};

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/** Pick a random element from an array. */
function randomItem(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

/** Build randomised query parameters. */
function randomQueryParams() {
  const params = new URLSearchParams();
  params.append('page', String(Math.floor(Math.random() * 5) + 1));
  params.append('pageSize', String(DEFAULT_PAGE_SIZE));

  // Randomly add optional filters
  if (Math.random() > 0.5) {
    params.append('status', randomItem(['PARSED', 'PENDING', 'ERROR']));
  }
  if (Math.random() > 0.5) {
    params.append('sortBy', randomItem(['createdAt', 'updatedAt', 'name']));
    params.append('sortOrder', randomItem(['asc', 'desc']));
  }

  return params.toString();
}

// ---------------------------------------------------------------------------
// Default function (VU code)
// ---------------------------------------------------------------------------
export default function () {
  const headers = getAuthHeaders();

  // --- List reports with query params ---
  group('list reports', function () {
    const qs = randomQueryParams();
    const url = `${BASE_URL}/api/query/reports?${qs}`;

    const res = http.get(url, {
      headers,
      tags: { endpoint: 'query_list' },
    });

    queryLatencyP50.add(res.timings.duration);
    queryLatencyP95.add(res.timings.duration);

    const ok = check(res, {
      'list status is 200': (r) => r.status === 200,
      'list returns JSON': (r) => {
        try {
          JSON.parse(r.body);
          return true;
        } catch (_) {
          return false;
        }
      },
    });

    querySuccessRate.add(ok ? 1 : 0);
  });

  sleep(0.5);

  // --- Get single report detail ---
  group('report detail', function () {
    const reportId = randomItem(TEST_REPORT_IDS);
    const url = `${BASE_URL}/api/query/reports/${reportId}`;

    const res = http.get(url, {
      headers,
      tags: { endpoint: 'query_detail' },
    });

    queryLatencyP50.add(res.timings.duration);
    queryLatencyP95.add(res.timings.duration);

    const ok = check(res, {
      'detail status is 200': (r) => r.status === 200,
      'detail has id field': (r) => {
        try {
          const body = JSON.parse(r.body);
          return body.id !== undefined;
        } catch (_) {
          return false;
        }
      },
    });

    querySuccessRate.add(ok ? 1 : 0);
  });

  // Short pause between iterations
  sleep(Math.random() * 1 + 0.5);
}
