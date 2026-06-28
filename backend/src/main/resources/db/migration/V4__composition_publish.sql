-- Publishing: a composition can be made public and shared via a short slug.
-- Matches the new fields on the Composition entity (Hibernate validates).
ALTER TABLE compositions
    ADD COLUMN is_public BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN slug      VARCHAR(32);

-- Slugs are unique when set (NULL allowed for unpublished compositions).
ALTER TABLE compositions
    ADD CONSTRAINT uq_compositions_slug UNIQUE (slug);

-- The explore feed lists public compositions newest-first.
CREATE INDEX idx_compositions_public ON compositions (is_public, updated_at DESC);
