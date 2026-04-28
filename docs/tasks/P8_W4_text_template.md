# P8-W4: Text Template Engine

**Phase:** P8
**Module:** engine-reporting:text-template (NEW submodule)
**Effort:** ~5 MD
**Priority:** HIGH
**Depends on:** P8-W5 (Named Query Catalog)
**Key insight:** Templates are DATA-SOURCE AGNOSTIC – any named query can be a binding target.

---

## Goal

Introduce a unified text-based report template engine. A template is markdown/HTML text
with `{{placeholder}}` blocks. Each placeholder is bound to a named query from the
Named Query Catalog (P8-W5). The render service resolves bindings, fetches data, and
calls the appropriate generator (PPTX/Excel/HTML) to produce output.

This replaces ad-hoc report generation and works for ALL data types:
- Parsed platform files (Excel, PPTX, PDF, CSV)
- ServiceNow ITSM data (incidents, requests)
- ServiceNow project data
- Form responses
- Any future data source accessible via Named Queries

---

## Architecture

```
TextTemplateController → TextTemplateService → TextTemplateRepository
                               ↓
                     TemplateRenderService
                               ↓
          [Dapr gRPC] engine-data:query → NamedQueryService.execute(queryId, params)
                               ↓
          [Dapr gRPC] processor-generators:pptx / :xls
                               ↓
                      Azure Blob → URL returned
```

---

## Data Model

### `TextTemplateEntity`
```
id, org_id, name, description, template_type (MARKDOWN/HTML),
content (TEXT – markdown with {{placeholders}}),
output_formats (JSONB ["PPTX","EXCEL","HTML_EMAIL"]),
data_bindings (JSONB – see BindingEntry schema),
scope (CENTRAL/LOCAL), is_system (boolean),
version (int), is_active, created_by, created_at, updated_at
```

### `TextTemplateVersionEntity`
```
id, template_id, version, content, data_bindings, created_by, created_at
```

### BindingEntry (JSONB schema in data_bindings.bindings[])
```json
{
  "placeholder": "{{INCIDENTS_TABLE}}",
  "type": "TABLE | SCALAR | CHART",
  "query_id": "<uuid of NamedQuery>",
  "params": { "groupId": "{{input.groupId}}" },
  "chart_type": "PIE | BAR | LINE",
  "label": "Open Incidents"
}
```

---

## New Submodule Structure

```
engine-reporting/text-template/
├── pom.xml
└── src/main/java/com/reportplatform/tmpl/
    ├── entity/
    │   ├── TextTemplateEntity.java
    │   └── TextTemplateVersionEntity.java
    ├── model/
    │   ├── BindingEntry.java
    │   ├── BindingType.java (TABLE, SCALAR, CHART)
    │   └── OutputFormat.java (PPTX, EXCEL, HTML_EMAIL)
    ├── dto/
    │   ├── TextTemplateDto.java
    │   ├── CreateTextTemplateRequest.java
    │   ├── UpdateTextTemplateRequest.java
    │   ├── RenderRequest.java   { templateId, outputFormat, params: Map<String,String> }
    │   └── RenderResponse.java  { jobId, downloadUrl, format, generatedAt }
    ├── repository/
    │   └── TextTemplateRepository.java
    ├── service/
    │   ├── TextTemplateService.java  (CRUD + versioning)
    │   └── TemplateRenderService.java (resolve bindings → call generators)
    └── controller/
        └── TextTemplateController.java
```

---

## Modified Files

- `engine-reporting/pom.xml` – add `<module>text-template</module>` + dependency management
- `engine-reporting/app/pom.xml` – add `engine-reporting-text-template` dependency
- `EngineReportingApplication.java` – add `com.reportplatform.tmpl` to scan packages
- `V6_0_001__create_text_templates.sql` – new migration

---

## REST Endpoints

| Method | Path | Description |
|---|---|---|
| GET | `/api/reporting/text-templates` | List templates (filterable by scope, type) |
| POST | `/api/reporting/text-templates` | Create template |
| GET | `/api/reporting/text-templates/{id}` | Get detail with bindings |
| PUT | `/api/reporting/text-templates/{id}` | Update (creates new version) |
| DELETE | `/api/reporting/text-templates/{id}` | Soft delete |
| GET | `/api/reporting/text-templates/{id}/versions` | Version history |
| POST | `/api/reporting/text-templates/{id}/render` | Render with params → returns downloadUrl |

---

## Acceptance Criteria

- [ ] Template created with markdown content and data bindings saved to DB
- [ ] Render with valid params resolves all bindings via Named Query service
- [ ] Output PPTX generated and downloadable URL returned
- [ ] Version history preserved when template updated
- [ ] Works with any Named Query data source (not tied to SN)
- [ ] System templates (is_system=true) not deletable by regular users
