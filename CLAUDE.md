# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this project is

CouchPilot is a planned local-first, privacy-focused UK TV recommendation app (see `general_idea.md` for the
full concept: TVmaze/TMDB as free data sources, an on-device recommendation engine, Room for local storage,
deep-links into UK catch-up apps like iPlayer/ITVX/Channel 4/My5, and no external user accounts).

The current code is a real skeleton of that app, not the full thing yet: two tabs (Tonight, Discover) behind
real navigation, Discover showing live TMDB trending shows with posters. Firebase AI Logic and the leftover
"Baking" sample it came with have been removed entirely — nothing in `general_idea.md` calls for an AI model,
so that dependency and its `google-services.json` requirement are gone. There is no TVmaze/EPG data, no
Room database, no recommendation engine, and no swipe onboarding yet — see `ROADMAP.md` for the phased plan
to build those out (Phase 1, nav shell + Discover, is done; Phase 2 is TVmaze next).

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
  interface, collected in the Composable with `collectAsState()`. Every current screen's ViewModel
  self-triggers its load in `init {}`, so `Loading` doubles as the initial state and there's no separate
  `Initial` variant sitting unreachable (`DiscoverUiState`/`TonightUiState`/`OnboardingUiState` are all just
  `Loading` / `Success` / `Error`) — only add an `Initial` state for a screen that genuinely waits for a user
  action before starting its first load. Follow this same shape (`XxxViewModel` + `XxxUiState` sealed
  interface) for new screens rather than introducing a different state-management approach.
- **No Firebase.** Firebase AI Logic and its `google-services.json` requirement were removed in roadmap
  Phase 1 along with the Baking sample — don't re-add them without a real reason; nothing in `general_idea.md`
  calls for an AI model.
- **Secrets/API keys** (currently just TMDB) must never be committed — per `general_idea.md`, this repo is
  public and keys are expected to stay out of git entirely.
  - Real values go in `secrets.properties` (gitignored — deliberately separate from the
    machine-specific `local.properties` so it can be synced across your own machines without clobbering
    `sdk.dir`). `local.properties.example` / `secrets.properties.example` are the checked-in templates
    documenting which keys a new dev needs to fill in.
  - `app/build.gradle.kts` reads `secrets.properties` at configuration time and exposes the values via
    `buildConfigField` (`buildFeatures.buildConfig = true`), so app code reads them as
    `BuildConfig.TMDB_API_KEY` / `BuildConfig.TMDB_READ_ACCESS_TOKEN` — never read `secrets.properties`
    directly from Kotlin/Java code, and never log/print those `BuildConfig` values. If `secrets.properties`
    is missing, the build still succeeds with empty-string values (TMDB calls fail at runtime instead).
- **minSdk 24 / targetSdk 37 / compileSdk 37**, Kotlin `2.2.10`, Java 11 compatibility, AGP `9.3.1`. Dependency
  versions are centralized in `gradle/libs.versions.toml` (version catalog) — add new dependencies there rather
  than hardcoding versions in `app/build.gradle.kts`.
  - **Kotlin version skew is a real, recurring trap here**: the project is pinned to Kotlin `2.2.10`, but
    plenty of libraries on their latest release are compiled with a newer Kotlin than that (e.g. Coil 3.5.0
    requires a Kotlin stdlib metadata version our 2.2.10 compiler can't read — `coil = "3.3.0"`, compiled
    against Kotlin 2.2.0, is what's actually in the catalog). If a new dependency's *latest* version fails to
    compile with a cryptic "compiled with an incompatible version of Kotlin" error, check `./gradlew
    :app:dependencyInsight --dependency kotlin-stdlib --configuration debugRuntimeClasspath` for what's forcing
    a newer stdlib, then pin that library to an older release built against a Kotlin close to `2.2.10` —
    don't reach for bumping the project's own Kotlin version as the first fix, since that cascades into KSP
    (`ksp = "2.2.10-2.0.2"`, version-locked to the exact Kotlin version) and the Compose compiler plugin too.
- **Navigation:** `androidx.navigation:navigation-compose` with `kotlinx.serialization`-backed type-safe
  routes (not string routes) — `presentation/navigation/Route.kt` (sealed `Route` with `@Serializable` data
  objects/classes) and `presentation/navigation/CouchPilotNavHost.kt` (bottom-tab `Scaffold` + `NavHost`,
  `composable<Route.X> { ... }`). Add new top-level screens as another `Route` + `TopLevelTab` entry there;
  add detail-style screens as a `Route` with no tab entry, wired into the `NavHost` block directly.
- **Images:** Coil (`coil-compose` + `coil-network-okhttp`) via `AsyncImage(model = someUrl, ...)` — used for
  TMDB posters in `DiscoverScreen`. `androidx.compose.material:material-icons-core` is an explicit dependency
  too (not pulled in transitively by `material3` in this AGP/Compose combination) — needed for any
  `Icons.Filled.*` usage, e.g. the nav bar's tab icons.
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
  - Only `getTrendingTvShows()` exists so far — verified against the real TMDB API on a device, and now
    rendered by `DiscoverScreen` as a poster grid (via Coil — see below).
  - **Data Source vs Repository:** a class that talks to one data source (one remote API, one DB) is a
    `*DataSource`; only a class that itself coordinates multiple data sources (e.g. remote + local cache) should
    be called a `*Repository`. If this app grows past a couple of features, promote `core/` and each feature
    package to real Gradle modules (`core:domain`, `core:data`, `feature:tmdb:domain`, `feature:tmdb:data`, ...)
    so `presentation` can't accidentally depend on `data` directly — the interfaces in `domain/` already exist
    for exactly that boundary, this would just make it enforced by the build graph instead of convention.
