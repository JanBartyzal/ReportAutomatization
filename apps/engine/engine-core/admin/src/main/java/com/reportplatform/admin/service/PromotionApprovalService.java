package com.reportplatform.admin.service;

import com.reportplatform.admin.model.dto.*;
import com.reportplatform.admin.model.entity.PromotionCandidateEntity;
import com.reportplatform.admin.model.entity.PromotionCandidateEntity.PromotionStatus;
import com.reportplatform.admin.repository.PromotionCandidateRepository;
import com.reportplatform.base.dapr.DaprClientWrapper;
import io.dapr.client.domain.HttpExtension;
import io.dapr.utils.TypeRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing the promotion approval workflow.
 * Handles listing, reviewing, approving/rejecting promotion candidates,
 * and coordinating with MS-SINK-TBL for table creation and data migration.
 */
@Service
public class PromotionApprovalService {

    private static final Logger logger = LoggerFactory.getLogger(PromotionApprovalService.class);

    private final PromotionCandidateRepository candidateRepository;
    private final DaprClientWrapper daprClientWrapper;

    @Value("${dapr.remote.ms-sink-tbl-app-id:ms-sink-tbl}")
    private String msSinkTblAppId;

    public PromotionApprovalService(PromotionCandidateRepository candidateRepository,
                                     DaprClientWrapper daprClientWrapper) {
        this.candidateRepository = candidateRepository;
        this.daprClientWrapper = daprClientWrapper;
    }

    /**
     * List promotion candidates with optional status filter and pagination.
     *
     * @param statusFilter optional status to filter by (e.g. "CANDIDATE",
     *                     "PENDING_REVIEW")
     * @param page         page number (1-based)
     * @param pageSize     number of items per page
     * @return paginated list of promotion candidate DTOs
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<PromotionCandidateDTO> listCandidates(String statusFilter, int page, int pageSize) {
        logger.info("Listing promotion candidates: statusFilter={}, page={}, pageSize={}", statusFilter, page,
                pageSize);

        PageRequest pageRequest = PageRequest.of(
                Math.max(0, page - 1),
                pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<PromotionCandidateEntity> entityPage;

        if (statusFilter != null && !statusFilter.isBlank()) {
            try {
                PromotionStatus status = PromotionStatus.valueOf(statusFilter.toUpperCase());
                List<PromotionCandidateEntity> filtered = candidateRepository.findByStatus(status);
                // Manual pagination for filtered results
                int start = (int) pageRequest.getOffset();
                int end = Math.min(start + pageSize, filtered.size());
                List<PromotionCandidateEntity> pageContent = start < filtered.size()
                        ? filtered.subList(start, end)
                        : List.of();

                PaginatedResponse<PromotionCandidateDTO> response = new PaginatedResponse<>();
                response.setPage(page);
                response.setPageSize(pageSize);
                response.setTotalItems(filtered.size());
                response.setTotalPages((int) Math.ceil((double) filtered.size() / pageSize));
                response.setData(pageContent.stream().map(this::toDTO).collect(Collectors.toList()));
                return response;
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid status filter: {}", statusFilter);
            }
        }

        entityPage = candidateRepository.findAll(pageRequest);

        PaginatedResponse<PromotionCandidateDTO> response = new PaginatedResponse<>();
        response.setPage(page);
        response.setPageSize(pageSize);
        response.setTotalItems((int) entityPage.getTotalElements());
        response.setTotalPages(entityPage.getTotalPages());
        response.setData(entityPage.getContent().stream().map(this::toDTO).collect(Collectors.toList()));
        return response;
    }

    /**
     * Get a specific promotion candidate by ID.
     *
     * @param id the candidate UUID
     * @return the promotion candidate DTO
     * @throws IllegalArgumentException if not found
     */
    @Transactional(readOnly = true)
    public PromotionCandidateDTO getCandidate(UUID id) {
        logger.info("Getting promotion candidate: id={}", id);
        PromotionCandidateEntity entity = candidateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promotion candidate not found: " + id));
        return toDTO(entity);
    }

    /**
     * Update the DDL of a promotion candidate and set status to PENDING_REVIEW.
     *
     * @param id      the candidate UUID
     * @param request the update request containing modified DDL
     * @return the updated promotion candidate DTO
     * @throws IllegalArgumentException if not found
     */
    @Transactional
    public PromotionCandidateDTO updateCandidate(UUID id, UpdatePromotionRequest request) {
        logger.info("Updating promotion candidate DDL: id={}", id);

        PromotionCandidateEntity entity = candidateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promotion candidate not found: " + id));

        if (request.getFinalDdl() != null) {
            entity.setFinalDdl(request.getFinalDdl());
        }
        if (request.getProposedIndexes() != null) {
            entity.setProposedIndexes(request.getProposedIndexes());
        }

        entity.setStatus(PromotionStatus.PENDING_REVIEW);
        PromotionCandidateEntity saved = candidateRepository.save(entity);

        logger.info("Updated promotion candidate: id={}, status={}", id, saved.getStatus());
        return toDTO(saved);
    }

    /**
     * Approve a promotion candidate and trigger table creation in MS-SINK-TBL.
     *
     * Sets status to APPROVED, records reviewer info, then calls MS-SINK-TBL
     * via Dapr gRPC to create the dedicated table. On success, sets status to
     * CREATED.
     *
     * @param id         the candidate UUID
     * @param reviewedBy the user ID of the reviewer
     * @return the approved promotion candidate DTO
     * @throws IllegalArgumentException if not found
     * @throws IllegalStateException    if candidate is not in a reviewable state
     */
    @Transactional
    public PromotionCandidateDTO approveCandidate(UUID id, String reviewedBy) {
        logger.info("Approving promotion candidate: id={}, reviewedBy={}", id, reviewedBy);

        PromotionCandidateEntity entity = candidateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promotion candidate not found: " + id));

        if (entity.getStatus() != PromotionStatus.CANDIDATE
                && entity.getStatus() != PromotionStatus.PENDING_REVIEW) {
            throw new IllegalStateException(
                    "Cannot approve candidate in status: " + entity.getStatus());
        }

        // Set approval metadata
        entity.setStatus(PromotionStatus.APPROVED);
        entity.setReviewedBy(reviewedBy);
        entity.setReviewedAt(Instant.now());
        candidateRepository.save(entity);

        // Determine the DDL to apply (finalDdl takes precedence over proposedDdl)
        String ddlToApply = entity.getFinalDdl() != null ? entity.getFinalDdl() : entity.getProposedDdl();

        // Call MS-SINK-TBL via Dapr to create the promoted table
        callCreatePromotedTable(ddlToApply, entity);

        // On successful table creation, update status to CREATED
        entity.setStatus(PromotionStatus.CREATED);
        PromotionCandidateEntity saved = candidateRepository.save(entity);

        logger.info("Promotion candidate approved and table created: id={}, table={}",
                id, entity.getProposedTableName());
        return toDTO(saved);
    }

    /**
     * Approve promotion with final DDL override.
     * Called from gRPC service.
     *
     * @param promotionId the candidate UUID as string
     * @param finalDdl    optional final DDL override
     * @return result with success status and message
     */
    @Transactional
    public ApprovalResult approvePromotion(String promotionId, String finalDdl) {
        logger.info("Approving promotion: promotionId={}, finalDdlProvided={}", promotionId, finalDdl != null);

        UUID id;
        try {
            id = UUID.fromString(promotionId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid promotion ID format: " + promotionId);
        }

        PromotionCandidateEntity entity = candidateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promotion candidate not found: " + promotionId));

        if (entity.getStatus() != PromotionStatus.CANDIDATE
                && entity.getStatus() != PromotionStatus.PENDING_REVIEW) {
            return new ApprovalResult(false, null, "Cannot approve candidate in status: " + entity.getStatus());
        }

        // Apply final DDL if provided
        if (finalDdl != null && !finalDdl.isBlank()) {
            entity.setFinalDdl(finalDdl);
        }

        // Determine the DDL to apply
        String ddlToApply = entity.getFinalDdl() != null ? entity.getFinalDdl() : entity.getProposedDdl();

        // Set approval metadata
        entity.setStatus(PromotionStatus.APPROVED);
        entity.setReviewedBy("system");
        entity.setReviewedAt(Instant.now());
        candidateRepository.save(entity);

        // Call MS-SINK-TBL via Dapr to create the promoted table
        callCreatePromotedTable(ddlToApply, entity);

        // On successful table creation, update status to CREATED
        entity.setStatus(PromotionStatus.CREATED);
        candidateRepository.save(entity);

        logger.info("Promotion approved and table created: id={}, table={}", promotionId,
                entity.getProposedTableName());
        return new ApprovalResult(true, entity.getProposedTableName(), "Promotion approved successfully");
    }

    /**
     * Result record for approvePromotion operation.
     */
    public record ApprovalResult(boolean success, String tableName, String message) {
    }

    /**
     * Get routing information for a mapping template.
     * Determines if there's a promoted table and dual-write status.
     *
     * @param mappingTemplateId the mapping template UUID as string
     * @return routing info result
     */
    @Transactional(readOnly = true)
    public RoutingInfoResult getRoutingInfo(String mappingTemplateId) {
        logger.info("Getting routing info for mapping template: {}", mappingTemplateId);

        UUID templateId;
        try {
            templateId = UUID.fromString(mappingTemplateId);
        } catch (IllegalArgumentException e) {
            return new RoutingInfoResult(false, null, false, null);
        }

        // Find if there's an active promoted table for this mapping
        List<PromotionCandidateEntity> candidates = candidateRepository
                .findByMappingTemplateIdAndStatusIn(templateId, EnumSet.of(
                        PromotionStatus.CREATED,
                        PromotionStatus.MIGRATING,
                        PromotionStatus.ACTIVE));

        if (candidates.isEmpty()) {
            return new RoutingInfoResult(false, null, false, null);
        }

        PromotionCandidateEntity candidate = candidates.get(0);
        boolean inDualWrite = candidate.getStatus() == PromotionStatus.CREATED
                || candidate.getStatus() == PromotionStatus.MIGRATING;

        // Query MS-SINK-TBL for actual dual-write end date
        String dualWriteUntil = null;
        if (inDualWrite) {
            try {
                DualWriteInfoResponse dualWriteInfo = daprClientWrapper.invokeMethod(
                        msSinkTblAppId,
                        "/api/v1/tables/" + candidate.getProposedTableName() + "/dual-write-info",
                        HttpExtension.GET,
                        new TypeRef<DualWriteInfoResponse>() {})
                        .block();
                if (dualWriteInfo != null) {
                    dualWriteUntil = dualWriteInfo.dualWriteUntil();
                }
            } catch (Exception e) {
                logger.warn("Failed to query dual-write info from MS-SINK-TBL for table '{}': {}",
                        candidate.getProposedTableName(), e.getMessage());
            }
        }

        return new RoutingInfoResult(true, candidate.getProposedTableName(), inDualWrite, dualWriteUntil);
    }

    /**
     * Result record for getRoutingInfo operation.
     */
    public record RoutingInfoResult(boolean hasPromotedTable, String tableName,
            boolean inDualWritePeriod, String dualWriteUntil) {
    }

    /**
     * Migrate data for a promotion candidate.
     * Called from gRPC service.
     *
     * @param promotionId the candidate UUID as string
     * @return migration result
     */
    @Transactional
    public MigrationResult migrateData(String promotionId) {
        logger.info("Migrating data for promotion: {}", promotionId);

        UUID id;
        try {
            id = UUID.fromString(promotionId);
        } catch (IllegalArgumentException e) {
            return new MigrationResult(null, 0, "Invalid promotion ID format: " + promotionId);
        }

        PromotionCandidateEntity entity = candidateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promotion candidate not found: " + promotionId));

        if (entity.getStatus() != PromotionStatus.CREATED
                && entity.getStatus() != PromotionStatus.ACTIVE) {
            return new MigrationResult(null, 0, "Cannot migrate data for candidate in status: " + entity.getStatus());
        }

        entity.setStatus(PromotionStatus.MIGRATING);
        candidateRepository.save(entity);

        // Call MS-SINK-TBL via Dapr to start data migration
        MigrateDataResponse migrateResponse = callMigrateData(entity);

        logger.info("Migration started for promotion: {}, migrationId={}", promotionId,
                migrateResponse.migrationId());

        return new MigrationResult(migrateResponse.migrationId(), migrateResponse.recordsMigrated(),
                migrateResponse.message());
    }

    /**
     * Result record for migrateData operation.
     */
    public record MigrationResult(String migrationId, long recordsMigrated, String message) {
    }

    /**
     * Reject a promotion candidate.
     *
     * @param id         the candidate UUID
     * @param reviewedBy the user ID of the reviewer
     * @throws IllegalArgumentException if not found
     */
    @Transactional
    public void rejectCandidate(UUID id, String reviewedBy) {
        logger.info("Rejecting promotion candidate: id={}, reviewedBy={}", id, reviewedBy);

        PromotionCandidateEntity entity = candidateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promotion candidate not found: " + id));

        entity.setStatus(PromotionStatus.REJECTED);
        entity.setReviewedBy(reviewedBy);
        entity.setReviewedAt(Instant.now());
        candidateRepository.save(entity);

        logger.info("Promotion candidate rejected: id={}", id);
    }

    /**
     * Trigger historical data migration from JSONB to the promoted table.
     * Sets status to MIGRATING and calls MS-SINK-TBL to start the migration.
     *
     * @param id the candidate UUID
     * @throws IllegalArgumentException if not found
     * @throws IllegalStateException    if candidate is not in CREATED or ACTIVE
     *                                  state
     */
    @Transactional
    public void triggerMigration(UUID id) {
        logger.info("Triggering migration for promotion candidate: id={}", id);

        PromotionCandidateEntity entity = candidateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promotion candidate not found: " + id));

        if (entity.getStatus() != PromotionStatus.CREATED
                && entity.getStatus() != PromotionStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Cannot trigger migration for candidate in status: " + entity.getStatus());
        }

        entity.setStatus(PromotionStatus.MIGRATING);
        candidateRepository.save(entity);

        // Call MS-SINK-TBL via Dapr to start data migration
        callMigrateData(entity);

        logger.info("Migration triggered for promotion candidate: id={}, table={}",
                id, entity.getProposedTableName());
    }

    /**
     * Get migration progress for a promotion candidate.
     *
     * @param id the candidate UUID
     * @return map containing status and progress information
     * @throws IllegalArgumentException if not found
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getMigrationStatus(UUID id) {
        logger.info("Getting migration status for promotion candidate: id={}", id);

        PromotionCandidateEntity entity = candidateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promotion candidate not found: " + id));

        Map<String, Object> result = new HashMap<>();
        result.put("candidate_id", entity.getId().toString());
        result.put("mapping_template_id", entity.getMappingTemplateId().toString());
        result.put("table_name", entity.getProposedTableName());
        result.put("status", entity.getStatus().name());
        result.put("updated_at", entity.getUpdatedAt().toString());

        // Query MS-SINK-TBL for actual migration progress
        try {
            MigrationProgressResponse progress = daprClientWrapper.invokeMethod(
                    msSinkTblAppId,
                    "/api/v1/tables/" + entity.getProposedTableName() + "/migration-progress",
                    HttpExtension.GET,
                    new TypeRef<MigrationProgressResponse>() {})
                    .block();
            if (progress != null) {
                result.put("records_migrated", progress.recordsMigrated());
                result.put("total_records", progress.totalRecords());
                result.put("progress_percent", progress.progressPercent());
                result.put("migration_status", progress.status());
            }
        } catch (Exception e) {
            logger.warn("Failed to get migration progress from MS-SINK-TBL for table '{}': {}",
                    entity.getProposedTableName(), e.getMessage());
        }

        return result;
    }

    // ==================== Private Helpers ====================

    /**
     * Call MS-SINK-TBL to create a promoted table.
     */
    private void callCreatePromotedTable(String ddl, PromotionCandidateEntity entity) {
        CreatePromotedTableRequest request = new CreatePromotedTableRequest(
                ddl, entity.getMappingTemplateId(), entity.getProposedTableName(), 30);

        CreatePromotedTableResponse response = daprClientWrapper.invokeMethod(
                msSinkTblAppId,
                "/api/v1/tables/create-promoted",
                request,
                HttpExtension.POST,
                new TypeRef<CreatePromotedTableResponse>() {})
                .block();

        if (response == null || !response.success()) {
            throw new RuntimeException("Failed to create promoted table '" + entity.getProposedTableName()
                    + "': " + (response != null ? response.message() : "null response from MS-SINK-TBL"));
        }

        logger.info("Created promoted table '{}' via MS-SINK-TBL", entity.getProposedTableName());
    }

    /**
     * Call MS-SINK-TBL to start data migration.
     */
    private MigrateDataResponse callMigrateData(PromotionCandidateEntity entity) {
        MigrateDataRequest request = new MigrateDataRequest(
                entity.getMappingTemplateId(), entity.getProposedTableName());

        MigrateDataResponse response = daprClientWrapper.invokeMethod(
                msSinkTblAppId,
                "/api/v1/tables/migrate",
                request,
                HttpExtension.POST,
                new TypeRef<MigrateDataResponse>() {})
                .block();

        if (response == null) {
            throw new RuntimeException("Null response from MS-SINK-TBL when migrating data for table '"
                    + entity.getProposedTableName() + "'");
        }

        logger.info("Migration started for table '{}': migrationId={}", entity.getProposedTableName(),
                response.migrationId());
        return response;
    }

    // ==================== Mapping Helpers ====================

    /**
     * Convert a PromotionCandidateEntity to a PromotionCandidateDTO.
     */
    private PromotionCandidateDTO toDTO(PromotionCandidateEntity entity) {
        PromotionCandidateDTO dto = new PromotionCandidateDTO();
        dto.setId(entity.getId());
        dto.setMappingTemplateId(entity.getMappingTemplateId());
        dto.setOrgId(entity.getOrgId());
        dto.setStatus(entity.getStatus().name());
        dto.setUsageCount(entity.getUsageCount());
        dto.setProposedTableName(entity.getProposedTableName());
        dto.setProposedDdl(entity.getProposedDdl());
        dto.setProposedIndexes(entity.getProposedIndexes());
        dto.setColumnTypeAnalysis(entity.getColumnTypeAnalysis());
        dto.setReviewedBy(entity.getReviewedBy());
        dto.setReviewedAt(entity.getReviewedAt());
        dto.setFinalDdl(entity.getFinalDdl());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
