package com.reportplatform.ing.model.dto;

import com.reportplatform.ing.model.ScanStatus;
import com.reportplatform.ing.model.UploadPurpose;

import java.time.Instant;
import java.util.UUID;

public record UploadResponse(
        UUID fileId,
        String filename,
        long sizeBytes,
        String mimeType,
        ScanStatus scanStatus,
        UploadPurpose uploadPurpose,
        Instant createdAt
) {
}
