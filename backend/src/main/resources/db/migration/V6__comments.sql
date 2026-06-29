-- Comments on compositions. Matches the Comment JPA entity (Hibernate runs in
-- validate mode, so columns/types/nullability must line up). Rows are removed
-- when either the composition or the author is deleted.
CREATE TABLE comments (
    id             BIGSERIAL    PRIMARY KEY,
    composition_id BIGINT       NOT NULL REFERENCES compositions (id) ON DELETE CASCADE,
    author_id      BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    body           TEXT         NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Reading a composition's comments (oldest first) is the common query.
CREATE INDEX idx_comments_composition ON comments (composition_id, created_at);
