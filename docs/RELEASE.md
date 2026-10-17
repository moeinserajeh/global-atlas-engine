# Release and hardening checklist

## CI gates
- [ ] Android CI succeeds: compile, unit tests, lint, debug assembly.
- [ ] Backend CI succeeds against PostGIS: migrations, tests, package.
- [ ] Geographic importer CI succeeds and real import coverage report is reviewed.
- [ ] Dependency and secret scanning reviewed; no unresolved critical/high findings.
- [ ] API contract and map geometry smoke tests pass.
- [ ] Release notes include source, license, dataset version, coverage gaps, and migration notes.

## Android
1. Update versionCode/versionName in android/app/build.gradle.kts.
2. Configure release signing with a protected keystore and GitHub Actions secrets; never commit credentials.
3. Run gradle :app:lintRelease :app:testDebugUnitTest :app:assembleRelease.
4. Test install, cold start, map selection, country/region details, bilingual search, offline cache on a physical device.
5. Verify release cleartext traffic is disabled.
The tag workflow produces an **unsigned** APK artifact; it is not a production-signed Play Store deliverable until signing is configured.

## Backend
1. Build: docker build -t global-atlas-api:local backend.
2. Verify Flyway migrations against an empty disposable PostGIS database.
3. Check /actuator/health, /api/v1/health, OpenAPI UI, and representative API endpoints.
4. Set DB_URL, DB_USERNAME, DB_PASSWORD in deployment environment; sample credentials are local-development only.
5. Configure TLS, rate limits, timeouts, backups, monitoring and rollback before public exposure.

## Geographic data acceptance
- [ ] Run data-import/import_geoboundaries.py against PostGIS.
- [ ] Review JSON/CSV reports, failed layers, missing ADM1, invalid geometries, and ISO mappings.
- [ ] Verify CC BY 4.0 attribution and per-layer source/license/version/reference date.
- [ ] Do not claim ADM1 coverage where the source has no data.

## Performance
Map GeoJSON endpoints should accept a bounded simplification tolerance and the global regions endpoint a bounded limit. Profile p50/p95 latency and payload size against a populated production-like database; do not claim benchmarks until measured.
