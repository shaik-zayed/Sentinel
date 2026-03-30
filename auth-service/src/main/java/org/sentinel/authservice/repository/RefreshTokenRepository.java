package org.sentinel.authservice.repository;

import org.sentinel.authservice.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByUserUserIdAndRevokedFalseOrderByIssuedAtDesc(UUID userId);//Finding by User's userid(id)

    List<RefreshToken> findByTokenFamilyId(UUID tokenFamilyId);

    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.user.userId = :userId AND rt.revoked = false")
    long countActiveByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :now, rt.revocationReason = :reason WHERE rt.user.userId = :userId AND rt.revoked = false")
    int revokeAllByUserId(@Param("userId") UUID userId,
                          @Param("now") Instant now,
                          @Param("reason") String reason);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :now, rt.revocationReason = :reason WHERE rt.tokenFamilyId = :familyId AND rt.revoked = false")
    int revokeByFamilyId(@Param("familyId") UUID familyId,
                         @Param("now") Instant now,
                         @Param("reason") String reason);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now OR (rt.revoked = true AND rt.revokedAt < :cutoffDate)")
    int deleteExpiredAndOldRevoked(@Param("now") Instant now,
                                   @Param("cutoffDate") Instant cutoffDate);
}