package com.reportplatform.excelsync.connector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SharePointConfig {

    private String tenantId;
    private String clientId;
    private String secretKeyVaultRef;
    private String siteUrl;
    private String driveName = "Documents";

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getSecretKeyVaultRef() { return secretKeyVaultRef; }
    public void setSecretKeyVaultRef(String secretKeyVaultRef) { this.secretKeyVaultRef = secretKeyVaultRef; }

    public String getSiteUrl() { return siteUrl; }
    public void setSiteUrl(String siteUrl) { this.siteUrl = siteUrl; }

    public String getDriveName() { return driveName; }
    public void setDriveName(String driveName) { this.driveName = driveName; }
}
