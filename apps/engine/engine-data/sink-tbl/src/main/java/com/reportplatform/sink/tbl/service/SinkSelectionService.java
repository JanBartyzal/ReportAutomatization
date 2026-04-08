package com.reportplatform.sink.tbl.service;

import com.reportplatform.sink.tbl.entity.SinkSelectionEntity;
import com.reportplatform.sink.tbl.repository.SinkSelectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing sink selections for reports and dashboards (FS25).
 */
@Service
public class SinkSelectionService {

    private static final Logger logger = LoggerFactory.getLogger(SinkSelectionService.class);

    private final SinkSelectionRepository selectionRepository;

    public SinkSelectionService(SinkSelectionRepository selectionRepository) {
        this.selectionRepository = selectionRepository;
    }

    @Transactional
    public SinkSelectionEntity upsertSelection(SelectionData data) {
        logger.info("Upserting selection for parsedTableId={}, periodId={}, reportType={}",
                data.parsedTableId(), data.periodId(), data.reportType());

        SinkSelectionEntity entity = selectionRepository
                .findByParsedTableIdAndPeriodIdAndReportType(
                        data.parsedTableId(), data.periodId(), data.reportType())
                .orElseGet(SinkSelectionEntity::new);

        entity.setParsedTableId(data.parsedTableId());
        entity.setOrgId(data.orgId());
        entity.setPeriodId(data.periodId());
        entity.setReportType(data.reportType());
        entity.setSelected(data.selected());
        entity.setPriority(data.priority());
        entity.setSelectedBy(data.selectedBy());
        entity.setSelectedAt(OffsetDateTime.now());
        entity.setNote(data.note());

        SinkSelectionEntity saved = selectionRepository.save(entity);
        logger.info("Selection upserted: id={}, selected={}", saved.getId(), saved.isSelected());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<SinkSelectionEntity> getSelectedSinks(String orgId, String periodId) {
        return selectionRepository.findByOrgIdAndPeriodIdAndSelectedTrueOrderByPriorityAsc(orgId, periodId);
    }

    @Transactional(readOnly = true)
    public List<SinkSelectionEntity> getSelectedSinks(String orgId, String periodId, String reportType) {
        return selectionRepository.findByOrgIdAndPeriodIdAndReportTypeAndSelectedTrueOrderByPriorityAsc(
                orgId, periodId, reportType);
    }

    @Transactional(readOnly = true)
    public List<SinkSelectionEntity> getSelectionsForTable(UUID parsedTableId) {
        return selectionRepository.findByParsedTableId(parsedTableId);
    }

    @Transactional
    public void deleteSelection(UUID selectionId) {
        logger.info("Deleting selection id={}", selectionId);
        selectionRepository.deleteById(selectionId);
    }

    public record SelectionData(
            UUID parsedTableId,
            String orgId,
            String periodId,
            String reportType,
            boolean selected,
            int priority,
            String selectedBy,
            String note) {
    }
}
