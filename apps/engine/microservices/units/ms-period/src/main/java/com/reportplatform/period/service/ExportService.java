package com.reportplatform.period.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.reportplatform.period.dto.PeriodStatusResponse;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

@Service
public class ExportService {

    private static final Logger log = LoggerFactory.getLogger(ExportService.class);

    private final CompletionTrackingService completionTrackingService;

    public ExportService(CompletionTrackingService completionTrackingService) {
        this.completionTrackingService = completionTrackingService;
    }

    public byte[] exportAsExcel(UUID periodId) throws IOException {
        PeriodStatusResponse status = completionTrackingService.getCompletionStatus(periodId);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Period Status");

            // Header
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            String[] headers = {"Organization", "Status", "Report Count"};
            for (int i = 0; i < headers.length; i++) {
                var cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowNum = 1;
            for (var entry : status.orgStatuses()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(entry.orgId());
                row.createCell(1).setCellValue(entry.status());
                row.createCell(2).setCellValue(entry.reportCount());
            }

            // Summary row
            Row summaryRow = sheet.createRow(rowNum + 1);
            summaryRow.createCell(0).setCellValue("Completion:");
            summaryRow.createCell(1).setCellValue(status.completionPct() + "%");
            summaryRow.createCell(2).setCellValue(status.approvedReports() + "/" + status.totalOrgs());

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportAsPdf(UUID periodId) {
        PeriodStatusResponse status = completionTrackingService.getCompletionStatus(periodId);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            document.add(new Paragraph("Period Status Report: " + status.periodName(), titleFont));
            document.add(new Paragraph("Completion: " + status.completionPct() + "% (" +
                    status.approvedReports() + "/" + status.totalOrgs() + ")", bodyFont));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3, 2, 1.5f});

            addCell(table, "Organization", headerFont, Color.LIGHT_GRAY);
            addCell(table, "Status", headerFont, Color.LIGHT_GRAY);
            addCell(table, "Reports", headerFont, Color.LIGHT_GRAY);

            for (var entry : status.orgStatuses()) {
                Color rowColor = getStatusColor(entry.status());
                addCell(table, entry.orgId(), bodyFont, rowColor);
                addCell(table, entry.status(), bodyFont, rowColor);
                addCell(table, String.valueOf(entry.reportCount()), bodyFont, rowColor);
            }

            document.add(table);
            document.close();

            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate PDF for period {}", periodId, e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    private void addCell(PdfPTable table, String text, Font font, Color bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setPadding(5);
        if (bgColor != null) {
            cell.setBackgroundColor(bgColor);
        }
        table.addCell(cell);
    }

    private Color getStatusColor(String status) {
        return switch (status) {
            case "APPROVED" -> new Color(198, 239, 206);
            case "SUBMITTED", "UNDER_REVIEW" -> new Color(255, 235, 156);
            case "REJECTED" -> new Color(255, 199, 206);
            default -> Color.WHITE;
        };
    }
}
