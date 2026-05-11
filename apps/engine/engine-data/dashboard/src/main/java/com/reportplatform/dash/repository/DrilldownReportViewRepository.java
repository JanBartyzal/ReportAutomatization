package com.reportplatform.dash.repository;

import com.reportplatform.dash.model.DrilldownReportViewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DrilldownReportViewRepository extends JpaRepository<DrilldownReportViewEntity, UUID> {
}
