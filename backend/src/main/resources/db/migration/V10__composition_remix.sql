-- Remixing: copying a public composition into your own library records where
-- it came from. Self-referencing FK, nullable (original compositions have no
-- source); set null on delete so a remix survives its source being removed.
ALTER TABLE compositions
    ADD COLUMN remixed_from_id BIGINT REFERENCES compositions(id) ON DELETE SET NULL;

CREATE INDEX idx_compositions_remixed_from ON compositions (remixed_from_id);
