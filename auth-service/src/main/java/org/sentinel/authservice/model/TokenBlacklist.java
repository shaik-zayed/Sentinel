package org.sentinel.authservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "token_blacklist")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "token_jti", unique = true, nullable = false, length = 100)
    private String tokenJti;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "blacklisted_at", nullable = false)
    private Instant blacklistedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "reason", length = 100)
    private String reason;

    @PrePersist
    protected void onCreate() {
        if (blacklistedAt == null) {
            blacklistedAt = Instant.now();
        }
    }
}