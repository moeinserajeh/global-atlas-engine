#!/usr/bin/env python3
"""Reproducible geoBoundaries gbOpen ADM0/ADM1 importer with validation and reports.

The importer is idempotent for records it processes. It commits each country/layer
as one transaction, records a machine-readable report, and never silently invents
an ISO code. Use only after reviewing the source metadata and attribution terms.
"""
from __future__ import annotations

import argparse
import csv
import datetime as dt
import json
import os
import sys
import time
from pathlib import Path
from typing import Any

import psycopg
import pycountry
import requests
from shapely.geometry import GeometryCollection, mapping, shape
from shapely.ops import unary_union
from shapely.validation import make_valid

API = "https://www.geoboundaries.org/api/current/gbOpen/ALL/{level}/"
ATTRIBUTION = "geoBoundaries (www.geoboundaries.org), gbOpen; attribution required"
SESSION = requests.Session()
SESSION.headers.update({"User-Agent": "GlobalAtlasEngine-data-import/1.0 (+https://github.com/moeinserajeh/global-atlas-engine)"})


def get_json(url: str) -> Any:
    response = SESSION.get(url, timeout=(15, 120))
    response.raise_for_status()
    return response.json()


def records_from_metadata(payload: Any) -> list[dict[str, Any]]:
    if isinstance(payload, list):
        return [item for item in payload if isinstance(item, dict)]
    if isinstance(payload, dict):
        if isinstance(payload.get("geoBoundaries"), list):
            return [item for item in payload["geoBoundaries"] if isinstance(item, dict)]
        if "boundaryISO" in payload:
            return [payload]
        return [item for item in payload.values() if isinstance(item, dict) and "boundaryISO" in item]
    return []


def metadata_for(level: str) -> list[dict[str, Any]]:
    rows = records_from_metadata(get_json(API.format(level=level)))
    unique: dict[str, dict[str, Any]] = {}
    for row in rows:
        iso3 = str(row.get("boundaryISO") or "").strip().upper()
        if len(iso3) == 3 and iso3.isalpha():
            if iso3 in unique:
                raise ValueError(f"Duplicate {level} metadata for ISO-3 {iso3}")
            unique[iso3] = row
    if not unique:
        raise ValueError(f"geoBoundaries returned no usable {level} metadata records")
    return sorted(unique.values(), key=lambda row: str(row.get("boundaryISO", "")))


def geometry_url(meta: dict[str, Any]) -> str:
    return str(meta.get("simplifiedGeometryGeoJSON") or meta.get("gjDownloadURL") or "").strip()


def feature_rows(meta: dict[str, Any]) -> list[dict[str, Any]]:
    url = geometry_url(meta)
    if not url:
        raise ValueError(f"No GeoJSON URL in metadata for {meta.get('boundaryID')}")
    payload = get_json(url)
    if not isinstance(payload, dict):
        raise ValueError(f"GeoJSON response is not an object for {meta.get('boundaryISO')}")
    if payload.get("type") == "FeatureCollection":
        features = payload.get("features", [])
    elif payload.get("type") == "Feature":
        features = [payload]
    else:
        raise ValueError(f"Unsupported GeoJSON type {payload.get('type')!r}")
    features = [f for f in features if isinstance(f, dict) and isinstance(f.get("geometry"), dict)]
    if not features:
        raise ValueError(f"GeoJSON contains no features with geometry for {meta.get('boundaryISO')}")
    return features


def properties(feature: dict[str, Any]) -> dict[str, Any]:
    value = feature.get("properties") or {}
    return value if isinstance(value, dict) else {}


def get_prop(props: dict[str, Any], *keys: str, default: str = "") -> str:
    for key in keys:
        value = props.get(key)
        if value is not None and str(value).strip():
            return str(value).strip()
    return default


def resolve_iso2(meta: dict[str, Any], feature: dict[str, Any]) -> str:
    props = properties(feature)
    iso3 = str(meta.get("boundaryISO") or "").strip().upper()
    explicit = get_prop(props, "ISO2", "iso2", "ISO_A2", "shapeISO", default="").upper()
    if len(explicit) == 2 and explicit.isalpha() and explicit not in {"-99", "ZZ"}:
        return explicit
    country = pycountry.countries.get(alpha_3=iso3)
    if country is None:
        raise ValueError(f"No ISO-3166 mapping for {iso3}; review and add an explicit mapping")
    return country.alpha_2


def merged_polygon_geojson(features: list[dict[str, Any]]) -> str:
    """Union all feature geometries and repair topology before PostGIS ingestion."""
    geometries = []
    for feature in features:
        try:
            geom = shape(feature["geometry"])
        except Exception as exc:
            raise ValueError(f"Invalid GeoJSON geometry: {exc}") from exc
        if geom.is_empty:
            continue
        if geom.geom_type not in {"Polygon", "MultiPolygon", "GeometryCollection"}:
            raise ValueError(f"Expected polygon boundary, got {geom.geom_type}")
        if not geom.is_valid:
            geom = make_valid(geom)
        if geom.geom_type == "GeometryCollection":
            polygons = [part for part in geom.geoms if part.geom_type in {"Polygon", "MultiPolygon"}]
            geom = unary_union(polygons) if polygons else GeometryCollection()
        if not geom.is_empty:
            geometries.append(geom)
    if not geometries:
        raise ValueError("No non-empty polygon geometries in layer")
    merged = unary_union(geometries)
    if not merged.is_valid:
        merged = make_valid(merged)
    if merged.geom_type == "GeometryCollection":
        polygons = [part for part in merged.geoms if part.geom_type in {"Polygon", "MultiPolygon"}]
        merged = unary_union(polygons) if polygons else GeometryCollection()
    if merged.is_empty or merged.geom_type not in {"Polygon", "MultiPolygon"}:
        raise ValueError(f"Union did not produce polygon geometry: {merged.geom_type}")
    if not merged.is_valid:
        raise ValueError("Geometry remains invalid after make_valid")
    return json.dumps(mapping(merged), separators=(",", ":"))


def metadata_values(meta: dict[str, Any]) -> tuple[str, str, str, str | None]:
    source = str(meta.get("boundarySource") or ATTRIBUTION)[:255]
    license_name = str(meta.get("boundaryLicense") or "See geoBoundaries per-layer metadata")[:255]
    version = str(meta.get("buildDate") or meta.get("boundaryYearRepresented") or "current")[:80]
    year = str(meta.get("boundaryYearRepresented") or "")
    reference_date = f"{year[:4]}-01-01" if len(year) >= 4 and year[:4].isdigit() else None
    return source, license_name, version, reference_date


def validate_postgis_geometry(cur: psycopg.Cursor, table: str, row_id: int) -> None:
    if table == "countries":
        cur.execute("SELECT ST_IsValid(geometry), ST_IsEmpty(geometry), ST_SRID(geometry), GeometryType(geometry) FROM countries WHERE id=%s", (row_id,))
    elif table == "administrative_regions":
        cur.execute("SELECT ST_IsValid(geometry), ST_IsEmpty(geometry), ST_SRID(geometry), GeometryType(geometry) FROM administrative_regions WHERE id=%s", (row_id,))
    else:
        raise ValueError(f"Unsupported table for geometry validation: {table}")
    result = cur.fetchone()
    if not result or result[0] is not True or result[1] is not False or result[2] != 4326 or result[3] not in {"MULTIPOLYGON", "POLYGON"}:
        raise ValueError(f"PostGIS geometry validation failed for {table} id={row_id}: {result}")


def insert_country(cur: psycopg.Cursor, meta: dict[str, Any], features: list[dict[str, Any]]) -> int:
    first = features[0]
    iso3 = str(meta.get("boundaryISO") or "").strip().upper()
    if len(iso3) != 3 or not iso3.isalpha():
        raise ValueError(f"Invalid ISO-3 value: {iso3!r}")
    iso2 = resolve_iso2(meta, first)
    name = get_prop(properties(first), "shapeName", "NAME_EN", "name", default=str(meta.get("boundaryName") or iso3))
    source, license_name, version, reference_date = metadata_values(meta)
    geometry = merged_polygon_geojson(features)
    cur.execute("""
        INSERT INTO countries (iso2, iso3, name_en, data_source, data_license,
                               dataset_version, reference_date, geometry)
        VALUES (%s, %s, %s, %s, %s, %s, %s,
                ST_Multi(ST_CollectionExtract(ST_MakeValid(ST_SetSRID(ST_GeomFromGeoJSON(%s), 4326)), 3)))
        ON CONFLICT (iso3) DO UPDATE SET
          iso2=EXCLUDED.iso2, name_en=EXCLUDED.name_en,
          data_source=EXCLUDED.data_source, data_license=EXCLUDED.data_license,
          dataset_version=EXCLUDED.dataset_version, reference_date=EXCLUDED.reference_date,
          geometry=EXCLUDED.geometry, updated_at=now()
        RETURNING id
    """, (iso2, iso3, name, source, license_name, version, reference_date, geometry))
    row_id = int(cur.fetchone()[0])
    validate_postgis_geometry(cur, "countries", row_id)
    return row_id


def insert_region(cur: psycopg.Cursor, country_id: int, meta: dict[str, Any], feature: dict[str, Any]) -> int:
    props = properties(feature)
    name = get_prop(props, "shapeName", "name", "NAME_1", "NAME_EN", default="")
    if not name:
        raise ValueError("ADM1 feature is missing a region name")
    code = get_prop(props, "shapeID", "shapeISO", "HASC_1", "GID_1", default="")
    source, license_name, version, reference_date = metadata_values(meta)
    geometry = merged_polygon_geojson([feature])
    cur.execute("""
        INSERT INTO administrative_regions
          (country_id, code, name_en, admin_level, data_source, data_license,
           dataset_version, reference_date, geometry)
        VALUES (%s, %s, %s, 1, %s, %s, %s, %s,
                ST_Multi(ST_CollectionExtract(ST_MakeValid(ST_SetSRID(ST_GeomFromGeoJSON(%s), 4326)), 3)))
        ON CONFLICT (country_id, code, name_en) DO UPDATE SET
          data_source=EXCLUDED.data_source, data_license=EXCLUDED.data_license,
          dataset_version=EXCLUDED.dataset_version, reference_date=EXCLUDED.reference_date,
          geometry=EXCLUDED.geometry, updated_at=now()
        RETURNING id
    """, (country_id, code or None, name, source, license_name, version, reference_date, geometry))
    row_id = int(cur.fetchone()[0])
    validate_postgis_geometry(cur, "administrative_regions", row_id)
    return row_id


def ensure_audit_table(cur: psycopg.Cursor) -> None:
    cur.execute("""
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
        )
    """)


def coverage_snapshot(cur: psycopg.Cursor) -> dict[str, Any]:
    cur.execute("SELECT count(*), count(*) FILTER (WHERE geometry IS NOT NULL), count(*) FILTER (WHERE geometry IS NOT NULL AND ST_IsValid(geometry)), count(DISTINCT iso3), count(DISTINCT iso2) FROM countries")
    countries = cur.fetchone()
    cur.execute("SELECT count(*), count(*) FILTER (WHERE geometry IS NOT NULL), count(*) FILTER (WHERE geometry IS NOT NULL AND ST_IsValid(geometry)), count(DISTINCT country_id), count(DISTINCT country_id) FILTER (WHERE admin_level=1) FROM administrative_regions")
    regions = cur.fetchone()
    cur.execute("""
        SELECT c.iso3, c.iso2, c.name_en, count(r.id) AS region_count,
               bool_and(r.geometry IS NULL OR ST_IsValid(r.geometry)) AS geometries_valid
        FROM countries c LEFT JOIN administrative_regions r ON r.country_id=c.id AND r.admin_level=1
        GROUP BY c.id, c.iso3, c.iso2, c.name_en ORDER BY c.iso3
    """)
    per_country = [
        {"iso3": row[0].strip(), "iso2": row[1].strip(), "name_en": row[2],
         "adm1_count": int(row[3]), "adm1_geometries_valid": row[4] is not False}
        for row in cur.fetchall()
    ]
    return {
        "countries": {"rows": countries[0], "with_geometry": countries[1], "valid_geometry": countries[2],
                      "distinct_iso3": countries[3], "distinct_iso2": countries[4]},
        "adm1": {"rows": regions[0], "with_geometry": regions[1], "valid_geometry": regions[2],
                 "countries_with_regions": regions[3], "countries_with_adm1": regions[4]},
        "countries_adm1_coverage": per_country,
    }


def write_reports(report: dict[str, Any], output_dir: Path) -> None:
    output_dir.mkdir(parents=True, exist_ok=True)
    stamp = dt.datetime.now(dt.timezone.utc).strftime("%Y%m%dT%H%M%SZ")
    json_path = output_dir / f"geoboundaries-import-{stamp}.json"
    json_path.write_text(json.dumps(report, ensure_ascii=False, indent=2, default=str) + "\n", encoding="utf-8")
    csv_path = output_dir / f"geoboundaries-coverage-{stamp}.csv"
    with csv_path.open("w", newline="", encoding="utf-8") as stream:
        writer = csv.DictWriter(stream, fieldnames=["iso3", "iso2", "name_en", "adm1_count", "adm1_geometries_valid"])
        writer.writeheader()
        writer.writerows(report.get("coverage", {}).get("countries_adm1_coverage", []))
    print(f"[REPORT] {json_path}")
    print(f"[REPORT] {csv_path}")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--level", choices=("ADM0", "ADM1", "both"), default="both")
    parser.add_argument("--limit", type=int, default=0, help="Limit country layers for a controlled trial")
    parser.add_argument("--pause", type=float, default=0.25, help="Delay between source-layer downloads")
    parser.add_argument("--report-dir", default="data-import/reports", help="Output directory for JSON and CSV reports")
    parser.add_argument("--coverage-only", action="store_true", help="Skip downloads and write current DB coverage report")
    parser.add_argument("--fail-on-missing-adm1", action="store_true", help="Return nonzero when any imported country has no ADM1 rows")
    args = parser.parse_args()
    if args.limit < 0 or args.pause < 0:
        parser.error("--limit and --pause must be non-negative")

    dsn = os.getenv("DATABASE_URL", "postgresql://atlas:atlas@localhost:5432/atlas")
    levels = ("ADM0", "ADM1") if args.level == "both" else (args.level,)
    started_at = dt.datetime.now(dt.timezone.utc)
    report: dict[str, Any] = {
        "source": "geoBoundaries gbOpen",
        "source_api": "https://www.geoboundaries.org/api.html",
        "license_note": "gbOpen is documented as CC BY 4.0 compliant; retain per-layer license/source metadata and review source records.",
        "started_at": started_at.isoformat(),
        "levels": {},
        "failures": [],
        "coverage": {},
    }
    exit_code = 0

    with psycopg.connect(dsn) as conn:
        with conn.cursor() as cur:
            cur.execute("CREATE EXTENSION IF NOT EXISTS postgis")
            ensure_audit_table(cur)
            conn.commit()

            if args.coverage_only:
                report["coverage"] = coverage_snapshot(cur)
            else:
                for level in levels:
                    try:
                        layers = metadata_for(level)
                    except Exception as exc:
                        report["failures"].append({"level": level, "iso3": None, "error": f"metadata fetch failed: {exc}"})
                        exit_code = 2
                        continue
                    source_count = len(layers)
                    if args.limit:
                        layers = layers[:args.limit]
                    stats = {"source_layers": source_count, "selected_layers": len(layers),
                             "imported_layers": 0, "imported_features": 0, "failed_layers": 0,
                             "skipped_layers": 0, "missing_adm1": []}
                    for index, meta in enumerate(layers, start=1):
                        iso3 = str(meta.get("boundaryISO") or "").strip().upper()
                        try:
                            features = feature_rows(meta)
                            if level == "ADM0":
                                # Every feature is unioned; archipelagos and multi-feature layers are not truncated.
                                country_id = insert_country(cur, meta, features)
                                imported_features = 1
                            else:
                                cur.execute("SELECT id FROM countries WHERE iso3=%s", (iso3,))
                                country_row = cur.fetchone()
                                if not country_row:
                                    raise ValueError("Country missing in DB; import ADM0 first")
                                country_id = int(country_row[0])
                                imported_features = 0
                                for feature in features:
                                    insert_region(cur, country_id, meta, feature)
                                    imported_features += 1
                                if imported_features == 0:
                                    stats["missing_adm1"].append(iso3)
                            conn.commit()
                            stats["imported_layers"] += 1
                            stats["imported_features"] += imported_features
                            print(f"[{index}/{len(layers)}] {level} {iso3}: imported {imported_features} feature(s)")
                        except Exception as exc:
                            conn.rollback()
                            stats["failed_layers"] += 1
                            report["failures"].append({"level": level, "iso3": iso3, "error": str(exc)})
                            print(f"[WARN] {level} {iso3}: {exc}", file=sys.stderr)
                        time.sleep(args.pause)
                    report["levels"][level] = stats
                    if stats["failed_layers"]:
                        exit_code = 1

                report["coverage"] = coverage_snapshot(cur)

            report["finished_at"] = dt.datetime.now(dt.timezone.utc).isoformat()
            report["duration_seconds"] = round((dt.datetime.now(dt.timezone.utc) - started_at).total_seconds(), 2)
            report["success"] = exit_code == 0 and not report["failures"]
            cur.execute("""
                INSERT INTO atlas_import_runs
                    (started_at, finished_at, source, admin_level, source_layers,
                     imported_layers, imported_features, failed_layers, report)
                VALUES (%s, now(), %s, %s, %s, %s, %s, %s, %s::jsonb)
            """, (
                started_at,
                "geoBoundaries gbOpen",
                args.level,
                sum(v.get("source_layers", 0) for v in report["levels"].values()),
                sum(v.get("imported_layers", 0) for v in report["levels"].values()),
                sum(v.get("imported_features", 0) for v in report["levels"].values()),
                len(report["failures"]),
                json.dumps(report, ensure_ascii=False, default=str),
            ))
            conn.commit()

    if args.fail_on_missing_adm1 and report.get("coverage"):
        missing = [row["iso3"] for row in report["coverage"].get("countries_adm1_coverage", []) if row["adm1_count"] == 0]
        report["coverage"]["countries_missing_adm1"] = missing
        if missing:
            print(f"[ERROR] {len(missing)} countries have no ADM1 records: {', '.join(missing[:30])}", file=sys.stderr)
            exit_code = 1
            report["success"] = False

    write_reports(report, Path(args.report_dir))
    print("[SUMMARY] " + json.dumps({
        "success": report.get("success", False),
        "failures": len(report["failures"]),
        "countries": report.get("coverage", {}).get("countries", {}).get("rows"),
        "countries_with_geometry": report.get("coverage", {}).get("countries", {}).get("with_geometry"),
        "adm1_rows": report.get("coverage", {}).get("adm1", {}).get("rows"),
        "countries_with_adm1": report.get("coverage", {}).get("adm1", {}).get("countries_with_adm1"),
    }, ensure_ascii=False))
    return exit_code


if __name__ == "__main__":
    raise SystemExit(main())
