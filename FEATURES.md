# Feature map

Every user-facing feature in CouchPilot, mapped to the files that implement it. See `GENERAL_IDEA.md`
for the concept behind each feature and `ROADMAP.md` for the phase/history each landed in.

## Tonight — 7-day UK broadcast schedule, ranked by taste

| Role | File |
| --- | --- |
| UI: day-selector chips + schedule list. | `tonight/presentation/TonightScreen.kt` |
| Loads the day's schedule, bridges TVmaze items to TMDB (`enrichSchedule()`) for posters/genre ids, re-ranks via `RecommendationScorer`. | `tonight/presentation/TonightViewModel.kt` |
| `Loading` / `Success(days, selectedDay, schedule)` / `Error`. | `tonight/presentation/TonightUiState.kt` |

## Discover — TMDB trending shows, filterable by watch provider, plus curated UK collections

| Role | File |
| --- | --- |
| UI: provider filter chips + trending poster grid; curated collection rows (full-span grid items) shown only when no provider filter is active. | `discover/presentation/DiscoverScreen.kt` |
| Loads providers + trending shows + curated collections concurrently; re-fetches trending on provider filter change (preserving collections, job-cancellation for rapid taps). | `discover/presentation/DiscoverViewModel.kt` |
| `Loading` / `Success(shows, providers, selectedProviderId, collections)` / `Error`; `DiscoverCollection(title, shows)`. | `discover/presentation/DiscoverUiState.kt` |
| 4 collection definitions (`title, genreId, minVoteCount`) - drives a live TMDB genre+GB-origin query, not a hardcoded show list. | `discover/domain/UkCultureCollections.kt` |
| Collections hydration + provider-filter tests. | `discover/presentation/DiscoverViewModelTest.kt` (test) |

## Search — direct title lookup for UK streaming availability (Watchmode)

| Role | File |
| --- | --- |
| UI: search bar + results list. | `watchmode/presentation/SearchScreen.kt` |
| Queries Watchmode, bridges TV-show hits with a TMDB id into `ShowDetailScreen` so they contribute to the taste profile like any other show. | `watchmode/presentation/SearchViewModel.kt` |
| Domain contracts + models for Watchmode search/source lookups. | `watchmode/domain/{WatchmodeRepository,WatchmodeSearchResult,WatchmodeSource}.kt` |
| Repository impl. | `watchmode/data/DefaultWatchmodeRepository.kt` |
| Retrofit network layer. | `watchmode/data/RetrofitWatchmodeRemoteDataSource.kt`, `WatchmodeService.kt` |
| Wire-format DTOs. | `watchmode/data/dto/{WatchmodeSearchDto,WatchmodeSourceDto}.kt` |
| Hilt bindings. | `watchmode/di/WatchmodeModule.kt` |

## Bookmarks — save a show for later, browse everything saved

| Role | File |
| --- | --- |
| UI: poster grid + empty state. | `bookmarks/presentation/BookmarksScreen.kt` |
| Collects the bookmarks table live, hydrates each id via `TmdbRepository`. | `bookmarks/presentation/BookmarksViewModel.kt` |
| `Loading` / `Success(shows)`. | `bookmarks/presentation/BookmarksUiState.kt` |
| Room entity — `showId` keyed, independent of the swipe/vote signal. | `bookmarks/data/local/BookmarkEntity.kt` |
| Room DAO — insert/delete/get/getAll/clearAll. | `bookmarks/data/local/BookmarkDao.kt` |
| Empty state, hydration order, dropped-on-failure coverage. | `bookmarks/presentation/BookmarksViewModelTest.kt` (test) |
| In-memory-Room DAO coverage. | `bookmarks/data/local/BookmarkDaoTest.kt` (androidTest) |

## Settings — clear local data, reset/retake onboarding, link to Profile

| Role | File |
| --- | --- |
| UI: clear-data confirm dialog, retake-swipes button, view-profile link. | `settings/presentation/SettingsScreen.kt` |
| Wraps `LocalDataManager.clearAllLocalData()` and `PreferencesRepository.setOnboardingCompleted()`. | `settings/presentation/SettingsViewModel.kt` |
| Holds `isClearing`/`didClear` flags. | `settings/presentation/SettingsUiState.kt` |
| Covers clear-data and retake-onboarding paths. | `settings/presentation/SettingsViewModelTest.kt` (test) |

## Onboarding — swipe-based cold start

| Role | File |
| --- | --- |
| UI: swipeable card stack + like/info/dislike/skip buttons + skip-deck close button. | `onboarding/presentation/OnboardingScreen.kt` |
| Drives the deck over Discover's trending pool, records swipes, completes/skips onboarding. | `onboarding/presentation/OnboardingViewModel.kt` |
| Deck state. | `onboarding/presentation/OnboardingUiState.kt` |
| Room entity for every recorded signal (`showId, genreIds, liked, timestamp, weight`) — shared with `ShowDetailScreen`'s vote/dwell signals. | `onboarding/data/local/SwipeEventEntity.kt` |
| Room DAO for swipe events. | `onboarding/data/local/SwipeEventDao.kt` |
| Covers swipe/skip/complete paths. | `onboarding/presentation/OnboardingViewModelTest.kt` (test) |

## Show Detail — poster, providers, vote, bookmark, dwell tracking

| Role | File |
| --- | --- |
| UI: poster, title, thumbs up/down, bookmark heart, provider rows, chip-origin CTA, Watchmode "check all sources" link. | `showdetail/presentation/ShowDetailScreen.kt` |
| Loads show + providers + bookmark state, records vote/dwell-time signals, toggles bookmark, launches provider apps/websites. | `showdetail/presentation/ShowDetailViewModel.kt` |
| `Loading` / `Success(show, providers, userVote, originProviderName, isBookmarked)` / `Error`. | `showdetail/presentation/ShowDetailUiState.kt` |
| Installed-app check + generic app-open intent, website-search fallback per provider. | `showdetail/data/AppLauncher.kt` |
| (interfaces backing the above, no Android imports) | `showdetail/domain/` |
| Hilt bindings. | `showdetail/di/` |
| Vote/dwell/bookmark/origin-provider coverage. | `showdetail/presentation/ShowDetailViewModelTest.kt` (test) |

## Profile — view your on-device taste profile

| Role | File |
| --- | --- |
| UI: liked/disliked counts + per-genre affinity bars. | `profile/presentation/ProfileScreen.kt` |
| Summarizes `SwipeEventDao` history through `RecommendationScorer`'s `PreferenceVector`. | `profile/presentation/ProfileViewModel.kt` |
| Success/empty state. | `profile/presentation/ProfileUiState.kt` |
| Empty-state + summarize/sort coverage. | `profile/presentation/ProfileViewModelTest.kt` (test) |

## Recommendation engine — on-device cosine-similarity scorer

| Role | File |
| --- | --- |
| `Map<genreId, Double>` + magnitude. | `recommendation/domain/PreferenceVector.kt` |
| Builds the vector from `SwipeEventDao` (liked +1.0 / disliked -1.0, weighted) and scores `genreIds` against it. | `recommendation/domain/RecommendationScorer.kt` |
| Hand-maintained TMDB TV genre id → name map (for `ProfileScreen`). | `recommendation/domain/TmdbTvGenres.kt` |
| Hilt bindings. | `recommendation/di/RecommendationModule.kt` |
| Scorer unit coverage on hand-built vectors. | `recommendation/domain/RecommendationScorerTest.kt` (test) |

---

## Shared infrastructure (used across features, not a feature on its own)

### Navigation & app shell

| Role | File |
| --- | --- |
| Type-safe sealed `Route` (`Tonight`, `Discover`, `Search`, `Bookmarks`, `Settings`, `Onboarding`, `Profile`, `ShowDetail`). | `presentation/navigation/Route.kt` |
| Bottom-tab `Scaffold` + `NavHost`, onboarding gating. | `presentation/navigation/CouchPilotNavHost.kt` |
| Exposes `hasCompletedOnboarding` for the nav gate. | `presentation/MainViewModel.kt` |
| Hosts `CouchPilotNavHost`, `enableEdgeToEdge()`, applies `CouchPilotTheme`. | `MainActivity.kt` |
| `@HiltAndroidApp` + `Configuration.Provider` (WorkManager), enqueues the prefetch worker. | `CouchPilotApp.kt` |
| Material3 theme. | `ui/theme/{Color,Theme,Type}.kt` |

### Persistence

| Role | File |
| --- | --- |
| Single Room `@Database` (v7): `TvShowEntity`, `ScheduleItemEntity`, `SwipeEventEntity`, `BookmarkEntity`. | `core/database/CouchPilotDatabase.kt` |
| Provides the DB + every DAO. | `core/database/di/DatabaseModule.kt` |
| DataStore-backed `hasCompletedOnboarding` flag. | `core/data/PreferencesRepository.kt` |
| Wipes Room + the onboarding flag behind one `clearAllLocalData()` call. | `core/domain/LocalDataManager.kt` / `core/data/DefaultLocalDataManager.kt` |
| Hilt binding for the above. | `core/di/LocalDataModule.kt` |

### Networking & shared data-layer plumbing

| Role | File |
| --- | --- |
| Base URLs, API paths, UK provider web-search paths for TMDB/TVmaze/Watchmode. | `AppEndpoint.kt` |
| Misc shared constants (e.g. default region `GB`). | `AppConstants.kt` |
| Builds a `Retrofit` from a shared `OkHttpClient` + a feature's base URL. | `core/data/RetrofitFactory.kt` |
| `safeCall { ... }` — Retrofit `Response`/exceptions → typed `Result<T, DataError.Network>`. | `core/data/SafeCall.kt` |
| Shared `OkHttpClient` (logging interceptor, debug-only body logging). | `core/di/NetworkModule.kt` |
| Generic success/typed-error result type used everywhere. | `core/domain/{Result,DataError}.kt` |
| `@HiltWorker` — prefetches TMDB trending + 7-day TVmaze schedule on unmetered Wi-Fi. | `core/sync/WeeklyPrefetchWorker.kt` |

### TMDB data source (backs Tonight, Discover, Search, ShowDetail, Bookmarks)

| Role | File |
| --- | --- |
| Domain contract + models, incl. `discoverByGenre(genreId, minVoteCount)` for Discover's UK Culture Collections. | `tmdb/domain/{TmdbRepository,TvShow,WatchProvider}.kt` |
| Cache-then-refresh impl (Room cache + TMDB network); `discoverByGenre` bypasses taste-based ranking on purpose. | `tmdb/data/DefaultTmdbRepository.kt` |
| Retrofit network layer. | `tmdb/data/RetrofitTmdbRemoteDataSource.kt`, `TmdbService.kt` |
| DTO↔domain↔entity mapping; poster/backdrop URL building. | `tmdb/data/TvShowMappers.kt`, `TmdbImages.kt` |
| Wire-format DTOs. | `tmdb/data/dto/{TvShowDto,FindByIdResponseDto,ShowWatchProvidersDto,WatchProviderDto,TmdbSearchDto}.kt` |
| Room cache. | `tmdb/data/local/{TvShowEntity,TvShowDao}.kt` |
| Hilt bindings. | `tmdb/di/TmdbModule.kt` |
| Coverage. | `tmdb/data/DefaultTmdbRepositoryTest.kt` (test), `tmdb/data/local/TvShowDaoTest.kt` (androidTest) |

### TVmaze data source (backs Tonight)

| Role | File |
| --- | --- |
| Domain contract, model, and the hand-maintained Freeview/Freesat channel whitelist. | `tvmaze/domain/{TvMazeRepository,ScheduleItem,FreeviewChannels}.kt` |
| Cache-then-refresh impl. | `tvmaze/data/DefaultTvMazeRepository.kt` |
| Retrofit network layer. | `tvmaze/data/RetrofitTvMazeRemoteDataSource.kt`, `TvMazeService.kt` |
| DTO↔domain↔entity mapping. | `tvmaze/data/TvMazeMappers.kt` |
| Wire-format DTO. | `tvmaze/data/dto/ScheduleDto.kt` |
| Room cache. | `tvmaze/data/local/{ScheduleItemEntity,ScheduleDao}.kt` |
| Hilt bindings. | `tvmaze/di/TvMazeModule.kt` |
| Coverage. | `tvmaze/data/DefaultTvMazeRepositoryTest.kt` (test), `tvmaze/data/local/ScheduleDaoTest.kt` (androidTest) |
