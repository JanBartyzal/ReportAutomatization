package com.reportplatform.dash.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Exports dashboard aggregation results as Excel workbooks.
 */
@Service
public class DashboardExcelExportService {

    private static final Logger log = LoggerFactory.getLogger(DashboardExcelExportService.class);
    private static final int MAX_EXPORT_ROWS = 50_000;

    public byte[] exportToExcel(List<Map<String, Object>> data, Map<String, Object> metadata)
            throws IOException {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("No data to export");
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = createHeaderStyle(workbook);
            Sheet sheet = workbook.createSheet("Dashboard Results");

            List<String> headers = data.get(0).keySet().stream().toList();
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Map<String, Object> row : data) {
                if (rowIdx > MAX_EXPORT_ROWS) break;
                Row dataRow = sheet.createRow(rowIdx++);
                for (int col = 0; col < headers.size(); col++) {
                    Cell cell = dataRow.createCell(col);
                    setCellValue(cell, row.get(headers.get(col)));
                }
            }

            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }

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
            log.info("Exported {} rows of dashboard data to Excel", data.size());
            return out.toByteArray();
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
}
