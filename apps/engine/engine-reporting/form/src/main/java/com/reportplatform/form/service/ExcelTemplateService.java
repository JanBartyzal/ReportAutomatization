package com.reportplatform.form.service;

import com.reportplatform.form.exception.FormNotFoundException;
import com.reportplatform.form.model.FormFieldEntity;
import com.reportplatform.form.repository.FormFieldRepository;
import com.reportplatform.form.repository.FormRepository;
import com.reportplatform.form.repository.FormVersionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFDataFormat;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ExcelTemplateService {

    private static final Logger log = LoggerFactory.getLogger(ExcelTemplateService.class);

    @PersistenceContext
    private EntityManager entityManager;

    private final FormRepository formRepository;
    private final FormVersionRepository formVersionRepository;
    private final FormFieldRepository formFieldRepository;

    public ExcelTemplateService(FormRepository formRepository,
                                FormVersionRepository formVersionRepository,
                                FormFieldRepository formFieldRepository) {
        this.formRepository = formRepository;
        this.formVersionRepository = formVersionRepository;
        this.formFieldRepository = formFieldRepository;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public byte[] generateTemplate(UUID formId, String orgId) throws IOException {
        if (orgId != null && !orgId.isBlank()) {
            try {
                java.util.UUID.fromString(orgId);
                entityManager.createNativeQuery("SELECT set_config('app.current_org_id', :orgId, true)")
                        .setParameter("orgId", orgId)
                        .getSingleResult();
            } catch (IllegalArgumentException e) {
                log.warn("Invalid orgId for RLS context: {}", orgId);
            }
        }

        var form = formRepository.findById(formId)
                .orElseThrow(() -> new FormNotFoundException(formId));

        var version = formVersionRepository.findTopByFormIdOrderByVersionNumberDesc(formId)
                .orElseThrow(() -> new FormNotFoundException(formId));

        var fields = formFieldRepository.findByFormVersionIdOrderBySortOrder(version.getId());

        if (fields.isEmpty()) {
            log.warn("Form {} version {} has no fields — generating empty template", formId, version.getVersionNumber());
        }

        // Group fields by section
        var fieldsBySection = fields.stream()
                .collect(Collectors.groupingBy(
                        f -> f.getSection() != null ? f.getSection() : "General",
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        try (var workbook = new XSSFWorkbook()) {
            // Create one sheet per section
            for (var entry : fieldsBySection.entrySet()) {
                createSectionSheet(workbook, entry.getKey(), entry.getValue());
            }

            // Create hidden metadata sheet
            createMetadataSheet(workbook, formId, version.getId());

            var out = new ByteArrayOutputStream();
            workbook.write(out);
            log.info("Generated Excel template for form {} version {}", formId, version.getVersionNumber());
            return out.toByteArray();
        }
    }

    private void createSectionSheet(XSSFWorkbook workbook, String sectionName, List<FormFieldEntity> fields) {
        var sheet = workbook.createSheet(sectionName);
        var headerRow = sheet.createRow(0);

        XSSFDataFormat format = workbook.createDataFormat();

        for (int i = 0; i < fields.size(); i++) {
            var field = fields.get(i);

            // Header
            var headerCell = headerRow.createCell(i);
            headerCell.setCellValue(field.getLabel());

            // Set column width
            sheet.setColumnWidth(i, Math.max(field.getLabel().length() * 256, 4000));

            // Apply data validation based on field type
            applyDataValidation(workbook, sheet, field, i, format);
        }
    }

    @SuppressWarnings("unchecked")
    private void applyDataValidation(XSSFWorkbook workbook, XSSFSheet sheet,
                                      FormFieldEntity field, int colIdx,
                                      XSSFDataFormat format) {
        DataValidationHelper dvHelper = sheet.getDataValidationHelper();
        CellRangeAddressList range = new CellRangeAddressList(1, 1000, colIdx, colIdx);
        var props = field.getProperties() != null ? field.getProperties() : Map.of();

        switch (field.getFieldType()) {
            case "number" -> {
                if (props.containsKey("min") && props.containsKey("max")) {
                    var constraint = dvHelper.createNumericConstraint(
                            DataValidationConstraint.ValidationType.DECIMAL,
                            DataValidationConstraint.OperatorType.BETWEEN,
                            String.valueOf(props.get("min")),
                            String.valueOf(props.get("max"))
                    );
                    var validation = dvHelper.createValidation(constraint, range);
                    validation.setShowErrorBox(true);
                    validation.createErrorBox("Invalid value",
                            field.getLabel() + " must be between " + props.get("min") + " and " + props.get("max"));
                    sheet.addValidationData(validation);
                }

                // Apply currency/number format
                XSSFCellStyle numStyle = workbook.createCellStyle();
                if (props.containsKey("currency")) {
                    numStyle.setDataFormat(format.getFormat("#,##0.00"));
                } else {
                    numStyle.setDataFormat(format.getFormat("#,##0.##"));
                }
                sheet.setDefaultColumnStyle(colIdx, numStyle);
            }
            case "percentage" -> {
                XSSFCellStyle pctStyle = workbook.createCellStyle();
                pctStyle.setDataFormat(format.getFormat("0.00%"));
                sheet.setDefaultColumnStyle(colIdx, pctStyle);
            }
            case "date" -> {
                XSSFCellStyle dateStyle = workbook.createCellStyle();
                dateStyle.setDataFormat(format.getFormat("yyyy-mm-dd"));
                sheet.setDefaultColumnStyle(colIdx, dateStyle);
            }
            case "dropdown" -> {
                if (props.containsKey("options")) {
                    var options = (List<String>) props.get("options");
                    var constraint = dvHelper.createExplicitListConstraint(
                            options.toArray(new String[0])
                    );
                    var validation = dvHelper.createValidation(constraint, range);
                    validation.setShowErrorBox(true);
                    validation.createErrorBox("Invalid selection",
                            "Please select one of the allowed values");
                    sheet.addValidationData(validation);
                }
            }
            default -> {
                // text, table, file_attachment - no specific Excel validation
            }
        }
    }

    private void createMetadataSheet(XSSFWorkbook workbook, UUID formId, UUID formVersionId) {
        var metaSheet = workbook.createSheet("__form_meta");
        var row0 = metaSheet.createRow(0);
        row0.createCell(0).setCellValue("form_id");
        row0.createCell(1).setCellValue(formId.toString());

        var row1 = metaSheet.createRow(1);
        row1.createCell(0).setCellValue("form_version_id");
        row1.createCell(1).setCellValue(formVersionId.toString());

        // Hide the metadata sheet
        workbook.setSheetHidden(workbook.getSheetIndex("__form_meta"), true);
    }
}
