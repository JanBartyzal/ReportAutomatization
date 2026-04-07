package com.reportplatform.ing.service;

import com.reportplatform.ing.model.FileEntity;
import com.reportplatform.ing.repository.FileRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Service for querying file metadata with proper RLS context.
 *
 * The files table has FORCE ROW LEVEL SECURITY with a policy that requires
 * app.current_org_id to be set. This service sets the context within a
 * @Transactional boundary so it applies to the repository query on the same connection.
 */
@Service
@Transactional(readOnly = true)
public class FileService {

    @PersistenceContext
    private EntityManager entityManager;

    private final FileRepository fileRepository;

    public FileService(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    public Page<FileEntity> listFiles(UUID orgId, Pageable pageable) {
        setRlsContext(orgId);
        return fileRepository.findByOrgIdOrderByCreatedAtDesc(orgId, pageable);
    }

    public Optional<FileEntity> getFileByIdAndOrg(UUID fileId, UUID orgId) {
        setRlsContext(orgId);
        return fileRepository.findByIdAndOrgId(fileId, orgId);
    }

    private void setRlsContext(UUID orgId) {
        entityManager.createNativeQuery("SELECT set_config('app.current_org_id', :orgId, true)")
                .setParameter("orgId", orgId.toString())
                .getSingleResult();
    }
}
