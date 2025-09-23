package com.chill_guys.vitae_backend.user;

import com.chill_guys.vitae_backend.user.model.entity.User;
import java.util.UUID;

public interface UserService {
    User create(String email, String accountName, String password);

    /** Upsert/link a social identity via OauthAccount; providerUserId is Google sub / GitHub id */
    User upsertIdentity(String provider, String providerUserId,
                        String email, boolean emailVerified, String fullName,
                        String accessToken, String refreshToken, Long expiresInSeconds, String scope);

    void touchLogin(UUID userId);

    void updatePassword(UUID userId, String newRawPassword);

    void markVerified(UUID userId, boolean verified);

    User loadByEmailOrAccountName(String input);

    User getUserBySessionId(UUID sessionId);

    /** Create a new session and return the opaque refresh token: "<sessionId>.<secret>" */
    String createRefreshToken(User user, String userAgent, String ip, String deviceId, long refreshTtlSeconds);

    /** Validate an opaque refresh token, rotate its secret + extend validity, and return the NEW opaque token. */
    String rotateRefreshToken(String opaqueRefreshToken, long newRefreshTtlSeconds);

    /** Revoke a single session by its id (kills that device). */
    void revokeSession(UUID sessionId, String reason);

    /** Revoke all active sessions for a user (global logout). */
    void revokeAllSessionsForUser(UUID userId, String reason);

}