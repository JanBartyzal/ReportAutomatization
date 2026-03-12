import pg from 'pg';

const { Pool } = pg;

let pool: pg.Pool | null = null;

const DB_CONFIG = {
  host: process.env.DB_HOST ?? 'localhost',
  port: parseInt(process.env.DB_PORT ?? '5432', 10),
  user: process.env.DB_USER ?? 'postgres',
  password: process.env.DB_PASSWORD ?? 'postgres',
  database: process.env.DB_NAME ?? 'reportplatform',
  max: 5,
  idleTimeoutMillis: 10_000,
  connectionTimeoutMillis: 5_000,
};

/**
 * Return a shared pg Pool. Created lazily on first call.
 */
export function getPool(): pg.Pool {
  if (!pool) {
    pool = new Pool(DB_CONFIG);
  }
  return pool;
}

/**
 * Close the database pool. Safe to call even if pool was never opened.
 */
export async function closePool(): Promise<void> {
  if (pool) {
    await pool.end();
    pool = null;
  }
}

// ---------------------------------------------------------------------------
// Test data identifiers - all prefixed with "test-" for easy cleanup
// ---------------------------------------------------------------------------

export const TEST_ORG_HOLDING = {
  id: 'test-org-holding-001',
  name: 'Test Holding Corp',
  type: 'HOLDING',
  parent_id: null as string | null,
};

export const TEST_ORG_COMPANY_A = {
  id: 'test-org-company-a',
  name: 'Test Company A',
  type: 'COMPANY',
  parent_id: 'test-org-holding-001',
};

export const TEST_ORG_COMPANY_B = {
  id: 'test-org-company-b',
  name: 'Test Company B',
  type: 'COMPANY',
  parent_id: 'test-org-holding-001',
};

/**
 * Remove all test data from the database.
 * Uses the "test-" prefix convention to identify rows created by tests.
 */
export async function cleanTestData(): Promise<void> {
  const db = getPool();

  // Order matters: delete dependent rows first (FK constraints).
  // Wrap in a transaction so partial failures don't leave dangling data.
  const client = await db.connect();
  try {
    await client.query('BEGIN');

    // Dashboard-related
    await client.query(`DELETE FROM dashboards WHERE id LIKE 'test-%' OR created_by LIKE 'test-%'`);

    // Report lifecycle
    await client.query(`DELETE FROM report_status_history WHERE report_id LIKE 'test-%'`);
    await client.query(`DELETE FROM reports WHERE id LIKE 'test-%' OR org_id LIKE 'test-%'`);

    // Parsed data
    await client.query(`DELETE FROM parsed_tables WHERE file_id LIKE 'test-%'`);
    await client.query(`DELETE FROM parsed_documents WHERE file_id LIKE 'test-%'`);

    // Files
    await client.query(`DELETE FROM files WHERE file_id LIKE 'test-%' OR org_id LIKE 'test-%'`);

    // Organizations & users (last due to FKs)
    await client.query(`DELETE FROM users WHERE user_id LIKE 'test-%'`);
    await client.query(`DELETE FROM organizations WHERE id LIKE 'test-%'`);

    await client.query('COMMIT');
  } catch (err) {
    await client.query('ROLLBACK');
    // Tables may not exist yet on first run; that is acceptable.
    console.warn('[db] cleanTestData: some tables may not exist yet, skipping.', (err as Error).message);
  } finally {
    client.release();
  }
}

/**
 * Seed the minimum test data required for integration tests.
 * Idempotent: uses ON CONFLICT DO NOTHING.
 */
export async function seedTestData(): Promise<void> {
  const db = getPool();
  const client = await db.connect();

  try {
    await client.query('BEGIN');

    // Organizations
    for (const org of [TEST_ORG_HOLDING, TEST_ORG_COMPANY_A, TEST_ORG_COMPANY_B]) {
      await client.query(
        `INSERT INTO organizations (id, name, type, parent_id)
         VALUES ($1, $2, $3, $4)
         ON CONFLICT (id) DO NOTHING`,
        [org.id, org.name, org.type, org.parent_id],
      );
    }

    // Test users
    const users = [
      {
        user_id: 'test-admin-001',
        email: 'admin@example.com',
        display_name: 'Test Admin',
        org_id: TEST_ORG_HOLDING.id,
        roles: ['HOLDING_ADMIN', 'ADMIN'],
      },
      {
        user_id: 'test-user-company-a',
        email: 'user-a@example.com',
        display_name: 'User Company A',
        org_id: TEST_ORG_COMPANY_A.id,
        roles: ['EDITOR'],
      },
      {
        user_id: 'test-user-company-b',
        email: 'user-b@example.com',
        display_name: 'User Company B',
        org_id: TEST_ORG_COMPANY_B.id,
        roles: ['EDITOR'],
      },
    ];

    for (const u of users) {
      await client.query(
        `INSERT INTO users (user_id, email, display_name, org_id, roles)
         VALUES ($1, $2, $3, $4, $5)
         ON CONFLICT (user_id) DO NOTHING`,
        [u.user_id, u.email, u.display_name, u.org_id, JSON.stringify(u.roles)],
      );
    }

    await client.query('COMMIT');
  } catch (err) {
    await client.query('ROLLBACK');
    console.warn('[db] seedTestData: some tables may not exist yet, skipping.', (err as Error).message);
  } finally {
    client.release();
  }
}
