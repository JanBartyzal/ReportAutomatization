# P8-W5: Named Query Catalog

**Phase:** P8
**Module:** engine-data:query (extension)
**Effort:** ~3 MD
**Priority:** HIGH (foundation for P8-W4)
**Key insight:** Named queries are DATA-SOURCE AGNOSTIC – they can query any table in the platform DB.

---

## Goal

Add a "Named Query Catalog" to engine-data:query. A named query is a saved, versioned,
parameterized SQL query with a name and description. It is the data binding target for
Text Templates (P8-W4) and can be used standalone for custom data access.

Named queries can query:
- Parsed platform data (parsed_tables, documents)
- Form responses
- ServiceNow ITSM data (snow_incidents, snow_requests)
- ServiceNow project data (snow_projects, snow_project_tasks, snow_project_budgets)
- Any future table in the platform DB

---

## Data Model

### `NamedQueryEntity`
```
id (UUID), org_id (UUID, nullable for SYSTEM queries),
name (VARCHAR 255, UNIQUE per org), description (TEXT),
sql_query (TEXT – parameterized with :paramName syntax),
params_schema (JSONB – JSON Schema for parameter validation),
data_source_hint (VARCHAR – e.g. PLATFORM, SNOW_ITSM, SNOW_PROJECTS, FORMS),
is_system (boolean – system queries can't be deleted),
is_active (boolean),
created_by (VARCHAR), created_at, updated_at
```

### `params_schema` JSONB example
```json
{
  "properties": {
    "groupId": { "type": "string", "description": "Resolver group UUID" },
    "startDate": { "type": "string", "format": "date" }
  },
  "required": ["groupId"]
}
```

---

## New Files (engine-data:query module)

- `model/NamedQueryEntity.java`
- `model/dto/NamedQueryDto.java`
- `model/dto/CreateNamedQueryRequest.java`
- `model/dto/UpdateNamedQueryRequest.java`
- `model/dto/NamedQueryExecuteRequest.java`  `{ params: Map<String,String> }`
- `model/dto/NamedQueryResultDto.java`  `{ queryId, rows: List<Map<String,Object>>, totalCount, executedAt }`
- `repository/NamedQueryRepository.java`
- `service/NamedQueryService.java`  – CRUD + execute(queryId, params)
- `controller/NamedQueryController.java`

---

## Migration

- `V12_0_1__qry_named_queries.sql` (engine-data migrations)

---

## REST Endpoints

| Method | Path | Description |
|---|---|---|
| GET | `/api/data/named-queries` | List queries (filterable by data_source_hint, is_system) |
| POST | `/api/data/named-queries` | Create query |
| GET | `/api/data/named-queries/{id}` | Get detail |
| PUT | `/api/data/named-queries/{id}` | Update |
| DELETE | `/api/data/named-queries/{id}` | Delete (only non-system) |
| POST | `/api/data/named-queries/{id}/execute` | Execute with params |
| GET | `/api/data/named-queries/{id}/schema` | Get params schema (for frontend form) |

---

## Security

- SQL injection prevention: use JPA `createNativeQuery` with named parameters only,
  never string concatenation
- RLS context set before query execution (org_id from JWT)
- System queries (is_system=true): read-only for non-admin users

---

## Acceptance Criteria

- [ ] Named query created with SQL and params_schema
- [ ] Execute with valid params returns rows as List<Map<String,Object>>
- [ ] Execute with invalid params returns 400 with validation errors
- [ ] RLS enforced – org_id set before query execution
- [ ] System queries not deletable by non-admin
