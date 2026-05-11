package org.sentinel.authservice.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sentinel.authservice.model.RefreshToken;
import org.sentinel.authservice.model.User;
import org.sentinel.authservice.exception.InvalidTokenException;
import org.sentinel.authservice.repository.RefreshTokenRepository;
import org.sentinel.authservice.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    @Value("${application.security.refresh-token.max-active-per-user:5}")
    private int maxActiveTokensPerUser;

    /**
     * Creates and persists a new refresh token
     */
    @Transactional
    public String createRefreshToken(User user, HttpServletRequest request) {

        // Enforce token limit per user
        enforceTokenLimit(user.getUserId());

        // Generate token family ID for rotation tracking
        UUID tokenFamilyId = UUID.randomUUID();

        // Generate the actual JWT refresh token
        UserDetails userDetails = new CustomUserDetails(user);
        String tokenString = jwtUtil.generateRefreshToken(userDetails);

        // Hash the token before storing (NEVER store plain tokens)
        String tokenHash = hashToken(tokenString);

        RefreshToken refreshToken = RefreshToken.builder()
                .tokenHash(tokenHash)
                .user(user)
                .tokenFamilyId(tokenFamilyId)
                .deviceInfo(extractDeviceInfo(request))
                .ipAddress(extractIpAddress(request))
                .userAgent(extractUserAgent(request))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusMillis(jwtUtil.getRefreshExpiration()))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
        log.info("Created refresh token for user: {}, family: {}", user.getUserId(), tokenFamilyId);

        // Return the actual JWT token (not the hash)
        return tokenString;
    }

    /**
     * Validates refresh token and checks against database
     */
    @Transactional
    public RefreshToken validateAndGetRefreshToken(String token) {

        // First validate JWT structure and signature
        if (!jwtUtil.isRefreshToken(token)) {
            log.warn("Invalid token type provided");
            throw new InvalidTokenException("Not a refresh token");
        }

        String tokenHash = hashToken(token);

        // Find in database
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found or already used"));

        // Security checks
        if (storedToken.isRevoked()) {
            // CRITICAL: Token reuse detected - revoke entire family
            log.warn("SECURITY ALERT: Revoked token reused! User: {}, Family: {}",
                    storedToken.getUser().getUserId(), storedToken.getTokenFamilyId());
            revokeTokenFamily(storedToken.getTokenFamilyId(), "Token reuse detected - possible theft");
            throw new SecurityException("Token reuse detected. All tokens in family revoked for security.");
        }

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            log.debug("Refresh token expired for user: {}", storedToken.getUser().getUserId());
            throw new InvalidTokenException("Refresh token expired");
        }

        // Update last used timestamp
        storedToken.setLastUsedAt(Instant.now());
        refreshTokenRepository.save(storedToken);

        return storedToken;
    }

    /**
     * Rotates refresh token (revokes old, creates new in same family)
     */
    @Transactional
    public String rotateRefreshToken(RefreshToken oldToken, HttpServletRequest request) {

        // Revoke the old token immediately
        oldToken.setRevoked(true);
        oldToken.setRevokedAt(Instant.now());
        oldToken.setRevocationReason("Rotated");
        refreshTokenRepository.save(oldToken);

        // Generate new JWT token
        UserDetails userDetails = new CustomUserDetails(oldToken.getUser());
        String newTokenString = jwtUtil.generateRefreshToken(userDetails);
        String newTokenHash = hashToken(newTokenString);

        // Create new token in the same family
        RefreshToken newToken = RefreshToken.builder()
                .tokenHash(newTokenHash)
                .user(oldToken.getUser())
                .tokenFamilyId(oldToken.getTokenFamilyId()) // SAME FAMILY - important for security
                .deviceInfo(extractDeviceInfo(request))
                .ipAddress(extractIpAddress(request))
                .userAgent(extractUserAgent(request))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusMillis(jwtUtil.getRefreshExpiration()))
                .revoked(false)
                .build();

        refreshTokenRepository.save(newToken);

        log.info("Rotated refresh token for user: {}, family: {}",
                oldToken.getUser().getUserId(), oldToken.getTokenFamilyId());

        return newTokenString;
    }

    /**
     * Revoke all tokens in a family (for security incidents)
     */
    @Transactional
    public void revokeTokenFamily(UUID tokenFamilyId, String reason) {
        int revoked = refreshTokenRepository.revokeByFamilyId(
                tokenFamilyId, Instant.now(), reason);
        log.warn("SECURITY: Revoked {} tokens in family: {} - Reason: {}", revoked, tokenFamilyId, reason);
    }

    /**
     * Revoke all user tokens (on logout all devices, password change, etc.)
     */
    @Transactional
    public void revokeAllUserTokens(UUID userId, String reason) {
        int revoked = refreshTokenRepository.revokeAllByUserId(
                userId, Instant.now(), reason);
        log.info("Revoked {} tokens for user: {} - Reason: {}", revoked, userId, reason);
    }

    /**
     * Revoke specific token by its JWT string
     */
    @Transactional
    public void revokeToken(String token, String reason) {
        String tokenHash = hashToken(token);
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Token not found"));

        if (!refreshToken.isRevoked()) {
            refreshToken.setRevoked(true);
            refreshToken.setRevokedAt(Instant.now());
            refreshToken.setRevocationReason(reason);
            refreshTokenRepository.save(refreshToken);

            log.info("Revoked refresh token for user: {} - Reason: {}",
                    refreshToken.getUser().getUserId(), reason);
        }
    }

    /**
     * Get all active tokens for a user
     */
    @Transactional(readOnly = true)
    public List<RefreshToken> getActiveUserTokens(UUID userId) {
        return refreshTokenRepository.findByUserUserIdAndRevokedFalseOrderByIssuedAtDesc(userId);
    }

    /**
     * Enforce maximum active tokens per user
     * Revokes oldest token when limit exceeded
     */
    private void enforceTokenLimit(UUID userId) {
        long activeTokens = refreshTokenRepository.countActiveByUserId(userId);

        if (activeTokens >= maxActiveTokensPerUser) {
            List<RefreshToken> userTokens = refreshTokenRepository.findByUserUserIdAndRevokedFalseOrderByIssuedAtDesc(userId);

            // Find and revoke the oldest token
            userTokens.stream()
                    .min(Comparator.comparing(RefreshToken::getIssuedAt))
                    .ifPresent(oldest -> {
                        oldest.setRevoked(true);
                        oldest.setRevokedAt(Instant.now());
                        oldest.setRevocationReason("Token limit exceeded");
                        refreshTokenRepository.save(oldest);
                        log.info("Revoked oldest token for user: {} due to limit", userId);
                    });
        }
    }

    /**
     * Hash token using SHA-256 (one-way hash)
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Extract IP address from request (handles proxy headers)
     */
    private String extractIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // If multiple IPs in X-Forwarded-For, get the first one
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * Extract user agent from request
     */
    private String extractUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null ? userAgent : "Unknown";
    }

    /**
     * Extract basic device info (can be enhanced with libraries)
     */
    private String extractDeviceInfo(HttpServletRequest request) {
        String userAgent = extractUserAgent(request);

        // Basic detection
        if (userAgent.contains("Mobile")) {
            return "Mobile";
        } else if (userAgent.contains("Tablet")) {
            return "Tablet";
        } else {
            return "Desktop";
        }
    }

    /**
     * Cleanup expired and old revoked tokens
     * Runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        Instant now = Instant.now();
        Instant cutoffDate = now.minus(Duration.ofDays(30)); // Keep revoked tokens for 30 days for audit

        int deleted = refreshTokenRepository.deleteExpiredAndOldRevoked(now, cutoffDate);
        log.info("Cleaned up {} expired/old refresh tokens", deleted);
    }
}