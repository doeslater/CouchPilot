# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this project is

CouchPilot is a local-first, privacy-focused UK TV recommendation app (see `general_idea.md` for the full
concept: TVmaze/TMDB as free data sources, an on-device recommendation engine, Room for local storage,
deep-links into UK catch-up apps like iPlayer/ITVX/Channel 4/My5, and no external user accounts).

`ROADMAP.md` tracks the phased build-out from the original bare-template skeleton to the current app — all
seven planned phases are done. Current shape: three bottom-nav tabs (Tonight — a 7-day UK broadcast schedule
ranked by on-device taste; Discover — TMDB trending shows filterable by watch provider; Settings — clear all
local data / reset onboarding), a swipe-based onboarding flow, a `ShowDetailScreen` with watch-provider
"open app" buttons and explicit up/downvote, and a plain-Kotlin cosine-similarity recommendation engine fed by
swipe/vote/dwell-time signals. Firebase AI Logic and the template's "Baking" sample were removed in Phase 1 —
nothing in `general_idea.md` calls for an AI model, so don't re-add that dependency without a real reason.

## Build / lint / test commands

Single Gradle module (`:app`), run from the repo root via the wrapper:

- Build debug APK: `./gradlew assembleDebug`
- Install on a connected device/emulator: `./gradlew installDebug`
- Lint (Android Lint): `./gradlew lint`
- Unit tests (JVM, `app/src/test`): `./gradlew testDebugUnitTest`
- Run a single unit test: `./gradlew testDebugUnitTest --tests "com.example.couchpilot.tonight.presentation.TonightViewModelTest"`
- Instrumented/UI tests (`app/src/androidTest`, needs a device/emulator): `./gradlew connectedDebugAndroidTest`
- Run a single instrumented test: `./gradlew connectedDebugAndroidTest --tests "com.example.couchpilot.onboarding.data.local.SwipeEventDaoTest"`

`connectedDebugAndroidTest` used to fail at `mergeDebugAndroidTestJavaResource` (`mockk-android` pulls in
`mockk-jvm`, which depends on `junit-jupiter`/`junit-platform` for its *own* tests — not anything this app's
test code uses, since app tests are JUnit4 — and six of those jars ship identical `META-INF/LICENSE.md` /
`META-INF/LICENSE-notice.md` files, colliding at merge time). Fixed with a `packaging { resources { excludes
+= ... } } }` block in `app/build.gradle.kts` excluding both files. Verified on-device: full suite green (7
tests: `ExampleInstrumentedTest`, `SwipeEventDaoTest`, `TvShowDaoTest`, `ScheduleDaoTest`, 0 failures/errors/
skipped).

## Architecture notes

- **Package root:** `com.example.couchpilot` (applicationId/namespace still uses the default `com.example` prefix
  from project creation — this is a real thing to know when adding new packages, not something to silently "fix").
- **UI:** Jetpack Compose only (Material3), no XML layouts, no Fragments/Views. Theme lives in `ui/theme/`
  (`Color.kt`, `Theme.kt`, `Type.kt`), applied at the root via `CouchPilotTheme` in `MainActivity`.
- **State pattern:** one `ViewModel` per screen exposing a `StateFlow<UiState>` where `UiState` is a sealed
  interface, collected in the Composable with `collectAsState()`. Every screen's ViewModel self-triggers its
  load in `init {}`, so `Loading` doubles as the initial state and there's no separate `Initial` variant
  sitting unreachable — only add an `Initial` state for a screen that genuinely waits for a user action before
  starting its first load. Follow this same shape (`XxxViewModel` + `XxxUiState` sealed interface) for new
  screens rather than introducing a different state-management approach.
- **No Firebase.** Removed in Phase 1 along with the Baking sample — don't re-add `firebase-ai`/`google-services`
  without a real reason; nothing in `general_idea.md` calls for an AI model.
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
    `google-services.json` isn't required at all (Firebase was removed in Phase 1).
- **minSdk 24 / targetSdk 37 / compileSdk 37**, Kotlin `2.2.10`, Java 11 compatibility. Dependency versions are
  centralized in `gradle/libs.versions.toml` (version catalog) — add new dependencies there rather than
  hardcoding versions in `app/build.gradle.kts`.
  - **Kotlin version skew is a real, recurring trap here**: the project is pinned to Kotlin `2.2.10`, but
    plenty of libraries on their latest release are compiled with a newer Kotlin than that (e.g. Coil 3.5.0
    requires a Kotlin stdlib metadata version our 2.2.10 compiler can't read — `coil = "3.3.0"`, compiled
    against Kotlin 2.2.0, is what's actually in the catalog). If a new dependency's *latest* version fails to
    compile with a cryptic "compiled with an incompatible version of Kotlin" error, check `./gradlew
    :app:dependencyInsight --dependency kotlin-stdlib --configuration debugRuntimeClasspath` for what's forcing
    a newer stdlib, then pin that library to an older release built against a Kotlin close to `2.2.10` —
    don't reach for bumping the project's own Kotlin version as the first fix, since that cascades into KSP
    (`ksp = "2.2.10-2.0.2"`, version-locked to the exact Kotlin version) and the Compose compiler plugin too.
  - `testOptions { unitTests.isReturnDefaultValues = true }` is set so unmocked Android SDK calls reached from
    a plain JVM unit test (e.g. `android.util.Log.w`) return a default instead of throwing "not mocked" —
    cheaper than `mockkStatic`-ing every such call, or pulling in Robolectric.
- **Navigation:** `androidx.navigation:navigation-compose` with `kotlinx.serialization`-backed type-safe
  routes (not string routes) — `presentation/navigation/Route.kt` (sealed `Route` with `@Serializable` data
  objects/classes: `Tonight`, `Discover`, `Settings`, `Onboarding`, `ShowDetail(id: Int)`) and
  `presentation/navigation/CouchPilotNavHost.kt` (bottom-tab `Scaffold` + `NavHost`, `composable<Route.X> { ... }`,
  bottom bar hidden on `Onboarding`). Add new top-level screens as another `Route` + `TopLevelTab` entry there;
  add detail-style screens as a `Route` with no tab entry, wired into the `NavHost` block directly.
  - A route arg read via `SavedStateHandle.toRoute<Route.X>()` decodes through a real `android.os.Bundle`
    round-trip, which throws in a plain JVM unit test without Robolectric. `ShowDetailViewModel` works around
    this with two constructors: an `internal` one taking the decoded `Int` directly (what tests call) and the
    `@Inject` one taking `SavedStateHandle` (what Hilt calls in production), delegating to the first — follow
    this pattern for any other ViewModel that both takes nav args and needs a unit test.
- **Images:** Coil3 (`coil-compose` + `coil-network-okhttp`) via `AsyncImage(model = someUrl, ...)`.
  `androidx.compose.material:material-icons-core` is an explicit dependency (not pulled in transitively by
  `material3` in this AGP/Compose combination) — but it's *core* only, no `material-icons-extended`. Check
  before assuming an `Icons.Filled.X`/`Icons.Outlined.X` exists (`ThumbUp` does, `ThumbDown` doesn't) — a plain
  emoji `Text()` is the established fallback for the couple of icons this project needed that aren't in core.
- **Dependency injection:** Hilt (KSP-based annotation processing, not kapt). `CouchPilotApp`
  (`@HiltAndroidApp`, also `Configuration.Provider` for WorkManager) is the `Application` class; `MainActivity`
  is `@AndroidEntryPoint`; ViewModels are `@HiltViewModel` with `@Inject constructor(...)` and obtained in
  Composables via `hiltViewModel()` — note the import is
  `androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel`, not the `androidx.hilt.navigation.compose` one
  (deprecated as of `hilt-navigation-compose` 1.3.0). Follow this same pattern for new ViewModels/screens.
- **Networking:** Retrofit + Gson. `core/data/RetrofitFactory.kt` builds a `Retrofit` from a shared
  `OkHttpClient` (`core/di/NetworkModule.kt`,
  logging-only, debug builds get `HttpLoggingInterceptor.Level.BODY`) + a feature's base URL; each feature adds
  auth as a per-request `@Header`, never as a `defaultRequest` on the shared client, so one API's token can't
  leak onto another API's calls. `core/data/SafeCall.kt` (`safeCall { retrofitService.call(...) }`) turns a
  Retrofit `Response<T>` + exceptions into a typed `Result<T, DataError.Network>` — reuse this for any new
  remote call rather than hand-rolling try/catch.
- **Persistence:** Room (`core/database/CouchPilotDatabase.kt`, `@Database`) is the single DB for
  `TvShowEntity`, `ScheduleItemEntity`, `SwipeEventEntity`, opened via `.fallbackToDestructiveMigration()`
  (`core/database/di/DatabaseModule.kt`) — **bump `version` on every entity schema change**, or the app crashes
  on launch with `Room cannot verify the data integrity`; with destructive migration this also means a version
  bump wipes all local data on next launch, which is expected/fine pre-release but worth remembering when
  debugging "why is my test data gone." Room's own blocking calls (e.g. `clearAllTables()`) **must** run off
  the main thread (`withContext(Dispatchers.IO) { ... }`) — Room asserts this itself and crashes if violated;
  this bit `DefaultLocalDataManager` for real in Phase 7. `androidx.datastore:datastore-preferences` handles
  the one simple flag (`PreferencesRepository.hasCompletedOnboarding`) that would be overkill for Room.
  DB writes go through Room's WAL journal — a recent write may only exist in the `-wal` file, not the base
  `.db` file, until checkpointed. To inspect on-device state, pull all three
  (`adb exec-out run-as com.example.couchpilot cat .../couchpilot.db{,-wal,-shm} > local-file`) and query the
  base file locally with `sqlite3` — `sqlite3` isn't present on the device itself.
- **Background work:** WorkManager (`core/sync/WeeklyPrefetchWorker.kt`, `@HiltWorker`), enqueued as a
  `NetworkType.UNMETERED` periodic request from `CouchPilotApp.onCreate()`.
- **Data layer / networking package layout:** domain and data are split by package, not by Gradle module
  (single-module project — see "Data Source vs Repository" below for when to promote this to real modules).
  Per feature: `<feature>/domain/` (domain models + repository/data-source **interfaces**, no Android imports),
  `<feature>/data/` (DTOs in `dto/`, the Retrofit `*Service` interface, `Retrofit<Feature>RemoteDataSource`,
  `<Model>Mappers.kt` for DTO↔domain↔entity conversions, `Default<Feature>Repository`, `local/` for Room
  `*Entity`/`*Dao` if the feature caches), `<feature>/di/` binds the interface via `@Binds` or `@Provides`.
  - `core/domain/Result.kt` + `DataError.kt` — a generic `Result<D, E : Error>` used everywhere a function can
    succeed or fail with a typed error (not exceptions), plus the shared `DataError.Network` / `DataError.Local`
    error enums, and `map`/`onSuccess`/`onFailure`/`asEmptyResult` extensions for chaining. Feature-specific
    errors implement `Error` directly instead of reusing `DataError`.
  - `core/domain/LocalDataManager.kt` (+ `core/data/DefaultLocalDataManager.kt`) — the one class that
    coordinates wiping both Room (`CouchPilotDatabase.clearAllTables()`) and the DataStore onboarding flag,
    behind a single `clearAllLocalData()` call. Named a "manager," not a repository, since it has no reads.
  - `tmdb/` and `tvmaze/` are both named/typed as `*Repository` (not `*DataSource`) even though each has only
    one remote source today, because each is the seam a local Room cache slots into (and now has) without
    presentation code changing — see general_idea.md's offline-first "Smart Cache" idea. Each repository impl
    is cache-then-refresh: return cached rows if fresh, otherwise hit the network and update the cache.
  - `recommendation/domain/{PreferenceVector,RecommendationScorer}.kt` — plain-Kotlin cosine similarity over
    TMDB genre IDs, built from `SwipeEventDao` history. Every recorded signal (onboarding swipe, explicit
    up/downvote on `ShowDetailScreen`, dwell-time on `ShowDetailScreen`) carries a `weight: Double` on
    `SwipeEventEntity` — explicit signals are `1.0`, dwell-time is a down-weighted `0.3` since lingering on a
    screen isn't the same confidence as a real decision. When wiring the scorer into a *new* data source,
    check that source's items actually carry TMDB integer genre IDs before calling `score()` — TVmaze exposes
    free-text genre names, a different vocabulary the scorer can't use directly (see `TonightViewModel`'s
    `enrichSchedule()` for the TMDB-bridge pattern that resolves this).
  - **Data Source vs Repository:** a class that talks to one data source (one remote API, one DB) is a
    `*DataSource`; only a class that itself coordinates multiple data sources (e.g. remote + local cache, or
    Room + DataStore) should be called a `*Repository`/`*Manager`. If this app grows past its current handful
    of features, promote `core/` and each feature package to real Gradle modules (`core:domain`, `core:data`,
    `feature:tmdb:domain`, `feature:tmdb:data`, ...) so `presentation` can't accidentally depend on `data`
    directly — the interfaces in `domain/` already exist for exactly that boundary, this would just make it
    enforced by the build graph instead of convention.
- **Testing:** JUnit4 + MockK + `kotlinx-coroutines-test` + Turbine, deliberately not JUnit5 — considered and
  rejected in Phase 7 since nothing being tested needed anything JUnit5-specific and migrating would have been
  pure churn. ViewModel tests mock constructor dependencies with MockK and drive coroutines with either
  `UnconfinedTestDispatcher` (when nothing in the code path uses `delay()`) or `StandardTestDispatcher` +
  `runTest(dispatcher)` + explicit `runCurrent()`/`advanceTimeBy()` (when it does, e.g. `ShowDetailViewModel`'s
  dwell timer) — reach for `runCurrent()` over `advanceUntilIdle()` whenever a pending `delay()` shouldn't be
  fast-forwarded by an unrelated assertion earlier in the same test.
