# Global Atlas Engine — اطلس جهان‌نما

An interactive world atlas for Android, designed to explore countries and their first-level administrative divisions with detailed country profiles and partial offline support.

## Architecture

### Android

- Kotlin and Jetpack Compose
- MVI with Clean Architecture and feature-oriented modules
- **Dependency injection: Hilt** for Android components and constructor injection
- Coroutines and Flow for asynchronous work and state streams
- Room for local persistence and offline cache
- Retrofit/OkHttp for HTTP networking, with Kotlin serialization for DTOs
- Navigation Compose for app navigation
- MapLibre Native Android, subject to a technical proof of concept
- Gradle Kotlin DSL and a version catalog (gradle/libs.versions.toml)

### Backend

- **Java 21 (LTS)**
- **Spring Boot 3.x**, selecting the latest stable patch compatible with Java 21 at implementation time
- Spring Dependency Injection / IoC and constructor injection
- Spring Web, Spring Validation, Spring Security when authentication is introduced
- PostgreSQL with PostGIS for spatial queries and geometry
- Spring Data JPA where appropriate
- Flyway for database migrations
- OpenAPI documentation
- Testcontainers for integration tests

### Toolchain and version policy

Use the latest **stable, mutually compatible** releases when implementation begins; avoid alpha/beta/RC dependencies for production foundations. Keep dependency versions centralized and record the selected versions in the version catalog and build files.

Baseline candidates checked on 2026-10-17:
- Kotlin 2.4.21
- Jetpack Compose BOM 2026.09.00
- Android Gradle Plugin 9.x, with built-in Kotlin support configured according to its migration guidance
- Java 21 LTS toolchain
- Latest stable Spring Boot 3.x patch compatible with Java 21, plus compatible stable PostgreSQL and PostGIS releases

Re-check official compatibility matrices before pinning the complete toolchain, especially AGP/Gradle/JDK, Hilt/KSP, Spring Boot 3.x/Java 21, and PostgreSQL/PostGIS combinations.

## Product scope

- Interactive map covering all countries
- Selectable first-level administrative divisions where source data is available
- Country and region detail pages
- Persian and English search
- Partial offline access to cached atlas data
- Dataset attribution, licensing, version, and reference date recorded for geographic data

## Initial implementation roadmap

1. Validate geographic data sources, licensing, and country/administrative-division coverage.
2. Build a map-rendering proof of concept on Android.
3. Define backend schemas and import pipeline for country and administrative-area geometry.
4. Implement country and region APIs with OpenAPI documentation.
5. Build Android navigation, Hilt modules, MVI state models, repositories, and local cache.
6. Add search, localization, offline behavior, and automated tests.
7. Add CI/CD for Android and backend: pull-request validation, formatting/lint, unit and integration tests, build artifacts, dependency/security checks, and release workflows.
8. Add dependency update automation and documented release builds.

## CI/CD plan

Use GitHub Actions as the initial CI/CD platform, with separate workflows for Android and backend so failures are isolated and feedback stays fast.

### Pull request / continuous integration

- Trigger on pull requests and pushes to the default branch.
- Android: configure JDK and Android SDK, validate Gradle wrapper, run lint and unit tests, and assemble the QA/debug variant.
- Backend: configure the selected Java LTS toolchain, run formatting checks, unit tests, integration tests with Testcontainers, and package the application.
- Validate OpenAPI and database migrations; run a lightweight repository and secret scan.
- Cache Gradle/Maven dependencies safely and upload test reports and build artifacts on failure/success as appropriate.
- Require CI checks to pass before merging.

### Delivery / release

- Publish versioned Android APK/AAB artifacts for tagged releases; signing keys and credentials must come from GitHub Actions secrets or a protected environment, never from the repository.
- Build a versioned backend container image and publish it to a container registry on release tags.
- Deploy only after tests pass, using protected GitHub Environments, approval gates where needed, and environment-specific secrets.
- Keep production deployment separate from pull-request workflows; never expose deployment credentials to untrusted PR code.
- Add Dependabot (or equivalent) for dependency update PRs and pin third-party GitHub Actions to reviewed versions or immutable SHAs.


## Geographic data and interactive map

The repository includes a geoBoundaries gbOpen import script for ADM0 country outlines and ADM1 first-level administrative divisions, plus MapLibre-based Android map rendering and GeoJSON endpoints.

- Source and API documentation: https://www.geoboundaries.org/api.html
- The geoBoundaries gbOpen product is documented as CC BY 4.0 compliant and requires attribution. The importer stores per-layer source/license/version metadata when provided.
- Run the importer instructions in [data-import/README.md](data-import/README.md).
- Map GeoJSON endpoints: `GET /api/v1/map/countries`, `GET /api/v1/map/countries/{iso2}/regions`, and `GET /api/v1/map/regions`.

**Important:** the importer is code, not a bundled dataset. The database remains empty until the migration is applied and the import script is run against a reachable PostGIS instance. Review the import report and source coverage before claiming full worldwide coverage. Region availability varies by source and country.

## Status

Initial project documentation and CI/CD plan. The application, backend, and workflow files have not yet been added.
