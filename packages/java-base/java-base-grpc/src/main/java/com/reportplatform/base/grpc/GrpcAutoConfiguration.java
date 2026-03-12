package com.reportplatform.base.grpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot auto-configuration for gRPC context propagation interceptors.
 * <p>
 * Automatically registers both server and client interceptors when gRPC
 * classes are on the classpath. Can be disabled with
 * {@code reportplatform.grpc.context-propagation.enabled=false}.
 */
@AutoConfiguration
@ConditionalOnClass(io.grpc.ServerInterceptor.class)
@ConditionalOnProperty(
        prefix = "reportplatform.grpc.context-propagation",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class GrpcAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(GrpcAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public ContextPropagationServerInterceptor contextPropagationServerInterceptor() {
        log.info("Registering gRPC ContextPropagationServerInterceptor");
        return new ContextPropagationServerInterceptor();
    }

    @Bean
    @ConditionalOnMissingBean
    public ContextPropagationClientInterceptor contextPropagationClientInterceptor() {
        log.info("Registering gRPC ContextPropagationClientInterceptor");
        return new ContextPropagationClientInterceptor();
    }
}
