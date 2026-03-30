package org.sentinel.apigateway.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret-key}")
    private String secretKeyString; // either Base64 OR raw string

    private SecretKey secretKey;
    private JwtParser jwtParser;

    @PostConstruct
    public void init() {
        try {
            byte[] keyBytes = decodeKey(secretKeyString);
            validateKeyLength(keyBytes);
            this.secretKey = Keys.hmacShaKeyFor(keyBytes);
            this.jwtParser = Jwts.parser()
                    .verifyWith(this.secretKey)
                    .build();
            log.info("JWT Token Provider initialized (HMAC)");
        } catch (Exception e) {
            log.error("Failed to initialize JWT token provider", e);
            throw new RuntimeException("JWT initialization failed", e);
        }
    }

    private byte[] decodeKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("JWT secret key must be configured");
        }
        try {
            // try base64 first
            return Decoders.BASE64.decode(key);
        } catch (IllegalArgumentException ex) {
            // not valid base64 — use raw bytes
//            return key.getBytes(StandardCharsets.UTF_8);
            throw new IllegalStateException("JWT secret must be Base64-encoded and at least 256 bits. Generate one using: openssl rand -base64 32", ex);
        }
    }

    private void validateKeyLength(byte[] keyBytes) {
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret key must be at least 256 bits (32 bytes)");
        }
    }

    public boolean validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.warn("Token is null or empty");
            return false;
        }
        try {
            jwtParser.parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token expired: {}", e.getMessage());
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    private Claims extractAllClaims(String token) {
        return jwtParser
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Date exp = claims.getExpiration();
            return exp != null && exp.before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Error checking token expiration: {}", e.getMessage());
            // treat parse error as expired/invalid
            return true;
        }
    }

    public String extractUsername(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getSubject();
        } catch (JwtException e) {
            log.error("Failed to extract username: {}", e.getMessage());
            return null;
        }
    }

    public String extractUserId(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Object userId = claims.get("userId");
            if (userId != null) {
                return userId.toString();
            }
            return claims.getSubject();
        } catch (JwtException e) {
            log.error("Failed to extract user id: {}", e.getMessage());
            return null;
        }
    }

    public List<String> extractAuthorities(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Object obj = claims.get("authorities");
            if (obj instanceof List<?>) {
                return ((List<?>) obj).stream()
                        .map(Object::toString)
                        .toList();
            }
            // trying "roles" as fallback
            Object rolesObj = claims.get("roles");
            if (rolesObj instanceof List<?>) {
                return ((List<?>) rolesObj).stream()
                        .map(Object::toString)
                        .toList();
            }
            return Collections.emptyList();
        } catch (JwtException e) {
            log.error("Failed to extract authorities: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            String tokenType = claims.get("tokenType", String.class);
            return "refresh".equalsIgnoreCase(tokenType);
        } catch (JwtException e) {
            return false;
        }
    }

    public Date extractExpiration(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getExpiration();
        } catch (JwtException e) {
            log.error("Failed to extract expiration: {}", e.getMessage());
            return null;
        }
    }

    public Object extractClaim(String token, String claimName) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.get(claimName);
        } catch (JwtException e) {
            log.error("Failed to extract claim '{}': {}", claimName, e.getMessage());
            return null;
        }
    }
}