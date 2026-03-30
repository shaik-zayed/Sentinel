package org.sentinel.authservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users", indexes = {
        @Index(name = "idx_email", columnList = "email"),
        @Index(name = "idx_created_at", columnList = "createdAt"),
        @Index(name = "idx_deleted_at", columnList = "deletedAt")
})
@ToString(exclude = {"password"})
@EqualsAndHashCode(of = {"userId"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID userId;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.USER;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean locked = false;

    @Column(nullable = false)
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    private LocalDateTime lastFailedLoginAt;
    private LocalDateTime lastSuccessfulLoginAt;

    @Column(length = 64)
    private String emailVerificationToken;

    private LocalDateTime emailVerificationTokenExpiresAt;
    private LocalDateTime emailVerifiedAt;

    @Column(length = 64)
    private String passwordResetToken;

    private LocalDateTime passwordResetTokenExpiresAt;

    private LocalDateTime deletedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return deletedAt == null;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
        this.enabled = false;
        this.locked = true;
    }

    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts++;
        this.lastFailedLoginAt = LocalDateTime.now();
        if (this.failedLoginAttempts >= 5) {
            this.locked = true;
        }
    }

    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.lastFailedLoginAt = null;
    }

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }

    public void markEmailAsVerified() {
        this.emailVerifiedAt = LocalDateTime.now();
        this.emailVerificationToken = null;
        this.emailVerificationTokenExpiresAt = null;
        this.enabled = true;
    }
}