package com.reportplatform.tmplpptx.service;

import com.reportplatform.tmplpptx.dto.PlaceholderMappingRequest;
import com.reportplatform.tmplpptx.dto.PlaceholderMappingResponse;
import com.reportplatform.tmplpptx.entity.PlaceholderMappingEntity;
import com.reportplatform.tmplpptx.exception.TemplateNotFoundException;
import com.reportplatform.tmplpptx.repository.PlaceholderMappingRepository;
import com.reportplatform.tmplpptx.repository.PptxTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class PlaceholderMappingService {

    private static final Logger log = LoggerFactory.getLogger(PlaceholderMappingService.class);

    private final PlaceholderMappingRepository mappingRepository;
    private final PptxTemplateRepository templateRepository;

    public PlaceholderMappingService(PlaceholderMappingRepository mappingRepository,
                                     PptxTemplateRepository templateRepository) {
        this.mappingRepository = mappingRepository;
        this.templateRepository = templateRepository;
    }

    /**
     * Configure placeholder-to-data-source mappings for a template.
     * Upserts: existing mappings for the same placeholder_key are updated.
     */
    public PlaceholderMappingResponse configureMappings(UUID templateId,
                                                         PlaceholderMappingRequest request,
                                                         String userId) {
        var template = templateRepository.findByIdAndActiveTrue(templateId)
                .orElseThrow(() -> new TemplateNotFoundException(templateId));

        for (var entry : request.mappings()) {
            var existing = mappingRepository.findByTemplateIdAndPlaceholderKey(
                    templateId, entry.placeholderKey());

            if (existing.isPresent()) {
                var mapping = existing.get();
                mapping.setDataSourceType(entry.dataSourceType());
                mapping.setDataSourceRef(entry.dataSourceRef());
                mapping.setTransformExpression(entry.transformExpression());
                mappingRepository.save(mapping);
            } else {
                mappingRepository.save(new PlaceholderMappingEntity(
                        template, entry.placeholderKey(), entry.dataSourceType(),
                        entry.dataSourceRef(), entry.transformExpression(), userId));
            }
        }

        log.info("Configured {} mappings for template id={}", request.mappings().size(), templateId);
        return getMappings(templateId);
    }

    /**
     * Get all mappings for a template.
     */
    @Transactional(readOnly = true)
    public PlaceholderMappingResponse getMappings(UUID templateId) {
        templateRepository.findByIdAndActiveTrue(templateId)
                .orElseThrow(() -> new TemplateNotFoundException(templateId));

        var mappings = mappingRepository.findByTemplateId(templateId).stream()
                .map(m -> new PlaceholderMappingResponse.MappingEntry(
                        m.getId(), m.getPlaceholderKey(), m.getDataSourceType(),
                        m.getDataSourceRef(), m.getTransformExpression(), m.getUpdatedAt()))
                .toList();

        return new PlaceholderMappingResponse(templateId, mappings);
    }
}
