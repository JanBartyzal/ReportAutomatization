package com.reportplatform.scan.grpc;

import com.reportplatform.proto.scanner.v1.*;
import com.reportplatform.scan.service.SecurityScannerService;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class ScannerGrpcService extends ScannerServiceGrpc.ScannerServiceImplBase {
    private static final Logger logger = LoggerFactory.getLogger(ScannerGrpcService.class);

    private final SecurityScannerService securityScannerService;

    public ScannerGrpcService(SecurityScannerService securityScannerService) {
        this.securityScannerService = securityScannerService;
    }

    @Override
    public void scanFile(ScanFileRequest request, StreamObserver<ScanFileResponse> responseObserver) {
        logger.info("Received ScanFile request for fileId={}", request.getFileId());

        try {
            SecurityScannerService.ScanResult result = securityScannerService.scanFile(
                    request.getFileId(),
                    request.getBlobUrl());

            ScanFileResponse response = ScanFileResponse.newBuilder()
                    .setFileId(request.getFileId())
                    .setResult(convertScanResult(result.status()))
                    .setThreatName(result.threatName() != null ? result.threatName() : "")
                    .setScanDurationMs(result.durationMs())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            logger.info("ScanFile completed: fileId={}, result={}", request.getFileId(), result.status());

        } catch (Exception e) {
            logger.error("ScanFile failed: {}", e.getMessage(), e);

            ScanFileResponse response = ScanFileResponse.newBuilder()
                    .setFileId(request.getFileId())
                    .setResult(ScanResult.SCAN_RESULT_ERROR)
                    .setThreatName("ERROR: " + e.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onError(e);
        }
    }

    @Override
    public void sanitizeFile(SanitizeFileRequest request, StreamObserver<SanitizeFileResponse> responseObserver) {
        logger.info("Received SanitizeFile request for fileId={}", request.getFileId());

        try {
            SecurityScannerService.SanitizeResult result = securityScannerService.sanitizeFile(
                    request.getFileId(),
                    request.getBlobUrl(),
                    request.getMimeType());

            SanitizeFileResponse response = SanitizeFileResponse.newBuilder()
                    .setFileId(request.getFileId())
                    .setSanitizedFile(BlobReference.newBuilder()
                            .setBlobUrl(result.sanitizedBlobUrl())
                            .build())
                    .addAllRemovedItems(result.removedItems())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            logger.info("SanitizeFile completed: fileId={}, removed={}", request.getFileId(), result.removedItems());

        } catch (Exception e) {
            logger.error("SanitizeFile failed: {}", e.getMessage(), e);
            responseObserver.onError(e);
        }
    }

    private ScanResult convertScanResult(SecurityScannerService.ScanStatus status) {
        return switch (status) {
            case SCAN_RESULT_CLEAN -> ScanResult.SCAN_RESULT_CLEAN;
            case SCAN_RESULT_INFECTED -> ScanResult.SCAN_RESULT_INFECTED;
            case SCAN_RESULT_ERROR -> ScanResult.SCAN_RESULT_ERROR;
            default -> ScanResult.SCAN_RESULT_UNSPECIFIED;
        };
    }
}
