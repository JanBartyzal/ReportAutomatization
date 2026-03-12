package com.reportplatform.srch.repository;

import com.reportplatform.srch.model.SearchIndexEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for search index operations.
 */
@Repository
public interface SearchRepository extends JpaRepository<SearchIndexEntity, UUID> {

    /**
     * Find all search entries for an organization.
     */
    Page<SearchIndexEntity> findByOrgId(String orgId, Pageable pageable);

    /**
     * Find search entries by entity type.
     */
    Page<SearchIndexEntity> findByOrgIdAndEntityType(String orgId, String entityType, Pageable pageable);

    /**
     * Find by entity type and entity ID.
     */
    Optional<SearchIndexEntity> findByEntityTypeAndEntityId(String entityType, UUID entityId);

    /**
     * Full-text search using PostgreSQL FTS.
     */
    @Query(value = """
            SELECT * FROM search_index
            WHERE org_id = :orgId
            AND search_vector @@ plainto_tsquery('czech,english', :query)
            ORDER BY ts_rank(search_vector, plainto_tsquery('czech,english', :query)) DESC
            """, nativeQuery = true)
    List<SearchIndexEntity> fullTextSearch(
            @Param("orgId") String orgId,
            @Param("query") String query);

    /**
     * Vector similarity search using pgvector.
     */
    @Query(value = """
            SELECT * FROM search_index
            WHERE org_id = :orgId
            AND embedding <=> CAST(:embedding AS vector) < :threshold
            ORDER BY embedding <=> CAST(:embedding AS vector)
            """, nativeQuery = true)
    List<SearchIndexEntity> vectorSearch(
            @Param("orgId") String orgId,
            @Param("embedding") String embedding,
            @Param("threshold") double threshold);

    /**
     * Combined FTS and vector search.
     */
    @Query(value = """
            SELECT * FROM search_index
            WHERE org_id = :orgId
            AND (
                search_vector @@ plainto_tsquery('czech,english', :query)
                OR (embedding IS NOT NULL AND embedding <=> CAST(:embedding AS vector) < :threshold)
            )
            ORDER BY (
                ts_rank(search_vector, plainto_tsquery('czech,english', :query)) +
                (1.0 - (embedding <=> CAST(:embedding AS vector)))
            ) DESC
            """, nativeQuery = true)
    List<SearchIndexEntity> combinedSearch(
            @Param("orgId") String orgId,
            @Param("query") String query,
            @Param("embedding") String embedding,
            @Param("threshold") double threshold);
}
