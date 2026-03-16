package com.reportplatform.qry.service;

import com.reportplatform.qry.model.ParsedTableEntity;
import com.reportplatform.qry.repository.QryParsedTableRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Service for exporting query results and dashboard data as Excel files.
 */
@Service
public class ExcelExportService {

    private static final Logger log = LoggerFactory.getLogger(ExcelExportService.class);
    private static final int MAX_EXPORT_ROWS = 50_000;

    private final QryParsedTableRepository parsedTableRepository;

    public ExcelExportService(QryParsedTableRepository parsedTableRepository) {
        this.parsedTableRepository = parsedTableRepository;
    }

    /**
     * Export parsed tables for an org as an Excel workbook.
     * Each parsed_table record becomes a separate sheet.
     */
    @Transactional(readOnly = true)
    public byte[] exportTablesToExcel(String orgId, String fileId, String sourceSheet) throws IOException {
        List<ParsedTableEntity> tables;

        if (fileId != null && sourceSheet != null) {
            tables = parsedTableRepository
                    .findByOrgIdAndFileIdAndSourceSheetContainingIgnoreCaseOrderByCreatedAtDesc(
                            orgId, fileId, sourceSheet, PageRequest.of(0, 100))
                    .getContent();
        } else if (fileId != null) {
            tables = parsedTableRepository
                    .findByOrgIdAndFileIdOrderByCreatedAtDesc(orgId, fileId, PageRequest.of(0, 100))
                    .getContent();
        } else {
            tables = parsedTableRepository
                    .findByOrgIdOrderByCreatedAtDesc(orgId, PageRequest.of(0, 100))
                    .getContent();
        }

        if (tables.isEmpty()) {
            throw new IllegalArgumentException("No tables found for the given criteria");
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = createHeaderStyle(workbook);

            int sheetIdx = 0;
            for (ParsedTableEntity table : tables) {
                String sheetName = sanitizeSheetName(table.getSourceSheet(), sheetIdx);
                Sheet sheet = workbook.createSheet(sheetName);
                writeTableToSheet(sheet, table, headerStyle);
                sheetIdx++;
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            log.info("Exported {} tables to Excel for org={}", tables.size(), orgId);
            return out.toByteArray();
        }
    }

    /**
     * Export aggregation results (list of row maps) as Excel.
     */
    public byte[] exportAggregationToExcel(List<Map<String, Object>> data, Map<String, Object> metadata)
            throws IOException {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("No data to export");
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = createHeaderStyle(workbook);
            Sheet sheet = workbook.createSheet("Aggregation Results");

            // Headers from first row keys
            List<String> headers = data.get(0).keySet().stream().toList();
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowIdx = 1;
            for (Map<String, Object> row : data) {
                if (rowIdx > MAX_EXPORT_ROWS) break;
                Row dataRow = sheet.createRow(rowIdx++);
                for (int col = 0; col < headers.size(); col++) {
                    Cell cell = dataRow.createCell(col);
                    Object value = row.get(headers.get(col));
                    setCellValue(cell, value);
                }
            }

            // Auto-size columns
            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            // Metadata sheet
            if (metadata != null && !metadata.isEmpty()) {
                Sheet metaSheet = workbook.createSheet("_metadata");
                int metaRow = 0;
                for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                    Row row = metaSheet.createRow(metaRow++);
                    row.createCell(0).setCellValue(entry.getKey());
                    setCellValue(row.createCell(1), entry.getValue());
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    @SuppressWarnings("unchecked")
    private void writeTableToSheet(Sheet sheet, ParsedTableEntity table, CellStyle headerStyle) {
        Object headersObj = table.getHeaders();
        Object rowsObj = table.getRows();

        int rowIdx = 0;

        // Write headers
        if (headersObj instanceof List<?> headerList) {
            Row headerRow = sheet.createRow(rowIdx++);
            for (int i = 0; i < headerList.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(String.valueOf(headerList.get(i)));
                cell.setCellStyle(headerStyle);
            }
        }

        // Write rows
        if (rowsObj instanceof List<?> rowList) {
            for (Object rowObj : rowList) {
                if (rowIdx > MAX_EXPORT_ROWS) break;
                Row dataRow = sheet.createRow(rowIdx++);
                if (rowObj instanceof List<?> cellList) {
                    for (int col = 0; col < cellList.size(); col++) {
                        Cell cell = dataRow.createCell(col);
                        setCellValue(cell, cellList.get(col));
                    }
                }
            }
        }

        // Auto-size first 20 columns max
        int colCount = 0;
        if (headersObj instanceof List<?> headerList) {
            colCount = headerList.size();
        }
        for (int i = 0; i < Math.min(colCount, 20); i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setBlank();
        } else if (value instanceof Number num) {
            cell.setCellValue(num.doubleValue());
        } else if (value instanceof Boolean bool) {
            cell.setCellValue(bool);
        } else {
            cell.setCellValue(String.valueOf(value));
        }
    }

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private String sanitizeSheetName(String name, int index) {
        if (name == null || name.isBlank()) {
            return "Sheet_" + (index + 1);
        }
        // Excel sheet name max 31 chars, no special chars
        String sanitized = name.replaceAll("[\\\\/:*?\\[\\]]", "_");
        if (sanitized.length() > 28) {
            sanitized = sanitized.substring(0, 28);
        }
        return sanitized + "_" + (index + 1);
    }
}
