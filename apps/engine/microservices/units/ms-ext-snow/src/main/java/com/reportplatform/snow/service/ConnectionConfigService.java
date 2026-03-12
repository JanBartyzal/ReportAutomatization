package com.reportplatform.snow.service;

import com.reportplatform.snow.model.dto.ConnectionDTO;
import com.reportplatform.snow.model.dto.CreateConnectionRequest;
import com.reportplatform.snow.model.dto.TestConnectionRequest;
import com.reportplatform.snow.model.dto.TestConnectionResponse;
import com.reportplatform.snow.model.entity.AuthType;
import com.reportplatform.snow.model.entity.ServiceNowConnectionEntity;
import com.reportplatform.snow.repository.ServiceNowConnectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ConnectionConfigService {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionConfigService.class);

    private final ServiceNowConnectionRepository connectionRepository;
    private final ServiceNowClient serviceNowClient;
    private final KeyVaultService keyVaultService;

    public ConnectionConfigService(ServiceNowConnectionRepository connectionRepository,
                                   ServiceNowClient serviceNowClient,
                                   KeyVaultService keyVaultService) {
        this.connectionRepository = connectionRepository;
        this.serviceNowClient = serviceNowClient;
        this.keyVaultService = keyVaultService;
    }

    @Transactional(readOnly = true)
    public List<ConnectionDTO> getAllConnections(UUID orgId) {
        logger.debug("Listing all connections for org: {}", orgId);
        return connectionRepository.findByOrgId(orgId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ConnectionDTO getConnection(UUID id) {
        ServiceNowConnectionEntity entity = connectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + id));
        return toDTO(entity);
    }

    public ConnectionDTO createConnection(UUID orgId, String createdBy, CreateConnectionRequest request) {
        ServiceNowConnectionEntity entity = new ServiceNowConnectionEntity();
        entity.setOrgId(orgId);
        entity.setName(request.getName());
        entity.setInstanceUrl(request.getInstanceUrl());
        entity.setAuthType(AuthType.valueOf(request.getAuthType().toUpperCase()));
        entity.setCredentialsRef(request.getCredentialsRef());
        entity.setCreatedBy(createdBy);
        entity.setEnabled(true);

        if (request.getTables() != null) {
            entity.setTables(toJsonArray(request.getTables()));
        }
        if (request.getMappingTemplateId() != null) {
            entity.setMappingTemplateId(request.getMappingTemplateId());
        }

        ServiceNowConnectionEntity saved = connectionRepository.save(entity);
        logger.info("Created ServiceNow connection: {} ({}) for org: {}", saved.getName(), saved.getId(), orgId);
        return toDTO(saved);
    }

    public ConnectionDTO updateConnection(UUID id, CreateConnectionRequest request) {
        ServiceNowConnectionEntity entity = connectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + id));

        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (request.getInstanceUrl() != null) {
            entity.setInstanceUrl(request.getInstanceUrl());
        }
        if (request.getAuthType() != null) {
            entity.setAuthType(AuthType.valueOf(request.getAuthType().toUpperCase()));
        }
        if (request.getCredentialsRef() != null) {
            entity.setCredentialsRef(request.getCredentialsRef());
        }
        if (request.getTables() != null) {
            entity.setTables(toJsonArray(request.getTables()));
        }
        if (request.getMappingTemplateId() != null) {
            entity.setMappingTemplateId(request.getMappingTemplateId());
        }

        ServiceNowConnectionEntity saved = connectionRepository.save(entity);
        logger.info("Updated ServiceNow connection: {} ({})", saved.getName(), saved.getId());
        return toDTO(saved);
    }

    public void deleteConnection(UUID id) {
        ServiceNowConnectionEntity entity = connectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + id));

        connectionRepository.delete(entity);
        logger.info("Deleted ServiceNow connection: {}", id);
    }

    @SuppressWarnings("unchecked")
    public TestConnectionResponse testConnection(TestConnectionRequest request) {
        logger.info("Testing connection to ServiceNow instance: {}", request.getInstanceUrl());

        long startTime = System.currentTimeMillis();
        try {
            // Retrieve credentials from KeyVault
            String credentials = keyVaultService.getSecret(request.getCredentialsRef());

            // Authenticate
            AuthType authType = AuthType.valueOf(request.getAuthType().toUpperCase());
            String accessToken = serviceNowClient.authenticate(
                    request.getInstanceUrl(), authType, credentials);

            // Test the connection
            Map<String, Object> result = serviceNowClient.testConnection(
                    request.getInstanceUrl(), accessToken);

            long latencyMs = System.currentTimeMillis() - startTime;

            // Extract instance version from response if available
            String instanceVersion = "unknown";
            if (result != null && result.containsKey("result")) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) result.get("result");
                if (results != null && !results.isEmpty()) {
                    Object value = results.get(0).get("value");
                    if (value != null) {
                        instanceVersion = value.toString();
                    }
                }
            }

            return new TestConnectionResponse(true, "Connection successful", instanceVersion, latencyMs);
        } catch (Exception ex) {
            long latencyMs = System.currentTimeMillis() - startTime;
            logger.error("Connection test failed for instance: {}", request.getInstanceUrl(), ex);
            return new TestConnectionResponse(false, "Connection failed: " + ex.getMessage(), null, latencyMs);
        }
    }

    private ConnectionDTO toDTO(ServiceNowConnectionEntity entity) {
        ConnectionDTO dto = new ConnectionDTO();
        dto.setId(entity.getId());
        dto.setOrgId(entity.getOrgId());
        dto.setName(entity.getName());
        dto.setInstanceUrl(entity.getInstanceUrl());
        dto.setAuthType(entity.getAuthType().name());
        dto.setEnabled(entity.isEnabled());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setTables(parseJsonArray(entity.getTables()));

        if (entity.getMappingTemplateId() != null) {
            dto.setMappingTemplateId(entity.getMappingTemplateId());
        }

        return dto;
    }

    private String toJsonArray(List<String> tables) {
        if (tables == null || tables.isEmpty()) {
            return "[]";
        }
        return "[" + tables.stream()
                .map(t -> "\"" + t.replace("\"", "\\\"") + "\"")
                .collect(Collectors.joining(",")) + "]";
    }

    private List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank() || "[]".equals(json.trim())) {
            return List.of();
        }
        // Simple parse for JSON array of strings: ["a","b","c"]
        String inner = json.trim();
        if (inner.startsWith("[")) {
            inner = inner.substring(1);
        }
        if (inner.endsWith("]")) {
            inner = inner.substring(0, inner.length() - 1);
        }
        return Arrays.stream(inner.split(","))
                .map(s -> s.trim().replaceAll("^\"|\"$", ""))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
