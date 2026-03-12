package com.reportplatform.ing.repository;

import com.reportplatform.ing.model.FileEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, UUID> {

    Page<FileEntity> findByOrgIdOrderByCreatedAtDesc(UUID orgId, Pageable pageable);

    Optional<FileEntity> findByIdAndOrgId(UUID id, UUID orgId);
}
