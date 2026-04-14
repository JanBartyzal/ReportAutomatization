package com.reportplatform.qry.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@SuppressWarnings("deprecation")
public class WebMvcConfig implements WebMvcConfigurer {

    private final RlsInterceptor rlsInterceptor;

    public WebMvcConfig(RlsInterceptor rlsInterceptor) {
        this.rlsInterceptor = rlsInterceptor;
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(rlsInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/v1/events/**"); // Exclude Dapr event endpoints
    }

    @Override
    public void configurePathMatch(@NonNull PathMatchConfigurer configurer) {
        // Nginx router appends trailing slashes; Spring Boot 3.x dropped
        // trailing-slash matching by default.
        configurer.setUseTrailingSlashMatch(true);
    }
}
