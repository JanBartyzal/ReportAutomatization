package com.reportplatform.excelsync.connector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportplatform.excelsync.config.KeyVaultService;
import com.reportplatform.excelsync.model.entity.ExportFlowDefinitionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SharePointConnector implements FileConnector {

    private static final Logger log = LoggerFactory.getLogger(SharePointConnector.class);
    private static final long SIMPLE_UPLOAD_MAX_SIZE = 4 * 1024 * 1024; // 4 MB
    private static final int MAX_RETRIES = 3;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final KeyVaultService keyVaultService;

    // Token cache: tenantId:clientId -> (token, expiry)
    private final ConcurrentHashMap<String, CachedToken> tokenCache = new ConcurrentHashMap<>();

    public SharePointConnector(ObjectMapper objectMapper, KeyVaultService keyVaultService) {
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(50 * 1024 * 1024))
                .build();
        this.objectMapper = objectMapper;
        this.keyVaultService = keyVaultService;
    }

    @Override
    public byte[] download(ExportFlowDefinitionEntity flow, String fileName) {
        SharePointConfig config = parseConfig(flow);
        String accessToken = getAccessToken(config);

        String siteId = resolveSiteId(config, accessToken);
        String driveId = resolveDriveId(siteId, config.getDriveName(), accessToken);

        String filePath = buildFilePath(flow, fileName);
        String downloadUrl = SharePointPathResolver.buildDriveItemByPathUrl(siteId, driveId, filePath);

        try {
            byte[] result = webClient.get()
                    .uri(downloadUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, response -> {
                        log.info("File not found at SharePoint path, will create new");
                        return Mono.empty();
                    })
                    .bodyToMono(byte[].class)
                    .block(Duration.ofSeconds(60));
            log.info("Downloaded {} bytes from SharePoint", result != null ? result.length : 0);
            return result;
        } catch (Exception e) {
            log.warn("Failed to download from SharePoint: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void upload(ExportFlowDefinitionEntity flow, byte[] content, String fileName) {
        SharePointConfig config = parseConfig(flow);
        String accessToken = getAccessToken(config);

        String siteId = resolveSiteId(config, accessToken);
        String driveId = resolveDriveId(siteId, config.getDriveName(), accessToken);

        String filePath = buildFilePath(flow, fileName);
        String uploadUrl = SharePointPathResolver.buildDriveItemByPathUrl(siteId, driveId, filePath);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                if (content.length <= SIMPLE_UPLOAD_MAX_SIZE) {
                    simpleUpload(uploadUrl, content, accessToken);
                } else {
                    largeFileUpload(driveId, filePath, content, accessToken);
                }
                log.info("Uploaded {} bytes to SharePoint: {}", content.length, filePath);
                return;
            } catch (Exception e) {
                if (attempt == MAX_RETRIES) {
                    throw new RuntimeException("SharePoint upload failed after " + MAX_RETRIES + " retries", e);
                }
                log.warn("Upload attempt {}/{} failed: {}", attempt, MAX_RETRIES, e.getMessage());
                try {
                    Thread.sleep(1000L * attempt); // exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry", ie);
                }
            }
        }
    }

    @Override
    public boolean testConnection(ExportFlowDefinitionEntity flow) {
        try {
            SharePointConfig config = parseConfig(flow);
            String accessToken = getAccessToken(config);
            String siteId = resolveSiteId(config, accessToken);
            resolveDriveId(siteId, config.getDriveName(), accessToken);
            log.info("SharePoint connection test successful");
            return true;
        } catch (Exception e) {
            log.warn("SharePoint connection test failed: {}", e.getMessage());
            return false;
        }
    }

    private String getAccessToken(SharePointConfig config) {
        String cacheKey = config.getTenantId() + ":" + config.getClientId();
        CachedToken cached = tokenCache.get(cacheKey);
        if (cached != null && cached.expiry.isAfter(Instant.now())) {
            return cached.token;
        }

        String clientSecret = resolveSecret(config.getSecretKeyVaultRef());

        String tokenUrl = String.format(
                "https://login.microsoftonline.com/%s/oauth2/v2.0/token", config.getTenantId());

        String responseBody = webClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(String.format(
                        "client_id=%s&client_secret=%s&scope=%s&grant_type=client_credentials",
                        config.getClientId(), clientSecret,
                        "https://graph.microsoft.com/.default"))
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(30));

        try {
            JsonNode json = objectMapper.readTree(responseBody);
            String accessToken = json.get("access_token").asText();
            int expiresIn = json.get("expires_in").asInt(3600);
            tokenCache.put(cacheKey, new CachedToken(accessToken,
                    Instant.now().plusSeconds(expiresIn - 300))); // 5 min buffer
            return accessToken;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse OAuth2 token response", e);
        }
    }

    private String resolveSiteId(SharePointConfig config, String accessToken) {
        SharePointPathResolver.ResolvedPath resolved = SharePointPathResolver.resolve(config.getSiteUrl());
        String siteUrl = SharePointPathResolver.buildGraphSiteUrl(resolved.tenant(), resolved.siteName());

        String responseBody = webClient.get()
                .uri(siteUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(30));

        try {
            JsonNode json = objectMapper.readTree(responseBody);
            return json.get("id").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve SharePoint site ID", e);
        }
    }

    private String resolveDriveId(String siteId, String driveName, String accessToken) {
        String drivesUrl = SharePointPathResolver.buildDrivesUrl(siteId);

        String responseBody = webClient.get()
                .uri(drivesUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(30));

        try {
            JsonNode json = objectMapper.readTree(responseBody);
            for (JsonNode drive : json.get("value")) {
                if (driveName.equals(drive.get("name").asText())) {
                    return drive.get("id").asText();
                }
            }
            // Fallback to first drive
            return json.get("value").get(0).get("id").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve SharePoint drive ID", e);
        }
    }

    private void simpleUpload(String uploadUrl, byte[] content, String accessToken) {
        webClient.put()
                .uri(uploadUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .bodyValue(content)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new RuntimeException(
                                        "SharePoint upload failed: " + response.statusCode() + " " + body))))
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(60));
    }

    private void largeFileUpload(String driveId, String filePath, byte[] content, String accessToken) {
        // Create upload session
        String sessionUrl = String.format(
                "https://graph.microsoft.com/v1.0/drives/%s/root:/%s:/createUploadSession",
                driveId, filePath);

        String sessionResponse = webClient.post()
                .uri(sessionUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("item", Map.of("@microsoft.graph.conflictBehavior", "replace")))
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(30));

        try {
            JsonNode json = objectMapper.readTree(sessionResponse);
            String uploadUrl = json.get("uploadUrl").asText();

            // Upload in chunks of 10MB
            int chunkSize = 10 * 1024 * 1024;
            int offset = 0;
            while (offset < content.length) {
                int end = Math.min(offset + chunkSize, content.length);
                byte[] chunk = new byte[end - offset];
                System.arraycopy(content, offset, chunk, 0, chunk.length);

                String contentRange = String.format("bytes %d-%d/%d", offset, end - 1, content.length);
                webClient.put()
                        .uri(uploadUrl)
                        .header("Content-Range", contentRange)
                        .bodyValue(chunk)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block(Duration.ofSeconds(120));

                offset = end;
            }
        } catch (Exception e) {
            throw new RuntimeException("Large file upload failed", e);
        }
    }

    private String buildFilePath(ExportFlowDefinitionEntity flow, String fileName) {
        String basePath = flow.getTargetPath();
        // Extract relative path from SharePoint URL
        try {
            SharePointPathResolver.ResolvedPath resolved = SharePointPathResolver.resolve(basePath);
            return resolved.relativePath() + "/" + fileName;
        } catch (Exception e) {
            return fileName;
        }
    }

    private SharePointConfig parseConfig(ExportFlowDefinitionEntity flow) {
        try {
            return objectMapper.readValue(flow.getSharepointConfig(), SharePointConfig.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse SharePoint configuration", e);
        }
    }

    private String resolveSecret(String secretKeyVaultRef) {
        if (secretKeyVaultRef == null || secretKeyVaultRef.isBlank()) {
            throw new IllegalArgumentException(
                    "SharePoint secretKeyVaultRef is not configured. " +
                    "Set SHAREPOINT_SECRET_KEYVAULT_REF or provide it in sharepoint_config.");
        }
        return keyVaultService.getSecret(secretKeyVaultRef);
    }

    private record CachedToken(String token, Instant expiry) {}
}
