package com.reportplatform.excelsync.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "excel-sync")
public class ExcelSyncProperties {

    private boolean enabled = true;
    private int threadPoolSize = 4;
    private List<String> allowedPaths = List.of("/mnt/exports");
    private String containerPathPrefix = "/mnt/exports/";
    private long lockTtlMs = 300_000;
    private int lockRetryCount = 3;
    private long lockRetryIntervalMs = 5_000;
    private int maxFileSizeMb = 50;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getThreadPoolSize() { return threadPoolSize; }
    public void setThreadPoolSize(int threadPoolSize) { this.threadPoolSize = threadPoolSize; }

    public List<String> getAllowedPaths() { return allowedPaths; }
    public void setAllowedPaths(List<String> allowedPaths) { this.allowedPaths = allowedPaths; }

    public String getContainerPathPrefix() { return containerPathPrefix; }
    public void setContainerPathPrefix(String containerPathPrefix) { this.containerPathPrefix = containerPathPrefix; }

    public long getLockTtlMs() { return lockTtlMs; }
    public void setLockTtlMs(long lockTtlMs) { this.lockTtlMs = lockTtlMs; }

    public int getLockRetryCount() { return lockRetryCount; }
    public void setLockRetryCount(int lockRetryCount) { this.lockRetryCount = lockRetryCount; }

    public long getLockRetryIntervalMs() { return lockRetryIntervalMs; }
    public void setLockRetryIntervalMs(long lockRetryIntervalMs) { this.lockRetryIntervalMs = lockRetryIntervalMs; }

    public int getMaxFileSizeMb() { return maxFileSizeMb; }
    public void setMaxFileSizeMb(int maxFileSizeMb) { this.maxFileSizeMb = maxFileSizeMb; }
}
