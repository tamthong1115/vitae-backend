package com.chill_guys.vitae_backend.user.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "friend_requests")
public class FriendRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;


    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;


    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "addressee_id", nullable = false)
    private User addressee;


    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FriendRequestStatus status = FriendRequestStatus.PENDING;


    @Column(name = "message", length = 200)
    private String message;


    @Column(name = "seen_at")
    private OffsetDateTime seenAt;


    @Column(name = "responded_at")
    private OffsetDateTime respondedAt;


    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;


    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    enum FriendRequestStatus {
        PENDING, DECLINED, CANCELLED, EXPIRED
    }
}