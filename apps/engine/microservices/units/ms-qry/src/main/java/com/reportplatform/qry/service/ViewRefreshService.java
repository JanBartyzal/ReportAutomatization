package com.reportplatform.qry.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for refreshing materialized views when new data arrives.
 * Called asynchronously after Pub/Sub events to keep the read model up to date.
 */
@Service
public class ViewRefreshService {

    private static final Logger log = LoggerFactory.getLogger(ViewRefreshService.class);

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Refreshes all materialized views concurrently.
     * This is safe to call from multiple threads as PostgreSQL handles
     * concurrent refresh locks internally.
     */
    @Async
    @Transactional
    public void refreshAllViews() {
        try {
            log.info("Starting materialized view refresh");
            entityManager.createNativeQuery("SELECT refresh_query_views()").getSingleResult();
            log.info("Materialized view refresh completed");
        } catch (Exception e) {
            log.error("Failed to refresh materialized views", e);
        }
    }
}
