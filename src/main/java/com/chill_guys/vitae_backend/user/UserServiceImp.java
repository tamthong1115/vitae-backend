package com.chill_guys.vitae_backend.user;

import com.chill_guys.vitae_backend.auth.OauthAccountRepository;
import com.chill_guys.vitae_backend.auth.UserSessionRepository;
import com.chill_guys.vitae_backend.auth.model.entity.OauthAccount;
import com.chill_guys.vitae_backend.auth.model.entity.UserSession;
import com.chill_guys.vitae_backend.user.model.entity.Role;
import com.chill_guys.vitae_backend.user.model.entity.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;


@Service
@AllArgsConstructor
public class UserServiceImp implements UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OauthAccountRepository oauthAccountRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;

    private static final SecureRandom secureRandom = new SecureRandom();


    private Role getDefaultUserRole() {
        return roleRepository.findByName(Role.RoleName.USER)
                .orElseThrow(() -> new RuntimeException("Default role USER not found"));
    }

    @Override
    @Transactional
    public User create(String email, String accountName, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email is already in use");
        }
        if (userRepository.existsByAccountName(accountName)) {
            throw new RuntimeException("Username is already in use");
        }

        String hashedPassword = password == null ? null : passwordEncoder.encode(password);
        User u = User.builder()
                .email(email)
                .accountName(accountName)
                .passwordHash(hashedPassword)
                .fullName(null)
                .status(User.UserStatus.ACTIVE)
                .isVerified(false)
                .twoFactorEnabled(false)
                .roles(Set.of(getDefaultUserRole()))
                .build();

        return userRepository.save(u);
    }

    @Override
    @Transactional
    public User upsertIdentity(String provider, String providerUserId,
                               String email, boolean emailVerified, String fullName,
                               String accessToken, String refreshToken, Long expiresInSeconds, String scope) {
        var existingIdentity = oauthAccountRepository.findByProviderAndProviderUserId(provider, providerUserId).orElse(null);
        if (existingIdentity != null) {
            var user = existingIdentity.getUser();
            boolean updated = false;
            if (fullName != null && !Objects.equals(fullName, user.getFullName())) {
                user.setFullName(fullName);
                updated = true;
            }
            if (emailVerified && !Objects.equals(email, user.getEmail())) {
                if (userRepository.existsByEmail(email)) {
                    throw new RuntimeException("Email is already in use");
                }
                user.setEmail(email);
                updated = true;
            }
            if (updated) {
                userRepository.save(user);
            }

            // update tokens
            if (accessToken != null) existingIdentity.setAccessToken(accessToken);
            if (refreshToken != null) existingIdentity.setRefreshToken(refreshToken);
            if (expiresInSeconds != null)
                existingIdentity.setTokenExpiresAt(OffsetDateTime.now().plusSeconds(expiresInSeconds));
            if (scope != null) existingIdentity.setScope(scope);
            oauthAccountRepository.save(existingIdentity);
            return user;
        }

        // No existing identity, create new user
        User user = null;
        if (email != null && emailVerified) {
            user = userRepository.findByEmail(email).orElse(null);
        }

        if (user == null) {
            user = userRepository.save(User.builder()
                    .email(email)
                    .accountName(null)
                    .passwordHash(null)
                    .fullName(fullName)
                    .status(User.UserStatus.ACTIVE)
                    .isVerified(emailVerified)
                    .twoFactorEnabled(false)
                    .roles(Set.of(getDefaultUserRole()))
                    .build());
        } else {
            boolean updated = false;
            if (fullName != null && !Objects.equals(fullName, user.getFullName())) {
                user.setFullName(fullName);
                updated = true;
            }
            if (!user.isVerified()) {
                user.setVerified(true);
                updated = true;
            }
            if (updated) {
                userRepository.save(user);
            }
        }

        var ou = OauthAccount.builder()
                .user(user)
                .provider(provider)
                .providerUserId(providerUserId)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .scope(scope)
                .tokenExpiresAt(expiresInSeconds == null ? null : OffsetDateTime.now().plusSeconds(expiresInSeconds))
                .build();
        oauthAccountRepository.save(ou);
        return user;
    }

    @Override
    @Transactional
    public void touchLogin(UUID userId) {
        userRepository.findById(userId).ifPresent(u -> {
            u.setLastLoginAt(OffsetDateTime.now());
            userRepository.save(u);
        });
    }

    @Override
    @Transactional
    public void updatePassword(UUID userId, String newRawPassword) {
        var u = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
        String hashedPassword = passwordEncoder.encode(newRawPassword);
        u.setPasswordHash(hashedPassword);
        userRepository.save(u);
    }


    @Override
    @Transactional
    public void markVerified(UUID userId, boolean verified) {
        var u = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
        u.setVerified(verified);
        userRepository.save(u);
    }

    @Override
    @Transactional(readOnly = true)
    public User loadByEmailOrAccountName(String input) {
        return userRepository.findByEmailOrAccountName(input)
                .orElseThrow(() -> new EntityNotFoundException("User not found with email or username: " + input));

    }

    @Override
    @Transactional(readOnly = true)
    public User getUserBySessionId(UUID sessionId) {
        var session = userSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Session not found"));
        if (session.getRevokedAt() != null || session.getValidUntil().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Session is revoked or expired");
        }
        return session.getUser();
    }

    /**
     * Generates a new opaque token "<sessionId>.<secret>" and persists the session with BCrypt(secret).
     */
    @Override
    @Transactional
    public String createRefreshToken(User user, String userAgent, String ip, String deviceId, long refreshTtlSeconds) {
        var session = UserSession.builder()
                .user(user)
                .userAgent(userAgent)
                .ip(ip)
                .deviceId(deviceId)
                .lastUsedAt(OffsetDateTime.now())
                .validUntil(OffsetDateTime.now().plusSeconds(refreshTtlSeconds))
                .build();
        session = userSessionRepository.save(session);

        String secret = newSecret();
        session.setRefreshTokenHash(secret);
        userSessionRepository.save(session);

        return opaque(session.getId(), secret);
    }

    @Override
    @Transactional
    public String rotateRefreshToken(String opaqueRefreshToken, long newRefreshTtlSeconds) {
        ParsedOpaque parsed = parseOpaque(opaqueRefreshToken);
        UserSession session = userSessionRepository.findById(parsed.sessionId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (session.getRevokedAt() != null || session.getValidUntil().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Invalid/expired refresh token");
        }

        if (!Objects.equals(session.getRefreshTokenHash(), parsed.secret())) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        String newSecret = newSecret();
        session.setRefreshTokenHash(newSecret);
        session.setLastUsedAt(OffsetDateTime.now());
        session.setValidUntil(OffsetDateTime.now().plusSeconds(newRefreshTtlSeconds));
        userSessionRepository.save(session);

        return opaque(session.getId(), newSecret);
    }

    public void revokeSession(UUID sessionId, String reason) {
        var session = userSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Session not found"));

        session.setRevokedAt(OffsetDateTime.now());
        session.setRevokedReason(reason);
        userSessionRepository.save(session);

    }


    public void revokeAllSessionsForUser(UUID userId, String reason) {
        var u = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        var sessions = userSessionRepository.findByUserAndRevokedAtIsNull(u);
        var now = OffsetDateTime.now();
        for (var s : sessions) {
            s.setRevokedAt(now);
            s.setRevokedReason(reason);
        }
        userSessionRepository.saveAll(sessions);

    }


    private static String newSecret() {
        byte[] buf = new byte[32];
        secureRandom.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    private static String opaque(UUID sessionId, String secret) {
        return sessionId.toString() + "." + secret;
    }

    private static ParsedOpaque parseOpaque(String opaque) {
        int dot = opaque.lastIndexOf('.');
        if (dot <= 0) throw new IllegalArgumentException("Invalid opaque token");
        var idPart = opaque.substring(0, dot);
        var secretPart = opaque.substring(dot + 1);
        try {
            return new ParsedOpaque(UUID.fromString(idPart), secretPart);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid opaque token", e);
        }
    }

    private record ParsedOpaque(UUID sessionId, String secret) {
    }

}
