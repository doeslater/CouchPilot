# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this project is

CouchPilot is a planned local-first, privacy-focused UK TV recommendation app (see `general_idea.md` for the
full concept: TVmaze/TMDB as free data sources, an on-device recommendation engine, Room for local storage,
deep-links into UK catch-up apps like iPlayer/ITVX/Channel 4/My5, and no external user accounts).

The current code is **not** that app yet — it is still the stock Android Studio template merged with Google's
Gemini-in-Firebase "Baking" quickstart sample (`BakingScreen.kt` / `BakingViewModel.kt` / `UiState.kt`: pick a
baked-goods photo, send it + a text prompt to a Firebase AI (`gemini-flash-latest`) model, show the response).
Expect to replace this sample screen as the real TV-recommendation features are built; there is no
recommendation engine, EPG fetching, or Room database implemented yet.

## Build / lint / test commands

Single Gradle module (`:app`), run from the repo root via the wrapper:

- Build debug APK: `./gradlew assembleDebug`
- Install on a connected device/emulator: `./gradlew installDebug`
- Lint (Android Lint): `./gradlew lint`
- Unit tests (JVM, `app/src/test`): `./gradlew testDebugUnitTest`
- Run a single unit test: `./gradlew testDebugUnitTest --tests "com.example.couchpilot.ExampleUnitTest"`
- Instrumented/UI tests (`app/src/androidTest`, needs a device/emulator): `./gradlew connectedDebugAndroidTest`
- Run a single instrumented test: `./gradlew connectedDebugAndroidTest --tests "com.example.couchpilot.ExampleInstrumentedTest"`

## Architecture notes

- **Package root:** `com.example.couchpilot` (applicationId/namespace still uses the default `com.example` prefix
  from project creation — this is a real thing to know when adding new packages, not something to silently "fix").
- **UI:** Jetpack Compose only (Material3, Compose BOM `2026.02.01`), no XML layouts, no Fragments/Views. Theme
  lives in `ui/theme/` (`Color.kt`, `Theme.kt`, `Type.kt`), applied at the root via `CouchPilotTheme` in
  `MainActivity`.
- **State pattern:** one `ViewModel` per screen exposing a `StateFlow<UiState>` where `UiState` is a sealed
  interface (`Initial` / `Loading` / `Success` / `Error`), collected in the Composable with `collectAsState()`.
  Follow this same shape (`XxxViewModel` + `XxxUiState` sealed interface) for new screens rather than introducing
  a different state-management approach.
- **AI integration:** uses Firebase AI Logic (`com.google.firebase:firebase-ai`, the `google-services` Gradle
  plugin) rather than calling a Gemini/TMDB/TVmaze REST endpoint directly. `Firebase.ai.generativeModel(...)` is
  called straight from the ViewModel (no repository layer exists yet). This requires an `app/google-services.json`
  which is **not** checked into the repo — get it from Firebase console setup before AI features will build/run
  against a real project; without it, `assembleDebug` fails at `processDebugGoogleServices` even though the
  code compiles.
- **Secrets/API keys** (Firebase config, TMDB) must never be committed — per `general_idea.md`, this repo is
  public and keys are expected to stay out of git entirely.
  - Real values go in `secrets.properties` (gitignored, API keys only — deliberately separate from the
    machine-specific `local.properties` so it can be synced across your own machines without clobbering
    `sdk.dir`) and `app/google-services.json` (gitignored). `local.properties.example` /
    `secrets.properties.example` are the checked-in templates documenting which keys a new dev needs to fill in.
  - `app/build.gradle.kts` reads `secrets.properties` at configuration time and exposes the values via
    `buildConfigField` (`buildFeatures.buildConfig = true`), so app code reads them as
    `BuildConfig.TMDB_API_KEY` / `BuildConfig.TMDB_READ_ACCESS_TOKEN` — never read `secrets.properties`
    directly from Kotlin/Java code, and never log/print those `BuildConfig` values. If `secrets.properties`
    is missing, the build still succeeds with empty-string values (TMDB calls fail at runtime instead).
- **minSdk 24 / targetSdk 37 / compileSdk 37**, Kotlin `2.2.10`, Java 11 compatibility, AGP `9.3.1`. Dependency
  versions are centralized in `gradle/libs.versions.toml` (version catalog) — add new dependencies there rather
  than hardcoding versions in `app/build.gradle.kts`.
- **Dependency injection:** Hilt (KSP-based annotation processing, not kapt). `CouchPilotApp`
  (`@HiltAndroidApp`) is registered as the `Application` class in the manifest; `MainActivity` is
  `@AndroidEntryPoint`; ViewModels are `@HiltViewModel` with `@Inject constructor(...)` and obtained in
  Composables via `hiltViewModel()` — note the import is `androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel`,
  not the `androidx.hilt.navigation.compose` one (deprecated as of `hilt-navigation-compose` 1.3.0). Follow this
  same pattern for new ViewModels/screens.
- **Data layer / networking:** domain and data are split by package, not by Gradle module (single-module
  project — see "Data Source vs Repository" below for when to promote this to real modules).
  - `core/domain/Result.kt` + `DataError.kt` — a generic `Result<D, E : Error>` used everywhere a function can
    succeed or fail with a typed error (not exceptions), plus the shared `DataError.Network` / `DataError.Local`
    error enums. Feature-specific errors implement `Error` directly instead of reusing `DataError`.
  - `core/data/SafeCall.kt` — `HttpClient.get<T>(url, queryParameters, block)` and `safeCall`/`responseToResult`
    turn a Ktor call + its exceptions into a typed `Result<T, DataError.Network>`. Reuse this for any new remote
    call rather than hand-rolling try/catch around Ktor.
  - `core/di/NetworkModule.kt` — provides the single shared `HttpClient` (JSON + logging only). Deliberately
    carries **no** auth — an API-specific token (like TMDB's) is added per-request in that API's own data source,
    not as a `defaultRequest` header on this shared client, or it would leak to whatever other API gets added
    next (e.g. TVmaze, per `general_idea.md`).
  - `tmdb/domain/{TvShow,TmdbRepository}.kt` — the domain model and repository interface; `tmdb/data/` — DTOs
    (`dto/`), `TmdbRoutes`/`TmdbImages` (endpoint + image URL builders — not secret, so not in `BuildConfig`),
    `KtorTmdbRemoteDataSource` (adds the `Authorization: Bearer ${BuildConfig.TMDB_READ_ACCESS_TOKEN}` header),
    `TvShowMappers.kt` (DTO → domain), `DefaultTmdbRepository`; `tmdb/di/TmdbModule.kt` binds the interface via
    `@Binds`. `TmdbRepository` is named/typed as a repository (not a data source) even though there's only one
    data source behind it today, because it's the seam a local Room cache would slot into later without
    presentation code changing — see general_idea.md's offline-first "Smart Cache" idea.
  - Only `getTrendingTvShows()` exists so far — verified against the real TMDB API on a device, not just
    compiled. Nothing in the UI calls it yet; `BakingScreen`/`BakingViewModel` are still the only visible screen.
  - **Data Source vs Repository:** a class that talks to one data source (one remote API, one DB) is a
    `*DataSource`; only a class that itself coordinates multiple data sources (e.g. remote + local cache) should
    be called a `*Repository`. If this app grows past a couple of features, promote `core/` and each feature
    package to real Gradle modules (`core:domain`, `core:data`, `feature:tmdb:domain`, `feature:tmdb:data`, ...)
    so `presentation` can't accidentally depend on `data` directly — the interfaces in `domain/` already exist
    for exactly that boundary, this would just make it enforced by the build graph instead of convention.
