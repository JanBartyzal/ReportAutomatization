package com.reportplatform.qry.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportplatform.qry.model.NamedQueryEntity;
import com.reportplatform.qry.model.dto.*;
import com.reportplatform.qry.repository.NamedQueryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Manages the Named Query Catalog: CRUD and parameterised execution.
 * <p>
 * SQL injection prevention: parameters are always bound via JPA named
 * parameters (setParameter) – never via string concatenation.
 * <p>
 * RLS context is set inside every {@code @Transactional} method so
 * PostgreSQL row-level security policies filter correctly.
 */
@Service
public class NamedQueryService {

    private static final Logger log = LoggerFactory.getLogger(NamedQueryService.class);
    private static final int DEFAULT_LIMIT = 500;
    private static final int MAX_LIMIT = 5000;

    /**
     * Pre-compiled word-boundary patterns for DML/DDL keywords.
     * Using \b prevents partial matches (e.g. "DELETEFOREVER" is not blocked,
     * but "DELETE FROM" and "DELETE\nFROM" both are).
     */
    private static final List<Pattern> FORBIDDEN_SQL_PATTERNS = List.of(
            Pattern.compile("\\bINSERT\\b"),
            Pattern.compile("\\bUPDATE\\b"),
            Pattern.compile("\\bDELETE\\b"),
            Pattern.compile("\\bDROP\\b"),
            Pattern.compile("\\bALTER\\b"),
            Pattern.compile("\\bTRUNCATE\\b"),
            Pattern.compile("\\bCREATE\\b"),
            Pattern.compile("\\bEXECUTE\\b"),
            Pattern.compile("\\bEXEC\\b"),
            Pattern.compile("\\bCALL\\b"),
            Pattern.compile("\\bGRANT\\b"),
            Pattern.compile("\\bREVOKE\\b")
    );

    private final NamedQueryRepository repository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    public NamedQueryService(NamedQueryRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    // ---- CRUD ----

    @Transactional(readOnly = true)
    public List<NamedQueryDto> listAccessible(UUID orgId, String dataSourceHint) {
        setRlsContext(orgId);
        List<NamedQueryEntity> entities = dataSourceHint != null && !dataSourceHint.isBlank()
                ? repository.findAccessibleByOrgIdAndDataSourceHint(orgId, dataSourceHint)
                : repository.findAccessibleByOrgId(orgId);
        return entities.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Page<NamedQueryDto> listAccessiblePageable(UUID orgId, int page, int size) {
        setRlsContext(orgId);
        return repository.findAccessibleByOrgIdPageable(orgId, PageRequest.of(page, Math.min(size, 100)))
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<NamedQueryDto> findById(UUID orgId, UUID queryId) {
        setRlsContext(orgId);
        return repository.findByIdAndOrgAccess(queryId, orgId).map(this::toDto);
    }

    @Transactional
    public NamedQueryDto create(UUID orgId, String createdBy, CreateNamedQueryRequest req) {
        assertReadOnly(req.sqlQuery());
        setRlsContext(orgId);
        if (repository.existsByOrgIdAndName(orgId, req.name())) {
            throw new IllegalArgumentException("A query named '" + req.name() + "' already exists for this organisation.");
        }
        NamedQueryEntity entity = new NamedQueryEntity(
                orgId,
                req.name(),
                req.description(),
                req.sqlQuery(),
                req.paramsSchema(),
                req.dataSourceHint(),
                false,
                createdBy
        );
        return toDto(repository.save(entity));
    }

    @Transactional
    public NamedQueryDto update(UUID orgId, UUID queryId, UpdateNamedQueryRequest req) {
        setRlsContext(orgId);
        NamedQueryEntity entity = repository.findByIdAndOrgAccess(queryId, orgId)
                .orElseThrow(() -> new NoSuchElementException("Named query not found: " + queryId));

        if (entity.isSystem()) {
            throw new IllegalStateException("System queries cannot be modified.");
        }
        if (req.name() != null) entity.setName(req.name());
        if (req.description() != null) entity.setDescription(req.description());
        if (req.sqlQuery() != null) {
            assertReadOnly(req.sqlQuery());
            entity.setSqlQuery(req.sqlQuery());
        }
        if (req.paramsSchema() != null) entity.setParamsSchema(req.paramsSchema());
        if (req.dataSourceHint() != null) entity.setDataSourceHint(req.dataSourceHint());
        if (req.active() != null) entity.setActive(req.active());
        entity.setUpdatedAt(Instant.now());
        return toDto(repository.save(entity));
    }

    @Transactional
    public void delete(UUID orgId, UUID queryId) {
        setRlsContext(orgId);
        NamedQueryEntity entity = repository.findByIdAndOrgAccess(queryId, orgId)
                .orElseThrow(() -> new NoSuchElementException("Named query not found: " + queryId));
        if (entity.isSystem()) {
            throw new IllegalStateException("System queries cannot be deleted.");
        }
        // Soft delete
        entity.setActive(false);
        entity.setUpdatedAt(Instant.now());
        repository.save(entity);
    }

    // ---- Execution ----

    /**
     * Executes a named query with the provided runtime parameters.
     * <p>
     * Security: parameters are bound with {@code query.setParameter()} – no
     * string interpolation into the SQL. The SQL itself is stored in DB and
     * validated on creation (must not start with DML keywords).
     *
     * @param orgId   caller's organisation (for RLS context + access check)
     * @param queryId target named query UUID
     * @param req     runtime parameters and optional row limit
     * @return result rows as list of column-name → value maps
     */
    @Transactional(readOnly = true)
    public NamedQueryResultDto execute(UUID orgId, UUID queryId, NamedQueryExecuteRequest req) {
        setRlsContext(orgId);

        NamedQueryEntity entity = repository.findByIdAndOrgAccess(queryId, orgId)
                .orElseThrow(() -> new NoSuchElementException("Named query not found: " + queryId));

        if (!entity.isActive()) {
            throw new IllegalStateException("Named query is not active: " + queryId);
        }

        String sql = entity.getSqlQuery().trim();
        assertReadOnly(sql);

        int limit = req.limit() != null ? Math.min(req.limit(), MAX_LIMIT) : DEFAULT_LIMIT;

        // Inject LIMIT if not already present (prevents runaway queries)
        if (!sql.toLowerCase().contains(" limit ")) {
            sql = sql + " LIMIT " + limit;
        }

        // Use Tuple result type to get column aliases via standard JPA API
        @SuppressWarnings("unchecked")
        jakarta.persistence.TypedQuery<Tuple> nativeQuery =
                (jakarta.persistence.TypedQuery<Tuple>) entityManager.createNativeQuery(sql, Tuple.class);

        // Validate required parameters against paramsSchema before binding
        Map<String, String> params = req.params() != null ? req.params() : Map.of();
        validateRequiredParams(entity.getParamsSchema(), params, queryId);

        // Bind named parameters with type-aware coercion based on paramsSchema
        Map<String, Object> schemaProperties = parseSchemaProperties(entity.getParamsSchema());
        for (Map.Entry<String, String> entry : params.entrySet()) {
            Object coerced = coerceParamValue(entry.getKey(), entry.getValue(), schemaProperties);
            nativeQuery.setParameter(entry.getKey(), coerced);
        }

        List<Tuple> rawResults = nativeQuery.getResultList();
        List<Map<String, Object>> rows = mapTupleResults(rawResults);

        log.debug("Named query '{}' executed: {} rows returned", entity.getName(), rows.size());

        return new NamedQueryResultDto(queryId, entity.getName(), rows, rows.size(), Instant.now());
    }

    // ---- Helpers ----

    /**
     * Validates that all parameters declared as {@code required} in the query's
     * paramsSchema are present in the caller-supplied params map.
     *
     * @throws IllegalArgumentException with a clear list of missing param names
     */
    private void validateRequiredParams(String paramsSchema, Map<String, String> params, UUID queryId) {
        if (paramsSchema == null || paramsSchema.isBlank() || paramsSchema.equals("{}")) return;
        try {
            Map<String, Object> schema = objectMapper.readValue(
                    paramsSchema, new TypeReference<Map<String, Object>>() {});
            Object requiredRaw = schema.get("required");
            if (!(requiredRaw instanceof List<?> required) || required.isEmpty()) return;

            List<String> missing = required.stream()
                    .map(Object::toString)
                    .filter(name -> !params.containsKey(name) || params.get(name) == null)
                    .toList();

            if (!missing.isEmpty()) {
                throw new IllegalArgumentException(
                        "Missing required parameters for query " + queryId + ": " + missing);
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Could not parse paramsSchema for validation (queryId={}): {}", queryId, e.getMessage());
        }
    }

    /**
     * Parses the {@code properties} section of a JSON Schema string into a map of
     * param-name → property descriptor (e.g. {@code {"type": "integer"}}).
     * Returns an empty map on any parse error so callers degrade gracefully.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseSchemaProperties(String paramsSchema) {
        if (paramsSchema == null || paramsSchema.isBlank() || paramsSchema.equals("{}")) return Map.of();
        try {
            Map<String, Object> schema = objectMapper.readValue(
                    paramsSchema, new TypeReference<Map<String, Object>>() {});
            Object props = schema.get("properties");
            if (props instanceof Map<?, ?> m) {
                return (Map<String, Object>) m;
            }
        } catch (Exception e) {
            log.warn("Could not parse paramsSchema properties: {}", e.getMessage());
        }
        return Map.of();
    }

    /**
     * Converts a string parameter value to the Java type declared in the schema.
     * <p>
     * Supported type coercions:
     * <ul>
     *   <li>{@code "integer"} / {@code "number"} → {@link Long} / {@link Double}</li>
     *   <li>{@code "boolean"} → {@link Boolean}</li>
     *   <li>anything else → kept as {@link String}</li>
     * </ul>
     * Falls back to the original string on parse failure so the DB can report a
     * type error rather than the service swallowing it silently.
     */
    private Object coerceParamValue(String paramName, String rawValue, Map<String, Object> schemaProperties) {
        if (rawValue == null) return null;
        Object propDef = schemaProperties.get(paramName);
        if (!(propDef instanceof Map<?, ?> propMap)) return rawValue;

        String type = String.valueOf(propMap.get("type"));
        return switch (type) {
            case "integer" -> {
                try { yield Long.parseLong(rawValue); }
                catch (NumberFormatException e) {
                    log.warn("Cannot coerce param '{}' value '{}' to integer; passing as string", paramName, rawValue);
                    yield rawValue;
                }
            }
            case "number" -> {
                try { yield Double.parseDouble(rawValue); }
                catch (NumberFormatException e) {
                    log.warn("Cannot coerce param '{}' value '{}' to number; passing as string", paramName, rawValue);
                    yield rawValue;
                }
            }
            case "boolean" -> Boolean.parseBoolean(rawValue);
            default -> rawValue;
        };
    }

    /**
     * Validates that the SQL is a read-only SELECT (or WITH…SELECT) statement.
     * <p>
     * Uses pre-compiled word-boundary regex patterns so that keywords embedded
     * inside identifiers (e.g. "update_count") are not falsely rejected, while
     * keywords followed by whitespace, newlines, or punctuation are caught.
     * Checked both on create/update (storage time) and on execute (runtime).
     * </p>
     *
     * @throws IllegalArgumentException if the SQL contains DML/DDL keywords
     */
    private void assertReadOnly(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("SQL query must not be blank.");
        }
        String upper = sql.stripLeading().toUpperCase();
        if (!upper.startsWith("SELECT") && !upper.startsWith("WITH")) {
            throw new IllegalArgumentException(
                    "Named queries must be SELECT statements only. DML operations are not permitted.");
        }
        for (Pattern pattern : FORBIDDEN_SQL_PATTERNS) {
            if (pattern.matcher(upper).find()) {
                throw new IllegalArgumentException(
                        "Named query contains forbidden keyword: " + pattern.pattern()
                                .replace("\\b", "").replace("\\", ""));
            }
        }
    }

    /**
     * Maps JPA Tuple result rows to column-name → value maps.
     * Column aliases are read via the standard JPA {@link TupleElement} API.
     */
    private List<Map<String, Object>> mapTupleResults(List<Tuple> tuples) {
        if (tuples.isEmpty()) return List.of();

        List<Map<String, Object>> rows = new ArrayList<>(tuples.size());
        for (Tuple tuple : tuples) {
            Map<String, Object> rowMap = new LinkedHashMap<>();
            List<TupleElement<?>> elements = tuple.getElements();
            for (int i = 0; i < elements.size(); i++) {
                TupleElement<?> el = elements.get(i);
                String alias = (el.getAlias() != null && !el.getAlias().isBlank())
                        ? el.getAlias() : "col_" + i;
                rowMap.put(alias, tuple.get(el));
            }
            rows.add(rowMap);
        }
        return rows;
    }

    private void setRlsContext(UUID orgId) {
        if (orgId != null) {
            entityManager.createNativeQuery(
                            "SELECT set_config('app.current_org_id', :orgId, true)")
                    .setParameter("orgId", orgId.toString())
                    .getSingleResult();
        }
    }

    private NamedQueryDto toDto(NamedQueryEntity e) {
        return new NamedQueryDto(
                e.getId(), e.getOrgId(), e.getName(), e.getDescription(),
                e.getSqlQuery(), e.getParamsSchema(), e.getDataSourceHint(),
                e.isSystem(), e.isActive(), e.getCreatedBy(),
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
