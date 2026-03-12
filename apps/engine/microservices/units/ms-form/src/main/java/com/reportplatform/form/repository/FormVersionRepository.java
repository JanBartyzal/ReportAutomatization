package com.reportplatform.form.repository;

import com.reportplatform.form.model.FormVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FormVersionRepository extends JpaRepository<FormVersionEntity, UUID> {

    List<FormVersionEntity> findByFormIdOrderByVersionNumberDesc(UUID formId);

    Optional<FormVersionEntity> findByFormIdAndVersionNumber(UUID formId, int versionNumber);

    Optional<FormVersionEntity> findTopByFormIdOrderByVersionNumberDesc(UUID formId);
}
