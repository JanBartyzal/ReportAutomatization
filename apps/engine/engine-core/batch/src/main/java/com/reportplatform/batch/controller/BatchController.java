package com.reportplatform.batch.controller;

import com.reportplatform.batch.model.entity.BatchEntity;
import com.reportplatform.batch.model.entity.BatchFileEntity;
import com.reportplatform.batch.repository.BatchRepository;
import com.reportplatform.batch.repository.BatchFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/batches")
public class BatchController {

    private static final Logger logger = LoggerFactory.getLogger(BatchController.class);

    private final BatchRepository batchRepository;
    private final BatchFileRepository batchFileRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public BatchController(BatchRepository batchRepository, BatchFileRepository batchFileRepository) {
        this.batchRepository = batchRepository;
        this.batchFileRepository = batchFileRepository;
    }

    @GetMapping({"", "/"})
    @Transactional
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<List<BatchEntity>> listBatches(
            @RequestParam(required = false) UUID holdingId,
            @RequestParam(required = false) String status,
            @RequestHeader(value = "X-Org-Id", required = false) String orgId) {

        setRlsContext(orgId);

        if (holdingId != null) {
            if (status != null) {
                return ResponseEntity.ok(batchRepository.findByHoldingIdAndStatus(
                        holdingId, BatchEntity.BatchStatus.valueOf(status.toUpperCase())));
            }
            return ResponseEntity.ok(batchRepository.findByHoldingId(holdingId));
        }

        return ResponseEntity.ok(batchRepository.findAll());
    }

    @GetMapping("/{batchId}")
    @Transactional
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<BatchEntity> getBatch(@PathVariable UUID batchId,
            @RequestHeader(value = "X-Org-Id", required = false) String orgId) {
        setRlsContext(orgId);
        return batchRepository.findById(batchId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping({"", "/"})
    @Transactional
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<BatchEntity> createBatch(@RequestBody Map<String, Object> request) {
        BatchEntity batch = new BatchEntity();
        batch.setName((String) request.get("name"));
        batch.setPeriod((String) request.get("period"));
        batch.setDescription((String) request.get("description"));
        String holdingId = (String) request.get("holding_id");
        if (holdingId == null) holdingId = (String) request.get("holdingId");
        if (holdingId == null) holdingId = (String) request.get("orgId");
        if (holdingId != null) {
            batch.setHoldingId(UUID.fromString(holdingId));
        }
        batch.setCreatedBy((String) request.getOrDefault("created_by", "system"));
        batch.setStatus(BatchEntity.BatchStatus.OPEN);

        // Set RLS context for the INSERT (holding_id must match app.current_org_id)
        if (batch.getHoldingId() != null) {
            setRlsContext(batch.getHoldingId().toString());
        }

        BatchEntity saved = batchRepository.save(batch);
        logger.info("Created batch: {} ({})", saved.getName(), saved.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{batchId}")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<BatchEntity> updateBatch(
            @PathVariable UUID batchId,
            @RequestBody Map<String, Object> request) {

        return batchRepository.findById(batchId)
                .map(batch -> {
                    if (request.containsKey("name")) {
                        batch.setName((String) request.get("name"));
                    }
                    if (request.containsKey("status")) {
                        BatchEntity.BatchStatus newStatus = BatchEntity.BatchStatus.valueOf(
                                ((String) request.get("status")).toUpperCase());
                        batch.setStatus(newStatus);
                        if (newStatus == BatchEntity.BatchStatus.CLOSED) {
                            batch.setClosedAt(Instant.now());
                        }
                    }
                    return ResponseEntity.ok(batchRepository.save(batch));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{batchId}")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Void> deleteBatch(@PathVariable UUID batchId) {
        if (!batchRepository.existsById(batchId)) {
            return ResponseEntity.notFound().build();
        }

        batchRepository.deleteById(batchId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{batchId}/status")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Map<String, Object>> getBatchStatus(@PathVariable UUID batchId) {
        return batchRepository.findById(batchId)
                .map(batch -> {
                    long fileCount = batchFileRepository.countByBatchId(batchId);
                    Map<String, Object> statusMap = new java.util.HashMap<>();
                    statusMap.put("batch_id", batch.getId());
                    statusMap.put("status", batch.getStatus());
                    statusMap.put("file_count", fileCount);
                    statusMap.put("period", batch.getPeriod());
                    return ResponseEntity.ok(statusMap);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ==================== Batch Files ====================

    @GetMapping("/{batchId}/files")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<List<BatchFileEntity>> listBatchFiles(@PathVariable UUID batchId) {
        if (!batchRepository.existsById(batchId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(batchFileRepository.findByBatchId(batchId));
    }

    @PostMapping("/{batchId}/files")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<BatchFileEntity> addFileToBatch(
            @PathVariable UUID batchId,
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {

        if (!batchRepository.existsById(batchId)) {
            return ResponseEntity.notFound().build();
        }

        UUID fileId = UUID.fromString(request.get("file_id"));

        // Check if already assigned
        if (batchFileRepository.findByBatchIdAndFileId(batchId, fileId).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        BatchFileEntity batchFile = new BatchFileEntity();
        batchFile.setBatchId(batchId);
        batchFile.setFileId(fileId);
        batchFile.setAddedBy(userId);

        BatchFileEntity saved = batchFileRepository.save(batchFile);
        logger.info("File {} added to batch {} by {}", fileId, batchId, userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @DeleteMapping("/{batchId}/files/{fileId}")
    @Transactional
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Void> removeFileFromBatch(
            @PathVariable UUID batchId,
            @PathVariable UUID fileId) {

        if (batchFileRepository.findByBatchIdAndFileId(batchId, fileId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        batchFileRepository.deleteByBatchIdAndFileId(batchId, fileId);
        logger.info("File {} removed from batch {}", fileId, batchId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/health")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    private void setRlsContext(String orgId) {
        if (orgId != null && !orgId.isBlank()) {
            try {
                UUID.fromString(orgId);
                entityManager.createNativeQuery("SET LOCAL app.current_org_id = '" + orgId + "'")
                        .executeUpdate();
                entityManager.flush();
            } catch (IllegalArgumentException ignored) {}
        }
    }
}
