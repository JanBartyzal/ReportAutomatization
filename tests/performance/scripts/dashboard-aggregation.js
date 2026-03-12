import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { getAuthHeaders } from './helpers/auth.js';
import { BASE_URL, TEST_DASHBOARD_IDS } from './helpers/config.js';

// ---------------------------------------------------------------------------
// Custom metrics
// ---------------------------------------------------------------------------
const aggregationDuration = new Trend('aggregation_duration', true);
const dashboardSuccessRate = new Rate('dashboard_success_rate');

// ---------------------------------------------------------------------------
// k6 options
// ---------------------------------------------------------------------------
export const options = {
  stages: [
    { duration: '30s', target: 15 },  // ramp up to 15 VUs
    { duration: '2m', target: 15 },   // sustain 15 VUs for 2 minutes
    { duration: '15s', target: 0 },   // ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<3000'],
    http_req_failed: ['rate<0.1'],
    aggregation_duration: ['p(50)<500', 'p(95)<3000', 'p(99)<5000'],
    dashboard_success_rate: ['rate>0.99'],
  },
  tags: {
    test_name: 'dashboard-aggregation',
  },
};

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/** Pick a random element from an array. */
function randomItem(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

// ---------------------------------------------------------------------------
// Default function (VU code)
// ---------------------------------------------------------------------------
export default function () {
  const headers = getAuthHeaders();

  // --- List dashboards ---
  group('list dashboards', function () {
    const url = `${BASE_URL}/api/dashboards`;

    const res = http.get(url, {
      headers,
      tags: { endpoint: 'dashboard_list' },
    });

    aggregationDuration.add(res.timings.duration);

    const ok = check(res, {
      'list status is 200': (r) => r.status === 200,
      'list returns array': (r) => {
        try {
          const body = JSON.parse(r.body);
          return Array.isArray(body) || Array.isArray(body.data);
        } catch (_) {
          return false;
        }
      },
    });

    dashboardSuccessRate.add(ok ? 1 : 0);
  });

  sleep(0.5);

  // --- Dashboard detail (with aggregated data) ---
  group('dashboard detail', function () {
    const dashboardId = randomItem(TEST_DASHBOARD_IDS);
    const url = `${BASE_URL}/api/dashboards/${dashboardId}`;

    const res = http.get(url, {
      headers,
      tags: { endpoint: 'dashboard_detail' },
    });

    aggregationDuration.add(res.timings.duration);

    const ok = check(res, {
      'detail status is 200': (r) => r.status === 200,
      'detail has id': (r) => {
        try {
          const body = JSON.parse(r.body);
          return body.id !== undefined;
        } catch (_) {
          return false;
        }
      },
      'detail has widgets or data': (r) => {
        try {
          const body = JSON.parse(r.body);
          return body.widgets !== undefined || body.data !== undefined;
        } catch (_) {
          return false;
        }
      },
    });

    dashboardSuccessRate.add(ok ? 1 : 0);
  });

  // Short pause between iterations
  sleep(Math.random() * 1 + 0.5);
}
