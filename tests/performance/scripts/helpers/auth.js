/**
 * Shared authentication helpers for k6 performance tests.
 *
 * The token can be supplied via the TEST_AUTH_TOKEN environment variable:
 *   k6 run -e TEST_AUTH_TOKEN=<jwt> scripts/query-latency.js
 *
 * When no token is provided a static placeholder is used. This is fine for
 * local development where the gateway may skip validation, but real
 * environments should always supply a valid JWT.
 */

const DEFAULT_TOKEN = 'perf-test-static-token';

/**
 * Return the Bearer token to use for authenticated requests.
 * @returns {string}
 */
export function getTestToken() {
  return __ENV.TEST_AUTH_TOKEN || DEFAULT_TOKEN;
}

/**
 * Return a headers object suitable for passing to k6 http methods.
 * Includes Authorization and a JSON Accept header.
 * @returns {Object}
 */
export function getAuthHeaders() {
  return {
    Authorization: `Bearer ${getTestToken()}`,
    Accept: 'application/json',
  };
}
