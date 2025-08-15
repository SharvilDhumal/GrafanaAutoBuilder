-- Create table for uploaded CSV metadata
CREATE TABLE IF NOT EXISTS uploaded_csv (
    id BIGSERIAL PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    uploaded_by VARCHAR(255),
    uploaded_at TIMESTAMPTZ NOT NULL,
    original_headers TEXT,
    source_hash VARCHAR(128)
);

-- Create table for dashboard references
CREATE TABLE IF NOT EXISTS dashboard_ref (
    id BIGSERIAL PRIMARY KEY,
    uploaded_csv_id BIGINT NOT NULL REFERENCES uploaded_csv(id) ON DELETE CASCADE,
    grafana_uid VARCHAR(64) NOT NULL UNIQUE,
    grafana_title VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    status VARCHAR(32),
    last_response TEXT
);

-- Create table for panel configurations
CREATE TABLE IF NOT EXISTS panel_config (
    id BIGSERIAL PRIMARY KEY,
    uploaded_csv_id BIGINT NOT NULL REFERENCES uploaded_csv(id) ON DELETE CASCADE,
    dashboard_ref_id BIGINT REFERENCES dashboard_ref(id) ON DELETE SET NULL,
    title VARCHAR(255),
    datasource_uid VARCHAR(255),
    sql_query TEXT,
    visualization VARCHAR(64),
    unit VARCHAR(64),
    thresholds VARCHAR(128),
    w INT,
    h INT,
    description TEXT
);
