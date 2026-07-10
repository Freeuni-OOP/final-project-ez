package com.algorythm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A saved composition (the pattern text + tempo) owned by a user.
 * Maps to the "compositions" table (see V3/V4 migrations). A composition can be
 * published (is_public) and shared via a short unique slug, and can carry
 * several tags (see V8 migration) set by its creator.
 */
@Entity
@Table(name = "compositions")
public class Composition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String pattern;

    @Column(nullable = false)
    private int bpm;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic = false;

    /** Short public id for share links; null until published. */
    @Column(unique = true, length = 32)
    private String slug;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "composition_tags",
            joinColumns = @JoinColumn(name = "composition_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new LinkedHashSet<>();

    /** The public composition this one was remixed from, if any (see V10 migration). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "remixed_from_id")
    private Composition remixedFrom;

    protected Composition() {
        // required by JPA
    }

    public Composition(User owner, String title, String pattern, int bpm) {
        this.owner = owner;
        this.title = title;
        this.pattern = pattern;
        this.bpm = bpm;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public int getBpm() {
        return bpm;
    }

    public void setBpm(int bpm) {
        this.bpm = bpm;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Set<Tag> getTags() {
        return tags;
    }

    /** Copies into a mutable set - Hibernate manages/clears this collection internally. */
    public void setTags(Set<Tag> tags) {
        this.tags = new LinkedHashSet<>(tags);
    }

    public Composition getRemixedFrom() {
        return remixedFrom;
    }

    public void setRemixedFrom(Composition remixedFrom) {
        this.remixedFrom = remixedFrom;
    }
}
