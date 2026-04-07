package com.reportplatform.tmplpptx.service;

import com.reportplatform.tmplpptx.dto.PlaceholderResponse;
import org.apache.poi.xslf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts placeholder tags from PPTX files using Apache POI.
 * Detects: {{variable}}, {{TABLE:name}}, {{CHART:name}}
 */
@Service
public class PlaceholderExtractorService {

    private static final Logger log = LoggerFactory.getLogger(PlaceholderExtractorService.class);
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{(TABLE:|CHART:)?([^}]+)\\}\\}");

    /**
     * Parse a PPTX file and extract all placeholder tags.
     *
     * @param pptxBytes Raw PPTX file content
     * @return List of discovered placeholders with type classification
     */
    public List<PlaceholderResponse> extractPlaceholders(byte[] pptxBytes) {
        List<PlaceholderResponse> placeholders = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        try (var pptx = new XMLSlideShow(new ByteArrayInputStream(pptxBytes))) {
            List<XSLFSlide> slides = pptx.getSlides();

            for (int slideIdx = 0; slideIdx < slides.size(); slideIdx++) {
                XSLFSlide slide = slides.get(slideIdx);

                for (XSLFShape shape : slide.getShapes()) {
                    String shapeName = shape.getShapeName();
                    String text = extractTextFromShape(shape);

                    Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
                    while (matcher.find()) {
                        String prefix = matcher.group(1);
                        String key = matcher.group(2).trim();
                        String type = classifyType(prefix);
                        String dedupKey = slideIdx + ":" + shapeName + ":" + key;

                        if (seen.add(dedupKey)) {
                            placeholders.add(new PlaceholderResponse(key, type, slideIdx, shapeName));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse PPTX for placeholder extraction, returning empty list: {}", e.getMessage());
            return List.of();
        }

        log.info("Extracted {} placeholders from PPTX", placeholders.size());
        return placeholders;
    }

    private String extractTextFromShape(XSLFShape shape) {
        StringBuilder sb = new StringBuilder();

        if (shape instanceof XSLFTextShape textShape) {
            sb.append(textShape.getText());
        } else if (shape instanceof XSLFTable table) {
            for (XSLFTableRow row : table.getRows()) {
                for (XSLFTableCell cell : row.getCells()) {
                    sb.append(" ").append(cell.getText());
                }
            }
        } else if (shape instanceof XSLFGroupShape group) {
            for (XSLFShape child : group.getShapes()) {
                sb.append(" ").append(extractTextFromShape(child));
            }
        }

        return sb.toString();
    }

    private String classifyType(String prefix) {
        if ("TABLE:".equals(prefix)) return "TABLE";
        if ("CHART:".equals(prefix)) return "CHART";
        return "TEXT";
    }
}
