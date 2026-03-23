package com.reportplatform.enginedata.common.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service handling persistence of pipeline data (tables, documents).
 * <p>
 * Uses native SQL queries via {@link EntityManager} to avoid circular module
 * dependencies between {@code common} and the {@code sink-*} modules.
 * </p>
 * <p>
 * Pipeline endpoints are {@code permitAll()}. The orchestrator provides
 * {@code orgId} in the request body (not header). We set RLS context
 * via {@code SET LOCAL app.current_org_id} before INSERTs.
 * </p>
 */
@Service
public class PipelineStoreService {

    private static final Logger log = LoggerFactory.getLogger(PipelineStoreService.class);

    @PersistenceContext
    private EntityManager entityManager;

    private final ObjectMapper objectMapper;

    public PipelineStoreService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Sets PostgreSQL RLS session variable for the current transaction.
     * Must be called within a {@code @Transactional} method before any INSERT/SELECT.
     */
    private void setRlsContext(String orgId) {
        if (orgId != null && !orgId.isBlank() && !"unknown".equals(orgId)) {
            try {
                UUID.fromString(orgId); // validate
                entityManager.createNativeQuery("SET LOCAL app.current_org_id = '" + orgId + "'")
                        .executeUpdate();
                entityManager.flush();
            } catch (IllegalArgumentException e) {
                log.warn("Invalid orgId for RLS context: {}", orgId);
            }
        }
    }

    // ---------------------------------------------------------------
    // STORE TABLE DATA
    // ---------------------------------------------------------------

    /**
     * Persists structured table data extracted from Excel/CSV files.
     * <p>
     * The {@code mappedData} field from the orchestrator is the stringified
     * JSON response of the MAP step, which contains the actual parsed data
     * under its own {@code mappedData} key. The parsed data for Excel files
     * follows the structure:
     * <pre>
     * {
     *   "file_id": "...",
     *   "sheets": [
     *     { "sheet_name": "...", "headers": [...], "rows": [[...], ...], "total_rows": N }
     *   ],
     *   "status": "COMPLETED"
     * }
     * </pre>
     *
     * @param fileId     the file identifier
     * @param orgId      the organization identifier
     * @param mappedData the stringified JSON with mapped/parsed data
     * @return number of table rows persisted
     */
    @Transactional
    public int storeTableData(String fileId, String orgId, String mappedData) {
        setRlsContext(orgId);
        Map<String, Object> data = parseJsonSafely(mappedData);
        if (data == null || data.isEmpty()) {
            log.warn("STORE table: empty or invalid mappedData for file [{}]", fileId);
            return 0;
        }

        // The MAP step wraps the parsed data: the outer JSON has fileId/status/mappedData.
        // Unwrap if needed.
        Map<String, Object> parsedData = unwrapMappedData(data);

        // Ensure a files record exists
        upsertFileRecord(fileId, orgId, parsedData);

        // Delete any existing parsed_tables for this file (idempotent re-store)
        int deleted = deleteExistingTables(fileId);
        if (deleted > 0) {
            log.info("STORE table: removed {} existing parsed_tables rows for file [{}]", deleted, fileId);
        }

        // Extract sheets/tables and persist
        int storedCount = 0;
        Object sheetsObj = parsedData.get("sheets");
        if (sheetsObj instanceof List<?> sheets) {
            for (Object sheetObj : sheets) {
                if (sheetObj instanceof Map<?, ?> sheet) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> sheetMap = (Map<String, Object>) sheet;
                    storedCount += persistSheet(fileId, orgId, sheetMap);
                }
            }
        }

        // Handle case where data has top-level headers/rows (single table, no sheets wrapper)
        if (storedCount == 0 && parsedData.containsKey("headers")) {
            storedCount += persistSheet(fileId, orgId, parsedData);
        }

        log.info("STORE table: persisted {} table(s) for file [{}] org [{}]", storedCount, fileId, orgId);
        return storedCount;
    }

    private int persistSheet(String fileId, String orgId, Map<String, Object> sheetData) {
        String sheetName = stringValue(sheetData, "sheet_name", "source_sheet", "name");
        Object headersObj = sheetData.get("headers");
        Object rowsObj = sheetData.get("rows");

        String headersJson = toJson(headersObj != null ? headersObj : List.of());
        String rowsJson = toJson(rowsObj != null ? rowsObj : List.of());

        // Build metadata from remaining fields
        Map<String, Object> metadata = new java.util.LinkedHashMap<>();
        Object totalRows = sheetData.get("total_rows");
        if (totalRows != null) {
            metadata.put("total_rows", totalRows);
        }
        Object status = sheetData.get("status");
        if (status != null) {
            metadata.put("status", status);
        }
        String metadataJson = toJson(metadata);

        UUID id = UUID.randomUUID();
        entityManager.createNativeQuery(
                        "INSERT INTO parsed_tables (id, file_id, org_id, source_sheet, headers, rows, metadata, created_at) " +
                                "VALUES (:id, :fileId, :orgId, :sourceSheet, CAST(:headers AS jsonb), CAST(:rows AS jsonb), " +
                                "CAST(:metadata AS jsonb), NOW()) " +
                                "ON CONFLICT (file_id, source_sheet) DO UPDATE SET " +
                                "headers = CAST(:headers AS jsonb), rows = CAST(:rows AS jsonb), " +
                                "metadata = CAST(:metadata AS jsonb)")
                .setParameter("id", id)
                .setParameter("fileId", fileId)
                .setParameter("orgId", orgId)
                .setParameter("sourceSheet", sheetName != null ? sheetName : "Sheet1")
                .setParameter("headers", headersJson)
                .setParameter("rows", rowsJson)
                .setParameter("metadata", metadataJson)
                .executeUpdate();

        return 1;
    }

    // ---------------------------------------------------------------
    // STORE DOCUMENT DATA
    // ---------------------------------------------------------------

    /**
     * Persists unstructured document content (PPTX slides, PDF pages, etc.).
     * <p>
     * For PPTX files, the parsed data has the structure:
     * <pre>
     * {
     *   "file_id": "...",
     *   "total_slides": N,
     *   "slides": [
     *     { "slide_index": 0, "title": "...", "texts": [...], "tables": [...], "notes": "..." }
     *   ]
     * }
     * </pre>
     *
     * @param fileId     the file identifier
     * @param orgId      the organization identifier
     * @param mappedData the stringified JSON with mapped/parsed data
     * @return number of document rows persisted
     */
    @Transactional
    public int storeDocumentData(String fileId, String orgId, String mappedData) {
        setRlsContext(orgId);
        Map<String, Object> data = parseJsonSafely(mappedData);
        if (data == null || data.isEmpty()) {
            log.warn("STORE doc: empty or invalid mappedData for file [{}]", fileId);
            return 0;
        }

        Map<String, Object> parsedData = unwrapMappedData(data);

        // Ensure a files record exists
        upsertFileRecord(fileId, orgId, parsedData);

        // Delete existing documents for this file (idempotent re-store)
        int deleted = deleteExistingDocuments(fileId);
        if (deleted > 0) {
            log.info("STORE doc: removed {} existing document rows for file [{}]", deleted, fileId);
        }

        int storedCount = 0;

        // Handle PPTX slides
        Object slidesObj = parsedData.get("slides");
        if (slidesObj instanceof List<?> slides) {
            for (Object slideObj : slides) {
                if (slideObj instanceof Map<?, ?> slide) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> slideMap = (Map<String, Object>) slide;
                    storedCount += persistSlideDocument(fileId, orgId, slideMap);
                }
            }
        }

        // Handle PDF pages
        Object pagesObj = parsedData.get("pages");
        if (pagesObj instanceof List<?> pages) {
            for (Object pageObj : pages) {
                if (pageObj instanceof Map<?, ?> page) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> pageMap = (Map<String, Object>) page;
                    storedCount += persistPageDocument(fileId, orgId, pageMap);
                }
            }
        }

        // If no slides or pages, store the entire content as a single document
        if (storedCount == 0 && !parsedData.isEmpty()) {
            storedCount += persistGenericDocument(fileId, orgId, parsedData);
        }

        log.info("STORE doc: persisted {} document(s) for file [{}] org [{}]", storedCount, fileId, orgId);
        return storedCount;
    }

    private int persistSlideDocument(String fileId, String orgId, Map<String, Object> slideData) {
        int slideIndex = slideData.containsKey("slide_index")
                ? ((Number) slideData.get("slide_index")).intValue()
                : 0;
        String documentType = "SLIDE_TEXT";

        // Build content JSON from slide data
        Map<String, Object> content = new java.util.LinkedHashMap<>();
        content.put("slide_index", slideIndex);
        content.put("title", slideData.getOrDefault("title", ""));
        content.put("texts", slideData.getOrDefault("texts", List.of()));
        content.put("notes", slideData.getOrDefault("notes", ""));

        // Tables from slides go into metadata
        Map<String, Object> metadata = new java.util.LinkedHashMap<>();
        Object tables = slideData.get("tables");
        if (tables != null) {
            metadata.put("tables", tables);
        }

        // Use SLIDE_TEXT_<index> to avoid unique constraint conflict on (file_id, document_type)
        String docTypeWithIndex = documentType + "_" + slideIndex;

        insertDocument(fileId, orgId, docTypeWithIndex, toJson(content), toJson(metadata));
        return 1;
    }

    private int persistPageDocument(String fileId, String orgId, Map<String, Object> pageData) {
        int pageIndex = pageData.containsKey("page_index")
                ? ((Number) pageData.get("page_index")).intValue()
                : 0;
        String docTypeWithIndex = "PDF_PAGE_" + pageIndex;

        insertDocument(fileId, orgId, docTypeWithIndex, toJson(pageData), toJson(Map.of()));
        return 1;
    }

    private int persistGenericDocument(String fileId, String orgId, Map<String, Object> data) {
        insertDocument(fileId, orgId, "GENERIC", toJson(data), toJson(Map.of()));
        return 1;
    }

    private void insertDocument(String fileId, String orgId, String documentType,
                                String contentJson, String metadataJson) {
        UUID id = UUID.randomUUID();
        entityManager.createNativeQuery(
                        "INSERT INTO documents (id, file_id, org_id, document_type, content, metadata, created_at) " +
                                "VALUES (:id, :fileId, :orgId, :documentType, CAST(:content AS jsonb), " +
                                "CAST(:metadata AS jsonb), NOW()) " +
                                "ON CONFLICT (file_id, document_type) DO UPDATE SET " +
                                "content = CAST(:content AS jsonb), metadata = CAST(:metadata AS jsonb)")
                .setParameter("id", id)
                .setParameter("fileId", fileId)
                .setParameter("orgId", orgId)
                .setParameter("documentType", documentType)
                .setParameter("content", contentJson)
                .setParameter("metadata", metadataJson)
                .executeUpdate();
    }

    // ---------------------------------------------------------------
    // FILES TABLE
    // ---------------------------------------------------------------

    private void upsertFileRecord(String fileId, String orgId, Map<String, Object> parsedData) {
        try {
            // Check if file record already exists
            Number count = (Number) entityManager.createNativeQuery(
                            "SELECT COUNT(*) FROM files WHERE id = CAST(:fileId AS uuid)")
                    .setParameter("fileId", fileId)
                    .getSingleResult();

            if (count.intValue() == 0) {
                // Derive filename and mime_type from parsed data if possible
                String filename = stringValue(parsedData, "filename", "file_name");
                if (filename == null) {
                    filename = "file_" + fileId;
                }
                String mimeType = deriveMimeType(parsedData);

                entityManager.createNativeQuery(
                                "INSERT INTO files (id, org_id, user_id, filename, size_bytes, mime_type, scan_status, created_at, updated_at) " +
                                        "VALUES (CAST(:fileId AS uuid), CAST(:orgId AS uuid), :userId, :filename, 0, :mimeType, 'PROCESSED', NOW(), NOW()) " +
                                        "ON CONFLICT (id) DO UPDATE SET scan_status = 'PROCESSED', updated_at = NOW()")
                        .setParameter("fileId", fileId)
                        .setParameter("orgId", orgId)
                        .setParameter("userId", "pipeline")
                        .setParameter("filename", filename)
                        .setParameter("mimeType", mimeType)
                        .executeUpdate();
                log.debug("Created files record for file [{}] org [{}]", fileId, orgId);
            } else {
                // Update status to PROCESSED
                entityManager.createNativeQuery(
                                "UPDATE files SET scan_status = 'PROCESSED', updated_at = NOW() WHERE id = CAST(:fileId AS uuid)")
                        .setParameter("fileId", fileId)
                        .executeUpdate();
            }
        } catch (Exception e) {
            // files record creation is best-effort; don't fail the store
            log.warn("Could not upsert files record for file [{}]: {}", fileId, e.getMessage());
        }
    }

    private String deriveMimeType(Map<String, Object> parsedData) {
        if (parsedData.containsKey("sheets")) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else if (parsedData.containsKey("slides")) {
            return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        } else if (parsedData.containsKey("pages")) {
            return "application/pdf";
        }
        return "application/octet-stream";
    }

    // ---------------------------------------------------------------
    // ROLLBACK (Saga compensation)
    // ---------------------------------------------------------------

    /**
     * Rolls back table data for a file (saga compensation).
     */
    @Transactional
    public int rollbackTableData(String fileId) {
        int deleted = deleteExistingTables(fileId);
        log.info("ROLLBACK table: deleted {} rows for file [{}]", deleted, fileId);
        return deleted;
    }

    /**
     * Rolls back document data for a file (saga compensation).
     */
    @Transactional
    public int rollbackDocumentData(String fileId) {
        int deleted = deleteExistingDocuments(fileId);
        log.info("ROLLBACK doc: deleted {} rows for file [{}]", deleted, fileId);
        return deleted;
    }

    // ---------------------------------------------------------------
    // HELPERS
    // ---------------------------------------------------------------

    private int deleteExistingTables(String fileId) {
        return entityManager.createNativeQuery("DELETE FROM parsed_tables WHERE file_id = :fileId")
                .setParameter("fileId", fileId)
                .executeUpdate();
    }

    private int deleteExistingDocuments(String fileId) {
        return entityManager.createNativeQuery("DELETE FROM documents WHERE file_id = :fileId")
                .setParameter("fileId", fileId)
                .executeUpdate();
    }

    /**
     * Unwraps the MAP step's response to get the actual parsed data.
     * The orchestrator sends mappedData which is the stringified MAP response:
     * {@code {"fileId":"...","status":"MAPPED","mappedData":"<actual parsed data>"}}
     * We need to parse the inner mappedData if it exists.
     */
    private Map<String, Object> unwrapMappedData(Map<String, Object> data) {
        Object innerMapped = data.get("mappedData");
        if (innerMapped instanceof String innerStr && !innerStr.isBlank()) {
            Map<String, Object> inner = parseJsonSafely(innerStr);
            if (inner != null && !inner.isEmpty()) {
                return inner;
            }
        }
        if (innerMapped instanceof Map<?, ?>) {
            @SuppressWarnings("unchecked")
            Map<String, Object> innerMap = (Map<String, Object>) innerMapped;
            return innerMap;
        }
        // Data might already be the parsed content (no wrapping)
        return data;
    }

    private Map<String, Object> parseJsonSafely(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON: {}", e.getMessage());
            return null;
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize to JSON: {}", e.getMessage());
            return "{}";
        }
    }

    /**
     * Extracts a string value from a map, trying multiple key names.
     */
    private String stringValue(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val instanceof String s && !s.isBlank()) {
                return s;
            }
        }
        return null;
    }
}
