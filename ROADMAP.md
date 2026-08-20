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
- [x] **Phase 5** — Real cosine-similarity scorer built and wired into both Discover and Tonight
      (see Phase 5's writeup — one real bug found/fixed getting the Tonight side working)
- [~] Phase 6 — Watch providers + a real `ShowDetailScreen` landed; the screen has no provider
      logos/deep-link buttons on it yet, and only onboarding's info button navigates to it —
      Discover's poster tap still opens a browser instead
- [ ] Phase 7 — Implicit signals, polish, tests

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

- The networking stack moved from Ktor to Retrofit + Gson partway through Phase 1/2 (see
  `core/data/RetrofitFactory.kt`, `core/di/NetworkModule.kt`), so this landed as
  `tvmaze/domain/{ScheduleItem,TvMazeRepository}.kt`, `tvmaze/data/{RetrofitTvMazeRemoteDataSource,
  TvMazeService,TvMazeMappers,dto/ScheduleDto}.kt`, `tvmaze/di/TvMazeModule.kt` — not the
  Ktor-named files originally sketched here, but the same package convention as `tmdb/`. Calls
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

### Phase 6 — Watch providers + honest "open the app" deep-link substitute (partially done)
**Goal:** ship the UK-streaming-availability idea, scoped to what's actually implementable.

- **Already landed, ahead of schedule**, during the Discover work: `TmdbRepository.getWatchProviders()`
  / `getTrendingTvShows(providerId)`, `GET /tv/{id}/watch/providers` + `/discover/tv`, region
  `GB` (via `Constants.DEFAULT_REGION`, fixed from an initial `"US"` default caught in review) — as
  filter chips on `DiscoverScreen`.
- **Also landed, during Phase 4's work**: `showdetail/presentation/{ShowDetailScreen,
  ShowDetailViewModel,ShowDetailUiState}.kt` and `TmdbRepository.getTvShowById(id)` — a real detail
  screen (poster, name, year, rating, overview, back button), `Route.ShowDetail`'s NavHost
  destination finally wired in. Verified on-device, reachable today only from onboarding's info
  button, not from Discover — `DiscoverScreen`'s poster tap still opens the show's TMDB web page in
  a browser rather than navigating to `ShowDetailScreen`.
- **Still to do:**
  - Wire `DiscoverScreen`'s poster tap to `Route.ShowDetail` instead of the browser stand-in.
  - Add provider logos + "Open" button per provider to `ShowDetailScreen` (the screen exists, but
    shows none of the watch-provider data `TmdbRepository.getWatchProviders()` already provides).
  - `showdetail/data/InstalledAppLauncher.kt` — `PackageManager` installed-check + generic app-open
    `Intent`, falling back to `market://details?id=...` when not installed. **Verify every provider
    package name (BBC iPlayer, ITVX, All4, My5, Netflix) against a real installed APK on a device
    as part of this phase's acceptance criteria — do not trust package names from search results**,
    and make clear in the UI this opens the app/store listing, not the specific episode.

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
