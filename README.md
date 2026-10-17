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

- Java with the latest compatible stable Spring Boot release
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
- Java LTS toolchain compatible with the selected Spring Boot release
- Latest stable Spring Boot, PostgreSQL, and PostGIS releases compatible with the deployment environment

Re-check official compatibility matrices before pinning the complete toolchain, especially AGP/Gradle/JDK, Hilt/KSP, Spring Boot/JDK, and PostgreSQL/PostGIS combinations.

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
7. Add CI checks, dependency update automation, and documented release builds.

## Status

Initial project documentation. The application and backend implementation have not yet been added.
