-- Moderation: a role on users plus a reports table.

-- Every user is a normal USER unless promoted to ADMIN. Existing rows default in.
ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';

-- A report targets exactly one thing: a composition or a comment (never both,
-- never neither). Rows are removed if the reporter or the reported item is deleted.
CREATE TABLE reports (
    id             BIGSERIAL    PRIMARY KEY,
    reporter_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    composition_id BIGINT       REFERENCES compositions (id) ON DELETE CASCADE,
    comment_id     BIGINT       REFERENCES comments (id) ON DELETE CASCADE,
    reason         TEXT,
    status         VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT report_target_exactly_one CHECK (
        (composition_id IS NOT NULL AND comment_id IS NULL)
        OR (composition_id IS NULL AND comment_id IS NOT NULL)
    )
);

-- Reviewing open reports is the common admin query.
CREATE INDEX idx_reports_status ON reports (status);
