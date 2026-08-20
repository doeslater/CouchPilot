# CouchPilot roadmap — from general_idea.md to a real app

## Context

`general_idea.md` describes CouchPilot's target shape: a local-first, privacy-focused UK TV
recommendation app built on TVmaze (schedule) + TMDB (metadata/trending/watch providers), with an
on-device recommendation engine, Room for local storage, a swipe-based cold-start, offline-first
caching, and deep links into UK catch-up apps.

This roadmap turns that idea doc into an ordered, independently-shippable sequence of phases, each
buildable/demoable on its own, rather than one big change. It also fact-checks the doc against the
real APIs and flags two places where it's aspirational rather than literally implementable (UK app
deep-linking, on-device TensorFlow Lite), with a pragmatic substitute for each. **This is meant to be
executed over multiple future sessions, not in one pass** — treat each phase as its own future unit
of work, and update the checkboxes below as phases land.

## Status

- [x] **Phase 1** — Navigation shell, real screens, Baking sample + Firebase AI removed
- [ ] Phase 2 — TVmaze integration ("What's On Tonight?" with real data)
- [ ] Phase 3 — Room offline-first cache + Wi-Fi prefetch
- [ ] Phase 4 — Swipe onboarding + preference storage
- [ ] Phase 5 — Local recommendation engine (heuristic, not TFLite)
- [ ] Phase 6 — Watch providers + honest "open the app" deep-link substitute
- [ ] Phase 7 — Implicit signals, polish, tests

## Reality checks against general_idea.md (apply throughout)

- **TVmaze**: real, free, no key. `GET /schedule?country=GB&date=YYYY-MM-DD` is the broadcast
  schedule endpoint (ISO code `GB`, not `UK`). It does **not** tag channels as Freeview/Freesat/Sky
  — that split has to be a hand-maintained channel-name whitelist in the app, not something the API
  hands you. Rate limit ~20 req/10s/IP — fine interactively, but don't fan out per-show enrichment
  calls in parallel.
- **TMDB ↔ TVmaze bridge**: no shared ID. Bridge via TVmaze's `externals.imdb` →
  `GET /find/{imdb_id}?external_source=imdb_id` on TMDB. Real and documented, but an extra
  per-show network call that needs caching, and won't always match.
- **TMDB watch providers**: `GET /tv/{id}/watch/providers`, region-keyed (`results.GB`). Confirmed
  via TMDB's own docs: the `link` field is a TMDB attribution page, not a deep link into the
  provider's app or content.
- **UK app deep links**: no documented public URI-scheme/App-Link contract from BBC/ITV/C4/Channel 5
  for jumping straight to a title inside their apps. Realistic ceiling: detect if the app is
  installed and open it generically, else fall back to its Play Store listing. This is a real
  downgrade from general_idea.md's "direct deep-link buttons that open... the show" — build it as
  "open app / open Play Store," and say so in the UI, don't imply it lands on the exact episode.
- **On-device recommendation**: a trained TensorFlow Lite model isn't realistic here — no dataset,
  no labels, no training pipeline, and per-user on-device *training* is a different, much bigger
  problem than general_idea.md actually needs. Build the doc's own "alternative light engine"
  instead: a plain-Kotlin genre-vector + cosine-similarity scorer, no ML dependency at all. TFLite
  is dropped from this roadmap; it's a real future idea if the app ever gets real usage data, not a
  scheduled phase.
- **Room as cache**: slots under `TmdbRepository`/`TvMazeRepository` exactly as their existing code
  comments anticipate — add a local data source, change the repository impl to cache-then-refresh,
  the `Result<List<T>, DataError>` contract doesn't change, so no presentation-layer changes ripple
  out from it.

## Phases

### Phase 1 — Navigation shell, real screens, kill the Baking sample ✅ done
**Goal:** first genuinely-CouchPilot build: real navigation, a real (if data-sparse) screen showing
live TMDB data, Firebase AI gone entirely.

- `BakingScreen.kt`/`BakingViewModel.kt`/`UiState.kt` deleted; per-screen `UiState`s
  (`DiscoverUiState`, `TonightUiState`) follow the same sealed-interface shape instead of one shared file.
- `firebase-ai`/`firebase-bom`/`google-services` plugin and the `google-services.json` requirement
  removed from `app/build.gradle.kts` and `gradle/libs.versions.toml`.
- `androidx.navigation:navigation-compose` (2.9.7) with `kotlinx.serialization`-backed type-safe
  routes — `presentation/navigation/{Route,CouchPilotNavHost}.kt`, a bottom-tab shell (Tonight /
  Discover); `Route.ShowDetail` is defined but has no destination yet (arrives in Phase 6).
- `discover/presentation/{DiscoverScreen,DiscoverViewModel,DiscoverUiState}.kt` — renders
  `TmdbRepository.getTrendingTvShows()` as a poster grid via Coil (`TmdbImages.posterUrl`).
  Verified on-device: real trending posters render correctly.
- `tonight/presentation/{TonightScreen,TonightViewModel,TonightUiState}.kt` — placeholder until
  Phase 2 lands real TVmaze data.
- `MainActivity.kt` hosts `CouchPilotNavHost` instead of `BakingScreen`.
- Added `io.coil-kt.coil3:coil-compose` (3.3.0 — pinned below latest because 3.5.0 requires a newer
  Kotlin stdlib metadata version than this project's Kotlin 2.2.10 compiler can read) +
  `coil-network-okhttp` for poster image loading, and `androidx.compose.material:material-icons-core`
  (not pulled in transitively by material3 here) for the nav bar icons.

### Phase 2 — TVmaze integration ("What's On Tonight?" with real data)
**Goal:** the Tonight grid shows the real UK broadcast schedule.

- `tvmaze/domain/{Schedule,TvMazeRepository}.kt`, `tvmaze/data/{KtorTvMazeRemoteDataSource,
  TvMazeRoutes,ScheduleMappers,dto/ScheduleDto}.kt`, `tvmaze/di/TvMazeModule.kt` — same package
  convention as `tmdb/`. Calls `/schedule?country=GB&date=...`, no auth header (reuses the shared,
  auth-free `core/di/NetworkModule` client and `core/data/SafeCall.kt` as-is).
- A hand-maintained `FreeviewChannels` whitelist (plain `Set<String>`) filtering the raw schedule
  client-side — document it as an ongoing maintenance cost, not a one-time list.
- `tmdb/data/TmdbFindBySource.kt` — the IMDb-bridge call for poster/metadata enrichment, with basic
  throttling (not parallel fan-out) given TVmaze's rate limit; UI must handle "no metadata matched,
  title only" rather than assuming full enrichment.
- `TonightViewModel` calls `TvMazeRepository`, filters by whitelist, enriches via the bridge where
  it resolves.

### Phase 3 — Room offline-first cache + Wi-Fi prefetch
**Goal:** the app works offline, per general_idea.md's "Smart Cache."

- `core/database/CouchPilotDatabase.kt` (`@Database`) + `core/database/di/DatabaseModule.kt`.
  Note: this one file necessarily imports entity types from `tmdb/`/`tvmaze/` — a small, accepted
  inversion of the "core doesn't know about features" layering, and one more concrete argument
  (alongside CLAUDE.md's existing note) for eventually promoting to real Gradle modules.
- `tmdb/data/local/{TvShowEntity,TvShowDao}.kt`, `tvmaze/data/local/{ScheduleEntity,ScheduleDao}.kt`.
- `DefaultTmdbRepository`/`DefaultTvMazeRepository` become cache-then-refresh: return cached rows
  immediately if present, refresh over the network, update the DB, re-emit. Repository interfaces
  don't change.
- `core/sync/WeeklyPrefetchWorker.kt` (`CoroutineWorker`), `PeriodicWorkRequest` constrained to
  `NetworkType.UNMETERED`, enqueued via `WorkManager.enqueueUniquePeriodicWork` from
  `CouchPilotApp.onCreate`.
- Decide and document an explicit cache TTL per data type up front (e.g. trending: daily; schedule:
  per-day, since it's date-scoped) — offline-first without a stated invalidation rule tends to just
  silently go stale.
- New deps: `androidx.room:room-runtime`/`room-ktx:2.8.4` + `ksp(room-compiler:2.8.4)`,
  `androidx.work:work-runtime:2.11.2` (depend on `work-runtime` directly, not `-ktx`, which is now a
  near-empty compat shim on recent WorkManager releases).

### Phase 4 — Swipe onboarding + preference storage
**Goal:** cold-start swipe UI that persists real signal (not literal "embeddings").

- `onboarding/presentation/{OnboardingScreen,OnboardingViewModel,OnboardingUiState}.kt` — swipeable
  card stack over Discover's existing TMDB trending pool (no new data source; independent of Phase
  2's completion). Build the drag gesture with `Modifier.pointerInput` + `Animatable`/`graphicsLayer`
  directly rather than a third-party swipe library.
- `onboarding/data/local/{SwipeEventEntity,SwipeEventDao}.kt` — store raw events
  (`showId, tmdbGenreIds, liked, timestampMillis`), not a pre-aggregated vector, so Phase 5's scorer
  can be iterated on/replayed without re-collecting data.
- `androidx.datastore:datastore-preferences` for a single "has completed onboarding" flag (Room
  would be overkill for one boolean). Nav gate: unset flag → `Route.Onboarding` before `Route.Tonight`.

### Phase 5 — Local recommendation engine (heuristic, not TFLite)
**Goal:** turn swipe/genre signal into real ranking — the app's actual "AI," deliberately plain Kotlin.

- `recommendation/domain/{PreferenceVector,RecommendationScorer}.kt` — genre-weight map built from
  `SwipeEventDao` (liked → positive weight, disliked → negative, optionally recency-decayed).
- `recommendation/domain/CosineSimilarityScorer.kt` — plain dot-product/norm arithmetic, zero new
  dependencies.
- `TonightViewModel`/`DiscoverViewModel` sort through the scorer instead of raw API order.
- Model the weight type as `Double` (not just ±1) now, so Phase 7's dwell-time signal doesn't force
  a rework later.
- Set expectations explicitly (in code comments and to yourself): ~19 TMDB TV genres makes this a
  coarse signal, good for "roughly matches what you swiped right on," not strong personalization.

### Phase 6 — Watch providers + honest "open the app" deep-link substitute
**Goal:** ship the UK-streaming-availability idea, scoped to what's actually implementable.

- `TmdbRepository.getWatchProviders(tvId): Result<WatchProviders, DataError>`,
  `tmdb/data/dto/WatchProvidersDto.kt`, `GET /tv/{id}/watch/providers`, GB-only domain model.
- `showdetail/presentation/{ShowDetailScreen,ShowDetailViewModel,ShowDetailUiState}.kt` — provider
  logos + "Open" button per provider. This is also where `Route.ShowDetail`'s NavHost destination
  finally gets wired in.
- `showdetail/data/InstalledAppLauncher.kt` — `PackageManager` installed-check + generic app-open
  `Intent`, falling back to `market://details?id=...` when not installed. **Verify every provider
  package name (BBC iPlayer, ITVX, All4, My5, Netflix) against a real installed APK on a device as
  part of this phase's acceptance criteria — do not trust package names from search results**, and
  make clear in the UI this opens the app/store listing, not the specific episode.

### Phase 7 — Implicit signals, polish, tests
**Goal:** close the "privacy-preserving AI sync" loop and bring test coverage up to the rest of the
architecture's implied standard.

- Dwell-time capture (`LaunchedEffect` timing on show cards/detail) feeding Phase 5's scorer as a
  weak, down-weightable signal (distraction ≠ interest — treat accordingly).
- Explicit up/downvote buttons, same storage path as swipe events.
- `settings/presentation/...` screen to clear local data / reset onboarding — meaningful proof point
  for the "no accounts, everything local" pitch.
- ViewModel unit tests for Tonight/Discover/Onboarding/ShowDetail/the scorer. Decide here whether to
  stay on the existing JUnit4 setup or adopt JUnit5+Turbine+AssertK (needs `useJUnitPlatform()` and
  new catalog entries — a real if small migration, not just one added dependency).

## Verification (per phase, not just at the end)

- After every phase: `./gradlew assembleDebug` clean, then `./gradlew installDebug` + launch on the
  connected device, `adb logcat` checked for `FATAL EXCEPTION` — compiling isn't enough, confirm on
  real hardware each time.
- Phase 2: log/inspect a real `/schedule?country=GB` response for a known date to confirm the
  whitelist filtering behaves before wiring it into the UI.
- Phase 3: kill network (airplane mode) after one successful fetch, relaunch, confirm cached data
  still renders.
- Phase 4: complete onboarding twice; confirm the flag persists across process death (not just
  in-memory) and the swipe events land in the DB (`adb shell run-as ... sqlite3` or a debug DB
  inspector).
- Phase 5: unit-test the cosine scorer directly on a couple of hand-built vectors with known
  expected ordering, before trusting it inside a ViewModel.
- Phase 6: manual on-device check — install/uninstall a provider app and confirm both branches
  (open app vs. Play Store fallback) actually fire.
