package com.reportplatform.sink.doc.repository;

import com.reportplatform.sink.doc.entity.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentEntity, UUID> {
    List<DocumentEntity> findByFileId(String fileId);

    List<DocumentEntity> findByFileIdAndOrgId(String fileId, String orgId);

    @Modifying
    @Query("DELETE FROM DocumentEntity d WHERE d.fileId = :fileId")
    int deleteByFileId(@Param("fileId") String fileId);
}
