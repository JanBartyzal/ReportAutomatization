-- P4b PPTX Template Manager tables
-- Stores PPTX templates, versions, extracted placeholders, and placeholder-to-data mappings

-- Template registry
CREATE TABLE IF NOT EXISTS pptx_templates (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id          VARCHAR(255),
    name            VARCHAR(512) NOT NULL,
    description     TEXT,
    scope           VARCHAR(20) NOT NULL DEFAULT 'CENTRAL'
                        CHECK (scope IN ('CENTRAL', 'LOCAL')),
    report_type     VARCHAR(255),
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_by      VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_pptx_templates_org_id ON pptx_templates(org_id);
CREATE INDEX idx_pptx_templates_active ON pptx_templates(is_active) WHERE is_active = true;
CREATE INDEX idx_pptx_templates_scope ON pptx_templates(scope);
CREATE INDEX idx_pptx_templates_report_type ON pptx_templates(report_type);

-- Template versions (each upload creates a new version)
CREATE TABLE IF NOT EXISTS template_versions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id     UUID NOT NULL REFERENCES pptx_templates(id) ON DELETE CASCADE,
    version         INT NOT NULL,
    blob_url        VARCHAR(2048) NOT NULL,
    file_size_bytes BIGINT,
    uploaded_by     VARCHAR(255),
    uploaded_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_current      BOOLEAN NOT NULL DEFAULT true,

    UNIQUE (template_id, version)
);

CREATE INDEX idx_template_versions_template ON template_versions(template_id);
CREATE INDEX idx_template_versions_current ON template_versions(template_id, is_current) WHERE is_current = true;

-- Extracted placeholders per version
CREATE TABLE IF NOT EXISTS template_placeholders (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version_id          UUID NOT NULL REFERENCES template_versions(id) ON DELETE CASCADE,
    placeholder_key     VARCHAR(255) NOT NULL,
    placeholder_type    VARCHAR(20) NOT NULL
                            CHECK (placeholder_type IN ('TEXT', 'TABLE', 'CHART')),
    slide_index         INT,
    shape_name          VARCHAR(255),
    detected_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_template_placeholders_version ON template_placeholders(version_id);

-- Placeholder-to-data-source mapping configuration (per template, not per version)
CREATE TABLE IF NOT EXISTS placeholder_mappings (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id         UUID NOT NULL REFERENCES pptx_templates(id) ON DELETE CASCADE,
    placeholder_key     VARCHAR(255) NOT NULL,
    data_source_type    VARCHAR(30) NOT NULL
                            CHECK (data_source_type IN ('FORM_FIELD', 'EXCEL_COLUMN', 'COMPUTED')),
    data_source_ref     VARCHAR(512) NOT NULL,
    transform_expression VARCHAR(1024),
    created_by          VARCHAR(255),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    UNIQUE (template_id, placeholder_key)
);

CREATE INDEX idx_placeholder_mappings_template ON placeholder_mappings(template_id);

-- Auto-update trigger for updated_at
CREATE OR REPLACE FUNCTION update_pptx_template_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_pptx_templates_updated_at
    BEFORE UPDATE ON pptx_templates
    FOR EACH ROW EXECUTE FUNCTION update_pptx_template_updated_at();

CREATE TRIGGER trg_placeholder_mappings_updated_at
    BEFORE UPDATE ON placeholder_mappings
    FOR EACH ROW EXECUTE FUNCTION update_pptx_template_updated_at();
