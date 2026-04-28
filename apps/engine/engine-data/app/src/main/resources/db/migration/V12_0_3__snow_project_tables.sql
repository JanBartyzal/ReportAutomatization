-- V12_0_3: ServiceNow Project Data – typed storage tables
-- Stores projects, tasks, and budget lines synced from ServiceNow pm_project tables.
-- Derived KPIs (RAG status, budget utilization, schedule variance) are pre-calculated.

-- ─── Projects ──────────────────────────────────────────────────────────────────

CREATE TABLE snow_projects (
    id                      UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    org_id                  UUID         NOT NULL,
    resolver_connection_id  UUID         NOT NULL,
    -- ServiceNow identifiers
    sys_id                  VARCHAR(32)  NOT NULL,
    number                  VARCHAR(50),
    short_description       TEXT,
    -- Status & phase
    status                  VARCHAR(50),
    phase                   VARCHAR(50),
    -- People
    manager_sys_id          VARCHAR(32),
    manager_name            VARCHAR(255),
    manager_email           VARCHAR(255),
    department              VARCHAR(255),
    -- Dates
    planned_start_date      DATE,
    planned_end_date        DATE,
    actual_start_date       DATE,
    projected_end_date      DATE,
    -- Progress
    percent_complete        NUMERIC(5,2),
    -- Budget (values in budget_currency from sync config)
    total_budget            NUMERIC(18,2),
    actual_cost             NUMERIC(18,2),
    projected_cost          NUMERIC(18,2),
    -- Derived KPIs
    budget_utilization_pct  NUMERIC(7,2),
    schedule_variance_days  INTEGER,
    milestone_completion_rate NUMERIC(5,2),
    cost_forecast_accuracy  NUMERIC(7,4),
    rag_status              VARCHAR(10),     -- GREEN | AMBER | RED
    -- Raw payload
    raw_fields              JSONB,
    -- Sync metadata
    synced_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    sn_updated_at           TIMESTAMPTZ,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_snow_project_conn_sysid UNIQUE (resolver_connection_id, sys_id)
);

CREATE INDEX idx_snow_projects_org        ON snow_projects (org_id);
CREATE INDEX idx_snow_projects_connection ON snow_projects (resolver_connection_id);
CREATE INDEX idx_snow_projects_status     ON snow_projects (status);
CREATE INDEX idx_snow_projects_rag        ON snow_projects (rag_status);
CREATE INDEX idx_snow_projects_manager    ON snow_projects (manager_email);
CREATE INDEX idx_snow_projects_synced     ON snow_projects (synced_at);

ALTER TABLE snow_projects ENABLE ROW LEVEL SECURITY;

CREATE POLICY rls_snow_proj_select ON snow_projects
    FOR SELECT USING (org_id::text = current_setting('app.current_org_id', TRUE));
CREATE POLICY rls_snow_proj_insert ON snow_projects
    FOR INSERT WITH CHECK (org_id::text = current_setting('app.current_org_id', TRUE));
CREATE POLICY rls_snow_proj_update ON snow_projects
    FOR UPDATE USING (org_id::text = current_setting('app.current_org_id', TRUE));
CREATE POLICY rls_snow_proj_delete ON snow_projects
    FOR DELETE USING (org_id::text = current_setting('app.current_org_id', TRUE));

GRANT SELECT, INSERT, UPDATE, DELETE ON snow_projects TO engine_data_user;

-- ─── Project Tasks ─────────────────────────────────────────────────────────────

CREATE TABLE snow_project_tasks (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    org_id              UUID         NOT NULL,
    project_id          UUID         NOT NULL REFERENCES snow_projects(id) ON DELETE CASCADE,
    -- ServiceNow identifiers
    sys_id              VARCHAR(32)  NOT NULL,
    number              VARCHAR(50),
    short_description   TEXT,
    -- Hierarchy
    parent_sys_id       VARCHAR(32),
    -- State
    state               VARCHAR(50),
    is_milestone        BOOLEAN      NOT NULL DEFAULT FALSE,
    -- People & dates
    assigned_to_name    VARCHAR(255),
    due_date            DATE,
    completed_at        TIMESTAMPTZ,
    -- Raw payload
    raw_fields          JSONB,
    synced_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_snow_task_proj_sysid UNIQUE (project_id, sys_id)
);

CREATE INDEX idx_snow_tasks_project     ON snow_project_tasks (project_id);
CREATE INDEX idx_snow_tasks_org         ON snow_project_tasks (org_id);
CREATE INDEX idx_snow_tasks_state       ON snow_project_tasks (state);
CREATE INDEX idx_snow_tasks_milestone   ON snow_project_tasks (is_milestone);

ALTER TABLE snow_project_tasks ENABLE ROW LEVEL SECURITY;

CREATE POLICY rls_snow_task_select ON snow_project_tasks
    FOR SELECT USING (org_id::text = current_setting('app.current_org_id', TRUE));
CREATE POLICY rls_snow_task_insert ON snow_project_tasks
    FOR INSERT WITH CHECK (org_id::text = current_setting('app.current_org_id', TRUE));
CREATE POLICY rls_snow_task_update ON snow_project_tasks
    FOR UPDATE USING (org_id::text = current_setting('app.current_org_id', TRUE));
CREATE POLICY rls_snow_task_delete ON snow_project_tasks
    FOR DELETE USING (org_id::text = current_setting('app.current_org_id', TRUE));

GRANT SELECT, INSERT, UPDATE, DELETE ON snow_project_tasks TO engine_data_user;

-- ─── Project Budget Lines ──────────────────────────────────────────────────────

CREATE TABLE snow_project_budgets (
    id              UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    org_id          UUID         NOT NULL,
    project_id      UUID         NOT NULL REFERENCES snow_projects(id) ON DELETE CASCADE,
    -- ServiceNow identifiers
    sys_id          VARCHAR(32)  NOT NULL,
    category        VARCHAR(255),
    fiscal_year     VARCHAR(10),
    planned_amount  NUMERIC(18,2),
    actual_amount   NUMERIC(18,2),
    -- Raw payload
    raw_fields      JSONB,
    synced_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_snow_budget_proj_sysid UNIQUE (project_id, sys_id)
);

CREATE INDEX idx_snow_budgets_project ON snow_project_budgets (project_id);
CREATE INDEX idx_snow_budgets_org     ON snow_project_budgets (org_id);

ALTER TABLE snow_project_budgets ENABLE ROW LEVEL SECURITY;

CREATE POLICY rls_snow_budget_select ON snow_project_budgets
    FOR SELECT USING (org_id::text = current_setting('app.current_org_id', TRUE));
CREATE POLICY rls_snow_budget_insert ON snow_project_budgets
    FOR INSERT WITH CHECK (org_id::text = current_setting('app.current_org_id', TRUE));
CREATE POLICY rls_snow_budget_update ON snow_project_budgets
    FOR UPDATE USING (org_id::text = current_setting('app.current_org_id', TRUE));
CREATE POLICY rls_snow_budget_delete ON snow_project_budgets
    FOR DELETE USING (org_id::text = current_setting('app.current_org_id', TRUE));

GRANT SELECT, INSERT, UPDATE, DELETE ON snow_project_budgets TO engine_data_user;

-- ─── Seed: Named Queries for Project Reports ──────────────────────────────────

INSERT INTO named_queries (id, org_id, name, description, data_source_type, sql_query, params_schema, is_system, created_by)
VALUES
(
    gen_random_uuid(),
    NULL,
    'Project RAG Dashboard',
    'Projects grouped by RAG status with key KPIs. Filter by connection and optional status.',
    'SNOW_PROJECTS',
    'SELECT p.number, p.short_description, p.status, p.phase, p.manager_name,
            p.percent_complete, p.budget_utilization_pct, p.schedule_variance_days,
            p.rag_status, p.planned_end_date, p.projected_end_date
     FROM snow_projects p
     WHERE (:connectionId::uuid IS NULL OR p.resolver_connection_id = :connectionId::uuid)
       AND (:status IS NULL OR p.status = :status)
     ORDER BY
       CASE p.rag_status WHEN ''RED'' THEN 1 WHEN ''AMBER'' THEN 2 ELSE 3 END,
       p.budget_utilization_pct DESC NULLS LAST
     LIMIT :_limit',
    '{"type":"object","properties":{"connectionId":{"type":"string","format":"uuid","description":"Filter by SN connection"},"status":{"type":"string","description":"Project status filter (active, closed, etc.)"}},"required":[]}',
    TRUE,
    'system'
),
(
    gen_random_uuid(),
    NULL,
    'Project Budget Overview',
    'Budget vs actual cost analysis per project with utilization percentage.',
    'SNOW_PROJECTS',
    'SELECT p.number, p.short_description, p.manager_name,
            p.total_budget, p.actual_cost, p.projected_cost,
            p.budget_utilization_pct,
            p.cost_forecast_accuracy,
            p.rag_status
     FROM snow_projects p
     WHERE (:connectionId::uuid IS NULL OR p.resolver_connection_id = :connectionId::uuid)
       AND p.total_budget IS NOT NULL
     ORDER BY p.budget_utilization_pct DESC NULLS LAST
     LIMIT :_limit',
    '{"type":"object","properties":{"connectionId":{"type":"string","format":"uuid","description":"Filter by SN connection"}},"required":[]}',
    TRUE,
    'system'
),
(
    gen_random_uuid(),
    NULL,
    'Project Milestone Status',
    'Milestone completion rates per project with overdue milestone counts.',
    'SNOW_PROJECTS',
    'SELECT p.number, p.short_description,
            COUNT(t.id) FILTER (WHERE t.is_milestone)           AS total_milestones,
            COUNT(t.id) FILTER (WHERE t.is_milestone AND t.state IN (''3'',''closed_complete'',''complete'')) AS completed_milestones,
            COUNT(t.id) FILTER (WHERE t.is_milestone AND t.due_date < CURRENT_DATE AND t.state NOT IN (''3'',''closed_complete'',''complete'')) AS overdue_milestones,
            p.milestone_completion_rate,
            p.rag_status
     FROM snow_projects p
     LEFT JOIN snow_project_tasks t ON t.project_id = p.id
     WHERE (:connectionId::uuid IS NULL OR p.resolver_connection_id = :connectionId::uuid)
     GROUP BY p.id, p.number, p.short_description, p.milestone_completion_rate, p.rag_status
     ORDER BY overdue_milestones DESC, p.milestone_completion_rate ASC
     LIMIT :_limit',
    '{"type":"object","properties":{"connectionId":{"type":"string","format":"uuid","description":"Filter by SN connection"}},"required":[]}',
    TRUE,
    'system'
);
