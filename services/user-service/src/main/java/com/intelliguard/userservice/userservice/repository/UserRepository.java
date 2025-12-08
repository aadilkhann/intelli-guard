package com.intelliguard.userservice.userservice.repository;

import com.intelliguard.userservice.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByEmailVerificationToken(String token);

    Optional<User> findByPasswordResetToken(String token);

    @Query("SELECT u FROM User u WHERE u.status = 'ACTIVE' AND u.deletedAt IS NULL")
    List<User> findAllActiveUsers();

    @Query("SELECT u FROM User u WHERE u.lockedUntil IS NOT NULL AND u.lockedUntil < :now")
    List<User> findLockedUsersExpiredBefore(LocalDateTime now);

    List<User> findByStatus(User.UserStatus status);
}
