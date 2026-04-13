package com.reportplatform.excelsync.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableConfigurationProperties(ExcelSyncProperties.class)
public class ExcelSyncConfig {

    @Bean(name = "excelSyncExecutor")
    public Executor excelSyncExecutor(ExcelSyncProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getThreadPoolSize());
        executor.setMaxPoolSize(properties.getThreadPoolSize() * 2);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("excel-sync-");
        executor.initialize();
        return executor;
    }
}
