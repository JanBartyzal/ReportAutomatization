package com.reportplatform.dash.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@SuppressWarnings("deprecation")
public class WebConfig implements WebMvcConfigurer {

    private final RlsInterceptor rlsInterceptor;

    public WebConfig(RlsInterceptor rlsInterceptor) {
        this.rlsInterceptor = rlsInterceptor;
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(rlsInterceptor)
                .addPathPatterns("/api/**");
    }

    @Override
    public void configurePathMatch(@NonNull PathMatchConfigurer configurer) {
        // Nginx router appends trailing slashes; Spring Boot 3.x dropped
        // trailing-slash matching by default, causing 403 on /api/dashboards/.
        configurer.setUseTrailingSlashMatch(true);
    }
}
