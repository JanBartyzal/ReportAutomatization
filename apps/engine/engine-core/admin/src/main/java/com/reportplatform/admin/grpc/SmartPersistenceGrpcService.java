package com.reportplatform.admin.grpc;

import com.reportplatform.admin.service.PromotionApprovalService;
import com.reportplatform.admin.service.PromotionDetectionService;
import com.reportplatform.proto.integration.v1.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * gRPC service implementation for Smart Persistence.
 * <p>
 * Exposes all smart persistence RPCs: get promotion candidates, approve
 * promotion,
 * get routing info, and migrate data.
 * </p>
 */
@GrpcService
public class SmartPersistenceGrpcService extends SmartPersistenceServiceGrpc.SmartPersistenceServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(SmartPersistenceGrpcService.class);

    private final PromotionDetectionService promotionDetectionService;
    private final PromotionApprovalService promotionApprovalService;

    public SmartPersistenceGrpcService(PromotionDetectionService promotionDetectionService,
            PromotionApprovalService promotionApprovalService) {
        this.promotionDetectionService = promotionDetectionService;
        this.promotionApprovalService = promotionApprovalService;
    }

    @Override
    public void getPromotionCandidates(GetCandidatesRequest request,
            StreamObserver<GetCandidatesResponse> responseObserver) {
        try {
            log.info("gRPC GetPromotionCandidates: statusFilter={}", request.getStatusFilter());

            var result = promotionDetectionService.getCandidates(
                    request.getStatusFilter(),
                    request.getPagination().getPage(),
                    request.getPagination().getPageSize());

            GetCandidatesResponse.Builder responseBuilder = GetCandidatesResponse.newBuilder();

            for (var candidate : result.getData()) {
                PromotionCandidate protoCandidate = PromotionCandidate.newBuilder()
                        .setId(candidate.getId().toString())
                        .setMappingTemplateId(candidate.getMappingTemplateId().toString())
                        .setStatus(candidate.getStatus())
                        .setUsageCount(candidate.getUsageCount())
                        .setProposedTableName(candidate.getProposedTableName())
                        .setProposedDdl(candidate.getProposedDdl())
                        .setProposedIndexes(candidate.getProposedIndexes())
                        .setColumnTypeAnalysis(candidate.getColumnTypeAnalysis())
                        .setCreatedAt(candidate.getCreatedAt() != null ? candidate.getCreatedAt().toString() : "")
                        .setReviewedBy(candidate.getReviewedBy() != null ? candidate.getReviewedBy() : "")
                        .setReviewedAt(candidate.getReviewedAt() != null ? candidate.getReviewedAt().toString() : "")
                        .build();
                responseBuilder.addCandidates(protoCandidate);
            }

            responseBuilder.setPagination(com.reportplatform.proto.common.v1.PaginationResponse.newBuilder()
                    .setPage(result.getPage())
                    .setPageSize(result.getPageSize())
                    .setTotalItems(result.getTotalItems())
                    .setTotalPages(result.getTotalPages())
                    .build());

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("GetPromotionCandidates failed: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to get promotion candidates: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void approvePromotion(ApprovePromotionRequest request,
            StreamObserver<ApprovePromotionResponse> responseObserver) {
        try {
            log.info("gRPC ApprovePromotion: promotionId={}", request.getPromotionId());

            var result = promotionApprovalService.approvePromotion(
                    request.getPromotionId(),
                    request.getFinalDdl());

            ApprovePromotionResponse response = ApprovePromotionResponse.newBuilder()
                    .setSuccess(result.success())
                    .setTableName(result.tableName() != null ? result.tableName() : "")
                    .setMessage(result.message())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("ApprovePromotion failed: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to approve promotion: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getRoutingInfo(GetRoutingInfoRequest request,
            StreamObserver<GetRoutingInfoResponse> responseObserver) {
        try {
            log.info("gRPC GetRoutingInfo: mappingTemplateId={}", request.getMappingTemplateId());

            var result = promotionApprovalService.getRoutingInfo(request.getMappingTemplateId());

            GetRoutingInfoResponse response = GetRoutingInfoResponse.newBuilder()
                    .setHasPromotedTable(result.hasPromotedTable())
                    .setTableName(result.tableName() != null ? result.tableName() : "")
                    .setInDualWritePeriod(result.inDualWritePeriod())
                    .setDualWriteUntil(result.dualWriteUntil() != null ? result.dualWriteUntil() : "")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("GetRoutingInfo failed: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to get routing info: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void migrateData(MigrateDataRequest request,
            StreamObserver<MigrateDataResponse> responseObserver) {
        try {
            log.info("gRPC MigrateData: promotionId={}", request.getPromotionId());

            var result = promotionApprovalService.migrateData(request.getPromotionId());

            MigrateDataResponse response = MigrateDataResponse.newBuilder()
                    .setMigrationId(result.migrationId() != null ? result.migrationId() : "")
                    .setStatus(com.reportplatform.proto.common.v1.ProcessingStatus.PROCESSING_STATUS_COMPLETED)
                    .setRecordsMigrated((int) result.recordsMigrated())
                    .setMessage(result.message())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("MigrateData failed: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to migrate data: " + e.getMessage())
                    .asRuntimeException());
        }
    }
}
