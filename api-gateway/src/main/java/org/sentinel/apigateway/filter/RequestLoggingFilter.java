package org.sentinel.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Global filter for request/response logging
 */
@Component
@Slf4j
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();

        ServerHttpRequest request = exchange.getRequest();
        String requestId = generateRequestId(request);

        // Adding request ID's to headers
        ServerHttpRequest modifiedRequest = request.mutate()
                .header(REQUEST_ID_HEADER, requestId)
                .header(TRACE_ID_HEADER, requestId)
                .build();

        log.info("Incoming Request: {} {} - RequestId: {} - IP: {}",
                request.getMethod(),
                request.getPath(),
                requestId,
                getClientIP(request));

        return chain.filter(exchange.mutate().request(modifiedRequest).build())
                .then(Mono.fromRunnable(() -> {
                    long duration = System.currentTimeMillis() - startTime;

                    log.info("Response: {} {} - Status: {} - Duration: {}ms - RequestId: {}",
                            request.getMethod(),
                            request.getPath(),
                            exchange.getResponse().getStatusCode(),
                            duration,
                            requestId);
                }));
    }

    private String generateRequestId(ServerHttpRequest request) {
        String existingRequestId = request.getHeaders().getFirst(REQUEST_ID_HEADER);
        return existingRequestId != null ? existingRequestId : UUID.randomUUID().toString();
    }

    private String getClientIP(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        if (request.getRemoteAddress() != null) {
            return request
                    .getRemoteAddress().
                    getAddress().
                    getHostAddress();
        }

        return "unknown";
    }

    @Override
    public int getOrder() {
        return -2;
    }
}