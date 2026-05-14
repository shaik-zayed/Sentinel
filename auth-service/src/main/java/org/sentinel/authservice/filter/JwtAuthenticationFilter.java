package org.sentinel.authservice.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.sentinel.authservice.service.TokenBlacklistService;
import org.sentinel.authservice.util.JwtUtil;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        if (isPublicEndpoint(request.getServletPath())) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(7);

            // Check if token is blacklisted
            String jti = jwtUtil.extractClaim(jwt, Claims::getId);
            if (tokenBlacklistService.isTokenBlacklisted(jti)) {
                log.warn("Attempt to use blacklisted token - JTI: {}", jti);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Please login to access.\"}");
                return;
            }

            final String userEmail = jwtUtil.extractUsername(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                authenticateUser(request, jwt, userEmail);
            }

        } catch (ExpiredJwtException e) {
            log.debug("JWT token expired: {}", e.getMessage());
            request.setAttribute("expired", true);
            request.setAttribute("errorMessage", "Token has expired");

        } catch (JwtException e) {
            log.error("JWT token validation error: {}", e.getMessage());
            request.setAttribute("invalid", true);
            request.setAttribute("errorMessage", "Invalid token");

        } catch (UsernameNotFoundException e) {
            log.error("User not found: {}", e.getMessage());
            request.setAttribute("userNotFound", true);
            request.setAttribute("errorMessage", "User not found");

        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage(), e);
            request.setAttribute("authError", true);
            request.setAttribute("errorMessage", "Authentication failed");
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateUser(HttpServletRequest request, String jwt, String userEmail) {
        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

            if (jwtUtil.isTokenValid(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.debug("User authenticated successfully: {}", userEmail);

            } else {
                log.warn("JWT token is invalid for user: {}", userEmail);
                request.setAttribute("invalid", true);
            }
        } catch (Exception e) {
            log.error("Error during user authentication: {}", e.getMessage());
            throw e;
        }
    }

    private boolean isPublicEndpoint(String path) {
        return path.contains("/api/v1/auth/register") ||
                path.contains("/api/v1/auth/login") ||
                path.contains("/api/v1/auth/verify-email") ||
                path.contains("/api/v1/auth/forgot-password") ||
                path.contains("/api/v1/auth/reset-password") ||
                path.contains("/actuator/health") ||
                path.contains("/actuator/info") ||
                path.contains("/swagger-ui") ||
                path.contains("/v3/api-docs");
    }
}