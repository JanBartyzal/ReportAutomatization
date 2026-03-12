package com.reportplatform.qry.pubsub;

import com.reportplatform.qry.model.dto.DataStoredEvent;
import com.reportplatform.qry.service.CacheService;
import com.reportplatform.qry.service.ViewRefreshService;
import io.dapr.Topic;
import io.dapr.client.domain.CloudEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dapr Pub/Sub subscriber that listens for data-stored events from sink services.
 * When new data arrives, it invalidates relevant caches and triggers
 * a refresh of the materialized views.
 */
@RestController
public class DataChangedSubscriber {

    private static final Logger log = LoggerFactory.getLogger(DataChangedSubscriber.class);

    private final CacheService cacheService;
    private final ViewRefreshService viewRefreshService;

    public DataChangedSubscriber(CacheService cacheService, ViewRefreshService viewRefreshService) {
        this.cacheService = cacheService;
        this.viewRefreshService = viewRefreshService;
    }

    @Topic(name = "data-stored", pubsubName = "${dapr.pubsub.name}")
    @PostMapping("/api/v1/events/data-stored")
    public ResponseEntity<Void> handleDataStored(@RequestBody CloudEvent<DataStoredEvent> event) {
        DataStoredEvent data = event.getData();
        if (data == null) {
            log.warn("Received data-stored event with null payload");
            return ResponseEntity.ok().build();
        }

        log.info("Received data-stored event: fileId={}, orgId={}, entityType={}, action={}",
                data.fileId(), data.orgId(), data.entityType(), data.action());

        // Invalidate cache entries for the affected file and org
        if (data.orgId() != null && data.fileId() != null) {
            cacheService.evict(cacheService.buildKey(data.orgId(), "file-data", data.fileId()));
            cacheService.evict(cacheService.buildKey(data.orgId(), "slides", data.fileId()));
            cacheService.evict(cacheService.buildKey(data.orgId(), "logs", data.fileId()));
            cacheService.evictByPattern(data.orgId(), "tables");
            cacheService.evictByPattern(data.orgId(), "document");
        }

        // Trigger async refresh of materialized views
        viewRefreshService.refreshAllViews();

        return ResponseEntity.ok().build();
    }
}
