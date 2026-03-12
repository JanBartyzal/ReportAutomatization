import { cleanTestData, closePool } from './helpers/db.js';

/**
 * Vitest globalTeardown hook.
 * Cleans up all test data and closes the database connection pool.
 */
export default async function teardown(): Promise<void> {
  console.log('[teardown] Cleaning up test data...');

  try {
    await cleanTestData();
  } catch (err) {
    console.warn('[teardown] Error during cleanup:', (err as Error).message);
  }

  await closePool();
  console.log('[teardown] Global teardown complete.');
}
