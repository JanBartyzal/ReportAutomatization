package com.reportplatform.enginecore.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Shared Dapr configuration for all engine-core modules.
 * Provides a single Dapr sidecar connection shared across auth, admin, batch, versioning, and audit.
 */
@Configuration
public class DaprConfig {

    @Value("${dapr.sidecar.host:localhost}")
    private String sidecarHost;

    @Value("${dapr.sidecar.http-port:3500}")
    private int sidecarHttpPort;

    @Value("${dapr.sidecar.grpc-port:50001}")
    private int sidecarGrpcPort;

    @Value("${dapr.pubsub.name:report-pubsub}")
    private String pubsubName;

    @Value("${dapr.statestore.name:report-statestore}")
    private String statestoreName;

    public String getSidecarHost() {
        return sidecarHost;
    }

    public int getSidecarHttpPort() {
        return sidecarHttpPort;
    }

    public int getSidecarGrpcPort() {
        return sidecarGrpcPort;
    }

    public String getPubsubName() {
        return pubsubName;
    }

    public String getStatestoreName() {
        return statestoreName;
    }

    /**
     * Returns the base URL for the Dapr HTTP sidecar.
     */
    public String getSidecarBaseUrl() {
        return "http://" + sidecarHost + ":" + sidecarHttpPort;
    }
}
