# Android architecture

Android is a Gradle multi-module project. The app is the composition root; feature modules depend on shared core modules, never on app.

## Modules
- `:app`: application entry point.
- `:core:common`: shared primitives.
- `:core:model`: country and administrative-region models.
- `:core:designsystem`: shared Compose theme and UI foundations.
- `:core:network`: Retrofit API and Hilt providers.
- `:core:database`: offline persistence boundary; Room caching remains to be implemented.
- `:core:datastore`: preferences boundary; DataStore implementation remains to be implemented.
- `:core:map`: MapLibre dependency boundary.
- `:core:testing`: shared testing utilities.
- `:feature:atlas`: atlas entry screen, MVI state/intents/effects and ViewModel.
- `:feature:country`: country feature boundary.
- `:feature:region`: administrative-region feature boundary.
- `:feature:search`: search feature boundary.
- `:feature:downloads`: offline download feature boundary.

## Architecture rules
1. Features must not depend on `:app`.
2. Shared models live in `:core:model`.
3. Network clients are provided through Hilt in `:core:network`.
4. UI state is immutable and user actions are represented as intents.
5. Data/domain/presentation implementations stay in their owning feature or core module.
6. No credentials or production secrets in source control.

## Status
Module boundaries and the initial Atlas MVI screen are in place. Repository/use-case implementations, Room caching, persistent preferences, and fully wired MapLibre rendering are still follow-up work. Android CI must verify Gradle, AGP, Kotlin, and SDK compatibility before release.
