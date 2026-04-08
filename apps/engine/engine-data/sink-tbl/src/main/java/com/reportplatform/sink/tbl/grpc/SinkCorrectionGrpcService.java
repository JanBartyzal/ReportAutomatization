package com.reportplatform.sink.tbl.grpc;

import com.reportplatform.proto.sink.v1.*;
import com.reportplatform.sink.tbl.entity.ExtractionLearningLogEntity;
import com.reportplatform.sink.tbl.entity.SinkCorrectionEntity;
import com.reportplatform.sink.tbl.entity.SinkSelectionEntity;
import com.reportplatform.sink.tbl.repository.ExtractionLearningLogRepository;
import com.reportplatform.sink.tbl.service.SinkCorrectionService;
import com.reportplatform.sink.tbl.service.SinkSelectionService;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * gRPC service implementation for SinkCorrectionService (FS25).
 * Handles corrections, selections, and learning hints via Dapr gRPC.
 */
@GrpcService
public class SinkCorrectionGrpcService extends SinkCorrectionServiceGrpc.SinkCorrectionServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(SinkCorrectionGrpcService.class);
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final SinkCorrectionService correctionService;
    private final SinkSelectionService selectionService;
    private final ExtractionLearningLogRepository learningLogRepository;

    public SinkCorrectionGrpcService(
            SinkCorrectionService correctionService,
            SinkSelectionService selectionService,
            ExtractionLearningLogRepository learningLogRepository) {
        this.correctionService = correctionService;
        this.selectionService = selectionService;
        this.learningLogRepository = learningLogRepository;
    }

    @Override
    public void createCorrection(CreateCorrectionRequest request,
                                  StreamObserver<CreateCorrectionResponse> responseObserver) {
        logger.info("Received CreateCorrection for parsedTableId={}", request.getParsedTableId());

        try {
            Map<String, Object> metadata = new HashMap<>(request.getMetadataMap());

            SinkCorrectionService.CorrectionData data = new SinkCorrectionService.CorrectionData(
                    UUID.fromString(request.getParsedTableId()),
                    request.getOrgId(),
                    request.getRowIndex() >= 0 ? request.getRowIndex() : null,
                    request.getColIndex() >= 0 ? request.getColIndex() : null,
                    request.getOriginalValue(),
                    request.getCorrectedValue(),
                    request.getCorrectionType(),
                    request.getCorrectedBy(),
                    request.getErrorCategory().isEmpty() ? null : request.getErrorCategory(),
                    metadata);

            SinkCorrectionEntity entity = correctionService.createCorrection(data);

            CreateCorrectionResponse response = CreateCorrectionResponse.newBuilder()
                    .setCorrectionId(entity.getId().toString())
                    .setCorrectedAt(entity.getCorrectedAt().format(ISO_FORMATTER))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("CreateCorrection failed: {}", e.getMessage(), e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void createBulkCorrections(CreateBulkCorrectionsRequest request,
                                       StreamObserver<CreateBulkCorrectionsResponse> responseObserver) {
        logger.info("Received CreateBulkCorrections with {} corrections", request.getCorrectionsCount());

        try {
            List<SinkCorrectionService.CorrectionData> corrections = new ArrayList<>();
            for (CreateCorrectionRequest cr : request.getCorrectionsList()) {
                Map<String, Object> metadata = new HashMap<>(cr.getMetadataMap());
                corrections.add(new SinkCorrectionService.CorrectionData(
                        UUID.fromString(cr.getParsedTableId()),
                        cr.getOrgId(),
                        cr.getRowIndex() >= 0 ? cr.getRowIndex() : null,
                        cr.getColIndex() >= 0 ? cr.getColIndex() : null,
                        cr.getOriginalValue(),
                        cr.getCorrectedValue(),
                        cr.getCorrectionType(),
                        cr.getCorrectedBy(),
                        cr.getErrorCategory().isEmpty() ? null : cr.getErrorCategory(),
                        metadata));
            }

            List<SinkCorrectionEntity> entities = correctionService.createBulkCorrections(corrections);

            CreateBulkCorrectionsResponse.Builder builder = CreateBulkCorrectionsResponse.newBuilder()
                    .setCorrectionsCreated(entities.size());
            for (SinkCorrectionEntity e : entities) {
                builder.addCorrectionIds(e.getId().toString());
            }

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("CreateBulkCorrections failed: {}", e.getMessage(), e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void deleteCorrection(DeleteCorrectionRequest request,
                                  StreamObserver<DeleteCorrectionResponse> responseObserver) {
        logger.info("Received DeleteCorrection for id={}", request.getCorrectionId());

        try {
            correctionService.deleteCorrection(UUID.fromString(request.getCorrectionId()));

            responseObserver.onNext(DeleteCorrectionResponse.newBuilder().setDeleted(true).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("DeleteCorrection failed: {}", e.getMessage(), e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void upsertSelection(UpsertSelectionRequest request,
                                 StreamObserver<UpsertSelectionResponse> responseObserver) {
        logger.info("Received UpsertSelection for parsedTableId={}", request.getParsedTableId());

        try {
            SinkSelectionService.SelectionData data = new SinkSelectionService.SelectionData(
                    UUID.fromString(request.getParsedTableId()),
                    request.getOrgId(),
                    request.getPeriodId().isEmpty() ? null : request.getPeriodId(),
                    request.getReportType().isEmpty() ? null : request.getReportType(),
                    request.getSelected(),
                    request.getPriority(),
                    request.getSelectedBy(),
                    request.getNote().isEmpty() ? null : request.getNote());

            SinkSelectionEntity entity = selectionService.upsertSelection(data);

            UpsertSelectionResponse response = UpsertSelectionResponse.newBuilder()
                    .setSelectionId(entity.getId().toString())
                    .setSelected(entity.isSelected())
                    .setSelectedAt(entity.getSelectedAt().format(ISO_FORMATTER))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("UpsertSelection failed: {}", e.getMessage(), e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getLearningHints(GetLearningHintsRequest request,
                                  StreamObserver<GetLearningHintsResponse> responseObserver) {
        logger.info("Received GetLearningHints for sourceType={}", request.getSourceType());

        try {
            List<ExtractionLearningLogEntity> logs;
            if (!request.getErrorCategory().isEmpty()) {
                logs = learningLogRepository.findBySourceTypeAndErrorCategoryOrderByCreatedAtDesc(
                        request.getSourceType(), request.getErrorCategory());
            } else {
                logs = learningLogRepository.findBySourceTypeAndAppliedFalseOrderByCreatedAtDesc(
                        request.getSourceType());
            }

            int maxHints = request.getMaxHints() > 0 ? request.getMaxHints() : 50;
            List<ExtractionLearningLogEntity> limited = logs.stream().limit(maxHints).toList();

            GetLearningHintsResponse.Builder builder = GetLearningHintsResponse.newBuilder()
                    .setTotalAvailable(logs.size());

            for (ExtractionLearningLogEntity log : limited) {
                LearningHint.Builder hint = LearningHint.newBuilder()
                        .setSourceType(log.getSourceType() != null ? log.getSourceType() : "")
                        .setErrorCategory(log.getErrorCategory() != null ? log.getErrorCategory() : "")
                        .setOriginalSnippet(log.getOriginalSnippet() != null ? log.getOriginalSnippet() : "{}")
                        .setCorrectedSnippet(log.getCorrectedSnippet() != null ? log.getCorrectedSnippet() : "{}")
                        .setConfidenceScore(log.getConfidenceScore() != null ? log.getConfidenceScore() : 0f)
                        .setOccurrenceCount(1);
                builder.addHints(hint.build());
            }

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("GetLearningHints failed: {}", e.getMessage(), e);
            responseObserver.onError(e);
        }
    }
}
