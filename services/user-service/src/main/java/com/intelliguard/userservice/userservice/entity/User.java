package com.intelliguard.userservice.userservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(length = 100)
    private String firstName;

    @Column(length = 100)
    private String lastName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.PENDING_VERIFICATION;

    @Column(nullable = false)
    private Integer failedLoginAttempts = 0;

    @Column
    private LocalDateTime lockedUntil;

    @Column(nullable = false)
    private Boolean emailVerified = false;

    @Column(length = 255)
    private String emailVerificationToken;

    @Column(length = 255)
    private String passwordResetToken;

    @Column
    private LocalDateTime passwordResetExpiresAt;

    @Column
    private LocalDateTime lastPasswordChangeAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime lastLoginAt;

    @Column
    private LocalDateTime deletedAt;

    public enum UserStatus {
        PENDING_VERIFICATION,
        ACTIVE,
        SUSPENDED,
        DELETED
    }

    public boolean isAccountNonLocked() {
        if (lockedUntil == null) {
            return true;
        }
        return LocalDateTime.now().isAfter(lockedUntil);
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE && emailVerified && deletedAt == null;
    }
}
