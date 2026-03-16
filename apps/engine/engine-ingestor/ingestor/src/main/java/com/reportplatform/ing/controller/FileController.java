package com.reportplatform.ing.controller;

import com.reportplatform.ing.model.FileEntity;
import com.reportplatform.ing.model.dto.FileDetailResponse;
import com.reportplatform.ing.model.dto.FileListResponse;
import com.reportplatform.ing.repository.FileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileRepository fileRepository;

    public FileController(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    /**
     * Returns a paginated list of files for the current organization.
     *
     * @param orgId organization ID from auth context
     * @param page  page number (0-based, default 0)
     * @param size  page size (default 20, max 100)
     * @return paginated file list
     */
    @GetMapping
    public ResponseEntity<FileListResponse> listFiles(
            @RequestHeader(value = "X-Org-Id") UUID orgId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        size = Math.min(size, 100);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<FileEntity> filePage = fileRepository.findByOrgIdOrderByCreatedAtDesc(orgId, pageRequest);

        var files = filePage.getContent().stream()
                .map(this::toDetailResponse)
                .toList();

        var pagination = new FileListResponse.PaginationMeta(
                filePage.getNumber(),
                filePage.getSize(),
                filePage.getTotalElements(),
                filePage.getTotalPages()
        );
        var response = new FileListResponse(files, pagination);

        return ResponseEntity.ok(response);
    }

    /**
     * Returns detail and processing status for a specific file.
     *
     * @param fileId file UUID
     * @param orgId  organization ID from auth context
     * @return file detail with current scan status
     */
    @GetMapping("/{file_id}")
    public ResponseEntity<FileDetailResponse> getFile(
            @PathVariable("file_id") UUID fileId,
            @RequestHeader(value = "X-Org-Id") UUID orgId) {

        FileEntity entity = fileRepository.findByIdAndOrgId(fileId, orgId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND,
                        "File not found: " + fileId));

        return ResponseEntity.ok(toDetailResponse(entity));
    }

    private FileDetailResponse toDetailResponse(FileEntity entity) {
        return new FileDetailResponse(
                entity.getId(),
                entity.getOrgId(),
                entity.getUserId(),
                entity.getFilename(),
                entity.getSizeBytes(),
                entity.getMimeType(),
                entity.getBlobUrl(),
                entity.getScanStatus().name(),
                entity.getUploadPurpose(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
