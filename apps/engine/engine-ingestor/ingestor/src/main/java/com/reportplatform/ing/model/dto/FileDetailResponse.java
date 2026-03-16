package com.reportplatform.ing.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.reportplatform.ing.model.ScanStatus;
import com.reportplatform.ing.model.UploadPurpose;

import java.time.Instant;
import java.util.UUID;

public record FileDetailResponse(
        @JsonProperty("file_id") UUID fileId,
        @JsonProperty("org_id") UUID orgId,
        @JsonProperty("user_id") UUID userId,
        String filename,
        @JsonProperty("size_bytes") long sizeBytes,
        @JsonProperty("mime_type") String mimeType,
        @JsonProperty("blob_url") String blobUrl,
        String status,
        @JsonProperty("upload_purpose") UploadPurpose uploadPurpose,
        @JsonProperty("uploaded_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt
) {
}
