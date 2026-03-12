package com.reportplatform.snow.controller;

import com.reportplatform.snow.model.dto.ConnectionDTO;
import com.reportplatform.snow.model.dto.CreateConnectionRequest;
import com.reportplatform.snow.model.dto.TestConnectionRequest;
import com.reportplatform.snow.model.dto.TestConnectionResponse;
import com.reportplatform.snow.service.ConnectionConfigService;
import com.reportplatform.snow.service.DataFetchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/integrations/servicenow")
public class IntegrationController {

    private static final Logger logger = LoggerFactory.getLogger(IntegrationController.class);

    private final ConnectionConfigService connectionConfigService;
    private final DataFetchService dataFetchService;

    public IntegrationController(ConnectionConfigService connectionConfigService,
                                 DataFetchService dataFetchService) {
        this.connectionConfigService = connectionConfigService;
        this.dataFetchService = dataFetchService;
    }

    // ==================== Connections ====================

    @GetMapping
    public ResponseEntity<List<ConnectionDTO>> listConnections(
            @RequestHeader("X-Org-Id") UUID orgId) {
        return ResponseEntity.ok(connectionConfigService.getAllConnections(orgId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConnectionDTO> getConnection(@PathVariable UUID id) {
        return ResponseEntity.ok(connectionConfigService.getConnection(id));
    }

    @PostMapping
    public ResponseEntity<ConnectionDTO> createConnection(
            @RequestHeader("X-Org-Id") UUID orgId,
            @RequestHeader("X-User-Id") String userId,
            @RequestBody CreateConnectionRequest request) {
        ConnectionDTO created = connectionConfigService.createConnection(orgId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConnectionDTO> updateConnection(
            @PathVariable UUID id,
            @RequestBody CreateConnectionRequest request) {
        return ResponseEntity.ok(connectionConfigService.updateConnection(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConnection(@PathVariable UUID id) {
        connectionConfigService.deleteConnection(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Test & Sync ====================

    @PostMapping("/test")
    public ResponseEntity<TestConnectionResponse> testConnection(
            @RequestBody TestConnectionRequest request) {
        TestConnectionResponse response = connectionConfigService.testConnection(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/sync")
    public ResponseEntity<Map<String, Object>> triggerSync(
            @PathVariable UUID id,
            @RequestParam(required = false) String sysUpdatedOnAfter) {
        logger.info("Manual sync triggered for connection: {}", id);
        DataFetchService.FetchResult result = dataFetchService.fetchAndStore(id, sysUpdatedOnAfter);
        return ResponseEntity.accepted().body(Map.of(
                "connection_id", id.toString(),
                "records_fetched", result.getRecordsFetched(),
                "records_stored", result.getRecordsStored(),
                "status", "COMPLETED"));
    }

    // ==================== Health ====================

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "ms-ext-snow"));
    }
}
