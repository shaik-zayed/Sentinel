package org.sentinel.authservice.service;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sentinel.authservice.exception.TokenBlacklistException;
import org.sentinel.authservice.model.TokenBlacklist;
import org.sentinel.authservice.repository.TokenBlacklistRepository;
import org.sentinel.authservice.util.JwtUtil;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service for managing blacklisted ACCESS TOKENS.
 * <p>
 * Important: This is for access tokens only (short-lived, stateless).
 * Refresh tokens should be managed via RefreshTokenService (long-lived, stateful with rotation).
 * <p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final TokenBlacklistRepository blacklistRepository;
    private final JwtUtil jwtUtil;
    private final CacheManager cacheManager;

    /**
     * Blacklist an access token (on logout or revocation)
     */
    @Transactional
    public void blacklistToken(String token, UUID userId, String reason) {
        try {
            String jti = jwtUtil.extractClaim(token, Claims::getId);
            Instant expiresAt = jwtUtil.extractExpirationAsInstantTime(token);

            if (blacklistRepository.existsByTokenJti(jti)) {
                log.debug("Token already blacklisted: {}", jti);
                return;
            }

            TokenBlacklist blacklistedToken = TokenBlacklist.builder()
                    .tokenJti(jti)
                    .userId(userId)
                    .expiresAt(expiresAt)
                    .reason(reason)
                    .build();

            blacklistRepository.save(blacklistedToken);

            // Evicts only this specific JTI from cache so that future checks hit the DB
            Cache cache = cacheManager.getCache("tokenBlacklist");
            if (cache != null) {
                cache.evict(jti);
            }

            log.info("Blacklisted access token - JTI: {}, User: {}, Reason: {}", jti, userId, reason);

        } catch (Exception e) {
            log.error("Failed to blacklist token: {}", e.getMessage(), e);
            throw new TokenBlacklistException("Token blacklisting failed", e);
        }
    }

    /**
     * Checks if token JTI is blacklisted (with caching)
     */
    @Cacheable(value = "tokenBlacklist", key = "#jti", unless = "#result == false")
    public boolean isTokenBlacklisted(String jti) {
        return blacklistRepository.existsByTokenJti(jti);
    }

    /**
     * Checks if token is blacklisted by extracting JTI from token
     */
    public boolean isTokenBlacklistedByToken(String token) {
        try {
            String jti = jwtUtil.extractClaim(token, Claims::getId);
            return isTokenBlacklisted(jti);
        } catch (Exception e) {
            log.warn("Error checking blacklist for token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Cleanup expired blacklist entries
     * Runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpiredBlacklist() {
        Instant now = Instant.now();
        int deleted = blacklistRepository.deleteExpiredTokens(now);

        // Clear entire cache after DB cleanup to remove all expired entries
        Cache cache = cacheManager.getCache("tokenBlacklist");
        if (cache != null) {
            cache.clear();
        }

        log.info("Cleaned up {} expired blacklist entries and cleared cache", deleted);
    }

    /**
     * Gets blacklist statistics for a user (useful for admin dashboard for later usage)
     */
    @Transactional(readOnly = true)
    public long getBlacklistCountForUser(UUID userId) {
        return blacklistRepository.countByUserId(userId);
    }
}