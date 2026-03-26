package com.reportplatform.enginereporting.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RlsInterceptor rlsInterceptor;

    public WebMvcConfig(RlsInterceptor rlsInterceptor) {
        this.rlsInterceptor = rlsInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rlsInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/v1/events/**");
    }
}
