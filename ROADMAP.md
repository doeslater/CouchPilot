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
- [x] **Phase 2** — TVmaze integration ("What's On Tonight?" with real data); later extended to a
      full "This Week" day-selector, not just today (see Phase 2's writeup)
- [x] **Phase 3** — Room offline-first cache + Wi-Fi prefetch
- [x] **Phase 4** — Swipe onboarding + preference storage

- [x] allow user to skip onboarding and view results without recommendations. A close (✕) icon
      button, top-end of `OnboardingScreen`, appears whenever the deck isn't finished and calls
      `OnboardingViewModel.skipOnboarding()` — sets `onboarding_completed` via
      `PreferencesRepository` (same call `completeOnboarding()` makes after a full swipe deck) with
      **no** `SwipeEventDao` insert, so Tonight/Discover land on the same rating-only cold-start
      ordering as a user who swiped on nothing overlapping their current lists (see Phase 5's
      `RecommendationScorer` cold-start note) — "results without recommendations" in practice means
      "results, ranked by rating instead of taste," since there's no unranked mode to fall back to.
      Covered by `OnboardingViewModelTest`'s `skipOnboarding completes onboarding without recording
      any swipe event`. Verified: `./gradlew testDebugUnitTest` and `assembleDebug` both clean.
      Follow-up: a skipped user needs a way back into the deck too, so `SettingsScreen` gained a
      "Retake taste swipes" `OutlinedButton` above the existing "Clear local data" one, wired to
      `SettingsViewModel.retakeOnboarding()` — resets just the `PreferencesRepository`
      `onboarding_completed` flag via `setOnboardingCompleted(false)`, deliberately *not* routing
      through `LocalDataManager.clearAllLocalData()`, so swipe history/cache survive a retake and
      only get overwritten by whatever new swipes the user makes this time. `CouchPilotNavHost`
      already reacts to the flag flipping false, so no navigation call was needed here, same as the
      existing clear-data path. New `SettingsViewModelTest` (this feature had no test file before)
      covers both `clearLocalData` and `retakeOnboarding`. Verified:
      `./gradlew assembleDebug testDebugUnitTest` clean.

- [x] have a user screen with analyse profile choices. `profile/presentation/{ProfileScreen,
      ProfileViewModel,ProfileUiState}.kt` — a detail-style screen (own `Route.Profile`, no
      bottom-nav tab, entered via a new "View your taste profile" button on `SettingsScreen`)
      surfacing the same `PreferenceVector` `RecommendationScorer` already builds internally for
      Tonight/Discover ranking, which until now only ever acted behind the scenes — this is the
      first place general_idea.md's "Privacy-Preserving AI Sync" becomes visible to the user
      instead of just powering ranking silently. Shows total signals recorded (liked/disliked
      counts, from `SwipeEventDao`) plus a per-genre affinity list (name + signed weight + a
      colored bar sized relative to the largest-magnitude genre), sorted strongest-liked to
      strongest-disliked. New `recommendation/domain/TmdbTvGenres.kt` is a hand-maintained TMDB TV
      genre id->name map (`GET /genre/tv/list`, a small fixed reference list — not worth a network
      call, same "hand-maintained reference list" pattern as `tvmaze/domain/FreeviewChannels.kt`)
      to make the vector's integer genre-id keys human-readable; falls back to `"Genre #<id>"` for
      any id not in the list. Handles the empty case (no swipe events yet, e.g. a user who used
      the new skip-onboarding path) with a plain explanatory message instead of an empty list.
      New `ProfileViewModelTest` covers both the empty state and the summarize/name/sort path.
      Verified: `./gradlew assembleDebug testDebugUnitTest` clean; `./gradlew lint` reproduces one
      pre-existing, unrelated failure (`RemoveWorkManagerInitializer` on `AndroidManifest.xml`,
      confirmed via `git stash` to already fail identically on a clean checkout) — not touched by
      this change, not yet fixed.

- [x] fix the `connectedDebugAndroidTest` packaging collision (see CLAUDE.md's build/test notes).
      Root cause tracked down via `./gradlew :app:dependencyInsight --dependency junit-jupiter
      --configuration debugAndroidTestRuntimeClasspath`: `mockk-android` pulls in `mockk-jvm`,
      which depends on `junit-jupiter`/`junit-platform` for *its own* test suite — nothing this
      app's tests use, since app tests are JUnit4 — and six of those jars each ship an identical
      `META-INF/LICENSE.md`. Fixed with a `packaging { resources { excludes += ... } }` block in
      `app/build.gradle.kts`; excluding just `LICENSE.md` surfaced a second, identical collision on
      `META-INF/LICENSE-notice.md` from the same jars (checked via `unzip -l` on one of them),
      excluded too. Verified on-device: `./gradlew connectedDebugAndroidTest` now passes clean (7
      tests: `ExampleInstrumentedTest`, `SwipeEventDaoTest`, `TvShowDaoTest`, `ScheduleDaoTest` — 0
      failures/errors/skipped), and `./gradlew assembleDebug testDebugUnitTest` still clean too.

- [x] **Phase 5** — Real cosine-similarity scorer built and wired into both Discover and Tonight
      (see Phase 5's writeup — one real bug found/fixed getting the Tonight side working)
- [x] **Phase 6** — Watch providers, `ShowDetailScreen` with provider logos + "Open" button, and
      Discover/Tonight both navigate to it — see Phase 6's writeup for the one caveat left
- [x] **Phase 7** — Dwell-time + explicit vote signals, a Settings screen to clear local data, and
      ViewModel unit tests across the app (see Phase 7's writeup — one real main-thread crash
      found/fixed getting the "clear data" path working)

## Reality checks against general_idea.md (apply throughout)

- **TVmaze**: real, free, no key. `GET /schedule?country=GB&date=YYYY-MM-DD` is the broadcast
  schedule endpoint (ISO code `GB`, not `UK`). It does **not** tag channels as Freeview/Freesat/Sky
  — that split has to be a hand-maintained channel-name whitelist in the app, not something the API
  hands you. Rate limit ~20 req/10s/IP applies to *TVmaze* calls specifically — the schedule is one
  call per load, so this isn't a practical constraint yet.
- **TMDB ↔ TVmaze bridge**: no shared ID. Bridge via TVmaze's `externals.imdb` →
  `GET /find/{imdb_id}?external_source=imdb_id` on TMDB (`TmdbRepository.getTvShowByImdbId()`).
  Real and documented, but an extra per-show network call that needs caching, and won't always
  match. These are TMDB calls, not TVmaze ones, so TVmaze's rate limit above doesn't apply to them
  — Phase 2 runs them concurrently per schedule item rather than throttling them.
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

### Phase 2 — TVmaze integration ("What's On Tonight?" with real data) ✅ done
**Goal:** the Tonight grid shows the real UK broadcast schedule.

- Landed as `tvmaze/domain/{ScheduleItem,TvMazeRepository}.kt`,
  `tvmaze/data/{RetrofitTvMazeRemoteDataSource,TvMazeService,TvMazeMappers,dto/ScheduleDto}.kt`,
  `tvmaze/di/TvMazeModule.kt` — Retrofit + Gson (`core/data/RetrofitFactory.kt`,
  `core/di/NetworkModule.kt`), same package convention as `tmdb/`. Calls
  `/schedule?country=GB&date=...` (via `Constants.DEFAULT_REGION`), no auth header.
- `tvmaze/domain/FreeviewChannels.kt` — a hand-maintained whitelist filtering the raw schedule
  client-side, matched by exact name or a `"<entry> "` prefix (regional variants like "BBC One
  London") — not a raw substring `contains`, which let short entries like `"W"` match anything
  containing the letter w. Document this list as an ongoing maintenance cost, not a one-time task.
- `TmdbRepository.getTvShowByImdbId()` (via TMDB's `/find/{imdb_id}` endpoint) is the IMDb-bridge
  call for poster/metadata enrichment, run concurrently per schedule item (`async`/`awaitAll` in
  `TonightViewModel.enrichSchedule`) rather than sequentially — these are TMDB calls, not TVmaze
  ones, so TVmaze's rate limit doesn't constrain them. UI handles "no metadata matched, title only"
  since not every show resolves.
- `TonightViewModel` calls `TvMazeRepository`, filters by whitelist, shows the raw schedule
  immediately, then re-emits once enrichment completes.
- Verified on-device: real dated schedule ("Thursday 20th August"), correct Freeview-only channels
  (ITV1, BBC One, Channel 4, E4), posters loading, no crashes.
- **Extended beyond this phase's original "tonight only" scope, into "This Week":**
  `TvMazeRepository.getScheduleForDate(date)` replaced the single-purpose `getTonightSchedule()`;
  `TonightViewModel` builds 7 `DayOption`s (Today/Tomorrow/weekday+date) and `TonightScreen` gets a
  `FilterChip` day-selector (same pattern as Discover's provider chips), each day an independent
  fetch with the same rapid-tap job-cancellation fix Discover needed. Verified on-device: switching
  days shows genuinely different real schedules (confirmed today vs. Friday 21st August).

### Phase 3 — Room offline-first cache + Wi-Fi prefetch ✅ done
**Goal:** the app works offline, per general_idea.md's "Smart Cache."

- `core/database/CouchPilotDatabase.kt` (`@Database`, now at version 4 — see Phases 4/5 for why it
  kept bumping) + `core/database/di/DatabaseModule.kt`. As anticipated, this file imports entity
  types from both `tmdb/` and `tvmaze/` — one more concrete argument (alongside CLAUDE.md's
  existing note) for eventually promoting to real Gradle modules.
- `tmdb/data/local/{TvShowEntity,TvShowDao}.kt`, `tvmaze/data/local/{ScheduleItemEntity,ScheduleDao}.kt`
  — real class names differ slightly from this doc's original sketch (`ScheduleEntity`), same idea.
- `DefaultTmdbRepository`/`DefaultTvMazeRepository` are cache-then-refresh, but with **different
  freshness policies that should probably be reconciled**: TMDB checks a 24h `lastUpdated` timestamp
  before trusting the cache (falls through to network once stale); the TVmaze schedule cache has
  **no TTL at all** — any non-empty cached row for a date is trusted forever, which is fine for a
  date already in the past but means a same-day schedule fetched early won't pick up later TVmaze
  corrections/additions without a manual cache clear. This is the "decide an explicit TTL per data
  type" work this phase originally called for — it's half-decided (TMDB) not fully.
- `core/sync/WeeklyPrefetchWorker.kt` (`CoroutineWorker`, `@HiltWorker`) prefetches TMDB trending +
  the next 7 days of TVmaze schedule concurrently (`async`/`awaitAll`), constrained to
  `NetworkType.UNMETERED` + `requiresBatteryNotLow`, enqueued via `enqueueUniquePeriodicWork` from
  `CouchPilotApp.onCreate` (`CouchPilotApp` now also implements `Configuration.Provider` for Hilt's
  `HiltWorkerFactory`).
- Room-backed DAO tests exist (`ScheduleDaoTest`, `TvShowDaoTest`, in-memory `Room` DB) plus
  repository unit tests with MockK (`DefaultTmdbRepositoryTest`, `DefaultTvMazeRepositoryTest`).
- New deps actually landed: `androidx.room:room-runtime`/`room-ktx:2.8.4` + `ksp(room-compiler:2.8.4)`,
  WorkManager + Hilt-Work integration, MockK for the repository tests.
- **Not yet verified**: the airplane-mode check below (kill network after one fetch, confirm cached
  data still renders) hasn't actually been run on-device yet — only the cache-hit path has unit-test
  coverage so far.

### Phase 4 — Swipe onboarding + preference storage ✅ done
**Goal:** cold-start swipe UI that persists real signal (not literal "embeddings").

- `onboarding/presentation/{OnboardingScreen,OnboardingViewModel,OnboardingUiState}.kt` — a swipeable
  card stack over Discover's existing TMDB trending pool (`.take(15)`), *plus* explicit
  dislike/info/like tap buttons (not just drag) — the info button routes to `Route.ShowDetail`
  (Phase 6). Drag gesture built with `Modifier.pointerInput` + `Animatable`/`graphicsLayer`, no
  third-party swipe library, exactly as planned.
- `onboarding/data/local/{SwipeEventEntity,SwipeEventDao}.kt` — raw events
  (`showId, genreIds: String (comma-joined), liked, timestampMillis`), matching the plan: not a
  pre-aggregated vector, so the scorer can be replayed without re-collecting data.
- `core/data/PreferencesRepository.kt` — `androidx.datastore:datastore-preferences`, a single
  `onboarding_completed` boolean, exactly as planned (Room would've been overkill). `MainViewModel`
  exposes it; `CouchPilotNavHost` gates into `Route.Onboarding` via a `LaunchedEffect` when unset,
  and pops back out to `Route.Tonight` once it flips true.
- **Two real bugs found and fixed here** (see git log for the full writeups):
  - `SwipeableCard`'s `remember { Animatable(0f) }` wasn't keyed to `show.id`, so Compose reused
    the previous card's post-swipe offset for the next card — it rendered flung off-screen,
    looking frozen after the first swipe. Fixed with `remember(show.id) { ... }`.
  - Adding `TvShowEntity.genreIds` (needed for Phase 5) changed the Room schema without bumping
    `CouchPilotDatabase`'s version, crashing every launch against an existing on-disk DB
    (`fallbackToDestructiveMigration()` only triggers on an actual version bump, not same-version
    schema drift). This happened *twice* in this phase's work (once for `TvShowEntity.genreIds`,
    again for `ScheduleItemEntity.genreIds` in Phase 5) — bump the version on every entity change,
    not just when it's convenient to remember.
- Verified on-device: a full 18-tap session through the 15-show deck completes onboarding and
  reaches Discover/Tonight with no crashes; individual dislike/info/like buttons and the drag
  gesture all independently confirmed working (info button correctly opens `ShowDetailScreen`,
  back navigation returns to the same onboarding card). **Not yet independently verified**: that
  the completed flag survives an actual process death after finishing onboarding (only verified
  within one continuous session).

### Phase 5 — Local recommendation engine (heuristic, not TFLite) ✅ done
**Goal:** turn swipe/genre signal into real ranking — the app's actual "AI," deliberately plain Kotlin.

- `recommendation/domain/{PreferenceVector,RecommendationScorer}.kt` — landed as two files, not
  three: `PreferenceVector` (a `Map<genreId, Double>` + its magnitude) and `RecommendationScorer`,
  which does double duty as both the vector-builder (`computePreferenceVector()`, reading
  `SwipeEventDao`: liked → +1.0, disliked → -1.0 per genre) *and* the cosine-similarity scorer
  (`score(genreIds, userTaste)`, normalized to `[0..1]`) — no separate `CosineSimilarityScorer`
  file, weight type is `Double` as planned. Zero new dependencies, plain `kotlin.math.sqrt`.
- `DiscoverViewModel`/`DefaultTmdbRepository.rankShows()` — sorts trending shows by
  `score(show.genreIds, userTaste)`. Straightforward: TMDB's own `TvShow.genreIds` is real and
  available with no bridging, so this one was correct on the first pass.
- `TonightViewModel`/`DefaultTvMazeRepository` — **this side had a real bug**: TVmaze schedule
  items don't carry TMDB genre IDs (TVmaze exposes free-text genre names, an incompatible
  vocabulary), so an initial version of the repository-level ranking called the scorer, computed a
  real preference vector, and then discarded it — scoring every item with a hardcoded `0.0`
  regardless, silently doing nothing while looking wired up. Fixed by moving the actual scoring to
  `TonightViewModel.enrichSchedule()`, the one place `ScheduleItem`s already get bridged to a real
  TMDB `TvShow` (for `posterUrl`) — genreIds now gets copied across in that same step, and the
  re-rank (`score()`, rating as tie-breaker/cold-start fallback) happens right after. The repository
  itself (`DefaultTvMazeRepository.rankByRating()`) now honestly does only a rating-only
  pre-enrichment sort, with no `RecommendationScorer` dependency pretending otherwise.
- `ScheduleItem`/`ScheduleItemEntity` gained a `genreIds` field to carry this (comma-joined in the
  entity, same convention as `SwipeEventEntity`/`TvShowEntity`) — always empty until enrichment
  populates it, by design.
- Verified on-device: Tonight's ranking visibly reorders by rating in the cold-start case (no
  genre overlap between swiped shows and the day's schedule); Discover's provider-filtered lists
  render correctly with the scorer active. No crashes across the fix + a fresh onboarding pass.
- Set expectations explicitly (in code and to yourself): ~19 TMDB TV genres makes this a coarse
  signal, good for "roughly matches what you swiped right on," not strong personalization.

### Phase 6 — Watch providers + honest "open the app" deep-link substitute ✅ done
**Goal:** ship the UK-streaming-availability idea, scoped to what's actually implementable.

- `TmdbRepository.getWatchProviders()` / `getTrendingTvShows(providerId)` (`GET /tv/{id}/watch/providers`
  + `/discover/tv`, region `GB` via `Constants.DEFAULT_REGION`) — filter chips on `DiscoverScreen`.
- `showdetail/presentation/{ShowDetailScreen,ShowDetailViewModel,ShowDetailUiState}.kt` — a real
  detail screen: poster, name, year, rating, overview, **provider logos + an "Open" button per
  provider** (`TmdbRepository.getWatchProvidersForShow(tvId)`, `GET /tv/{id}/watch/providers`,
  region-keyed, `flatrate`+`buy`+`rent` deduped by provider id).
- `showdetail/data/AppLauncher.kt` — `PackageManager` installed-check + generic app-open `Intent`,
  falling back to `market://details?id=...` (then a plain web Play Store URL) when not installed.
  Both `DiscoverScreen`'s poster tap and `TonightScreen`'s schedule rows now navigate to
  `Route.ShowDetail` through the same `onShowClick` callback threaded down from
  `CouchPilotNavHost` (Tonight's rows are only clickable once enrichment resolves a real TMDB id).
- **Two real bugs found and fixed getting this working** (see git log for the full writeups):
  - `DefaultTmdbRepository.getTvShowById()` only ever checked the Room cache with no network
    fallback, so any show reached via Tonight (never inserted into `tvShowDao`, since only
    `getTrendingTvShows()` populates it) always showed "Show not found" — confirmed on-device.
    Fixed by adding TMDB's `GET /tv/{id}` as a fallback (a new `TvShowDetailDto`, since that
    endpoint returns `genres: [{id,name}]` objects, not the flat `genre_ids: [int]` list/discover
    endpoints use) and caching the result for next time.
  - `AppLauncher.launchProviderApp()`: an unmapped provider fell through to a `market://
    details?id=null` / matching web URL instead of doing nothing.
- **Known caveat, carried forward from the original plan, not addressed**: provider package names
  in `AppLauncher` (BBC iPlayer, ITVX, etc.) are still unverified against real installed APKs — do
  that before trusting the "open app" path actually opens the right app on a real device.
- Verified on-device: Tonight's "Dragons' Den" (previously "Show not found") now opens a full
  detail screen with real data via the network-fallback fix, no crash. Discover's equivalent path
  shares the identical `onShowClick` wiring already confirmed working on the Tonight side, though a
  SystemUI overlay glitch on this device (unrelated to the app) prevented a fresh screenshot of it
  specifically this round.

### Phase 7 — Implicit signals, polish, tests ✅ done
**Goal:** close the "privacy-preserving AI sync" loop and bring test coverage up to the rest of the
architecture's implied standard.

- `SwipeEventEntity` gained a `weight: Double = 1.0` column (DB version 5 → 6) so
  `RecommendationScorer.computePreferenceVector()` can treat signals with different confidence
  differently (`delta = (liked ? 1 : -1) * weight`) instead of every event counting the same.
- **Dwell-time capture**: `ShowDetailViewModel` starts a `viewModelScope.launch { delay(8s); ... }`
  timer once a show loads, recording a weak positive (`weight = 0.3`) if the user is still on the
  screen after 8 seconds. Deliberately *not* a Compose `LaunchedEffect` as originally sketched —
  living on `viewModelScope` instead makes it plain suspend logic (unit-testable with a
  `TestDispatcher` + `advanceTimeBy`) and gets cancellation for free: backing out before the
  threshold cancels the coroutine via `onCleared()`, so a quick glance records nothing at all, only
  genuine dwelling does.
- **Explicit up/downvote**: 👍/👎 buttons on `ShowDetailScreen` next to the title, writing to the
  same `SwipeEventDao` as onboarding's swipes (`weight = 1.0`, full confidence). Plain emoji glyphs,
  not `Icons.Filled.ThumbUp/ThumbDown` — the latter's outline/filled variants only exist in
  `material-icons-extended`, which wasn't worth pulling in for two icons.
- **Settings screen**: new `core/domain/LocalDataManager` interface (+`DefaultLocalDataManager`,
  Hilt-bound) wraps `CouchPilotDatabase.clearAllTables()` and resets the onboarding flag behind one
  `clearAllLocalData()` call; `settings/presentation/{SettingsScreen,SettingsViewModel,
  SettingsUiState}.kt` puts a confirm-dialog-gated button behind it, reachable via a third
  "Settings" bottom-nav tab (`Route.Settings`). Resetting the onboarding flag alone sends the user
  back through Onboarding — `CouchPilotNavHost` already reacted to that flag before this phase.
- **Real bug found and fixed**: `clearAllTables()` is a blocking Room call and crashed with
  `IllegalStateException` when invoked directly from `viewModelScope.launch {}` (main thread) —
  confirmed on-device (tapping "Clear everything" crashed the app). Fixed by wrapping the call in
  `withContext(Dispatchers.IO)` inside `DefaultLocalDataManager`; re-verified on-device with a clean
  tap-through (no crash, swipe_events/schedule_items rows dropped to 0, `tv_shows` repopulated by
  the next trending fetch, onboarding flag reset and the UI landed back on the swipe screen).
- **ViewModel unit tests** added for `TonightViewModel`, `DiscoverViewModel`, `ShowDetailViewModel`
  (`OnboardingViewModel`/`RecommendationScorer` already had them from earlier phases) — all MockK +
  JUnit4, matching what was already there.
  - **Decision: stayed on JUnit4**, did not migrate to JUnit5. Turbine and `kotlinx-coroutines-test`
    were already in the catalog and unused, MockK was already the mocking convention, and nothing
    about testing dwell-time (`StandardTestDispatcher` + `advanceTimeBy`/`runCurrent`) needed
    anything JUnit5-specific — a migration would have been pure churn for a solo hobbyist project.
  - `androidx.navigation.SavedStateHandle.toRoute()` decodes route args via a real
    `android.os.Bundle` round-trip, which throws in a plain JVM unit test without Robolectric.
    Rather than pull in Robolectric for one `Int` arg, `ShowDetailViewModel` now has two
    constructors: a private-ish `internal` one taking `showId: Int` directly (what tests call), and
    the `@Inject`-annotated one taking `SavedStateHandle` (what Hilt calls in production),
    delegating to the first. Production decoding behavior is unchanged.
  - Also hit `android.util.Log.w` throwing "not mocked" from `DiscoverViewModel`'s provider-load
    failure path — fixed by adding `testOptions { unitTests.isReturnDefaultValues = true }` to
    `app/build.gradle.kts` rather than `mockkStatic`-ing `Log` in every test that touches it.
- Verified: `./gradlew testDebugUnitTest` green (20 tests) and `assembleDebug` clean; on-device,
  confirmed the vote buttons and dwell timer both land correct rows in `swipe_events` (pulled the
  live `couchpilot.db` + its `-wal`/`-shm` files via `run-as`/`exec-out` to see uncheckpointed
  writes) with the expected `weight` values, and confirmed the Settings clear-data flow end-to-end.

## Verification (per phase, not just at the end)

- After every phase: `./gradlew assembleDebug` clean, then `./gradlew installDebug` + launch on the
  connected device, `adb logcat` checked for `FATAL EXCEPTION` — compiling isn't enough, confirm on
  real hardware each time.
- Phase 2: log/inspect a real `/schedule?country=GB` response for a known date to confirm the
  whitelist filtering behaves before wiring it into the UI.
- Phase 3: kill network (airplane mode) after one successful fetch, relaunch, confirm cached data
  still renders.
- Phase 4: complete onboarding, force-stop the app (not just background it), relaunch, and confirm
  it goes straight to Tonight instead of restarting onboarding — done once end-to-end within a
  single session, but not yet re-verified specifically across a real process death.
- Phase 5: unit-test the cosine scorer directly on a couple of hand-built vectors with known
  expected ordering, before trusting it inside a ViewModel. When wiring a scorer into a *new* data
  source, check whether that source's items actually carry the genre vocabulary the scorer expects
  (TMDB integer IDs) before writing the ranking call — TVmaze's items didn't, which is exactly how
  Phase 5's dead-code bug happened here.
- Phase 6: manual on-device check — install/uninstall a provider app and confirm both branches
  (open app vs. Play Store fallback) actually fire.
- Phase 7: for anything writing to Room from a ViewModel coroutine, don't assume `Dispatchers.Main`
  is safe just because the call is inside `viewModelScope.launch {}` — `clearAllTables()` crashing
  on-device is exactly that mistake. To verify a signal actually landed with the right weight
  without waiting on `StateFlow`, pull the live db file **and** its `-wal`/`-shm` companions (Room
  runs in WAL mode; recent writes live there until checkpointed, not in the base `.db` file) via
  `adb exec-out run-as <pkg> cat ...` and query with a local `sqlite3`.
