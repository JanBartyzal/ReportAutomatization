package com.reportplatform.sink.tbl.backend;

import com.reportplatform.sink.tbl.service.TableSinkService;

import java.util.List;

/**
 * Storage backend abstraction for table sink writes.
 * <p>
 * Implementations route bulk-insert and delete operations to a specific
 * persistence target (PostgreSQL, Spark/Delta Lake, …). The active backend
 * is selected at runtime by {@link com.reportplatform.sink.tbl.service.StorageRoutingService}
 * based on org-level and source-type configuration, enabling zero-downtime
 * coexistence of multiple backends during migration.
 * </p>
 */
public interface TableStorageBackend {

    /**
     * Returns the unique backend identifier (e.g. "POSTGRES", "SPARK").
     */
    String backendType();

    /**
     * Persist (or submit for persistence) a batch of structured table records.
     *
     * <p><strong>Return-value contract:</strong> the returned integer represents
     * the number of records <em>accepted</em> by the backend, which may differ
     * from the number already durably stored:
     * <ul>
     *   <li>{@code PostgresTableStorageBackend} — records are committed synchronously;
     *       the return value equals records durably persisted.</li>
     *   <li>{@code SparkTableStorageBackend} — a single Pub/Sub event is published;
     *       actual Delta-Lake persistence is asynchronous. The return value equals
     *       the input {@code records.size()} (submitted, not yet confirmed).</li>
     * </ul>
     * Callers must not interpret this value as a durable-write confirmation for
     * async backends.
     *
     * @param fileId     source file identifier
     * @param orgId      owning organisation
     * @param sourceType record origin ("FILE", "FORM", "SERVICE_NOW", …)
     * @param records    parsed table records to persist
     * @return number of records accepted/submitted by this backend
     */
    int bulkInsert(
            String fileId,
            String orgId,
            String sourceType,
            List<TableSinkService.TableRecordData> records);

    /**
     * Remove (or submit removal of) all records for the given file.
     * Used as a Saga compensating action.
     *
     * <p>For async backends (e.g. Spark) deletion is event-driven and the
     * return value is 0 — the actual removal happens outside this JVM.</p>
     *
     * @param fileId source file identifier
     * @return number of records synchronously removed (0 for async backends)
     */
    int deleteByFileId(String fileId);
}
