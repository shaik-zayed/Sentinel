package org.sentinel.apigateway.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sentinel.apigateway.util.JwtTokenProvider;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtValidationFilter implements GlobalFilter, Ordered {

    private final JwtTokenProvider jwtTokenProvider;
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh",
            "/api/v1/auth/verify-email",
            "/api/v1/auth/resend-verification",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/actuator",
            "/health",
            "/metrics"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().toString();

        if (isPublicEndpoint(path)) {
            log.debug("Public endpoint access: {}", path);
            return chain.filter(exchange);
        }
        String token = extractToken(exchange.getRequest());

        if (token == null) {
            log.warn("Missing authorization token for path: {}", path);
            return sendUnauthorized(exchange, "Missing authorization token");
        }

        try {
            if (!jwtTokenProvider.validateToken(token)) {
                log.warn("Invalid token for path: {}", path);
                return sendUnauthorized(exchange, "Invalid or expired token");
            }

            if (jwtTokenProvider.isRefreshToken(token)) {
                log.warn("Refresh token used for API access on path: {}", path);
                return sendUnauthorized(exchange, "Invalid token type");
            }

            String username = jwtTokenProvider.extractUsername(token);
            String userId = jwtTokenProvider.extractUserId(token);
            List<String> authorities = jwtTokenProvider.extractAuthorities(token);

            //-----------------Added to fix UUID error----------------------
            if (userId != null && !isValidUUID(userId)) {
                log.warn("Invalid userId format in JWT token: {}", userId);
                return sendUnauthorized(exchange, "Invalid user identifier format");
            }
            //--------------------------------------------------------------

            ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                    .header("X-User-Email", username)
                    .header("X-User-Id", userId)
                    .header("X-User-Authorities", String.join(",", authorities))
                    .build();

            log.debug("User {} authenticated successfully for path: {}", username, path);
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        } catch (Exception e) {
            log.error("Authentication failed for path {}: {}", path, e.getMessage());
            return sendUnauthorized(exchange, "Authentication failed");
        }
    }

    private boolean isPublicEndpoint(String path) {
        return PUBLIC_ENDPOINTS.stream()
                .anyMatch(path::startsWith);
    }

    private String extractToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(AUTHORIZATION_HEADER);

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }

        return null;
    }

    private Mono<Void> sendUnauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");

        String body = String.format(
                "{\"error\":\"Unauthorized\",\"message\":\"%s\",\"timestamp\":%d}",
                message, System.currentTimeMillis()
        );

        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse()
                        .bufferFactory().wrap(body.getBytes())));
    }

    @Override
    public int getOrder() {
        return -1;
    }

    //-----------------Added to fix UUID error----------------------
    private boolean isValidUUID(String uuid) {
        try {
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    //-------------------------------------------------------------------------
}


