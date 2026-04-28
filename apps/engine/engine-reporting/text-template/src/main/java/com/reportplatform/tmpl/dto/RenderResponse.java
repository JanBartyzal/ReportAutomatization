package com.reportplatform.tmpl.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Result of a text template render operation.
 * The generated file is stored in Azure Blob Storage; the downloadUrl
 * is a pre-signed URL valid for a limited time.
 */
public record RenderResponse(
        UUID templateId,
        String templateName,
        String outputFormat,
        /** Azure Blob Storage pre-signed download URL. */
        String downloadUrl,
        /** Blob name for direct reference. */
        String blobName,
        Instant generatedAt
) {}
