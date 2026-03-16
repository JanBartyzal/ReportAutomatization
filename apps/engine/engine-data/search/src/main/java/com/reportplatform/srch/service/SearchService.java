package com.reportplatform.srch.service;

import com.reportplatform.srch.model.SearchIndexEntity;
import com.reportplatform.srch.repository.SearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for search operations.
 */
@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final SearchRepository searchRepository;

    public SearchService(SearchRepository searchRepository) {
        this.searchRepository = searchRepository;
    }

    @Value("${search.vector.similarity-threshold:0.7}")
    private double similarityThreshold;

    /**
     * Index a document for search.
     */
    @Transactional
    public SearchIndexEntity indexDocument(String orgId, String entityType, UUID entityId,
            String title, String content, float[] embedding) {

        SearchIndexEntity entity = searchRepository.findByEntityTypeAndEntityId(entityType, entityId)
                .orElse(SearchIndexEntity.builder()
                        .orgId(orgId)
                        .entityType(entityType)
                        .entityId(entityId)
                        .build());

        entity.setTitle(title);
        entity.setContent(content);
        entity.setEmbedding(embedding);

        return searchRepository.save(entity);
    }

    /**
     * Search documents based on query and type.
     */
    public List<SearchIndexEntity> search(String orgId, String query, SearchType type,
            float[] queryEmbedding, Pageable pageable) {
        switch (type) {
            case TEXT:
                return searchRepository.fullTextSearch(orgId, query);
            case SEMANTIC:
                if (queryEmbedding == null || queryEmbedding.length == 0) {
                    log.warn("No embedding provided for semantic search");
                    return List.of();
                }
                String embeddingStr = Arrays.toString(queryEmbedding);
                return searchRepository.vectorSearch(orgId, embeddingStr, similarityThreshold);
            case COMBINED:
                String embStr = queryEmbedding != null ? Arrays.toString(queryEmbedding) : null;
                if (embStr != null) {
                    return searchRepository.combinedSearch(orgId, query, embStr, similarityThreshold);
                }
                return searchRepository.fullTextSearch(orgId, query);
            default:
                return List.of();
        }
    }

    /**
     * Get autocomplete suggestions.
     */
    public List<String> getSuggestions(String orgId, String partial, int limit) {
        // Use FTS prefix search for suggestions
        List<SearchIndexEntity> results = searchRepository.fullTextSearch(orgId, partial + "*");
        return results.stream()
                .map(SearchIndexEntity::getTitle)
                .filter(t -> t != null && !t.isEmpty())
                .distinct()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Get search results for an organization.
     */
    public Page<SearchIndexEntity> getSearchResults(String orgId, String entityType, Pageable pageable) {
        if (entityType != null && !entityType.isEmpty()) {
            return searchRepository.findByOrgIdAndEntityType(orgId, entityType, pageable);
        }
        return searchRepository.findByOrgId(orgId, pageable);
    }

    /**
     * Delete search entry.
     */
    @Transactional
    public void deleteByEntity(String entityType, UUID entityId) {
        searchRepository.findByEntityTypeAndEntityId(entityType, entityId)
                .ifPresent(searchRepository::delete);
    }

    /**
     * Search type enum.
     */
    public enum SearchType {
        TEXT,
        SEMANTIC,
        COMBINED
    }
}
