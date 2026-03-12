-- V1__create_form_tables.sql
-- Form engine: forms, versions, fields, responses, field values, comments, assignments

-- ============================================================================
-- FORMS - Top-level form definitions
-- ============================================================================
CREATE TABLE IF NOT EXISTS forms (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id          VARCHAR(255) NOT NULL,
    title           VARCHAR(500) NOT NULL,
    description     TEXT,
    scope           VARCHAR(20)  NOT NULL DEFAULT 'CENTRAL'
                    CHECK (scope IN ('CENTRAL', 'LOCAL')),
    status          VARCHAR(20)  NOT NULL DEFAULT 'DRAFT'
                    CHECK (status IN ('DRAFT', 'PUBLISHED', 'CLOSED')),
    owner_org_id    VARCHAR(255),
    created_by      VARCHAR(255) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- FORM VERSIONS - Immutable snapshots of form definitions
-- ============================================================================
CREATE TABLE IF NOT EXISTS form_versions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    form_id         UUID         NOT NULL REFERENCES forms(id) ON DELETE CASCADE,
    version_number  INTEGER      NOT NULL,
    schema_def      JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_by      VARCHAR(255) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE(form_id, version_number)
);

-- ============================================================================
-- FORM FIELDS - Field definitions per version
-- ============================================================================
CREATE TABLE IF NOT EXISTS form_fields (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    form_version_id UUID         NOT NULL REFERENCES form_versions(id) ON DELETE CASCADE,
    field_key       VARCHAR(255) NOT NULL,
    field_type      VARCHAR(50)  NOT NULL
                    CHECK (field_type IN ('text', 'number', 'percentage', 'date', 'dropdown', 'table', 'file_attachment')),
    label           VARCHAR(500) NOT NULL,
    section         VARCHAR(255),
    section_description TEXT,
    sort_order      INTEGER      NOT NULL DEFAULT 0,
    required        BOOLEAN      NOT NULL DEFAULT FALSE,
    properties      JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE(form_version_id, field_key)
);

-- ============================================================================
-- FORM RESPONSES - User submissions per form version
-- ============================================================================
CREATE TABLE IF NOT EXISTS form_responses (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    form_id         UUID         NOT NULL REFERENCES forms(id) ON DELETE CASCADE,
    form_version_id UUID         NOT NULL REFERENCES form_versions(id) ON DELETE CASCADE,
    org_id          VARCHAR(255) NOT NULL,
    period_id       UUID,
    user_id         VARCHAR(255) NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'DRAFT'
                    CHECK (status IN ('DRAFT', 'SUBMITTED')),
    data            JSONB        NOT NULL DEFAULT '{}'::jsonb,
    submitted_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- FORM FIELD VALUES - Individual field values within a response
-- ============================================================================
CREATE TABLE IF NOT EXISTS form_field_values (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    response_id     UUID         NOT NULL REFERENCES form_responses(id) ON DELETE CASCADE,
    field_key       VARCHAR(255) NOT NULL,
    value           TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE(response_id, field_key)
);

-- ============================================================================
-- FORM FIELD COMMENTS - Per-field comments on response values
-- ============================================================================
CREATE TABLE IF NOT EXISTS form_field_comments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    response_id     UUID         NOT NULL REFERENCES form_responses(id) ON DELETE CASCADE,
    field_key       VARCHAR(255) NOT NULL,
    comment         TEXT         NOT NULL,
    user_id         VARCHAR(255) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- FORM ASSIGNMENTS - Which orgs are assigned to fill which forms
-- ============================================================================
CREATE TABLE IF NOT EXISTS form_assignments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    form_id         UUID         NOT NULL REFERENCES forms(id) ON DELETE CASCADE,
    org_id          VARCHAR(255) NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                    CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED')),
    assigned_by     VARCHAR(255) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE(form_id, org_id)
);

-- ============================================================================
-- INDEXES
-- ============================================================================

-- Forms
CREATE INDEX idx_forms_org_id ON forms (org_id);
CREATE INDEX idx_forms_status ON forms (status);
CREATE INDEX idx_forms_scope ON forms (scope);

-- Form versions
CREATE INDEX idx_form_versions_form_id ON form_versions (form_id);

-- Form fields
CREATE INDEX idx_form_fields_version_id ON form_fields (form_version_id);
CREATE INDEX idx_form_fields_section ON form_fields (form_version_id, section);

-- Form responses
CREATE INDEX idx_form_responses_form_id ON form_responses (form_id);
CREATE INDEX idx_form_responses_org_id ON form_responses (org_id);
CREATE INDEX idx_form_responses_org_period_version ON form_responses (org_id, period_id, form_version_id);
CREATE INDEX idx_form_responses_status ON form_responses (status);

-- Form field values
CREATE INDEX idx_form_field_values_response_id ON form_field_values (response_id);

-- Form field comments
CREATE INDEX idx_form_field_comments_response_id ON form_field_comments (response_id);
CREATE INDEX idx_form_field_comments_response_field ON form_field_comments (response_id, field_key);

-- Form assignments
CREATE INDEX idx_form_assignments_form_id ON form_assignments (form_id);
CREATE INDEX idx_form_assignments_org_id ON form_assignments (org_id);

-- ============================================================================
-- TRIGGERS - Auto-update updated_at
-- ============================================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_forms_updated_at
    BEFORE UPDATE ON forms
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_form_responses_updated_at
    BEFORE UPDATE ON form_responses
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_form_field_values_updated_at
    BEFORE UPDATE ON form_field_values
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_form_field_comments_updated_at
    BEFORE UPDATE ON form_field_comments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_form_assignments_updated_at
    BEFORE UPDATE ON form_assignments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
