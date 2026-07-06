-- Likes: one row per (user, composition). The composite primary key makes a user
-- liking the same composition twice impossible. Rows are cleaned up if either the
-- user or the composition is deleted.
CREATE TABLE composition_likes (
    user_id        BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    composition_id BIGINT       NOT NULL REFERENCES compositions(id) ON DELETE CASCADE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, composition_id)
);

-- Speeds up like counts per composition.
CREATE INDEX idx_composition_likes_composition ON composition_likes (composition_id);
