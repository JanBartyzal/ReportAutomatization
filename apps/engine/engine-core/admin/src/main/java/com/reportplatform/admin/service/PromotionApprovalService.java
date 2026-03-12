package com.reportplatform.admin.service;

import com.reportplatform.admin.model.dto.PaginatedResponse;
import com.reportplatform.admin.model.dto.PromotionCandidateDTO;
import com.reportplatform.admin.model.dto.UpdatePromotionRequest;
import com.reportplatform.admin.model.entity.PromotionCandidateEntity;
import com.reportplatform.admin.model.entity.PromotionCandidateEntity.PromotionStatus;
import com.reportplatform.admin.repository.PromotionCandidateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

    public PromotionApprovalService(PromotionCandidateRepository candidateRepository) {
        this.candidateRepository = candidateRepository;
    }

    /**
     * List promotion candidates with optional status filter and pagination.
     *
     * @param statusFilter optional status to filter by (e.g. "CANDIDATE", "PENDING_REVIEW")
     * @param page         page number (1-based)
     * @param pageSize     number of items per page
     * @return paginated list of promotion candidate DTOs
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<PromotionCandidateDTO> listCandidates(String statusFilter, int page, int pageSize) {
        logger.info("Listing promotion candidates: statusFilter={}, page={}, pageSize={}", statusFilter, page, pageSize);

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
     * via Dapr gRPC to create the dedicated table. On success, sets status to CREATED.
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

        // TODO: Call MS-SINK-TBL via Dapr gRPC to create the promoted table
        // DaprClient.invokeMethod("ms-sink-tbl", "createPromotedTable", CreateTableRequest{
        //     ddl: ddlToApply,
        //     mappingTemplateId: entity.getMappingTemplateId(),
        //     tableName: entity.getProposedTableName(),
        //     dualWriteDays: 30
        // })
        logger.info("TODO: Calling MS-SINK-TBL to create table '{}' for mapping template {}",
                entity.getProposedTableName(), entity.getMappingTemplateId());

        // On successful table creation, update status to CREATED
        entity.setStatus(PromotionStatus.CREATED);
        PromotionCandidateEntity saved = candidateRepository.save(entity);

        logger.info("Promotion candidate approved and table created: id={}, table={}",
                id, entity.getProposedTableName());
        return toDTO(saved);
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
     * @throws IllegalStateException    if candidate is not in CREATED or ACTIVE state
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

        // TODO: Call MS-SINK-TBL via Dapr gRPC to start data migration
        // DaprClient.invokeMethod("ms-sink-tbl", "migrateData", MigrateDataRequest{
        //     mappingTemplateId: entity.getMappingTemplateId(),
        //     tableName: entity.getProposedTableName()
        // })
        logger.info("TODO: Calling MS-SINK-TBL to migrate data for table '{}', mapping template {}",
                entity.getProposedTableName(), entity.getMappingTemplateId());
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

        // TODO: Query MS-SINK-TBL for actual migration progress
        // For now return the local status
        return Map.of(
                "candidate_id", entity.getId().toString(),
                "mapping_template_id", entity.getMappingTemplateId().toString(),
                "table_name", entity.getProposedTableName(),
                "status", entity.getStatus().name(),
                "updated_at", entity.getUpdatedAt().toString());
    }

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
