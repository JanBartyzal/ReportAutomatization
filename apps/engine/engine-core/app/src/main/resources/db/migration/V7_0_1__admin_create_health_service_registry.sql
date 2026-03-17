-- Health Service Registry: stores service URLs for the health dashboard.
-- Allows runtime management of monitored services without application restart.

CREATE TABLE IF NOT EXISTS health_service_registry (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    service_id      VARCHAR(100)    NOT NULL UNIQUE,
    display_name    VARCHAR(200)    NOT NULL,
    health_url      VARCHAR(500)    NOT NULL,
    enabled         BOOLEAN         NOT NULL DEFAULT TRUE,
    sort_order      INTEGER         NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

-- Seed default services (P8 consolidated architecture)
INSERT INTO health_service_registry (service_id, display_name, health_url, sort_order) VALUES
    ('engine-core',          'Engine Core (Auth/Admin)',  'http://localhost:8081/actuator/health', 1),
    ('ms-gw',                'API Gateway (Nginx)',       'http://ms-gw:80/health',               2),
    ('engine-ingestor',      'Engine Ingestor',           'http://ms-ing:8080/actuator/health',   3),
    ('engine-orchestrator',  'Engine Orchestrator',       'http://engine-orchestrator:8080/actuator/health', 4),
    ('engine-data',          'Engine Data',               'http://engine-data:8100/actuator/health',         5),
    ('engine-reporting',     'Engine Reporting',          'http://engine-reporting:8105/actuator/health',    6),
    ('engine-integrations',  'Engine Integrations',       'http://engine-integrations:8080/actuator/health', 7),
    ('processor-atomizers',  'Processor Atomizers',       'http://processor-atomizers:8088/actuator/health', 8),
    ('processor-generators', 'Processor Generators',      'http://processor-generators:8111/actuator/health',9)
ON CONFLICT (service_id) DO NOTHING;
