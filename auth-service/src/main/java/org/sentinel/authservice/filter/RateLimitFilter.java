package org.sentinel.authservice.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    private static final int MAX_LOGIN_ATTEMPTS_PER_HOUR = 5;

    private final Map<String, RateLimitInfo> requestCounts = new ConcurrentHashMap<>();
    private final Map<String, RateLimitInfo> loginAttempts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String clientIP = getClientIP(request);
        String path = request.getRequestURI();

        // Apply stricter rate limiting to log in endpoint
        if (path.contains("/api/v1/auth/login") && isLoginRateLimitExceeded(clientIP)) {
                log.warn("Login rate limit exceeded for IP: {}", clientIP);
                sendRateLimitResponse(response, "Too many login attempts. Please try again later.");
                return;
            }

        // General rate limiting for all endpoints
        if (isGeneralRateLimitExceeded(clientIP)) {
            log.warn("General rate limit exceeded for IP: {}", clientIP);
            sendRateLimitResponse(response, "Too many requests. Please try again later.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isGeneralRateLimitExceeded(String clientIP) {
        RateLimitInfo info = requestCounts.computeIfAbsent(
                clientIP,
                k -> new RateLimitInfo()
        );

        if (info.shouldReset(60)) { // 1 minute window
            info.reset();
        }

        return info.increment() > MAX_REQUESTS_PER_MINUTE;
    }

    private boolean isLoginRateLimitExceeded(String clientIP) {
        RateLimitInfo info = loginAttempts.computeIfAbsent(
                clientIP,
                k -> new RateLimitInfo()
        );

        if (info.shouldReset(3600)) { // 1 hour window
            info.reset();
        }

        return info.increment() > MAX_LOGIN_ATTEMPTS_PER_HOUR;
    }

    private void sendRateLimitResponse(HttpServletResponse response, String message)
            throws IOException {
        response.setStatus(429); // Too Many Requests
        response.setContentType("application/json");
        response.getWriter().write(String.format(
                "{\"error\":\"rate_limit_exceeded\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
                message,
                Instant.now()
        ));
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }

    private static class RateLimitInfo {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart = System.currentTimeMillis();

        public int increment() {
            return count.incrementAndGet();
        }

        public boolean shouldReset(long windowSeconds) {
            long elapsed = (System.currentTimeMillis() - windowStart) / 1000;
            return elapsed > windowSeconds;
        }

        public void reset() {
            count.set(0);
            windowStart = System.currentTimeMillis();
        }
    }
}