package com.reportplatform.template.tmpl.grpc;

import com.reportplatform.proto.integration.v1.*;
import com.reportplatform.template.tmpl.entity.PromotedTableEntity;
import com.reportplatform.template.tmpl.service.PromotionService;
import com.reportplatform.template.tmpl.service.PromotionService.PromotionCandidate;
import com.reportplatform.template.tmpl.service.PromotionService.RoutingInfo;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

/**
 * gRPC service implementation for Smart Persistence Promotion (FS24).
 * Handles promotion candidate detection, approval, routing info, and data migration.
 */
@GrpcService
public class SmartPersistenceGrpcService extends SmartPersistenceServiceGrpc.SmartPersistenceServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(SmartPersistenceGrpcService.class);

    private final PromotionService promotionService;

    public SmartPersistenceGrpcService(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @Override
    public void getPromotionCandidates(GetCandidatesRequest request,
                                        StreamObserver<GetCandidatesResponse> responseObserver) {
        logger.info("Received GetPromotionCandidates request");

        try {
            long threshold = 5; // default
            List<PromotionCandidate> candidates = promotionService.getCandidates(threshold);

            GetCandidatesResponse.Builder responseBuilder = GetCandidatesResponse.newBuilder();

            for (PromotionCandidate candidate : candidates) {
                responseBuilder.addCandidates(com.reportplatform.proto.integration.v1.PromotionCandidate.newBuilder()
                        .setMappingTemplateId(candidate.mappingTemplateId().toString())
                        .setStatus("CANDIDATE")
                        .setUsageCount(candidate.usageCount())
                        .setProposedTableName(candidate.proposedTableName())
                        .setProposedDdl(candidate.proposedDdl())
                        .build());
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

            logger.info("GetPromotionCandidates completed: {} candidates", candidates.size());

        } catch (Exception e) {
            logger.error("GetPromotionCandidates failed: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void approvePromotion(ApprovePromotionRequest request,
                                  StreamObserver<ApprovePromotionResponse> responseObserver) {
        logger.info("Received ApprovePromotion request: promotionId={}", request.getPromotionId());

        try {
            UUID mappingTemplateId = UUID.fromString(request.getPromotionId());
            String userId = request.getContext().getUserId();
            String finalDdl = request.getFinalDdl().isBlank() ? null : request.getFinalDdl();

            PromotedTableEntity result = promotionService.approvePromotion(mappingTemplateId, finalDdl, userId);

            ApprovePromotionResponse response = ApprovePromotionResponse.newBuilder()
                    .setSuccess(true)
                    .setTableName(result.getTableName())
                    .setMessage("Table created successfully: " + result.getTableName())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("ApprovePromotion failed: {}", e.getMessage(), e);
            responseObserver.onNext(ApprovePromotionResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Failed: " + e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getRoutingInfo(GetRoutingInfoRequest request,
                                StreamObserver<GetRoutingInfoResponse> responseObserver) {
        logger.info("Received GetRoutingInfo request: mappingTemplateId={}",
                request.getMappingTemplateId());

        try {
            UUID mappingTemplateId = UUID.fromString(request.getMappingTemplateId());
            RoutingInfo info = promotionService.getRoutingInfo(mappingTemplateId);

            GetRoutingInfoResponse.Builder builder = GetRoutingInfoResponse.newBuilder()
                    .setHasPromotedTable(info.hasPromotedTable())
                    .setInDualWritePeriod(info.inDualWritePeriod());

            if (info.tableName() != null) {
                builder.setTableName(info.tableName());
            }
            if (info.dualWriteUntil() != null) {
                builder.setDualWriteUntil(info.dualWriteUntil());
            }

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("GetRoutingInfo failed: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void migrateData(MigrateDataRequest request,
                             StreamObserver<MigrateDataResponse> responseObserver) {
        logger.info("Received MigrateData request: promotionId={}", request.getPromotionId());

        try {
            UUID promotionId = UUID.fromString(request.getPromotionId());
            int count = promotionService.migrateData(promotionId);

            MigrateDataResponse response = MigrateDataResponse.newBuilder()
                    .setMigrationId(UUID.randomUUID().toString())
                    .setStatus(com.reportplatform.proto.common.v1.ProcessingStatus.PROCESSING_STATUS_COMPLETED)
                    .setRecordsMigrated(count)
                    .setMessage("Migration completed: " + count + " records")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("MigrateData failed: {}", e.getMessage(), e);
            responseObserver.onNext(MigrateDataResponse.newBuilder()
                    .setStatus(com.reportplatform.proto.common.v1.ProcessingStatus.PROCESSING_STATUS_FAILED)
                    .setMessage("Migration failed: " + e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }
}
