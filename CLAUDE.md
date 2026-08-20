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
    is missing, the build still succeeds with empty-string values (TMDB calls would just fail at runtime).
    No repository/networking code consumes these yet — TMDB isn't actually called anywhere in the app.
- **minSdk 24 / targetSdk 37 / compileSdk 37**, Kotlin `2.2.10`, Java 11 compatibility, AGP `9.3.1`. Dependency
  versions are centralized in `gradle/libs.versions.toml` (version catalog) — add new dependencies there rather
  than hardcoding versions in `app/build.gradle.kts`.
- **Dependency injection:** Hilt (KSP-based annotation processing, not kapt). `CouchPilotApp`
  (`@HiltAndroidApp`) is registered as the `Application` class in the manifest; `MainActivity` is
  `@AndroidEntryPoint`; ViewModels are `@HiltViewModel` with `@Inject constructor(...)` and obtained in
  Composables via `hiltViewModel()` (not the plain `viewModel()` factory). Follow this same pattern for new
  ViewModels/screens. No `@Module`/`@Provides` bindings exist yet — add them once there's an actual dependency
  (e.g. a TMDB repository) that needs providing rather than direct `@Inject constructor` construction.
