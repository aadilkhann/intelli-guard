package com.intelliguard.userservice.userservice.repository;

import com.intelliguard.userservice.userservice.entity.RefreshToken;
import com.intelliguard.userservice.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findByUser(User user);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :now WHERE rt.user = :user")
    void revokeAllUserTokens(User user, LocalDateTime now);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    void deleteExpiredTokens(LocalDateTime now);

    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user = :user AND rt.revoked = false AND rt.expiresAt > :now")
    List<RefreshToken> findValidTokensByUser(User user, LocalDateTime now);
}
