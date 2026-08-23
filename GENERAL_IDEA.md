Here is an architecture and feature concept for a local-first, privacy-focused UK TV recommendation app.

**Free API & Tech Stack**

* **Data Sources (100% Free):** TVmaze (UK broadcast schedule), TMDB (trending titles, metadata, posters, watch providers), Watchmode (granular UK streaming availability) — see Part 2 below for what each is best for, pricing, and a comparison matrix.


* **On-Device Recommendation Engine:**
* **TensorFlow Lite (Recommendation Model):** Runs locally on the user's phone to process user history and generate match scores without sending personal data to a backend.
* **Alternative Light Engine:** Cosine similarity via Vector Search (using Android Room or ObjectBox vector extensions).


* **Local Data Storage:**
* **Room Database:** Stores local user watch history, genre preferences, and show ratings directly on the device.



---

**Core Concept Ideas & Features**

* **1. "What's On Tonight?" Local Grid**
* Pulls live UK EPG (Electronic Program Guide) data via TVmaze for Freeview/Freesat channels.
* Filters the current evening's broadcast schedule through the local AI model to rank channels by user preference.


* **2. "Tinder-Style" Swipe Training**
* Rapid cold-start onboarding where users swipe right on shows they like and left on shows they don't.
* Embeddings are stored in the local Room DB to build an instant personal preference profile.


* **3. UK Streaming Search & Deep-Links**
* Matches recommendations to availability on UK catch-up services (BBC iPlayer, ITVX, Channel 4, My5).
* A dedicated Search tab powered by Watchmode allows looking up any title to see exactly where it's streamable in the UK, including formats (HD/4K) and rental/purchase prices.
* Includes direct buttons that open the provider's website or app.


* **4. Offline-First "Smart Cache"**
* The app fetches the current week's metadata when connected to Wi-Fi.
* Offline scoring allows recommendations to display instantly without continuous internet requests.


* **5. Privacy-Preserving AI Sync**
* No external user accounts needed. All learning occurs locally via implicit feedback (time spent looking at a show card, saved items) and explicit feedback (upvotes/downvotes).

* **6. Bookmarking**
* A heart toggle on a show's detail screen saves it to a local "Bookmarks" list, browsable from its own bottom-nav tab.
* Kept deliberately separate from the up/downvote taste signal — bookmarking means "I want to come back to this," not a genre preference, so it doesn't feed the recommendation engine.

**Development Notes**

* Load Android API skills when working on platform-specific code.
* This repo is public. API keys are secret and must never be committed — the AI agent must not commit them to git.

---

## Part 2: API Landscape for a UK TV Recommendation Engine

To build a TV recommendation engine or app tailored for the UK market, you need a combination of APIs that handle
**content metadata**, **recommendation logic**, and **UK streaming availability** (e.g., BBC iPlayer, ITVX,
Channel 4, UK Netflix, Disney+). The three already in use (TMDB, TVmaze, Watchmode) cover this; a fourth is
listed below as a freemium alternative worth knowing about if Watchmode's free tier ever becomes a constraint.

### 1. The Movie Database (TMDB)

* **Best for:** Algorithmic TV show recommendations, high-quality poster images, and detailed metadata.
* **Recommendation Feature:** Has dedicated recommendation endpoints based on similarity, user votes, genres, and watch history.
* **UK Context:** Supports localization query parameters (`language=en-GB` and `watch_region=GB`).
* **Pricing:** 100% free for non-commercial use with an API key.

### 2. TVmaze

* **Best for:** Live UK TV schedules (EPG), UK network data (BBC, ITV, Sky), and episode tracking.
* **Recommendation Feature:** No explicit recommendation engine, but allows filtering by genres, UK schedule dates, and network IDs.
* **UK Context:** Full coverage of UK terrestrial and regional TV schedules (`ISO country code = GB`).
* **Pricing:** 100% free, no API key required for the core public REST API.

### 3. Watchmode

* **Best for:** Provider availability ("where to watch in the UK") and deep-linking directly into streaming apps.
* **Recommendation Feature:** Includes title recommendation endpoints filtered by UK streaming availability (BBC iPlayer, ITVX, All 4, Prime Video UK, etc.).
* **UK Context:** Highly specific UK regional streaming and broadcast catalog mapping.
* **Pricing:** Freemium — 1,000 free API calls/month.

### 4. Streaming Availability API (Movie of the Night, via RapidAPI) — not currently used

* **Best for:** Cross-platform UK streaming availability and genre-based recommendations.
* **Recommendation Feature:** Search and filtering by popularity, release year, rating, and platform availability in `gb`.
* **UK Context:** Strong support for UK streaming services (`country=gb`).
* **Pricing:** Freemium — 100–1,000 requests/month depending on backend configuration.

### Comparison Matrix

| API | Key Purpose | Recommendations Endpoint | UK Streaming Support | Free Tier Limits |
| --- | --- | --- | --- | --- |
| **TMDB** | Metadata, posters, similar shows | Yes (`/tv/{id}/recommendations`) | Basic (via JustWatch data) | Generous (rate limited) |
| **TVmaze** | UK schedules & network data | Manual (genre / network filtering) | Broadcast/EPG focus (`country=GB`) | Unlimited public requests |
| **Watchmode** | "Where to watch" in UK & links | Yes (`/titles/`, `/list-titles/`) | Extensive (iPlayer, ITVX, etc.) | 1,000 calls/month |

### Recommended Architecture

1. **Fetch recommendations:** call TMDB's `/tv/{tv_id}/recommendations` endpoint for recommended show IDs and posters.
2. **Check UK availability:** pass those show IDs (or IMDb IDs) to Watchmode with `region=gb` to show where a title streams in the UK.
3. **Display live schedule:** if the show airs on live UK TV (e.g., BBC One), query TVmaze with `country=GB` for upcoming air dates.

---

## Part 3: Feature Backlog (beyond the current MVP)

Ideas for future phases, grouped by theme and deduplicated from earlier brainstorming — the current app only
implements the "Core Concept Ideas & Features" above (see `ROADMAP.md` for what's actually built).
Backlog items move up into that list once they ship, as Bookmarking (item 6) just did.

### Discovery & Curation

* **"Time Budget" Dial:** Filter content by available viewing time, from a 20-minute comedy short to a full 90-minute film.
* **"Mood & Vibe" Selector:** Replace genre tags with natural-language moods like *"British Cozy Crime,"* *"Mind-Bending Thriller,"* or *"Lightweight Background TV."* Can be combined with the Time Budget Dial into a single "Mood & Time" quiz.
* **Live Freeview EPG Highlights:** A "What's On Right Now" feed across linear UK channels (BBC One, ITV1, Channel 4, Channel 5) alongside streaming picks.
* **Spoiler-Free Episode Tracker:** Tracks watched episodes across multi-series shows without revealing episode titles, thumbnails, or synopses that give away plot twists.
* **UK Culture Collections:** Curated categories around national viewing habits — *"Bingeable Box Sets,"* *"Award-Winning British Dramas,"* *"Panel Shows & Comedy,"* *"Best of British Docs."*
* **"Trending in the UK Today":** A daily top-10 list reflecting what's actively trending across British streaming charts and social conversation.
* **"Best of British" Sub-Genre Deep Dives:** Niche categorizations beyond standard genres — *Cozy British Murder Mystery* (*Death in Paradise*, *Midsomer Murders*), *Witty Panel Shows & Stand-up* (*Taskmaster*, *Would I Lie to You?*), *Post-Pub Background Comedy* (*Peep Show*, *The Inbetweeners*), *Gritty Northern Crime Dramas* (*Happy Valley*, *Sherwood*).
* **BFI & Cult Classic Picks:** Archive British television, forgotten 90s/00s miniseries, and critically acclaimed UK indie films.
* **"Second Screen" vs. "High Focus" Mode:** Tags shows by required engagement — background-friendly (*Bake Off*, *Escape to the Country*) vs. subtitled/complex plots needing full attention.
* **British Weather-Based Recommendations:** Cozy comfort TV on rainy days, short upbeat outdoor/travel series on sunny weekends.
* **"Broadcaster Jargon" Translator:** Maps traditional schedule jargon (*Catch-Up*, *Box Sets*, *Linear Simcasts*, *FAST Channels*) into unified, clean rows.
* **Cast & Crew Cross-App Tracker:** Follow specific actors, writers, or directors (e.g., Phoebe Waller-Bridge, Sally Wainwright, David Tennant) across UK platforms and get alerted when new or archive work becomes available.
* **Random Episode Shuffler:** For long-running sitcoms or panel shows (*Peep Show*, *Taskmaster*), pick a random highly-rated episode to skip decision fatigue.
* **"Episode Length" Hard Cap:** A slider to filter by exact duration (e.g., "only shows with episodes under 28 minutes").

### UK Streaming & Deep-Linking

* **"Where to Watch" Regional Direct Links:** Shows exactly where a title streams in the UK across BBC iPlayer, ITVX, Channel 4, My5, Netflix, Disney+, Prime Video, and Sky/NOW, with buttons that open the provider directly to the title's playback screen.
* **"Free-to-Air Only" / "No-Paywall Budget Mode":** A single toggle hides paid subscription platforms (Netflix, Disney+, Sky) and reorganizes the UI around 100% free UK apps (BBC iPlayer, ITVX, Channel 4, My5, U, STV Player, Pluto TV, Tubi).
* **"Leaving Soon" Alerts:** Tracks UK catch-up rights expirations and surfaces an immediate watchlist (e.g., "leaving iPlayer in 3 days") sorted by urgency.
* **Multi-Platform App Consolidation Engine:** Flags content cross-published on secondary free platforms (e.g., UKTV/U shows available inside the Channel 4 app) and routes to the user's preferred launcher.
* **"Freely" & Freeview Broadband Grid:** A live broadband-based EPG grid combining traditional Freeview channels with IP-streamed live stations.
* **Ad-Tier vs. Premium Catalog Mapping:** Marks whether a recommended show is free-with-ads or behind a paid ad-free tier (e.g., ITVX Premium, Netflix Standard with Ads vs. Premium), including extra catalogue access like BritBox content.
* **FAST Channel Aggregator:** Integrates linear FAST streams (Pluto TV, Tubi, Samsung TV Plus) alongside traditional Freeview schedules.
* **Regional Freeview Channel Filter:** Customizes live EPG listings by UK nation/region (BBC One Scotland, ITV Wales, STV Player) so schedules and local news match the viewer's transmission zone.

### Compliance, Personalization & Alerts

* **Watchlist & Provider Toggle:** Users tick off streaming subscriptions they pay for so the app hides content locked behind paywalls they don't have.
* **Post-Watershed & BBFC/Ofcom Controls:** Filters recommendations using Ofcom watershed rules and official BBFC ratings (U, PG, 12, 15, 18); mature content can stay locked until after 21:00 unless unlocked via biometric auth.
* **"Ditch or Keep" Prompt:** If a user stops halfway through an episode or film, ask whether to keep it active or drop it from recommendations.
* **Series Return Notifications:** Alerts when a new series of a saved favorite drops (e.g., *The Traitors*, *Line of Duty*, *Doctor Who*).
* **TV Licence Compliance Filter:** A soft toggle ("I have a UK TV Licence") that suppresses BBC iPlayer and live TV suggestions when disabled, while keeping non-BBC ad-supported platforms (ITVX, Channel 4, My5, Tubi) available.
* **Season Catch-up Calculator:** Calculates episodes-per-night needed to finish previous seasons before a new one premieres.
* **Smart "Where Did I Leave Off?" Hub:** Tracks manual episode progress across fragmented apps without needing account integration for every service.
* **Broadcast vs. Catch-up Sync + Calendar Export:** Builds a calendar for weekly linear releases (e.g., *Strictly Come Dancing*) vs. all-at-once box-set drops, exportable to the device's system calendar.
* **"Next Up" Airing Radar:** Notifies the user the moment a live broadcast (e.g., a Channel 4 drama) finishes airing and becomes available on catch-up.
* **Commute Download Estimator:** Calculates storage size and download time over Wi-Fi for offline viewing before a low-connectivity commute.

### Social & Group Viewing

* **Social & Group Recommendation ("Match Mode"):** Two or more users swipe on titles to find overlapping watchlists for group viewing nights.
* **Nearby Device "Couch Match":** Pairs two nearby Android phones via Bluetooth/Wi-Fi Direct to intersect saved watchlists without a backend server.
* **Pass-the-Phone Swipe Mode:** A quick session where friends vote *Watch*/*Skip* on trailer cards until a mutual match appears.
* **Friend Vouching:** Send a 1-tap recommendation ("Dave recommended this") to another user's in-app inbox with an optional text tag.
* **Spoiler-Safe Community Reactions:** A lightweight voting system tagging shows with safety attributes (*"Gory,"* *"Cliffhanger Finale,"* *"Feel-Good,"* *"Heavy Drama"*) without revealing plot details.

### Android Platform Integration

* **Google TV Ambient/Daydream Screen Saver:** Pushes high-resolution artwork and "What's On Tonight" cards to the screen background when idle.
* **App Icon Quick-Action Shortcuts:** Long-press the home screen icon for 1-tap options ("Freeview Right Now", "Quick 30m Comedy", "Scan Nearby Match").
* **System Share Target:** Share links or titles from Chrome, X, or WhatsApp into the app to check UK streaming availability or add to a local watchlist.
* **Dynamic Notification Badges:** Rich notifications with inline actions ("Launch iPlayer", "Snooze 15m") when a saved broadcast or sports event is about to air.


