package com.reportplatform.sink.tbl.grpc;

import com.reportplatform.proto.sink.v1.*;
import com.reportplatform.sink.tbl.service.TableSinkService;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * gRPC service implementation for TableSinkService.
 * Handles incoming gRPC requests from MS-ORCH via Dapr sidecar.
 */
@GrpcService
public class TableSinkGrpcService extends TableSinkServiceGrpc.TableSinkServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(TableSinkGrpcService.class);
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final TableSinkService tableSinkService;

    public TableSinkGrpcService(TableSinkService tableSinkService) {
        this.tableSinkService = tableSinkService;
    }

    @Override
    public void bulkInsert(BulkInsertRequest request, StreamObserver<BulkInsertResponse> responseObserver) {
        logger.info("Received BulkInsert request for fileId={}, orgId={}, records={}",
                request.getFileId(), request.getOrgId(), request.getRecordsCount());

        try {
            // Convert proto records to service data objects
            List<TableSinkService.TableRecordData> records = new ArrayList<>();
            for (TableRecord protoRecord : request.getRecordsList()) {
                List<String> headers = new ArrayList<>(protoRecord.getHeadersList());
                List<List<String>> rows = new ArrayList<>();
                for (TableRecordRow row : protoRecord.getRowsList()) {
                    rows.add(new ArrayList<>(row.getCellsList()));
                }
                Map<String, String> metadata = protoRecord.getMetadataMap();

                records.add(new TableSinkService.TableRecordData(
                        protoRecord.getRecordId(),
                        protoRecord.getSourceSheet(),
                        headers,
                        rows,
                        metadata));
            }

            int insertedCount = tableSinkService.bulkInsert(
                    request.getFileId(),
                    request.getOrgId(),
                    request.getSourceType(),
                    records);

            BulkInsertResponse response = BulkInsertResponse.newBuilder()
                    .setRecordsInserted(insertedCount)
                    .setStatus(ProcessingStatus.PROCESSING_STATUS_SUCCESS)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            logger.info("BulkInsert completed: inserted={}", insertedCount);

        } catch (Exception e) {
            logger.error("BulkInsert failed: {}", e.getMessage(), e);

            BulkInsertResponse response = BulkInsertResponse.newBuilder()
                    .setRecordsInserted(0)
                    .setStatus(ProcessingStatus.PROCESSING_STATUS_ERROR)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onError(e);
        }
    }

    @Override
    public void deleteByFileId(DeleteByFileIdRequest request, StreamObserver<DeleteResponse> responseObserver) {
        logger.info("Received DeleteByFileId request for fileId={}", request.getFileId());

        try {
            int deletedCount = tableSinkService.deleteByFileId(request.getFileId());

            DeleteResponse response = DeleteResponse.newBuilder()
                    .setRecordsDeleted(deletedCount)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            logger.info("DeleteByFileId completed: deleted={}", deletedCount);

        } catch (Exception e) {
            logger.error("DeleteByFileId failed: {}", e.getMessage(), e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void storeFormResponse(StoreFormResponseRequest request,
            StreamObserver<StoreFormResponseResponse> responseObserver) {
        logger.info("Received StoreFormResponse request for orgId={}, periodId={}, fields={}",
                request.getOrgId(), request.getPeriodId(), request.getFieldsCount());

        try {
            // Convert proto fields to service data objects
            List<TableSinkService.FormFieldValueData> fields = new ArrayList<>();
            for (FormFieldValue protoField : request.getFieldsList()) {
                fields.add(new TableSinkService.FormFieldValueData(
                        protoField.getFieldId(),
                        protoField.getValue(),
                        protoField.getDataType()));
            }

            String responseId = tableSinkService.storeFormResponse(
                    request.getOrgId(),
                    request.getPeriodId(),
                    request.getFormVersionId(),
                    fields);

            String submittedAt = OffsetDateTime.now().format(ISO_FORMATTER);

            StoreFormResponseResponse response = StoreFormResponseResponse.newBuilder()
                    .setResponseId(responseId)
                    .setSubmittedAt(submittedAt)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            logger.info("StoreFormResponse completed: responseId={}", responseId);

        } catch (Exception e) {
            logger.error("StoreFormResponse failed: {}", e.getMessage(), e);
            responseObserver.onError(e);
        }
    }
}
