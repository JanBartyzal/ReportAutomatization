package com.reportplatform.qry.repository;

import com.reportplatform.qry.model.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QryDocumentRepository extends JpaRepository<DocumentEntity, UUID> {

    List<DocumentEntity> findByFileId(String fileId);

    List<DocumentEntity> findByFileIdAndDocumentType(String fileId, String documentType);

    /** Finds all SLIDE_TEXT_N documents for a file. Caller should sort by slideIndex. */
    @Query("SELECT d FROM QryDocumentEntity d WHERE d.fileId = :fileId AND d.documentType LIKE 'SLIDE_TEXT_%'")
    List<DocumentEntity> findSlideDocumentsByFileId(@Param("fileId") String fileId);

    Optional<DocumentEntity> findByIdAndOrgId(UUID id, String orgId);
}
