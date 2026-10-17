CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE countries (
    id BIGSERIAL PRIMARY KEY,
    iso2 CHAR(2) NOT NULL UNIQUE,
    iso3 CHAR(3) NOT NULL UNIQUE,
    name_en VARCHAR(160) NOT NULL,
    name_fa VARCHAR(160),
    capital VARCHAR(160),
    population BIGINT,
    area_km2 NUMERIC(14, 2),
    geometry geometry(MultiPolygon, 4326),
    data_source VARCHAR(255) NOT NULL,
    data_license VARCHAR(255) NOT NULL,
    dataset_version VARCHAR(80) NOT NULL,
    reference_date DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT countries_iso2_uppercase CHECK (iso2 = upper(iso2)),
    CONSTRAINT countries_iso3_uppercase CHECK (iso3 = upper(iso3)),
    CONSTRAINT countries_population_nonnegative CHECK (population IS NULL OR population >= 0),
    CONSTRAINT countries_area_nonnegative CHECK (area_km2 IS NULL OR area_km2 >= 0)
);

CREATE INDEX idx_countries_geometry ON countries USING GIST (geometry);
CREATE INDEX idx_countries_name_en ON countries (lower(name_en));
CREATE INDEX idx_countries_name_fa ON countries (name_fa);

CREATE TABLE administrative_regions (
    id BIGSERIAL PRIMARY KEY,
    country_id BIGINT NOT NULL REFERENCES countries(id) ON DELETE CASCADE,
    code VARCHAR(32),
    name_en VARCHAR(160) NOT NULL,
    name_fa VARCHAR(160),
    admin_level SMALLINT NOT NULL DEFAULT 1,
    geometry geometry(MultiPolygon, 4326),
    data_source VARCHAR(255) NOT NULL,
    data_license VARCHAR(255) NOT NULL,
    dataset_version VARCHAR(80) NOT NULL,
    reference_date DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT administrative_regions_level_positive CHECK (admin_level > 0),
    CONSTRAINT administrative_regions_country_code_name_unique UNIQUE (country_id, code, name_en)
);

CREATE INDEX idx_admin_regions_country_id ON administrative_regions (country_id);
CREATE INDEX idx_admin_regions_geometry ON administrative_regions USING GIST (geometry);
CREATE INDEX idx_admin_regions_name_en ON administrative_regions (lower(name_en));
CREATE INDEX idx_admin_regions_name_fa ON administrative_regions (name_fa);
