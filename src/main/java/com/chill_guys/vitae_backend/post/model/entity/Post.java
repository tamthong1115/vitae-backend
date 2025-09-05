package com.chill_guys.vitae_backend.post.model.entity;

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
@Table(name = "posts")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;


    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;


    @Column(name = "content", columnDefinition = "text")
    private String content;


    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", length = 20)
    private Visibility visibility;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repost_of_post_id")
    private Post repostOf;

    @Column(name = "is_archived")
    private Boolean isArchived;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "reactions_count")
    private Integer reactionsCount;

    @Column(name = "comments_count")
    private Integer commentsCount;

    public enum Visibility {
        PUBLIC,
        PRIVATE,
        FRIENDS,
        CUSTOM
    }

}