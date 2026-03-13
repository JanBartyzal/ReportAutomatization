package com.reportplatform.batch.controller;

import com.reportplatform.batch.model.entity.BatchEntity;
import com.reportplatform.batch.repository.BatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/batches")
public class BatchController {

    private static final Logger logger = LoggerFactory.getLogger(BatchController.class);

    private final BatchRepository batchRepository;

    public BatchController(BatchRepository batchRepository) {
        this.batchRepository = batchRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<List<BatchEntity>> listBatches(
            @RequestParam(required = false) UUID holdingId,
            @RequestParam(required = false) String status) {

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
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<BatchEntity> getBatch(@PathVariable UUID batchId) {
        return batchRepository.findById(batchId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<BatchEntity> createBatch(@RequestBody Map<String, Object> request) {
        BatchEntity batch = new BatchEntity();
        batch.setName((String) request.get("name"));
        batch.setPeriod((String) request.get("period"));
        batch.setDescription((String) request.get("description"));
        batch.setHoldingId(UUID.fromString((String) request.get("holding_id")));
        batch.setCreatedBy((String) request.getOrDefault("created_by", "system"));
        batch.setStatus(BatchEntity.BatchStatus.OPEN);

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
                    int fileCount = 0; // Would query batch_files table
                    Map<String, Object> statusMap = new java.util.HashMap<>();
                    statusMap.put("batch_id", batch.getId());
                    statusMap.put("status", batch.getStatus());
                    statusMap.put("file_count", fileCount);
                    statusMap.put("period", batch.getPeriod());
                    return ResponseEntity.ok(statusMap);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/health")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
