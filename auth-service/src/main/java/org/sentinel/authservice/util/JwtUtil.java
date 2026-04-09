package org.sentinel.authservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.sentinel.authservice.service.CustomUserDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Utility class for JWT token operations.
 * Handles token generation, validation, and claims extraction.
 */
@Slf4j
@Component
public class JwtUtil {

    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";
    private static final String AUTHORITIES_CLAIM = "authorities";

    private final SecretKey secretKey;
    @Getter
    private final long jwtExpiration;
    @Getter
    private final long refreshExpiration;

    public JwtUtil(
            @Value("${application.security.jwt.secret-key}") String secretKey,
            @Value("${application.security.jwt.access-token.expiration}") long jwtExpiration,
            @Value("${application.security.jwt.refresh-token.expiration}") long refreshExpiration) {

        validateSecretKey(secretKey);
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
        this.jwtExpiration = jwtExpiration;
        this.refreshExpiration = refreshExpiration;

        log.info("JWT Util initialized with expiration: {}ms, refresh: {}ms",
                jwtExpiration, refreshExpiration);
    }

    /**
     * Validates that the secret key meets minimum security requirements
     */
    private void validateSecretKey(String secretKey) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException(
                    "JWT secret key must be configured via environment variable JWT_SECRET_KEY");
        }

        byte[] decodedKey = Decoders.BASE64.decode(secretKey);
        if (decodedKey.length < 32) { // 256 bits minimum for HS256
            throw new IllegalStateException(
                    "JWT secret key must be at least 256 bits (32 bytes) long");
        }
    }

    /**
     * Extracts username (subject) from token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts a specific claim from the token
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
        final Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }

    /**
     * Checks if token is a refresh token
     */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
            return TOKEN_TYPE_REFRESH.equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Generates an access token for the given user
     */
    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put(TOKEN_TYPE_CLAIM, TOKEN_TYPE_ACCESS);
        extraClaims.put(AUTHORITIES_CLAIM, userDetails
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList());

        //-----------------Added to fix UUID error----------------------
        if (userDetails instanceof CustomUserDetails) {
            UUID userId = ((CustomUserDetails) userDetails).getUserId();
            extraClaims.put("userId", userId.toString());
        }
        //-----------------Added to fix UUID error----------------------


        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    /**
     * Generates a refresh token for the given user
     */
    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(TOKEN_TYPE_CLAIM, TOKEN_TYPE_REFRESH);

        // Add userId to refresh token too
        if (userDetails instanceof CustomUserDetails) {
            UUID userId = ((CustomUserDetails) userDetails).getUserId();
            claims.put("userId", userId.toString());
        }

        return buildToken(claims, userDetails, refreshExpiration);
    }

    /**
     * Builds a JWT token with the specified claims and expiration
     */
    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration) {

        long currentTimeMillis = System.currentTimeMillis();
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .id(jti)
                .issuedAt(new Date(currentTimeMillis))
                .expiration(new Date(currentTimeMillis + expiration))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Validates token against user details
     * Checks username match and expiration
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            final String username = extractUsername(token);
            boolean isUsernameValid = username.equals(userDetails.getUsername());
            boolean isNotExpired = !isTokenExpired(token);

            if (!isUsernameValid) {
                log.warn("Token username mismatch. Expected: {}, Found: {}",
                        userDetails.getUsername(), username);
            }

            return isUsernameValid && isNotExpired;

        } catch (ExpiredJwtException e) {
            log.debug("Token expired for user: {}", userDetails.getUsername());
            return false;
        } catch (JwtException e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Checks if token is expired
     */
    private boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /**
     * Extracts expiration date from token
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public Instant extractExpirationAsInstantTime(String token) {
        return extractExpiration(token).toInstant();
    }

    /**
     * Extracts all claims from token
     *
     * @throws ExpiredJwtException if token is expired
     * @throws JwtException        if token is invalid
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.debug("Attempted to parse expired JWT token");
            throw e;
        } catch (JwtException e) {
            log.error("JWT parsing failed: {}", e.getMessage());
            throw new JwtException("Invalid JWT token", e);
        }
    }
}