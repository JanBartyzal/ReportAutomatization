package com.reportplatform.snow.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class TestConnectionRequest {

    @NotBlank(message = "Instance URL is required")
    @Size(max = 500, message = "Instance URL must not exceed 500 characters")
    private String instanceUrl;

    @NotNull(message = "Auth type is required")
    private String authType;

    @NotBlank(message = "Credentials reference is required")
    @Size(max = 255, message = "Credentials reference must not exceed 255 characters")
    private String credentialsRef;

    public String getInstanceUrl() {
        return instanceUrl;
    }

    public void setInstanceUrl(String instanceUrl) {
        this.instanceUrl = instanceUrl;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public String getCredentialsRef() {
        return credentialsRef;
    }

    public void setCredentialsRef(String credentialsRef) {
        this.credentialsRef = credentialsRef;
    }
}
