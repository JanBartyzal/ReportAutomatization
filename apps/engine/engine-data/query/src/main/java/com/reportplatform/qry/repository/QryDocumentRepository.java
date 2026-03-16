package com.reportplatform.qry.repository;

import com.reportplatform.qry.model.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QryDocumentRepository extends JpaRepository<DocumentEntity, UUID> {

    List<DocumentEntity> findByFileId(String fileId);

    List<DocumentEntity> findByFileIdAndDocumentType(String fileId, String documentType);

    Optional<DocumentEntity> findByIdAndOrgId(UUID id, String orgId);
}
