package com.chill_guys.vitae_backend.auth;

import com.chill_guys.vitae_backend.auth.model.entity.UserSession;
import com.chill_guys.vitae_backend.user.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
    List<UserSession> findByUserAndRevokedAtIsNull(User user);
    long deleteByValidUntilBefore(OffsetDateTime cutoff);
}
