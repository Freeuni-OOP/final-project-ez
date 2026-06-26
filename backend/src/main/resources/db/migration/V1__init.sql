-- Baseline migration: proves Flyway runs against Postgres on startup.
-- Real domain tables (users, compositions, ...) are added in their feature issues.
CREATE TABLE app_info (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

INSERT INTO app_info (name) VALUES ('algorythm');
