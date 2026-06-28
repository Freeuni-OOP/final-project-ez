-- Saved compositions, each owned by a user. Matches the Composition JPA entity
-- (Hibernate runs in validate mode, so columns/types/nullability must line up).
CREATE TABLE compositions (
    id         BIGSERIAL    PRIMARY KEY,
    owner_id   BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title      VARCHAR(120) NOT NULL,
    pattern    TEXT         NOT NULL,
    bpm        INTEGER      NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Listing a user's compositions is the common query.
CREATE INDEX idx_compositions_owner ON compositions (owner_id);
