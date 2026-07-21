package com.bankplatform.common.error;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration
@ConditionalOnClass(AuthenticationEntryPoint.class)
public class SecurityErrorHandlingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ApiErrorResponseWriter apiErrorResponseWriter(ObjectMapper objectMapper) {
        return new ApiErrorResponseWriter(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthenticationEntryPoint jsonAuthenticationEntryPoint(ApiErrorResponseWriter writer) {
        return new JsonAuthenticationEntryPoint(writer);
    }

    @Bean
    @ConditionalOnMissingBean
    public AccessDeniedHandler jsonAccessDeniedHandler(ApiErrorResponseWriter writer) {
        return new JsonAccessDeniedHandler(writer);
    }
}
