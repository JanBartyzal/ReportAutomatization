package com.reportplatform.sink.doc.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportplatform.sink.doc.entity.DocumentEntity;
import com.reportplatform.sink.doc.repository.DocumentRepository;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Service layer for document sink operations.
 */
@Service
public class DocumentSinkService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentSinkService.class);
    private static final String EMBEDDING_TOPIC = "document-embedding";

    private final DocumentRepository documentRepository;
    private final ObjectMapper objectMapper;
    private final DaprClient daprClient;

    public DocumentSinkService(DocumentRepository documentRepository, ObjectMapper objectMapper) {
        this.documentRepository = documentRepository;
        this.objectMapper = objectMapper;
        this.daprClient = new DaprClientBuilder().build();
    }

    /**
     * Store document and publish event for embedding generation.
     */
    @Transactional
    public StoreDocumentResult storeDocument(
            String fileId, String orgId, String documentType,
            String content, Map<String, String> metadata) {

        logger.info("Storing document: fileId={}, orgId={}, type={}", fileId, orgId, documentType);

        DocumentEntity entity = new DocumentEntity();
        entity.setFileId(fileId);
        entity.setOrgId(orgId);
        entity.setDocumentType(documentType);
        entity.setContent(content);
        entity.setCreatedAt(OffsetDateTime.now());

        try {
            entity.setMetadata(objectMapper.writeValueAsString(metadata));
        } catch (Exception e) {
            entity.setMetadata("{}");
        }

        entity = documentRepository.save(entity);

        // Publish event for async embedding generation
        publishEmbeddingEvent(entity.getId().toString(), fileId, orgId, content);

        logger.info("Document stored with id={}, embedding queued", entity.getId());

        return new StoreDocumentResult(entity.getId().toString(), true);
    }

    /**
     * Delete document by file ID (Saga compensating action).
     */
    @Transactional
    public int deleteByFileId(String fileId) {
        logger.info("Deleting documents for fileId={}", fileId);
        int deleted = documentRepository.deleteByFileId(fileId);
        logger.info("Deleted {} documents", deleted);
        return deleted;
    }

    private void publishEmbeddingEvent(String documentId, String fileId, String orgId, String content) {
        try {
            Map<String, String> event = Map.of(
                    "documentId", documentId,
                    "fileId", fileId,
                    "orgId", orgId);
            daprClient.publishEvent(
                    "pubsub",
                    EMBEDDING_TOPIC,
                    event,
                    Map.of(Metadata.TOPIC_METADATA_NAME, EMBEDDING_TOPIC)).block();
            logger.info("Published embedding event for documentId={}", documentId);
        } catch (Exception e) {
            logger.error("Failed to publish embedding event: {}", e.getMessage());
        }
    }

    public record StoreDocumentResult(String documentId, boolean embeddingQueued) {
    }
}
