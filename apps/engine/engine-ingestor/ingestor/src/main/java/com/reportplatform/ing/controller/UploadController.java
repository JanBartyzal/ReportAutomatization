package com.reportplatform.ing.controller;

import com.reportplatform.ing.model.UploadPurpose;
import com.reportplatform.ing.model.dto.UploadResponse;
import com.reportplatform.ing.service.UploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class UploadController {

    private static final Logger log = LoggerFactory.getLogger(UploadController.class);

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    /**
     * Multipart streaming upload endpoint.
     * <p>
     * The file is streamed to Azure Blob Storage without being fully loaded into memory.
     * After upload, the file undergoes MIME validation, virus scanning, and sanitization.
     *
     * @param file          the uploaded file (multipart)
     * @param uploadPurpose intended purpose: PARSE (default) or FORM_IMPORT
     * @param orgId         organization ID (injected from auth context / header)
     * @param userId        user ID (injected from auth context / header)
     * @return file metadata with file_id and processing status
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "upload_purpose", defaultValue = "PARSE") UploadPurpose uploadPurpose,
            @RequestHeader(value = "X-Org-Id") UUID orgId,
            @RequestHeader(value = "X-User-Id") UUID userId) {

        log.info("Upload request: filename={}, purpose={}, org={}", file.getOriginalFilename(), uploadPurpose, orgId);

        UploadResponse response = uploadService.handleUpload(file, uploadPurpose, orgId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
