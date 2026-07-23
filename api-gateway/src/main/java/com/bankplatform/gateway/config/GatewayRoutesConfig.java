package com.bankplatform.gateway.config;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouterFunction<ServerResponse> userServiceRoute(
            @Value("${bank-platform.routes.user-service}") String userServiceUri,
            RedisSlidingWindowRateLimiter rateLimiter) {
        return route("user-service")
                .route(
                        path("/api/v1/users/**")
                                .or(path("/api/v1/auth/**"))
                                .or(path("/.well-known/jwks.json")),
                        http())
                .before(uri(userServiceUri))
                .filter(rateLimitFilter(rateLimiter))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> accountServiceRoute(
            @Value("${bank-platform.routes.account-service}") String accountServiceUri,
            RedisSlidingWindowRateLimiter rateLimiter) {
        return route("account-service")
                .route(path("/api/v1/accounts/**"), http())
                .before(uri(accountServiceUri))
                .filter(rateLimitFilter(rateLimiter))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> transactionServiceRoute(
            @Value("${bank-platform.routes.transaction-service}") String transactionServiceUri,
            RedisSlidingWindowRateLimiter rateLimiter) {
        return route("transaction-service")
                .route(path("/api/v1/transactions/**"), http())
                .before(uri(transactionServiceUri))
                .filter(rateLimitFilter(rateLimiter))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> auditServiceRoute(
            @Value("${bank-platform.routes.audit-service}") String auditServiceUri,
            RedisSlidingWindowRateLimiter rateLimiter) {
        return route("audit-service")
                .route(path("/api/v1/audit/**"), http())
                .before(uri(auditServiceUri))
                .filter(rateLimitFilter(rateLimiter))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> paymentServiceRoute(
            @Value("${bank-platform.routes.payment-service}") String paymentServiceUri,
            RedisSlidingWindowRateLimiter rateLimiter) {
        return route("payment-service")
                .route(path("/api/v1/payments/**"), http())
                .before(uri(paymentServiceUri))
                .filter(rateLimitFilter(rateLimiter))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> reportingServiceRoute(
            @Value("${bank-platform.routes.reporting-service}") String reportingServiceUri,
            RedisSlidingWindowRateLimiter rateLimiter) {
        return route("reporting-service")
                .route(path("/api/v1/reports/**"), http())
                .before(uri(reportingServiceUri))
                .filter(rateLimitFilter(rateLimiter))
                .build();
    }

    private HandlerFilterFunction<ServerResponse, ServerResponse> rateLimitFilter(
            RedisSlidingWindowRateLimiter rateLimiter) {
        return (request, next) -> {
            String key =
                    request.remoteAddress()
                            .map(address -> address.getAddress().getHostAddress())
                            .orElse("unknown");
            if (rateLimiter.tryAcquire(key)) {
                return next.handle(request);
            }
            return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS).build();
        };
    }
}
