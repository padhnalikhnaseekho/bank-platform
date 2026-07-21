package com.bankplatform.gateway.config;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouterFunction<ServerResponse> userServiceRoute(
            @Value("${bank-platform.routes.user-service}") String userServiceUri) {
        return route("user-service")
                .route(path("/api/v1/users/**").or(path("/api/v1/auth/**")).or(path("/.well-known/jwks.json")),
                        http())
                .before(uri(userServiceUri))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> accountServiceRoute(
            @Value("${bank-platform.routes.account-service}") String accountServiceUri) {
        return route("account-service")
                .route(path("/api/v1/accounts/**"), http())
                .before(uri(accountServiceUri))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> transactionServiceRoute(
            @Value("${bank-platform.routes.transaction-service}") String transactionServiceUri) {
        return route("transaction-service")
                .route(path("/api/v1/transactions/**"), http())
                .before(uri(transactionServiceUri))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> auditServiceRoute(
            @Value("${bank-platform.routes.audit-service}") String auditServiceUri) {
        return route("audit-service")
                .route(path("/api/v1/audit/**"), http())
                .before(uri(auditServiceUri))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> paymentServiceRoute(
            @Value("${bank-platform.routes.payment-service}") String paymentServiceUri) {
        return route("payment-service")
                .route(path("/api/v1/payments/**"), http())
                .before(uri(paymentServiceUri))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> reportingServiceRoute(
            @Value("${bank-platform.routes.reporting-service}") String reportingServiceUri) {
        return route("reporting-service")
                .route(path("/api/v1/reports/**"), http())
                .before(uri(reportingServiceUri))
                .build();
    }
}
