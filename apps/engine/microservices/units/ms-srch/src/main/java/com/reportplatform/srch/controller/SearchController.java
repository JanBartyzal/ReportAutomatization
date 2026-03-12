package com.reportplatform.srch.controller;

import com.reportplatform.srch.model.SearchIndexEntity;
import com.reportplatform.srch.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for search operations.
 */
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Slf4j
public class SearchController {

    private final SearchService searchService;

    /**
     * Search documents.
     * GET /api/search?q=query&type=text|semantic|combined
     */
    @GetMapping
    public ResponseEntity<List<SearchResult>> search(
            @AuthenticationPrincipal UserDetails user,
            @RequestHeader("X-Org-Id") String orgId,
            @RequestParam String q,
            @RequestParam(defaultValue = "combined") String type,
            @RequestParam(required = false) float[] embedding,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int page) {

        String userId = user.getUsername();
        Pageable pageable = PageRequest.of(page, limit);

        SearchService.SearchType searchType = switch (type.toLowerCase()) {
            case "text" -> SearchService.SearchType.TEXT;
            case "semantic" -> SearchService.SearchType.SEMANTIC;
            default -> SearchService.SearchType.COMBINED;
        };

        List<SearchIndexEntity> results = searchService.search(orgId, q, searchType, embedding, pageable);

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

    /**
     * Get autocomplete suggestions.
     * GET /api/search/suggest?q=partial
     */
    @GetMapping("/suggest")
    public ResponseEntity<List<String>> suggest(
            @AuthenticationPrincipal UserDetails user,
            @RequestHeader("X-Org-Id") String orgId,
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
    public ResponseEntity<Page<SearchResult>> getAll(
            @AuthenticationPrincipal UserDetails user,
            @RequestHeader("X-Org-Id") String orgId,
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

    public record SearchResult(
            java.util.UUID id,
            String entityType,
            java.util.UUID entityId,
            String title,
            String content) {
    }
}
