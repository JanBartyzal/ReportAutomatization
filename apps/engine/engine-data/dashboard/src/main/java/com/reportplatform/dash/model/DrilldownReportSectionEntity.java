package com.reportplatform.dash.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "drilldown_report_sections")
public class DrilldownReportSectionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "report_id", nullable = false)
    private UUID reportId;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "section_key", nullable = false, length = 100)
    private String sectionKey;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "component_type", nullable = false, length = 30)
    private String componentType;

    @Column(name = "source_type", nullable = false, length = 30)
    private String sourceType;

    @Column(name = "source_ref_id")
    private UUID sourceRefId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "query_config", columnDefinition = "jsonb", nullable = false)
    private String queryConfig = "{}";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "drill_config", columnDefinition = "jsonb", nullable = false)
    private String drillConfig = "{}";

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    public UUID getId() {
        return id;
    }

    public UUID getReportId() {
        return reportId;
    }

    public void setReportId(UUID reportId) {
        this.reportId = reportId;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public void setOrgId(UUID orgId) {
        this.orgId = orgId;
    }

    public String getSectionKey() {
        return sectionKey;
    }

    public void setSectionKey(String sectionKey) {
        this.sectionKey = sectionKey;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getComponentType() {
        return componentType;
    }

    public void setComponentType(String componentType) {
        this.componentType = componentType;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public UUID getSourceRefId() {
        return sourceRefId;
    }

    public void setSourceRefId(UUID sourceRefId) {
        this.sourceRefId = sourceRefId;
    }

    public String getQueryConfig() {
        return queryConfig;
    }

    public void setQueryConfig(String queryConfig) {
        this.queryConfig = queryConfig;
    }

    public String getDrillConfig() {
        return drillConfig;
    }

    public void setDrillConfig(String drillConfig) {
        this.drillConfig = drillConfig;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
}
