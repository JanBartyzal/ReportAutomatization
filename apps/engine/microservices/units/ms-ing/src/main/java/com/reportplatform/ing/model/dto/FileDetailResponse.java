package com.reportplatform.ing.model.dto;

import com.reportplatform.ing.model.ScanStatus;
import com.reportplatform.ing.model.UploadPurpose;

import java.time.Instant;
import java.util.UUID;

public record FileDetailResponse(
        UUID fileId,
        UUID orgId,
        UUID userId,
        String filename,
        long sizeBytes,
        String mimeType,
        String blobUrl,
        ScanStatus scanStatus,
        UploadPurpose uploadPurpose,
        Instant createdAt,
        Instant updatedAt
) {
}
