# Wavve

A native Android (Kotlin + Jetpack Compose) music app: online streaming via the
Piped API, playlists, live radio, albums, and an auto-updating podcast feed.

## Opening the project

1. Open this folder (`Wavve/`) in Android Studio (Koala or newer recommended).
2. Let Android Studio generate the Gradle wrapper on first sync (File ▸ Sync
   Project with Gradle Files) — the wrapper jar isn't checked in since this was
   built without network access to download it.
3. Build & run on a device/emulator running API 26+.

## What's implemented

- **Songs** — search via the Piped API (a privacy-friendly YouTube front-end),
  cached locally in Room, add to playlists, tap to stream.
- **Playlists** — create, add/remove songs, play as a queue.
- **Live Radio** — KiSS 92.5 Toronto, streamed via ExoPlayer/HLS.
- **Albums** — browse albums built from your library, track lists.
- **Podcasts** — Roz & Mocha's full episode catalog, pulled live from their
  RSS feed. A WorkManager job checks that same feed every 30 minutes in the
  background and fires a notification when a new episode drops — nothing to
  maintain by hand.
- **Player** — play/pause, next/previous, repeat (off/all/one), shuffle, a
  seeker showing elapsed and remaining time, and a 10-band equalizer (Android's
  built-in `Equalizer` effect, attached to ExoPlayer's audio session so it
  survives track changes) with built-in presets (Flat, Bass Boost, Treble
  Boost, Vocal, Electronic) plus save-your-own presets.
- **Mini player** — docked above the bottom nav: artwork, play/pause,
  next/previous, and a seek progress bar; tap to expand to the full player.

## Two things worth double-checking before you rely on them

1. **KiSS 92.5's stream URL** (`Constants.KISS_925_STREAM_URL`) — Rogers
   stations are generally served via their Leanstream infrastructure, and
   that's what's wired in, but I couldn't verify it by actually hitting the
   stream from this environment. If live radio 404s, open kiss925.com's web
   player, check the network tab for the actual `.m3u8`/`.aac` URL, and drop
   it in `Constants.kt` — nothing else needs to change.
2. **Piped instance** (`Constants.PIPED_BASE_URL`) — public Piped instances
   come and go. A couple of fallbacks are listed right above it in
   `Constants.kt` if the default one stops responding.

The Roz & Mocha RSS feed (`https://feeds.simplecast.com/v_BVbu6v`) was
verified directly, so that one should just work.

## Architecture

- **Playback**: single ExoPlayer instance inside a `MediaSessionService`
  (`PlaybackService`), giving lock-screen/notification/Bluetooth controls for
  free. `PlayerManager` wraps a `MediaController` connection and republishes
  state as `StateFlow`s; `PlayerViewModel` is the one shared instance every
  screen and the mini/full player observe.
- **Data**: Retrofit (Piped), OkHttp + a small hand-rolled RSS parser
  (podcasts), Room (songs/playlists/albums/EQ presets/podcast-seen-state).
- **DI**: Hilt throughout.
- **UI**: Jetpack Compose, Navigation Compose, Material 3.

## Known gaps / next steps

- Album creation UI isn't wired up yet (the data layer supports it — songs
  just need a "create/add to album" flow mirroring the playlist one).
- No offline caching of resolved stream URLs (Piped URLs expire; songs are
  re-resolved each time they're played, which is correct but means no true
  offline playback yet).
- Android Auto isn't wired into this rebuild (an earlier iteration of Wavve
  had this via the Car App library — worth re-adding if you want it back).
