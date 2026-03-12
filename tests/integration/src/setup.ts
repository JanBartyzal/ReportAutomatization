import axios from 'axios';
import { cleanTestData, seedTestData, closePool } from './helpers/db.js';

const GATEWAY_URL = process.env.GATEWAY_URL ?? 'http://localhost';
const AUTH_HEALTH = process.env.AUTH_HEALTH_URL ?? 'http://localhost:8081/actuator/health';
const ING_HEALTH = process.env.ING_HEALTH_URL ?? 'http://localhost:8082/actuator/health';

const MAX_RETRIES = 30;
const RETRY_INTERVAL_MS = 2_000;

/**
 * Poll a URL until it returns HTTP 200, or give up after MAX_RETRIES.
 */
async function waitForService(name: string, url: string): Promise<void> {
  for (let attempt = 1; attempt <= MAX_RETRIES; attempt++) {
    try {
      const res = await axios.get(url, { timeout: 3_000, validateStatus: () => true });
      if (res.status >= 200 && res.status < 300) {
        console.log(`[setup] ${name} is healthy (attempt ${attempt})`);
        return;
      }
      console.log(`[setup] ${name} returned ${res.status}, retrying (${attempt}/${MAX_RETRIES})...`);
    } catch {
      console.log(`[setup] ${name} not reachable, retrying (${attempt}/${MAX_RETRIES})...`);
    }
    await sleep(RETRY_INTERVAL_MS);
  }
  throw new Error(`[setup] ${name} at ${url} did not become healthy within ${MAX_RETRIES * RETRY_INTERVAL_MS / 1000}s`);
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

/**
 * Vitest globalSetup function.
 * Runs once before the entire test suite.
 */
export async function setup(): Promise<void> {
  console.log('[setup] Waiting for services to become healthy...');

  // Check all three services in parallel
  await Promise.all([
    waitForService('Nginx Gateway', `${GATEWAY_URL}/health`),
    waitForService('MS-AUTH', AUTH_HEALTH),
    waitForService('MS-ING', ING_HEALTH),
  ]);

  console.log('[setup] All services healthy. Preparing test data...');

  // Clean stale test data from previous runs, then seed fresh data
  await cleanTestData();
  await seedTestData();

  // Close the pool so it can be re-opened by tests in their own process
  await closePool();

  console.log('[setup] Global setup complete.');
}

/**
 * Vitest globalTeardown function.
 * Runs once after the entire test suite.
 */
export async function teardown(): Promise<void> {
  console.log('[teardown] Cleaning up test data...');
  await cleanTestData();
  await closePool();
  console.log('[teardown] Done.');
}
