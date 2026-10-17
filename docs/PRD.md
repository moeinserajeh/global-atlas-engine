# Global Atlas Engine — Product Requirements Document (PRD)

**Product:** اطلس جهان‌نما / Global Atlas Engine  
**Platform:** Android, Java/Spring Boot backend  
**Audience:** General public  
**Languages:** Persian (fa) and English (en)  
**Document version:** 1.0  
**Status date:** 2026-10-18  
**Status policy:** A checked box means the specified repository artifact or implementation exists, **not** that it has passed build, integration, production, data-coverage, or acceptance testing. Unchecked items remain unimplemented, incomplete, or unverified.

## 1. Product vision

Provide a bilingual, interactive atlas of the world's countries and their first-level administrative divisions, with searchable country/region information, explicit data provenance, and useful partial offline access.

### Goals
- Explore all supported countries and first-level administrative areas (ADM1).
- Tap a country, inspect its boundaries, then tap an ADM1 area.
- Read country and region profiles in Persian and English.
- Search countries and regions by name, with local-script support where data permits.
- Cache or download selected information for offline use.
- Make source, license, data version, reference date, and geographic coverage transparent.
- Maintain modular, testable Android and backend implementations.

### Non-goals for MVP
- Street-by-street navigation or turn-by-turn directions.
- Guaranteed universally consistent province nomenclature (ADM1 naming varies by country).
- Unlicensed map data, undisclosed boundary disputes, or pretending missing ADM1 data is complete.
- Mandatory user accounts, payments, or social networking.

## 2. Personas and core journeys

**Explorer:** opens the world map, selects a country, sees its outline and information, drills down to ADM1 regions.  
**Researcher/student:** searches a country or region in Persian or English, checks names and source metadata.  
**Offline traveler:** downloads selected country metadata/geometry while online and reopens it without a connection.

### Core journeys / acceptance
- [ ] From launch, a user can select a country on the world map and see its correct name and ISO code.
- [ ] After selecting a country, the app displays all available ADM1 geometries from the imported dataset.
- [ ] Tapping an ADM1 geometry opens the correct region details, with a clear fallback when information is missing.
- [ ] Search returns relevant results in both languages and handles empty/error/loading states.
- [ ] Previously downloaded country information and geometry remain accessible offline.
- [ ] Each data view shows source, license, version, and reference date when available.
- [ ] End-to-end journey passes on an Android emulator and a physical device.

## 3. Functional requirements and implementation tracking

### FR-01 — Project foundation (P0)
- [x] GitHub repository, README and roadmap created.
- [x] Android Gradle project and application entry point added.
- [x] Android Gradle multi-module declarations: app, core, feature.
- [x] Separate module build files for common, model, designsystem, network, database, datastore, map, testing, atlas, country, region, search, downloads.
- [x] Initial Hilt application setup and dependency wiring.
- [x] Initial MVI contract (Intent/State/Effect) and ViewModel in feature:atlas.
- [ ] Full Clean Architecture implementation with domain use cases and repository interfaces across features.
- [ ] All module dependencies validated by a successful clean build.
- [ ] Shared convention plugins/build-logic and centralized version catalog.
- [ ] Dependency versions verified against official compatibility matrices.

### FR-02 — World map (P0)
- [x] Initial MapLibre Android map screen source committed.
- [x] Map implementation moved to feature:atlas.
- [x] Code for requesting country GeoJSON and rendering boundary overlays committed.
- [x] Code for selecting a country and requesting its ADM1 GeoJSON committed.
- [x] Code for selecting a rendered ADM1 region committed.
- [x] Map country-selection callback wired to Atlas MVI state.
- [ ] Rendering and interaction verified by successful Android build and device tests.
- [ ] Robust zoom-to-country and zoom-to-region behavior.
- [ ] Correct selection highlight, selection reset, and overlay loading/error UX.
- [ ] Attribution and tile provider usage compliance verified.
- [ ] Efficient geometry loading, simplification, and map performance benchmarked.

### FR-03 — Country information (P0)
- [x] Backend countries schema and initial country repository/controller source committed.
- [x] Backend endpoints for listing, fetching by ISO2, and country-name search committed.
- [x] Shared Android country model committed.
- [ ] Country detail screen with meaningful facts, local names, and accessible layout.
- [ ] Android data/domain layers and country repository wired to API.
- [ ] Country facts, completeness, and localization validated.

### FR-04 — Administrative divisions / provinces (P0)
- [x] Backend ADM1 database table and indexes committed.
- [x] Backend region-list endpoint and country-specific GeoJSON endpoint committed.
- [x] Shared Android administrative-region model committed.
- [x] Initial map-region selection source committed.
- [ ] Region details UI and navigation implemented.
- [ ] All-country ADM1 import executed, validated, and coverage reported.
- [ ] Disputed, missing, unusual, and multi-level administrative divisions handled explicitly.
- [ ] Boundary tap hit-testing and source-to-region IDs integration tested.

### FR-05 — Geographic data ingestion (P0)
- [x] geoBoundaries gbOpen ADM0/ADM1 import script committed.
- [x] PostgreSQL/PostGIS schema and Flyway migration committed.
- [x] Data source and attribution documentation committed.
- [x] Importer source includes provenance fields and upsert logic.
- [ ] Importer executed successfully against a populated PostGIS instance.
- [ ] Import coverage audit: ISO code matching, country count, ADM1 counts, and geometry validity.
- [ ] Automated, idempotent data refresh and rollback strategy.
- [ ] Licenses, reference dates, and source-version metadata verified for every published dataset.
- [ ] Explicit handling of countries/territories without available ADM1 coverage.

### FR-06 — Search (P0)
- [x] Backend country search endpoint source committed.
- [x] Search feature Gradle module declared.
- [x] Search query field represented in initial Atlas MVI state.
- [ ] Android search UI connected to live backend results.
- [ ] Region search endpoint and client implementation.
- [ ] Persian/English normalization, transliteration strategy, and ranking.
- [ ] Debounce, pagination, empty/error states, and selection navigation.
- [ ] Search correctness tests for non-Latin and alternate names.

### FR-07 — Offline experience (P1)
- [x] core:database, core:datastore, and feature:downloads module boundaries declared.
- [ ] Room database, DAO, migrations, and cache entities implemented.
- [ ] DataStore settings and download preferences implemented.
- [ ] Download manifest, per-country metadata/geometry packages, and version checks.
- [ ] Offline-first repository behavior and stale-data indicators.
- [ ] Download progress, pause/retry, cancellation, storage limits, and eviction.
- [ ] Airplane-mode acceptance tests.

### FR-08 — Localization and accessibility (P0)
- [x] Persian/English support recorded in project scope.
- [ ] Android string resources for fa/en and locale selection.
- [ ] RTL and LTR layouts verified.
- [ ] Accessible semantics, contrast, font scaling, screen-reader labels.
- [ ] Missing-translation and fallback policy.

### FR-09 — Favorites/history (P2)
- [ ] Favorite countries and regions locally.
- [ ] Recently viewed places.
- [ ] Persistence and clearing history controls.

### FR-10 — Backend APIs (P0)
- [x] Java 21 / Spring Boot backend project source committed.
- [x] Health controller and initial health test source committed.
- [x] Country list/detail/search endpoints committed.
- [x] Country region-list endpoint committed.
- [x] GeoJSON map endpoints committed.
- [x] PostgreSQL/PostGIS Docker Compose and Flyway migration committed.
- [ ] Successful Maven verify and backend startup demonstrated.
- [ ] OpenAPI schema published and validated.
- [ ] Region search, dataset manifest, version, and download APIs.
- [ ] Request validation, consistent error model, pagination bounds, and observability.
- [ ] Spatial query performance and PostGIS integration tests.

### FR-11 — CI/CD and release (P0)
- [x] GitHub Actions Android CI workflow committed.
- [x] GitHub Actions backend CI workflow committed.
- [x] Android workflow updated to target app debug assembly and unit tests.
- [ ] Android CI passes for current commit.
- [ ] Backend CI passes for current commit.
- [ ] Lint, formatting, coverage thresholds, and integration test gates.
- [ ] Android debug APK published as a CI artifact.
- [ ] Signed Android release AAB/APK using protected secrets.
- [ ] Backend container publishing, release tags, deployment approvals.
- [ ] Dependabot/security scanning and release notes.

## 4. Technical architecture

### Android
- Kotlin + Jetpack Compose, MVI, Clean Architecture.
- Hilt dependency injection.
- Gradle Kotlin DSL, app/core/feature modularization.
- Retrofit networking; MapLibre rendering.
- Planned Room and DataStore for partial offline access.
- Coroutines/Flow and immutable UI state.

**Modules:** `:app`, `:core:common`, `:core:model`, `:core:designsystem`, `:core:network`, `:core:database`, `:core:datastore`, `:core:map`, `:core:testing`, `:feature:atlas`, `:feature:country`, `:feature:region`, `:feature:search`, `:feature:downloads`.

**Dependency rule:** feature modules may depend on core modules; core must not depend on feature or app. Domain contracts should not depend on Android UI or transport DTOs. The app owns composition and top-level navigation.

### Backend
- Java 21 and Spring Boot 3.x with constructor injection.
- REST/GeoJSON, PostgreSQL + PostGIS, Flyway.
- Country, region, spatial-map and dataset metadata capabilities.
- Versioned geographic source import, validation and attribution.

## 5. Data model

### Country
`id`, `iso2`, `iso3`, `nameEn`, `nameFa?`, `geometry`, `source`, `license`, `sourceVersion`, `referenceDate`, `updatedAt`.

### AdministrativeRegion
`id`, `countryId`, `adminLevel` (ADM1 for MVP), `sourceRegionCode?`, `nameEn`, `nameFa?`, `geometry`, `source`, `license`, `sourceVersion`, `referenceDate`, `updatedAt`.

### DatasetManifest (planned)
`datasetId`, `scope`, `countryIso2?`, `adminLevel`, `version`, `generatedAt`, `source`, `license`, `referenceDate`, `downloadUrl`, `checksum`, `sizeBytes`.

### OfflineRecord (planned)
`datasetId`, `version`, `downloadedAt`, `lastAccessedAt`, `state`, `localPath`, `sizeBytes`.

### Favorite (planned)
`entityType`, `entityId`, `createdAt`.

All planned fields are conceptual PRD requirements; do not assume they already exist in database migrations or API responses.

## 6. API contract and status

| Method | Path | Status |
|---|---|---|
| GET | `/api/v1/health` | [x] Source committed |
| GET | `/api/v1/countries` | [x] Source committed |
| GET | `/api/v1/countries/{iso2}` | [x] Source committed |
| GET | `/api/v1/countries/search?q=` | [x] Source committed |
| GET | `/api/v1/countries/{iso2}/regions` | [x] Source committed |
| GET | `/api/v1/map/countries` | [x] Source committed |
| GET | `/api/v1/map/regions` | [x] Source committed |
| GET | `/api/v1/map/countries/{iso2}/regions` | [x] Source committed |
| GET | `/api/v1/regions/search?q=` | [ ] Planned |
| GET | `/api/v1/datasets/manifest` | [ ] Planned |
| GET | `/api/v1/datasets/{id}/download` | [ ] Planned |

API source committed does not imply that endpoints were deployed or verified.

## 7. Screens and navigation

| Screen | Priority | Status |
|---|---|---|
| Launch / world map | P0 | [x] Initial source; [ ] verified |
| Country selection overlay | P0 | [x] Initial source; [ ] verified |
| ADM1 selection overlay | P0 | [x] Initial source; [ ] verified |
| Country details | P0 | [ ] |
| Region details | P0 | [ ] |
| Country/region search results | P0 | [ ] |
| Offline downloads and progress | P1 | [ ] |
| Settings and language | P1 | [ ] |
| Favorites/history | P2 | [ ] |
| About, licenses, sources | P0 | [ ] |

Navigation rules: map -> country -> region; search -> country/region; downloads -> available offline data; clear back navigation and selected-country context.

## 8. Non-functional requirements

- **Performance:** define and measure cold-start, map-ready, selection latency, search response time, memory use, and data-download size on representative devices.
- **Reliability:** graceful handling of no network, HTTP errors, missing data, invalid geometry, and interrupted downloads.
- **Security:** HTTPS in release, no embedded production secrets, validated inputs, dependency and secret scans, protected signing keys.
- **Accessibility:** RTL/LTR, TalkBack semantics, scalable typography, and adequate contrast.
- **Privacy:** no account or tracking required for MVP; disclose analytics if introduced.
- **Data quality:** auditable dataset version, coverage report, valid geometries, and source attribution.
- **Maintainability:** modular boundaries, contract tests, unit/integration/UI tests, code review, CI.
- **Legal:** respect geoBoundaries and tile-provider licenses, usage policies, and attribution requirements.

## 9. MVP release gates

The MVP is **not complete** until every gate below passes:

- [ ] Android clean build succeeds in CI.
- [ ] Backend clean build and integration tests succeed.
- [ ] Import job runs and produces a documented worldwide country/ADM1 coverage report.
- [ ] Country -> ADM1 selection -> details journey works on real data.
- [ ] Search in Persian and English works for countries and regions.
- [ ] Offline cache/download works for at least a representative supported country.
- [ ] Source/license/version are visible and accurate.
- [ ] Critical accessibility and RTL tests pass.
- [ ] Security, license, and tile-provider checks pass.
- [ ] Installable APK generated and smoke-tested.

## 10. Delivery phases

### Phase A — Repository and architecture
- [x] Initial Android and backend projects committed.
- [x] Initial module graph and Hilt/MVI scaffolding committed.
- [x] Initial Docker/PostGIS and workflows committed.
- [ ] Clean build and architecture dependency checks pass.

### Phase B — Data and world map
- [x] Initial geographic importer and map/GeoJSON implementation committed.
- [ ] Real dataset imported, audited, and tested end-to-end.
- [ ] Interactive country and ADM1 selection fully validated.

### Phase C — Profiles and search
- [ ] Country and region repositories/use cases and detail screens.
- [ ] Full country and region search with bilingual UX.
- [ ] Navigation, empty/error states, and UI tests.

### Phase D — Offline and polish
- [ ] Room/DataStore, download/versioning and offline-first flow.
- [ ] Localization, accessibility, source attribution, and performance improvements.

### Phase E — Release
- [ ] Passing CI, reproducible builds, test reports, signed release artifacts, documentation and release checklist.

## 11. Risks and mitigations

1. **ADM1 data gaps or disputed borders:** track coverage and sources by ISO code; communicate omissions; do not invent boundaries.
2. **Large GeoJSON payloads:** simplify geometry, add bounding-box/zoom-scoped queries and caching.
3. **Dependency compatibility:** pin and validate Gradle/AGP/Kotlin/KSP/Hilt/MapLibre/SDK combinations in CI.
4. **Offline storage:** download only selected datasets and implement size limits, checksums, and eviction.
5. **Localization gaps:** maintain fallback naming and translation completeness reports.
6. **Tile server restrictions:** display attribution and use an approved tile service or hosted tiles.
7. **Security:** remove permissive cleartext network settings from production.

## 12. Definition of Done (per requirement)

A feature can be marked fully done only when:
- implementation and module boundaries are reviewed;
- unit/integration/UI tests appropriate to the feature pass;
- Android/backend build and CI pass;
- error/empty/offline states are handled;
- accessibility and localization are reviewed;
- source, license and data provenance are documented where relevant;
- acceptance criteria are demonstrated using actual imported data.

## 13. Traceability and references

- Android architecture: [android/ARCHITECTURE.md](../android/ARCHITECTURE.md)
- Android modules: [android/settings.gradle.kts](../android/settings.gradle.kts)
- Map feature: [feature/atlas](../android/feature/atlas)
- Backend source: [backend](../backend)
- Data import: [data-import](../data-import)
- Android CI: [.github/workflows/android-ci.yml](../.github/workflows/android-ci.yml)
- Backend CI: [.github/workflows/backend-ci.yml](../.github/workflows/backend-ci.yml)
- Data source: https://www.geoboundaries.org/api.html

**Maintenance rule:** Update the checkbox only after confirming the matching artifact or acceptance test. Do not use a checked implementation box as proof of a passing build or released feature.
