-- Allow init script (infra/init/setup.py) to update health service URLs.
-- The original V7_0_1 migration seeded with ON CONFLICT DO NOTHING,
-- meaning URLs could never be updated from init.json.
-- This migration clears the hardcoded seed data so that the init script
-- becomes the single source of truth for health service URLs.

DELETE FROM health_service_registry
WHERE service_id IN (
    'engine-core', 'ms-gw', 'engine-ingestor', 'engine-orchestrator',
    'engine-data', 'engine-reporting', 'engine-integrations',
    'processor-atomizers', 'processor-generators'
);
