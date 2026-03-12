package com.reportplatform.tmplpptx.service;

import com.reportplatform.tmplpptx.dto.PlaceholderResponse;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.junit.jupiter.api.Test;

import java.awt.geom.Rectangle2D;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlaceholderExtractorServiceTest {

    private final PlaceholderExtractorService service = new PlaceholderExtractorService();

    @Test
    void shouldExtractTextPlaceholder() throws IOException {
        byte[] pptx = createPptxWithText("Company: {{company_name}}");
        List<PlaceholderResponse> result = service.extractPlaceholders(pptx);

        assertEquals(1, result.size());
        assertEquals("company_name", result.get(0).key());
        assertEquals("TEXT", result.get(0).type());
        assertEquals(0, result.get(0).slideIndex());
    }

    @Test
    void shouldExtractTablePlaceholder() throws IOException {
        byte[] pptx = createPptxWithText("{{TABLE:expenses}}");
        List<PlaceholderResponse> result = service.extractPlaceholders(pptx);

        assertEquals(1, result.size());
        assertEquals("expenses", result.get(0).key());
        assertEquals("TABLE", result.get(0).type());
    }

    @Test
    void shouldExtractChartPlaceholder() throws IOException {
        byte[] pptx = createPptxWithText("{{CHART:revenue}}");
        List<PlaceholderResponse> result = service.extractPlaceholders(pptx);

        assertEquals(1, result.size());
        assertEquals("revenue", result.get(0).key());
        assertEquals("CHART", result.get(0).type());
    }

    @Test
    void shouldExtractMultiplePlaceholders() throws IOException {
        byte[] pptx = createPptxWithText("{{title}} - {{TABLE:data}} - {{CHART:graph}}");
        List<PlaceholderResponse> result = service.extractPlaceholders(pptx);

        assertEquals(3, result.size());
    }

    @Test
    void shouldReturnEmptyForNoPlaceholders() throws IOException {
        byte[] pptx = createPptxWithText("Just some regular text");
        List<PlaceholderResponse> result = service.extractPlaceholders(pptx);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldRejectInvalidPptx() {
        assertThrows(IllegalArgumentException.class, () ->
                service.extractPlaceholders(new byte[]{1, 2, 3}));
    }

    private byte[] createPptxWithText(String text) throws IOException {
        try (XMLSlideShow pptx = new XMLSlideShow()) {
            XSLFSlide slide = pptx.createSlide();
            XSLFTextBox textBox = slide.createTextBox();
            textBox.setAnchor(new Rectangle2D.Double(50, 50, 400, 100));
            textBox.setText(text);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            pptx.write(out);
            return out.toByteArray();
        }
    }
}
