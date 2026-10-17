#!/usr/bin/env python3
"""Import open geoBoundaries ADM0/ADM1 GeoJSON into PostgreSQL/PostGIS.

Source: geoBoundaries API, gbOpen release. Attribution is required; preserve
per-layer source, license, version and reference date in the database.
"""
from __future__ import annotations

import argparse
import datetime as dt
import json
import os
import sys
import time
from typing import Any

import psycopg
import requests

API = "https://www.geoboundaries.org/api/current/gbOpen/ALL/{level}/"
ATTRIBUTION = "geoBoundaries (www.geoboundaries.org), gbOpen; attribution required"
SESSION = requests.Session()
SESSION.headers.update({"User-Agent": "GlobalAtlasEngine-data-import/0.1 (open-data attribution)"})


def get_json(url: str) -> Any:
    response = SESSION.get(url, timeout=90)
    response.raise_for_status()
    return response.json()


def records_from_metadata(payload: Any) -> list[dict[str, Any]]:
    if isinstance(payload, list):
        return [item for item in payload if isinstance(item, dict)]
    if isinstance(payload, dict):
        if "geoBoundaries" in payload and isinstance(payload["geoBoundaries"], list):
            return [item for item in payload["geoBoundaries"] if isinstance(item, dict)]
        if "boundaryISO" in payload:
            return [payload]
        return [item for item in payload.values() if isinstance(item, dict) and "boundaryISO" in item]
    return []


def metadata_for(level: str) -> list[dict[str, Any]]:
    rows = records_from_metadata(get_json(API.format(level=level)))
    unique: dict[str, dict[str, Any]] = {}
    for row in rows:
        iso3 = str(row.get("boundaryISO") or "").upper()
        if iso3 and len(iso3) == 3:
            unique[iso3] = row
    return list(unique.values())


def geometry_url(meta: dict[str, Any]) -> str:
    return str(meta.get("simplifiedGeometryGeoJSON") or meta.get("gjDownloadURL") or "")


def feature_rows(meta: dict[str, Any]) -> list[dict[str, Any]]:
    url = geometry_url(meta)
    if not url:
        raise ValueError(f"No GeoJSON URL in metadata for {meta.get('boundaryID')}")
    payload = get_json(url)
    if payload.get("type") == "FeatureCollection":
        return [f for f in payload.get("features", []) if isinstance(f, dict)]
    if payload.get("type") == "Feature":
        return [payload]
    raise ValueError(f"Unsupported GeoJSON document from {url}")


def properties(feature: dict[str, Any]) -> dict[str, Any]:
    value = feature.get("properties") or {}
    return value if isinstance(value, dict) else {}


def get_prop(props: dict[str, Any], *keys: str, default: str = "") -> str:
    for key in keys:
        value = props.get(key)
        if value is not None and str(value).strip():
            return str(value).strip()
    return default


def insert_country(cur: psycopg.Cursor, meta: dict[str, Any], feature: dict[str, Any]) -> int:
    props = properties(feature)
    iso3 = str(meta.get("boundaryISO") or "").upper()
    iso2 = get_prop(props, "ISO2", "iso2", "ISO_A2", default="")
    if len(iso2) != 2:
        # ISO-2 is not always included in geoBoundaries properties. Keep an explicit
        # import error rather than guessing country codes from names.
        raise ValueError(f"Missing ISO-3166 alpha-2 code for {iso3}; provide a reviewed mapping.")
    name = get_prop(props, "shapeName", "NAME_EN", "name", default=str(meta.get("boundaryName") or iso3))
    source = get_prop(props, "source", default=str(meta.get("boundarySource") or ATTRIBUTION))
    license_name = str(meta.get("boundaryLicense") or "See geoBoundaries metadata")
    version = str(meta.get("buildDate") or meta.get("boundaryYearRepresented") or "current")
    ref_date = None
    year = str(meta.get("boundaryYearRepresented") or "")
    if year[:4].isdigit():
        ref_date = f"{year[:4]}-01-01"
    cur.execute("""
        INSERT INTO countries (iso2, iso3, name_en, data_source, data_license,
                               dataset_version, reference_date, geometry)
        VALUES (%s, %s, %s, %s, %s, %s, %s,
                ST_Multi(ST_SetSRID(ST_GeomFromGeoJSON(%s), 4326)))
        ON CONFLICT (iso3) DO UPDATE SET
          iso2=EXCLUDED.iso2, name_en=EXCLUDED.name_en,
          data_source=EXCLUDED.data_source, data_license=EXCLUDED.data_license,
          dataset_version=EXCLUDED.dataset_version, reference_date=EXCLUDED.reference_date,
          geometry=EXCLUDED.geometry, updated_at=now()
        RETURNING id
    """, (iso2.upper(), iso3, name, source, license_name, version, ref_date,
          json.dumps(feature.get("geometry"))))
    return int(cur.fetchone()[0])


def insert_region(cur: psycopg.Cursor, country_id: int, meta: dict[str, Any],
                  feature: dict[str, Any]) -> None:
    props = properties(feature)
    name = get_prop(props, "shapeName", "name", "NAME_1", "NAME_EN", default="Unnamed region")
    code = get_prop(props, "shapeID", "shapeISO", "HASC_1", default="")
    source = str(meta.get("boundarySource") or ATTRIBUTION)
    license_name = str(meta.get("boundaryLicense") or "See geoBoundaries metadata")
    version = str(meta.get("buildDate") or meta.get("boundaryYearRepresented") or "current")
    year = str(meta.get("boundaryYearRepresented") or "")
    ref_date = f"{year[:4]}-01-01" if year[:4].isdigit() else None
    cur.execute("""
        INSERT INTO administrative_regions
          (country_id, code, name_en, admin_level, data_source, data_license,
           dataset_version, reference_date, geometry)
        VALUES (%s, %s, %s, 1, %s, %s, %s, %s,
                ST_Multi(ST_SetSRID(ST_GeomFromGeoJSON(%s), 4326)))
        ON CONFLICT (country_id, code, name_en) DO UPDATE SET
          data_source=EXCLUDED.data_source, data_license=EXCLUDED.data_license,
          dataset_version=EXCLUDED.dataset_version, reference_date=EXCLUDED.reference_date,
          geometry=EXCLUDED.geometry, updated_at=now()
    """, (country_id, code or None, name, source, license_name, version, ref_date,
          json.dumps(feature.get("geometry"))))


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--level", choices=("ADM0", "ADM1", "both"), default="both")
    parser.add_argument("--limit", type=int, default=0, help="Optional max number of country layers for a trial run")
    parser.add_argument("--pause", type=float, default=0.25, help="Delay between metadata downloads")
    args = parser.parse_args()
    dsn = os.getenv("DATABASE_URL", "postgresql://atlas:atlas@localhost:5432/atlas")
    levels = ("ADM0", "ADM1") if args.level == "both" else (args.level,)
    failed: list[str] = []
    with psycopg.connect(dsn) as conn:
        with conn.cursor() as cur:
            cur.execute("CREATE EXTENSION IF NOT EXISTS postgis")
            for level in levels:
                try:
                    layers = metadata_for(level)
                except Exception as exc:
                    print(f"[ERROR] Could not fetch {level} metadata: {exc}", file=sys.stderr)
                    return 2
                if args.limit:
                    layers = layers[:args.limit]
                print(f"[INFO] {level}: found {len(layers)} country layers")
                for index, meta in enumerate(layers, start=1):
                    iso3 = str(meta.get("boundaryISO") or "???")
                    try:
                        features = feature_rows(meta)
                        if level == "ADM0":
                            if not features:
                                raise ValueError("ADM0 layer contains no features")
                            # Some source layers are multipart. Keep first feature as country outline.
                            country_id = insert_country(cur, meta, features[0])
                            conn.commit()
                            print(f"[{index}/{len(layers)}] country {iso3} imported")
                        else:
                            country_meta = next((x for x in metadata_for("ADM0")
                                                 if str(x.get("boundaryISO") or "").upper() == iso3), None)
                            if country_meta is None:
                                raise ValueError("No matching ADM0 metadata; import ADM0 first")
                            # ISO-2 lookup must be based on the already imported ADM0 row.
                            cur.execute("SELECT id FROM countries WHERE iso3=%s", (iso3,))
                            country_row = cur.fetchone()
                            if not country_row:
                                raise ValueError("Country missing in DB; run --level ADM0 first")
                            country_id = int(country_row[0])
                            for feature in features:
                                insert_region(cur, country_id, meta, feature)
                            conn.commit()
                            print(f"[{index}/{len(layers)}] {iso3}: {len(features)} regions imported")
                    except Exception as exc:
                        conn.rollback()
                        failed.append(f"{level} {iso3}: {exc}")
                        print(f"[WARN] {level} {iso3}: {exc}", file=sys.stderr)
                    time.sleep(max(0, args.pause))
    print(f"[DONE] failures={len(failed)}")
    if failed:
        print("\n".join(failed), file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
