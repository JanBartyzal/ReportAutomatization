package com.reportplatform.form.service;

import com.reportplatform.form.exception.FormNotFoundException;
import com.reportplatform.form.exception.InvalidFormStateException;
import com.reportplatform.form.model.FormFieldEntity;
import com.reportplatform.form.repository.FormFieldRepository;
import com.reportplatform.form.repository.FormVersionRepository;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ExcelImportService {

    private static final Logger log = LoggerFactory.getLogger(ExcelImportService.class);

    private final FormVersionRepository formVersionRepository;
    private final FormFieldRepository formFieldRepository;

    public ExcelImportService(FormVersionRepository formVersionRepository,
                              FormFieldRepository formFieldRepository) {
        this.formVersionRepository = formVersionRepository;
        this.formFieldRepository = formFieldRepository;
    }

    public Map<String, Object> importExcel(UUID formId, MultipartFile file) throws IOException {
        try (var workbook = new XSSFWorkbook(file.getInputStream())) {
            // Check for metadata sheet
            var metaSheet = workbook.getSheet("__form_meta");
            if (metaSheet != null) {
                return importFromTemplate(formId, workbook, metaSheet);
            } else {
                return importArbitrary(formId, workbook);
            }
        }
    }

    private Map<String, Object> importFromTemplate(UUID formId, XSSFWorkbook workbook, XSSFSheet metaSheet) {
        // Read metadata
        String metaFormId = metaSheet.getRow(0).getCell(1).getStringCellValue();
        String metaVersionId = metaSheet.getRow(1).getCell(1).getStringCellValue();

        UUID templateVersionId = UUID.fromString(metaVersionId);

        // Verify version exists
        var templateVersion = formVersionRepository.findById(templateVersionId)
                .orElseThrow(() -> new InvalidFormStateException("Template version not found: " + metaVersionId));

        // Check if version matches the form
        if (!templateVersion.getFormId().equals(formId)) {
            throw new InvalidFormStateException("Template form_id does not match target form");
        }

        // Get latest version for comparison
        var latestVersion = formVersionRepository.findTopByFormIdOrderByVersionNumberDesc(formId)
                .orElseThrow(() -> new FormNotFoundException(formId));

        boolean versionMatch = latestVersion.getId().equals(templateVersionId);

        // Get fields for the template version
        var fields = formFieldRepository.findByFormVersionIdOrderBySortOrder(templateVersionId);
        var fieldsBySection = fields.stream()
                .collect(Collectors.groupingBy(
                        f -> f.getSection() != null ? f.getSection() : "General",
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        // Parse data from sheets
        var data = new HashMap<String, Object>();
        for (var entry : fieldsBySection.entrySet()) {
            var sheet = workbook.getSheet(entry.getKey());
            if (sheet == null) continue;

            parseSheetData(sheet, entry.getValue(), data);
        }

        var result = new HashMap<String, Object>();
        result.put("data", data);
        result.put("versionMatch", versionMatch);
        result.put("templateVersionId", templateVersionId.toString());
        result.put("latestVersionId", latestVersion.getId().toString());

        if (!versionMatch) {
            result.put("warning", "Template version does not match latest form version. Data may need manual review.");
        }

        log.info("Imported Excel template for form {} (versionMatch={})", formId, versionMatch);
        return result;
    }

    private Map<String, Object> importArbitrary(UUID formId, XSSFWorkbook workbook) {
        // Parse all sheets and extract column headers + data
        var result = new HashMap<String, Object>();
        var sheets = new HashMap<String, Object>();

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            var sheet = workbook.getSheetAt(i);
            String sheetName = sheet.getSheetName();
            if ("__form_meta".equals(sheetName)) continue;

            var sheetData = new HashMap<String, Object>();
            var headerRow = sheet.getRow(0);
            if (headerRow == null) continue;

            var headers = new java.util.ArrayList<String>();
            for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                var cell = headerRow.getCell(c);
                headers.add(cell != null ? cell.getStringCellValue() : "column_" + c);
            }
            sheetData.put("headers", headers);

            var rows = new java.util.ArrayList<Map<String, Object>>();
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                var row = sheet.getRow(r);
                if (row == null) continue;

                var rowData = new LinkedHashMap<String, Object>();
                for (int c = 0; c < headers.size(); c++) {
                    var cell = row.getCell(c);
                    if (cell != null) {
                        rowData.put(headers.get(c), getCellValue(cell));
                    }
                }
                if (!rowData.isEmpty()) {
                    rows.add(rowData);
                }
            }
            sheetData.put("rows", rows);
            sheets.put(sheetName, sheetData);
        }

        result.put("sheets", sheets);
        result.put("requiresMapping", true);
        result.put("message", "Arbitrary Excel file parsed. Column-to-field mapping required.");

        log.info("Imported arbitrary Excel for form {} with {} sheets", formId, sheets.size());
        return result;
    }

    private void parseSheetData(XSSFSheet sheet, List<FormFieldEntity> fields, Map<String, Object> data) {
        // First data row (row 1, row 0 is headers)
        var dataRow = sheet.getRow(1);
        if (dataRow == null) return;

        for (int i = 0; i < fields.size(); i++) {
            var field = fields.get(i);
            var cell = dataRow.getCell(i);
            if (cell != null) {
                data.put(field.getFieldKey(), getCellValue(cell));
            }
        }
    }

    private Object getCellValue(org.apache.poi.ss.usermodel.Cell cell) {
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toString();
                }
                yield cell.getNumericCellValue();
            }
            case BOOLEAN -> cell.getBooleanCellValue();
            case FORMULA -> cell.getCellFormula();
            default -> null;
        };
    }
}
