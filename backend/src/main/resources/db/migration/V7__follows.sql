-- Follows: one row per (follower, following). The composite primary key makes a
-- user following the same person twice impossible. Rows are removed if either
-- user is deleted.
CREATE TABLE follows (
    follower_id  BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    following_id BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (follower_id, following_id)
);

-- Counting a user's followers queries by following_id (the PK already covers
-- the follower_id direction).
CREATE INDEX idx_follows_following ON follows (following_id);
