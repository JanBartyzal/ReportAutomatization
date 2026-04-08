package com.reportplatform.sink.tbl.repository;

import com.reportplatform.sink.tbl.entity.ExtractionLearningLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for extraction_learning_log table operations (FS25).
 */
public interface ExtractionLearningLogRepository extends JpaRepository<ExtractionLearningLogEntity, UUID> {

    List<ExtractionLearningLogEntity> findBySourceTypeAndAppliedFalseOrderByCreatedAtDesc(String sourceType);

    List<ExtractionLearningLogEntity> findByParsedTableIdOrderByCreatedAtDesc(UUID parsedTableId);

    List<ExtractionLearningLogEntity> findBySourceTypeAndErrorCategoryOrderByCreatedAtDesc(
            String sourceType, String errorCategory);

    long countByAppliedFalse();
}
