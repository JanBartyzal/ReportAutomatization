import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { getAuthHeaders } from './helpers/auth.js';
import { BASE_URL } from './helpers/config.js';

// ---------------------------------------------------------------------------
// Custom metrics
// ---------------------------------------------------------------------------
const uploadDuration = new Trend('upload_duration', true);
const uploadSuccessRate = new Rate('upload_success_rate');

// ---------------------------------------------------------------------------
// k6 options
// ---------------------------------------------------------------------------
export const options = {
  stages: [
    { duration: '30s', target: 10 },  // ramp up to 10 VUs
    { duration: '2m', target: 10 },   // sustain 10 VUs for 2 minutes
    { duration: '15s', target: 0 },   // ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<5000'],
    http_req_failed: ['rate<0.1'],
    upload_duration: ['p(50)<1000', 'p(95)<5000', 'p(99)<10000'],
    upload_success_rate: ['rate>0.99'],
  },
  tags: {
    test_name: 'upload-throughput',
  },
};

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/**
 * Generate a small mock binary payload (10 KB).
 * k6 does not have a Node Buffer, so we build a string-based body.
 */
function createTestFilePayload() {
  const size = 10 * 1024; // 10 KB
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
  let data = '';
  for (let i = 0; i < size; i++) {
    data += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return {
    data: http.file(data, 'test-report.pdf', 'application/pdf'),
  };
}

// ---------------------------------------------------------------------------
// Default function (VU code)
// ---------------------------------------------------------------------------
export default function () {
  const url = `${BASE_URL}/api/files/upload`;
  const headers = getAuthHeaders();

  const filePayload = createTestFilePayload();

  const res = http.post(url, filePayload, {
    headers: {
      Authorization: headers.Authorization,
    },
    tags: { endpoint: 'upload' },
  });

  // Record custom metrics
  uploadDuration.add(res.timings.duration);

  const success = check(res, {
    'upload status is 200 or 201': (r) => r.status === 200 || r.status === 201,
    'response has body': (r) => r.body && r.body.length > 0,
  });

  uploadSuccessRate.add(success ? 1 : 0);

  // Pace requests – one upload per ~2 seconds per VU
  sleep(Math.random() * 2 + 1);
}
