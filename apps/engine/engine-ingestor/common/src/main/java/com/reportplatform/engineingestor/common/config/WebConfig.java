package com.reportplatform.engineingestor.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Enable trailing slash matching so that /api/upload and /api/upload/ both resolve.
 * Needed because nginx gateway rewrites add trailing slashes.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @SuppressWarnings("deprecation")
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.setUseTrailingSlashMatch(true);
    }
}
