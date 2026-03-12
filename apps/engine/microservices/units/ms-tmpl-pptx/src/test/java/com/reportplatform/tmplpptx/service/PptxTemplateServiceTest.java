package com.reportplatform.tmplpptx.service;

import com.reportplatform.tmplpptx.dto.PlaceholderResponse;
import com.reportplatform.tmplpptx.dto.TemplateUploadResponse;
import com.reportplatform.tmplpptx.entity.PptxTemplateEntity;
import com.reportplatform.tmplpptx.entity.TemplatePlaceholderEntity;
import com.reportplatform.tmplpptx.entity.TemplateVersionEntity;
import com.reportplatform.tmplpptx.exception.TemplateNotFoundException;
import com.reportplatform.tmplpptx.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PptxTemplateServiceTest {

    @Mock private PptxTemplateRepository templateRepository;
    @Mock private TemplateVersionRepository versionRepository;
    @Mock private TemplatePlaceholderRepository placeholderRepository;
    @Mock private BlobStorageService blobStorageService;
    @Mock private PlaceholderExtractorService extractorService;
    @Mock private MultipartFile mockFile;

    @InjectMocks
    private PptxTemplateService service;

    private final UUID templateId = UUID.randomUUID();

    @BeforeEach
    void setUp() throws IOException {
        lenient().when(mockFile.getBytes()).thenReturn(new byte[]{1, 2, 3});
        lenient().when(mockFile.getOriginalFilename()).thenReturn("test.pptx");
    }

    @Test
    void uploadTemplateShouldCreateTemplateAndVersion() {
        var savedTemplate = new PptxTemplateEntity("Test", "org-1", "CENTRAL", null, "user-1");
        when(templateRepository.save(any())).thenReturn(savedTemplate);
        when(blobStorageService.uploadTemplate(any(), eq(1), any(), anyString()))
                .thenReturn("http://blob/test.pptx");
        when(versionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(extractorService.extractPlaceholders(any()))
                .thenReturn(List.of(new PlaceholderResponse("name", "TEXT", 0, "TextBox")));
        when(placeholderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TemplateUploadResponse result = service.uploadTemplate(
                mockFile, "Test", "Desc", "CENTRAL", null, "org-1", "user-1");

        assertNotNull(result);
        assertEquals("Test", result.name());
        assertEquals(1, result.version());
        assertEquals(1, result.placeholders().size());
        verify(templateRepository).save(any());
        verify(versionRepository).save(any());
    }

    @Test
    void deactivateTemplateShouldSetInactive() {
        var template = new PptxTemplateEntity("Test", "org-1", "CENTRAL", null, "user-1");
        when(templateRepository.findByIdAndActiveTrue(templateId)).thenReturn(Optional.of(template));
        when(templateRepository.save(any())).thenReturn(template);

        service.deactivateTemplate(templateId);

        assertFalse(template.isActive());
        verify(templateRepository).save(template);
    }

    @Test
    void deactivateNonExistentTemplateShouldThrow() {
        when(templateRepository.findByIdAndActiveTrue(templateId)).thenReturn(Optional.empty());
        assertThrows(TemplateNotFoundException.class, () -> service.deactivateTemplate(templateId));
    }

    @Test
    void getTemplateDetailShouldReturnDetail() {
        var template = new PptxTemplateEntity("Test", "org-1", "CENTRAL", null, "user-1");
        var version = new TemplateVersionEntity(template, 1, "http://blob/t.pptx", 1000L, "user-1");
        when(templateRepository.findByIdAndActiveTrue(templateId)).thenReturn(Optional.of(template));
        when(versionRepository.findByTemplateIdAndCurrentTrue(templateId)).thenReturn(Optional.of(version));
        when(placeholderRepository.findByVersionId(any())).thenReturn(List.of());

        var result = service.getTemplateDetail(templateId);

        assertNotNull(result);
        assertEquals("Test", result.name());
    }
}
