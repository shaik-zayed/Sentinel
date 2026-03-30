package org.sentinel.authservice.repository;

import org.sentinel.authservice.model.TokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for managing blacklisted access tokens.
 * <p>
 * Note: This is for ACCESS TOKENS only (short-lived, stateless).
 * Refresh tokens should be managed via RefreshTokenRepository (long-lived, stateful).
 * <p>
 */
@Repository
public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, UUID> {

    /**
     * Check if a token JTI is blacklisted
     */
    boolean existsByTokenJti(String tokenJti);

    /**
     * Delete expired blacklist entries
     */
    @Modifying
    @Query("DELETE FROM TokenBlacklist t WHERE t.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") Instant now);

    /**
     * Count blacklisted tokens for a specific user
     * (Useful for monitoring)
     */
    @Query("SELECT COUNT(t) FROM TokenBlacklist t WHERE t.userId = :userId")
    long countByUserId(@Param("userId") UUID userId);

    /**
     * Find all blacklisted tokens for a user
     * (Useful for admin dashboard or debugging)
     */
    @Query("SELECT t FROM TokenBlacklist t WHERE t.userId = :userId ORDER BY t.blacklistedAt DESC")
    List<TokenBlacklist> findByUserId(@Param("userId") UUID userId);
}