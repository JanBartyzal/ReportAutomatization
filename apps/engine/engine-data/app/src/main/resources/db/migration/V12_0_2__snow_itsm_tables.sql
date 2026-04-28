-- V12_0_2: ServiceNow ITSM dedicated typed tables
-- Replaces generic JSONB dump approach with proper column types, indexes and RLS.
-- These tables are populated by ItsmSyncService in engine-integrations.

-- ─── Incidents ────────────────────────────────────────────────────────────────
CREATE TABLE snow_incidents (
    id                       UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    sys_id                   VARCHAR(32)  NOT NULL,
    number                   VARCHAR(20),
    org_id                   UUID         NOT NULL,
    connection_id            UUID         NOT NULL,
    resolver_group_id        UUID         NOT NULL,

    -- Descriptive fields
    short_description        TEXT,
    state                    VARCHAR(50),
    priority                 VARCHAR(10),
    urgency                  VARCHAR(10),
    impact                   VARCHAR(10),
    category                 VARCHAR(100),
    subcategory              VARCHAR(100),

    -- Assignment
    assignment_group_sys_id  VARCHAR(32),
    assignment_group_name    VARCHAR(255),
    assigned_to              VARCHAR(255),
    opened_by                VARCHAR(255),

    -- Timestamps
    opened_at                TIMESTAMPTZ,
    resolved_at              TIMESTAMPTZ,
    closed_at                TIMESTAMPTZ,
    sla_due                  TIMESTAMPTZ,

    -- SLA & derived metrics
    is_sla_breached          BOOLEAN      NOT NULL DEFAULT FALSE,
    resolution_time_hours    NUMERIC(10, 2),
    age_days                 NUMERIC(10, 2),
    close_code               VARCHAR(100),

    -- Full record for forward-compatibility
    raw_fields               JSONB,

    synced_at                TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_snow_incident_org_sys UNIQUE (org_id, sys_id)
);

CREATE INDEX idx_snow_inc_org_id         ON snow_incidents (org_id);
CREATE INDEX idx_snow_inc_connection     ON snow_incidents (connection_id);
CREATE INDEX idx_snow_inc_resolver_group ON snow_incidents (resolver_group_id);
CREATE INDEX idx_snow_inc_state          ON snow_incidents (state);
CREATE INDEX idx_snow_inc_priority       ON snow_incidents (priority);
CREATE INDEX idx_snow_inc_opened_at      ON snow_incidents (opened_at);
CREATE INDEX idx_snow_inc_sla_breached   ON snow_incidents (is_sla_breached);

ALTER TABLE snow_incidents ENABLE ROW LEVEL SECURITY;

CREATE POLICY rls_snow_incidents_select ON snow_incidents
    FOR SELECT USING (org_id::text = current_setting('app.current_org_id', TRUE));
CREATE POLICY rls_snow_incidents_insert ON snow_incidents
    FOR INSERT WITH CHECK (org_id::text = current_setting('app.current_org_id', TRUE));
CREATE POLICY rls_snow_incidents_update ON snow_incidents
    FOR UPDATE USING (org_id::text = current_setting('app.current_org_id', TRUE));
CREATE POLICY rls_snow_incidents_delete ON snow_incidents
    FOR DELETE USING (org_id::text = current_setting('app.current_org_id', TRUE));

GRANT SELECT, INSERT, UPDATE, DELETE ON snow_incidents TO engine_data_user;

-- ─── Requests ─────────────────────────────────────────────────────────────────
CREATE TABLE snow_requests (
    id                       UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    sys_id                   VARCHAR(32)  NOT NULL,
    number                   VARCHAR(20),
    org_id                   UUID         NOT NULL,
    connection_id            UUID         NOT NULL,
    resolver_group_id        UUID         NOT NULL,

    short_description        TEXT,
    state                    VARCHAR(50),
    approval                 VARCHAR(50),
    stage                    VARCHAR(100),

    assignment_group_sys_id  VARCHAR(32),
    assignment_group_name    VARCHAR(255),
    requested_for            VARCHAR(255),
    requested_by             VARCHAR(255),

    opened_at                TIMESTAMPTZ,
    due_date                 TIMESTAMPTZ,
    closed_at                TIMESTAMPTZ,

    -- Derived
    age_days                 NUMERIC(10, 2),

    raw_fields               JSONB,

    synced_at                TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_snow_request_org_sys UNIQUE (org_id, sys_id)
);

CREATE INDEX idx_snow_req_org_id         ON snow_requests (org_id);
CREATE INDEX idx_snow_req_connection     ON snow_requests (connection_id);
CREATE INDEX idx_snow_req_resolver_group ON snow_requests (resolver_group_id);
CREATE INDEX idx_snow_req_state          ON snow_requests (state);
CREATE INDEX idx_snow_req_opened_at      ON snow_requests (opened_at);

ALTER TABLE snow_requests ENABLE ROW LEVEL SECURITY;

CREATE POLICY rls_snow_requests_select ON snow_requests
    FOR SELECT USING (org_id::text = current_setting('app.current_org_id', TRUE));
CREATE POLICY rls_snow_requests_insert ON snow_requests
    FOR INSERT WITH CHECK (org_id::text = current_setting('app.current_org_id', TRUE));
CREATE POLICY rls_snow_requests_update ON snow_requests
    FOR UPDATE USING (org_id::text = current_setting('app.current_org_id', TRUE));
CREATE POLICY rls_snow_requests_delete ON snow_requests
    FOR DELETE USING (org_id::text = current_setting('app.current_org_id', TRUE));

GRANT SELECT, INSERT, UPDATE, DELETE ON snow_requests TO engine_data_user;

-- ─── Seed ITSM Named Queries ──────────────────────────────────────────────────
-- System named queries for ITSM templates (visible to all orgs)
INSERT INTO named_queries (id, org_id, name, description, sql_query, params_schema, data_source_hint, is_system, created_by)
VALUES
(
    gen_random_uuid(), NULL,
    'itsm_incident_open_count',
    'Count of open incidents for a resolver group',
    'SELECT COUNT(*) AS open_count FROM snow_incidents WHERE resolver_group_id = :groupId AND state NOT IN (''resolved'',''closed'',''cancelled'')',
    '{"properties":{"groupId":{"type":"string","description":"Resolver group UUID"}},"required":["groupId"]}',
    'SNOW_ITSM', TRUE, 'system'
),
(
    gen_random_uuid(), NULL,
    'itsm_incident_priority_distribution',
    'Count of open incidents grouped by priority',
    'SELECT priority, COUNT(*) AS cnt FROM snow_incidents WHERE resolver_group_id = :groupId AND state NOT IN (''resolved'',''closed'',''cancelled'') GROUP BY priority ORDER BY priority',
    '{"properties":{"groupId":{"type":"string"}},"required":["groupId"]}',
    'SNOW_ITSM', TRUE, 'system'
),
(
    gen_random_uuid(), NULL,
    'itsm_incident_sla_breach_rate',
    'SLA breach percentage for a resolver group',
    'SELECT COUNT(*) FILTER (WHERE is_sla_breached) AS breached, COUNT(*) AS total, ROUND(100.0 * COUNT(*) FILTER (WHERE is_sla_breached) / NULLIF(COUNT(*),0), 2) AS breach_pct FROM snow_incidents WHERE resolver_group_id = :groupId',
    '{"properties":{"groupId":{"type":"string"}},"required":["groupId"]}',
    'SNOW_ITSM', TRUE, 'system'
),
(
    gen_random_uuid(), NULL,
    'itsm_incident_top10_open',
    'Top 10 oldest open incidents for a resolver group',
    'SELECT number, short_description, priority, state, opened_at, age_days, assigned_to FROM snow_incidents WHERE resolver_group_id = :groupId AND state NOT IN (''resolved'',''closed'',''cancelled'') ORDER BY age_days DESC NULLS LAST LIMIT 10',
    '{"properties":{"groupId":{"type":"string"}},"required":["groupId"]}',
    'SNOW_ITSM', TRUE, 'system'
),
(
    gen_random_uuid(), NULL,
    'itsm_incident_trend_7d',
    'Daily incident open/resolved counts for the last 7 days',
    'SELECT DATE_TRUNC(''day'', opened_at) AS day, COUNT(*) AS opened FROM snow_incidents WHERE resolver_group_id = :groupId AND opened_at >= NOW() - INTERVAL ''7 days'' GROUP BY day ORDER BY day',
    '{"properties":{"groupId":{"type":"string"}},"required":["groupId"]}',
    'SNOW_ITSM', TRUE, 'system'
);
