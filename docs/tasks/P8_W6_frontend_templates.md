# P8-W6: Frontend – Text Templates & Named Queries UI

**Phase:** P8
**Module:** apps/frontend + packages/types
**Effort:** ~3 MD
**Priority:** HIGH
**Depends on:** P8-W4, P8-W5

---

## Goal

Frontend UI for managing Text Templates and Named Queries. Templates are the primary
entry point for end users to generate reports from any data source.

---

## New Pages

### 1. `TextTemplateListPage.tsx`
- Route: `/templates/text`
- List all text templates (card grid or table)
- Filter: scope (CENTRAL/LOCAL), output format
- Actions: View, Edit, Render, Delete
- "New Template" button → TextTemplateEditorPage

### 2. `TextTemplateEditorPage.tsx`
- Route: `/templates/text/new` + `/templates/text/:id/edit`
- Fields: name, description, scope, output formats (multi-select)
- Content editor: CodeMirror (markdown mode) with syntax highlighting
- **Data Bindings panel** (right sidebar):
  - Browse Named Query catalog
  - Click query → inserts `{{BINDING_NAME}}` at cursor
  - Shows placeholder list with bound queries
- Save → POST/PUT to `/api/reporting/text-templates`

### 3. `TextTemplateRenderPage.tsx`
- Route: `/templates/text/:id/render`
- Shows template metadata and required params from data_bindings
- Dynamic form generated from params_schema of bound queries
- Output format selector (PPTX / Excel)
- "Generate Report" button → POST `/api/reporting/text-templates/{id}/render`
- Progress indicator → download link when ready

### 4. `NamedQueryPage.tsx`
- Route: `/admin/named-queries`
- List all named queries (admin only)
- Create / edit query with SQL editor (CodeMirror with SQL mode)
- Test execution panel: enter params, click Run, see result table
- Shows which templates use each query (back-references)

---

## Modified Files

### `apps/frontend/src/App.tsx`
Add routes:
```tsx
<Route path="/templates/text" element={<TextTemplateListPage />} />
<Route path="/templates/text/new" element={<TextTemplateEditorPage />} />
<Route path="/templates/text/:id/edit" element={<TextTemplateEditorPage />} />
<Route path="/templates/text/:id/render" element={<TextTemplateRenderPage />} />
<Route path="/admin/named-queries" element={<AdminGuard><NamedQueryPage /></AdminGuard>} />
```

### `TemplateListPage.tsx`
Add tabs: "PPTX Templates" | "Text Templates"

### `AppLayout.tsx` (navigation)
Add "Text Templates" under Templates section.

---

## New API Clients

### `apps/frontend/src/api/textTemplates.ts`
- `getTextTemplates(filters)` → `TextTemplate[]`
- `createTextTemplate(req)` → `TextTemplate`
- `updateTextTemplate(id, req)` → `TextTemplate`
- `deleteTextTemplate(id)` → void
- `renderTextTemplate(id, req)` → `RenderResponse`

### `apps/frontend/src/api/namedQueries.ts`
- `getNamedQueries(filters)` → `NamedQuery[]`
- `createNamedQuery(req)` → `NamedQuery`
- `updateNamedQuery(id, req)` → `NamedQuery`
- `deleteNamedQuery(id)` → void
- `executeNamedQuery(id, params)` → `NamedQueryResult`

---

## New Types (`packages/types/`)

### `src/textTemplates.ts`
```typescript
export type TemplateType = 'MARKDOWN' | 'HTML';
export type OutputFormat = 'PPTX' | 'EXCEL' | 'HTML_EMAIL';
export type BindingType = 'TABLE' | 'SCALAR' | 'CHART';
export interface BindingEntry { ... }
export interface TextTemplate { id, name, description, templateType, content, outputFormats, dataBindings, scope, isSystem, version, createdAt, updatedAt }
export interface CreateTextTemplateRequest { ... }
export interface RenderRequest { outputFormat: OutputFormat; params: Record<string,string> }
export interface RenderResponse { jobId, downloadUrl, format, generatedAt }
```

### `src/namedQueries.ts`
```typescript
export interface NamedQuery { id, name, description, sqlQuery, paramsSchema, dataSourceHint, isSystem, isActive, createdAt }
export interface NamedQueryResult { queryId, rows: Record<string,unknown>[], totalCount, executedAt }
export interface NamedQueryExecuteRequest { params: Record<string,string> }
```

---

## Acceptance Criteria

- [ ] Text template list shows all templates with scope badge
- [ ] Editor saves template with data bindings
- [ ] Render page form generated dynamically from bound query params
- [ ] "Generate Report" → PPTX/Excel downloaded
- [ ] Named query admin page shows SQL editor + live test execution
