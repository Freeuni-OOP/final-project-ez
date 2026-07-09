-- Tags: a shared vocabulary (genre, mood, ...) set by a composition's creator.
-- Names are stored normalized (lowercase, trimmed) so "Lofi" and "lofi" are the
-- same tag; the unique constraint enforces that at the DB level too.
CREATE TABLE tags (
                      id   BIGSERIAL    PRIMARY KEY,
                      name VARCHAR(30)  NOT NULL UNIQUE
);

-- Many-to-many: a composition can carry several tags, a tag can be reused
-- across many compositions. Rows are removed if either side is deleted.
CREATE TABLE composition_tags (
                                  composition_id BIGINT NOT NULL REFERENCES compositions (id) ON DELETE CASCADE,
                                  tag_id         BIGINT NOT NULL REFERENCES tags (id) ON DELETE CASCADE,
                                  PRIMARY KEY (composition_id, tag_id)
);

-- Filtering the explore feed by tag queries from the tag side.
CREATE INDEX idx_composition_tags_tag ON composition_tags (tag_id);