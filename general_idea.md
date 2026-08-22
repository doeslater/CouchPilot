Here is an architecture and feature concept for a local-first, privacy-focused UK TV recommendation app.

**Free API & Tech Stack**

* **Data Sources (100% Free):**
* **TVmaze API:** Free, no-key required. Supports schedule filtering specifically for UK broadcasts (e.g., BBC, ITV, Channel 4, Sky).
* **TMDB API (The Movie Database):** Free non-commercial API key. Used for pulling trending titles, deep metadata, posters, and UK "watch providers" (iPlayer, Netflix UK, etc.).
* **Watchmode API:** Provides granular UK streaming availability (formats, pricing, and direct links) for any TV show or movie.


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


**6. load android api skills.
**7.  git repo will be public. API keys will be sceret. ai agent will nut commit to git.  

