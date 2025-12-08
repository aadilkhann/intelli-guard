package com.intelliguard.userservice.userservice.service;

import com.intelliguard.userservice.userservice.dto.*;
import com.intelliguard.userservice.userservice.entity.RefreshToken;
import com.intelliguard.userservice.userservice.entity.Role;
import com.intelliguard.userservice.userservice.entity.User;
import com.intelliguard.userservice.userservice.repository.RefreshTokenRepository;
import com.intelliguard.userservice.userservice.repository.RoleRepository;
import com.intelliguard.userservice.userservice.repository.UserRepository;
import com.intelliguard.userservice.userservice.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${jwt.expiration:3600000}")
    private Long jwtExpiration;

    @Value("${jwt.refresh-expiration:604800000}")
    private Long refreshExpiration;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        // Get default role (VIEWER)
        Role defaultRole = roleRepository.findByName("VIEWER")
                .orElseThrow(() -> new IllegalStateException("Default role not found"));

        // Create user
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(defaultRole)
                .status(User.UserStatus.PENDING_VERIFICATION)
                .emailVerified(false)
                .failedLoginAttempts(0)
                .emailVerificationToken(UUID.randomUUID().toString())
                .build();

        user = userRepository.save(user);

        // Generate tokens
        String accessToken = jwtUtil.generateToken(user.getEmail(), user.getRole().getName(), user.getId());
        String refreshTokenValue = jwtUtil.generateRefreshToken(user.getEmail());

        // Save refresh token
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenValue)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpiration / 1000))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .tokenType("Bearer")
                .expiresIn(jwtExpiration / 1000)
                .user(mapToUserResponse(user))
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        // Check if account is locked
        if (!user.isAccountNonLocked()) {
            throw new IllegalStateException("Account is locked until " + user.getLockedUntil());
        }

        // Check if account is deleted
        if (user.getDeletedAt() != null) {
            throw new IllegalStateException("Account has been deleted");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            handleFailedLogin(user);
            throw new BadCredentialsException("Invalid email or password");
        }

        // Reset failed login attempts
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // Generate tokens
        String accessToken = jwtUtil.generateToken(user.getEmail(), user.getRole().getName(), user.getId());
        String refreshTokenValue = jwtUtil.generateRefreshToken(user.getEmail());

        // Save refresh token (revoke old ones first)
        refreshTokenRepository.revokeAllUserTokens(user, LocalDateTime.now());

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenValue)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpiration / 1000))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .tokenType("Bearer")
                .expiresIn(jwtExpiration / 1000)
                .user(mapToUserResponse(user))
                .build();
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        // Find refresh token
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        // Validate refresh token
        if (!refreshToken.isValid()) {
            throw new IllegalArgumentException("Refresh token is expired or revoked");
        }

        // Get user
        User user = refreshToken.getUser();

        // Generate new access token
        String accessToken = jwtUtil.generateToken(user.getEmail(), user.getRole().getName(), user.getId());

        // Optionally rotate refresh token
        String newRefreshTokenValue = jwtUtil.generateRefreshToken(user.getEmail());

        // Revoke old refresh token
        refreshToken.setRevoked(true);
        refreshToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);

        // Create new refresh token
        RefreshToken newRefreshToken = RefreshToken.builder()
                .user(user)
                .token(newRefreshTokenValue)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpiration / 1000))
                .revoked(false)
                .build();
        refreshTokenRepository.save(newRefreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshTokenValue)
                .tokenType("Bearer")
                .expiresIn(jwtExpiration / 1000)
                .user(mapToUserResponse(user))
                .build();
    }

    private void handleFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        // Lock account after 5 failed attempts for 15 minutes
        if (attempts >= 5) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(15));
        }

        userRepository.save(user);
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().getName())
                .status(user.getStatus().name())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
