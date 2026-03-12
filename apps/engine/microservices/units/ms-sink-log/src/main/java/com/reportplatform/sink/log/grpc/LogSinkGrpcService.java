package com.reportplatform.sink.log.grpc;

import com.reportplatform.proto.sink.v1.*;
import com.reportplatform.sink.log.service.LogSinkService;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@GrpcService
public class LogSinkGrpcService extends LogSinkServiceGrpc.LogSinkServiceImplBase {
    private static final Logger logger = LoggerFactory.getLogger(LogSinkGrpcService.class);

    private final LogSinkService logSinkService;

    public LogSinkGrpcService(LogSinkService logSinkService) {
        this.logSinkService = logSinkService;
    }

    @Override
    public void appendLog(AppendLogRequest request, StreamObserver<AppendLogResponse> responseObserver) {
        logger.info("Received AppendLog request: fileId={}, step={}", request.getFileId(), request.getStepName());

        try {
            LogSinkService.AppendLogResult result = logSinkService.appendLog(
                    request.getFileId(),
                    request.getWorkflowId(),
                    request.getStepName(),
                    request.getStatus(),
                    request.getDurationMs(),
                    request.getErrorDetail(),
                    request.getMetadataMap());

            AppendLogResponse response = AppendLogResponse.newBuilder()
                    .setLogId(result.logId())
                    .setRecordedAt(result.recordedAt())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("AppendLog failed: {}", e.getMessage(), e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void batchAppendLog(BatchAppendLogRequest request, StreamObserver<AppendLogResponse> responseObserver) {
        logger.info("Received BatchAppendLog request: entries={}", request.getEntriesCount());

        try {
            List<LogSinkService.AppendLogRequest> entries = new ArrayList<>();
            for (AppendLogRequest protoReq : request.getEntriesList()) {
                entries.add(new LogSinkService.AppendLogRequest(
                        protoReq.getFileId(),
                        protoReq.getWorkflowId(),
                        protoReq.getStepName(),
                        protoReq.getStatus(),
                        protoReq.getDurationMs(),
                        protoReq.getErrorDetail(),
                        protoReq.getMetadataMap()));
            }

            List<LogSinkService.AppendLogResult> results = logSinkService.batchAppendLog(entries);

            for (LogSinkService.AppendLogResult result : results) {
                AppendLogResponse response = AppendLogResponse.newBuilder()
                        .setLogId(result.logId())
                        .setRecordedAt(result.recordedAt())
                        .build();
                responseObserver.onNext(response);
            }

            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("BatchAppendLog failed: {}", e.getMessage(), e);
            responseObserver.onError(e);
        }
    }
}
