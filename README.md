# Global Atlas Engine — اطلس جهان‌نما

An interactive world atlas for Android to explore countries and first-level administrative divisions, with detailed profiles and partial offline support.

## Architecture
- **Android:** Kotlin, Jetpack Compose, MVI/Clean Architecture, Hilt, Room, DataStore, Retrofit and feature/core modules.
- **Backend:** Java 21, Spring Boot 3, PostgreSQL/PostGIS, Flyway and OpenAPI.
- **Geographic source:** geoBoundaries gbOpen ADM0/ADM1. See [source and license notes](data-import/SOURCES.md).

## Current capabilities
- Selectable countries and first-level administrative regions where source data exists.
- Country/region profiles and Persian/English search.
- GeoJSON endpoints with bounded geometry simplification to reduce map payloads.
- Partial offline cache for metadata and downloaded geometry.
- Geographic import pipeline with validation, audit records and coverage reports.

## Local development
```bash
docker compose up -d postgis
cd backend
DB_URL=jdbc:postgresql://localhost:5432/atlas DB_USERNAME=atlas DB_PASSWORD=atlas mvn spring-boot:run
```
Health: `http://localhost:8080/api/v1/health`  
OpenAPI UI: `http://localhost:8080/swagger-ui.html`

Open `android/` in Android Studio to run the client. The emulator uses `http://10.0.2.2:8080/`; physical devices need an environment-specific reachable URL. Debug cleartext HTTP is for development only; release builds disable it.

## CI/CD
- Android CI builds the app and runs unit tests.
- Backend CI runs Maven verification against PostGIS.
- Geographic importer CI runs its test suite.
- A tag beginning with `v` validates release builds, uploads an unsigned Android APK artifact and publishes the backend container to GHCR.
- Dependabot opens weekly dependency-update pull requests.

See [docs/RELEASE.md](docs/RELEASE.md) for release acceptance, security, performance, licensing and rollback guidance.

## Geographic data caveat
The importer is code, not a bundled dataset. The database remains empty until the import is run against a reachable PostGIS instance. Review JSON/CSV coverage reports before claiming full worldwide coverage; ADM1 availability varies by country.

## Project documentation
- [Android architecture](android/ARCHITECTURE.md)
- [Data importer instructions](data-import/README.md)
- [Release and hardening checklist](docs/RELEASE.md)
- [Security policy](SECURITY.md)
- [PRD فارسی](docs/PRD.fa.md)
- [English PRD](docs/PRD.md)
