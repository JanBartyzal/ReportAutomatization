package com.reportplatform.dash.repository;

import com.reportplatform.dash.model.DrilldownReportSectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DrilldownReportSectionRepository extends JpaRepository<DrilldownReportSectionEntity, UUID> {

    List<DrilldownReportSectionEntity> findByReportIdAndOrgIdOrderByDisplayOrderAsc(UUID reportId, UUID orgId);

    void deleteByReportIdAndOrgId(UUID reportId, UUID orgId);
}
