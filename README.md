# Global Atlas Engine — اطلس جهان‌نما

An interactive world atlas for Android, designed to explore countries and their first-level administrative divisions with detailed country profiles and partial offline support.

## Planned architecture

### Android
- Kotlin and Jetpack Compose
- MVI with Clean Architecture
- Room, Coroutines, and Flow
- Manual dependency wiring with an `AppContainer` (no DI framework)
- MapLibre Native Android, subject to a technical proof of concept

### Backend
- Java 21 and Spring Boot
- PostgreSQL with PostGIS
- Flyway database migrations
- OpenAPI documentation

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
5. Build Android navigation, MVI state models, repositories, and local cache.
6. Add search, localization, offline behavior, and automated tests.

## Status

Initial project documentation. The implementation has not yet been added.
