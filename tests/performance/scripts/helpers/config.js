/**
 * Shared configuration for k6 performance tests.
 *
 * All values can be overridden via environment variables passed to k6
 * using the -e flag, e.g.:
 *   k6 run -e BASE_URL=https://staging.example.com scripts/query-latency.js
 */

export const BASE_URL = __ENV.BASE_URL || 'http://localhost';

/** Pre-seeded report IDs used by query tests. */
export const TEST_REPORT_IDS = (__ENV.TEST_REPORT_IDS || '1,2,3,4,5').split(',');

/** Pre-seeded dashboard IDs used by dashboard tests. */
export const TEST_DASHBOARD_IDS = (__ENV.TEST_DASHBOARD_IDS || '1,2,3').split(',');

/** Default page size for list endpoints. */
export const DEFAULT_PAGE_SIZE = parseInt(__ENV.DEFAULT_PAGE_SIZE || '20', 10);
