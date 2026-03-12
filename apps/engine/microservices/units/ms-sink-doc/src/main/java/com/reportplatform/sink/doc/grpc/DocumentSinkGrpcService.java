package com.reportplatform.sink.doc.grpc;

import com.reportplatform.proto.sink.v1.*;
import com.reportplatform.sink.doc.service.DocumentSinkService;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * gRPC service implementation for DocumentSinkService.
 */
@GrpcService
public class DocumentSinkGrpcService extends DocumentSinkServiceGrpc.DocumentSinkServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(DocumentSinkGrpcService.class);

    private final DocumentSinkService documentSinkService;

    public DocumentSinkGrpcService(DocumentSinkService documentSinkService) {
        this.documentSinkService = documentSinkService;
    }

    @Override
    public void storeDocument(StoreDocumentRequest request, StreamObserver<StoreDocumentResponse> responseObserver) {
        logger.info("Received StoreDocument request for fileId={}, orgId={}", request.getFileId(), request.getOrgId());

        try {
            Map<String, String> metadata = request.getMetadataMap();

            DocumentSinkService.StoreDocumentResult result = documentSinkService.storeDocument(
                    request.getFileId(),
                    request.getOrgId(),
                    request.getDocumentType(),
                    request.getContent(),
                    metadata);

            StoreDocumentResponse response = StoreDocumentResponse.newBuilder()
                    .setDocumentId(result.documentId())
                    .setEmbeddingQueued(result.embeddingQueued())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            logger.info("StoreDocument completed: documentId={}", result.documentId());

        } catch (Exception e) {
            logger.error("StoreDocument failed: {}", e.getMessage(), e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void deleteByFileId(DeleteByFileIdRequest request, StreamObserver<DeleteResponse> responseObserver) {
        logger.info("Received DeleteByFileId request for fileId={}", request.getFileId());

        try {
            int deletedCount = documentSinkService.deleteByFileId(request.getFileId());

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
}
