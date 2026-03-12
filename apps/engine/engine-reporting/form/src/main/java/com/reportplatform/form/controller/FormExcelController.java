package com.reportplatform.form.controller;

import com.reportplatform.form.service.ExcelImportService;
import com.reportplatform.form.service.ExcelTemplateService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/forms/{formId}")
public class FormExcelController {

    private final ExcelTemplateService excelTemplateService;
    private final ExcelImportService excelImportService;

    public FormExcelController(ExcelTemplateService excelTemplateService,
                               ExcelImportService excelImportService) {
        this.excelTemplateService = excelTemplateService;
        this.excelImportService = excelImportService;
    }

    @GetMapping("/export/excel-template")
    public ResponseEntity<byte[]> exportExcelTemplate(@PathVariable UUID formId) throws IOException {
        byte[] template = excelTemplateService.generateTemplate(formId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=form_template_" + formId + ".xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(template);
    }

    @PostMapping("/import/excel")
    public ResponseEntity<Map<String, Object>> importExcel(
            @PathVariable UUID formId,
            @RequestParam("file") MultipartFile file) throws IOException {
        var result = excelImportService.importExcel(formId, file);
        return ResponseEntity.ok(result);
    }
}
