package com.reportplatform.srch.controller;

import com.reportplatform.srch.model.SearchIndexEntity;
import com.reportplatform.srch.service.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for search operations.
 */
@RestController
@RequestMapping({"/api/v1/search", "/api/search"})
public class SearchController {

    private static final Logger log = LoggerFactory.getLogger(SearchController.class);

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * Search documents.
     * GET /api/search?q=query&type=text|semantic|combined
     */
    @GetMapping({"", "/"})
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<List<SearchResult>> search(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Org-Id", required = false) String orgId,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "combined") String type,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int page) {

        if (q.isBlank()) {
            return ResponseEntity.ok(java.util.List.of());
        }

        Pageable pageable = PageRequest.of(page, limit);

        SearchService.SearchType searchType = switch (type.toLowerCase()) {
            case "text" -> SearchService.SearchType.TEXT;
            case "semantic" -> SearchService.SearchType.SEMANTIC;
            default -> SearchService.SearchType.COMBINED;
        };

        try {
            List<SearchIndexEntity> results = searchService.search(orgId, q, searchType, null, pageable);

            List<SearchResult> response = results.stream()
                    .map(e -> new SearchResult(
                            e.getId(),
                            e.getEntityType(),
                            e.getEntityId(),
                            e.getTitle(),
                            e.getContent()))
                    .toList();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.warn("Search failed: {}", e.getMessage());
            return ResponseEntity.ok(java.util.List.of());
        }
    }

    /**
     * Get autocomplete suggestions.
     * GET /api/search/suggest?q=partial
     */
    @GetMapping("/suggest")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<List<String>> suggest(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Org-Id", required = false) String orgId,
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit) {

        List<String> suggestions = searchService.getSuggestions(orgId, q, limit);
        return ResponseEntity.ok(suggestions);
    }

    /**
     * Get all indexed documents for the org.
     * GET /api/search/all?entityType=...
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Page<SearchResult>> getAll(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Org-Id", required = false) String orgId,
            @RequestParam(required = false) String entityType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<SearchIndexEntity> results = searchService.getSearchResults(orgId, entityType, pageable);

        Page<SearchResult> response = results.map(e -> new SearchResult(
                e.getId(),
                e.getEntityType(),
                e.getEntityId(),
                e.getTitle(),
                e.getContent()));

        return ResponseEntity.ok(response);
    }

    /**
     * Semantic/vector search endpoint.
     * POST /api/search/semantic
     */
    @PostMapping("/semantic")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<List<SearchResult>> semanticSearch(
            @RequestHeader(value = "X-Org-Id", required = false) String orgId,
            @RequestBody Map<String, Object> body) {

        String query = (String) body.getOrDefault("query", body.getOrDefault("q", ""));
        int limit = body.containsKey("limit") ? ((Number) body.get("limit")).intValue() : 20;

        if (orgId == null || orgId.isBlank()) {
            return ResponseEntity.ok(List.of());
        }

        Pageable pageable = PageRequest.of(0, limit);
        List<SearchIndexEntity> results = searchService.search(
                orgId, query, SearchService.SearchType.SEMANTIC, null, pageable);

        List<SearchResult> response = results.stream()
                .map(e -> new SearchResult(
                        e.getId(),
                        e.getEntityType(),
                        e.getEntityId(),
                        e.getTitle(),
                        e.getContent()))
                .toList();

        return ResponseEntity.ok(response);
    }

    public record SearchResult(
            java.util.UUID id,
            String entityType,
            java.util.UUID entityId,
            String title,
            String content) {
    }
}
