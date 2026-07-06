package com.algorythm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/** Composite key for a like: (user_id, composition_id). */
@Embeddable
public class CompositionLikeId implements Serializable {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "composition_id")
    private Long compositionId;

    protected CompositionLikeId() {
        // required by JPA
    }

    public CompositionLikeId(Long userId, Long compositionId) {
        this.userId = userId;
        this.compositionId = compositionId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getCompositionId() {
        return compositionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CompositionLikeId other)) {
            return false;
        }
        return Objects.equals(userId, other.userId)
                && Objects.equals(compositionId, other.compositionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, compositionId);
    }
}
