package com.reportplatform.enginecore.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Re-enables trailing slash matching for all controllers.
 * Spring Boot 3.x disabled this by default, but nginx rewrites
 * bare API paths (e.g. /api/batches → /api/batches/) for location matching.
 */
@SuppressWarnings("deprecation")
@Configuration
public class TrailingSlashConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.setUseTrailingSlashMatch(true);
    }
}
