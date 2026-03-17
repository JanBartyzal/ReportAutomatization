package com.reportplatform.orch.service;

import com.reportplatform.orch.config.ServiceRoutingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * Routes file types to the appropriate atomizer Dapr app-id.
 * <p>
 * After P8 consolidation, all atomizer microservices are merged into a single
 * {@code processor-atomizers} service. The Dapr app-id is resolved from
 * {@link ServiceRoutingConfig}. gRPC service names within the consolidated
 * service remain unchanged.
 * </p>
 */
@Service
public class FileTypeRouter {

    private static final Logger log = LoggerFactory.getLogger(FileTypeRouter.class);

    private static final Set<String> SUPPORTED_FILE_TYPES = Set.of(
            "PPTX", "XLSX", "DOCX", "PDF", "CSV", "JSON", "XML");

    private static final Map<String, String> MIME_TO_EXTENSION = Map.ofEntries(
            Map.entry("application/vnd.openxmlformats-officedocument.presentationml.presentation", "PPTX"),
            Map.entry("application/vnd.ms-powerpoint", "PPTX"),
            Map.entry("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "XLSX"),
            Map.entry("application/vnd.ms-excel", "XLSX"),
            Map.entry("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "DOCX"),
            Map.entry("application/msword", "DOCX"),
            Map.entry("application/pdf", "PDF"),
            Map.entry("text/csv", "CSV"),
            Map.entry("application/json", "JSON"),
            Map.entry("application/xml", "XML"),
            Map.entry("text/xml", "XML")
    );

    private final ServiceRoutingConfig routingConfig;

    public FileTypeRouter(ServiceRoutingConfig routingConfig) {
        this.routingConfig = routingConfig;
    }

    /**
     * Resolves the Dapr app-id for the atomizer that handles the given file type.
     * <p>
     * After consolidation, all file types route to the single
     * {@code processor-atomizers} app-id. The internal gRPC service names
     * (e.g., PptxAtomizerService, ExcelAtomizerService) remain unchanged.
     * </p>
     *
     * @param fileType the file extension (case-insensitive), e.g. "PPTX", "xlsx"
     * @return the Dapr app-id for the atomizer service
     * @throws IllegalArgumentException if the file type is not supported
     */
    public String resolveAtomizerAppId(String fileType) {
        String normalized = fileType.trim();
        // If input looks like a MIME type, map it to an extension first
        if (normalized.contains("/")) {
            String mapped = MIME_TO_EXTENSION.get(normalized.toLowerCase());
            if (mapped != null) {
                normalized = mapped;
            } else {
                log.error("Unsupported MIME type: {}", fileType);
                throw new IllegalArgumentException("Unsupported file type: " + fileType);
            }
        } else {
            normalized = normalized.toUpperCase();
        }
        if (!SUPPORTED_FILE_TYPES.contains(normalized)) {
            log.error("Unsupported file type: {}", fileType);
            throw new IllegalArgumentException("Unsupported file type: " + fileType);
        }
        String appId = routingConfig.processorAtomizers();
        log.debug("Routed file type [{}] to atomizer app-id [{}]", normalized, appId);
        return appId;
    }

    /**
     * Checks whether the given file type is supported by any atomizer.
     *
     * @param fileType the file extension (case-insensitive)
     * @return true if a matching atomizer exists
     */
    public boolean isSupported(String fileType) {
        String normalized = fileType.trim();
        if (normalized.contains("/")) {
            return MIME_TO_EXTENSION.containsKey(normalized.toLowerCase());
        }
        return SUPPORTED_FILE_TYPES.contains(normalized.toUpperCase());
    }
}
