package com.chill_guys.vitae_backend.auth.model.entity;

import com.chill_guys.vitae_backend.user.model.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "sessions")
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Lưu HASH của refresh token (không lưu token raw)
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "refresh_token_hash", nullable = false)
    private String refreshTokenHash;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "ip")
    private String ip;

    @Column(name = "device_id", length = 100)
    private String deviceId;

    @Column(name = "last_used_at")
    private OffsetDateTime lastUsedAt;

    @Column(name = "valid_until", nullable = false)
    private OffsetDateTime validUntil;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "revoked_reason")
    private String revokedReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}