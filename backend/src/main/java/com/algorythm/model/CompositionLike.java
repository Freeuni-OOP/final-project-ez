package com.algorythm.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A like by a user on a composition. Maps to "composition_likes" (V5 migration);
 * the (user_id, composition_id) primary key enforces one-like-per-user.
 */
@Entity
@Table(name = "composition_likes")
public class CompositionLike {

    @EmbeddedId
    private CompositionLikeId id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CompositionLike() {
        // required by JPA
    }

    public CompositionLike(Long userId, Long compositionId) {
        this.id = new CompositionLikeId(userId, compositionId);
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public CompositionLikeId getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
