package com.reportplatform.engineingestor.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Shared Dapr configuration for all engine-ingestor modules.
 * Provides a single Dapr sidecar connection shared across ingestor and scanner.
 */
@Configuration
@ConfigurationProperties(prefix = "dapr.sidecar")
public class DaprConfig {

    private String host = "localhost";
    private int httpPort = 3500;
    private int grpcPort = 50001;

    @Value("${dapr.pubsub.name:reportplatform-pubsub}")
    private String pubsubName;

    @Value("${dapr.pubsub.topic.file-uploaded:file-uploaded}")
    private String fileUploadedTopic;

    @Value("${dapr.scan.app-id:engine-ingestor}")
    private String scanAppId;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    public int getGrpcPort() {
        return grpcPort;
    }

    public void setGrpcPort(int grpcPort) {
        this.grpcPort = grpcPort;
    }

    public String getPubsubName() {
        return pubsubName;
    }

    public String getFileUploadedTopic() {
        return fileUploadedTopic;
    }

    public String getScanAppId() {
        return scanAppId;
    }

    /**
     * Returns the base URL for the Dapr HTTP sidecar.
     */
    public String getSidecarBaseUrl() {
        return "http://" + host + ":" + httpPort;
    }
}
