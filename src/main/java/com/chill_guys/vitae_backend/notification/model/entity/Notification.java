package com.chill_guys.vitae_backend.notification.model.entity;

import com.chill_guys.vitae_backend.user.model.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private NotificationType type;

    @Column(name = "data", columnDefinition = "jsonb")
    private String data;

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public enum NotificationType {
        FRIEND_REQUEST, FRIEND_ACCEPT, POST_REACTION, COMMENT, COMMENT_REACTION, MENTION, FOLLOW, SYSTEM
    }
}
