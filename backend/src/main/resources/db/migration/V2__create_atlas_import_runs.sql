CREATE TABLE IF NOT EXISTS atlas_import_runs (
    id BIGSERIAL PRIMARY KEY,
    started_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    source VARCHAR(80) NOT NULL,
    admin_level VARCHAR(8) NOT NULL,
    source_layers INTEGER NOT NULL DEFAULT 0,
    imported_layers INTEGER NOT NULL DEFAULT 0,
    imported_features INTEGER NOT NULL DEFAULT 0,
    failed_layers INTEGER NOT NULL DEFAULT 0,
    report JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX IF NOT EXISTS idx_atlas_import_runs_started_at
    ON atlas_import_runs (started_at DESC);
