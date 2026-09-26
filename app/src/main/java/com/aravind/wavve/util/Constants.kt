package com.aravind.wavve.util

object Constants {

    // ---- Piped (privacy-friendly YouTube front-end) API ----
    // Piped has many public instances; they go down or get rate-limited often.
    // PipedInstanceProvider fetches a LIVE list from https://piped-instances.kavin.rocks/
    // at runtime and uses that instead. PIPED_INSTANCES below is only the fallback
    // used if that fetch fails (e.g. no network on first launch) — keep it pruned of
    // known-dead instances, but don't rely on it being fully up to date.
    const val PIPED_BASE_URL = "https://pipedapi.kavin.rocks/"
    const val PIPED_INSTANCES_LIST_URL = "https://piped-instances.kavin.rocks/"

    // Confirmed dead as of 2026-09-22, removed: pipedapi.leptons.xyz (502),
    // api.piped.projectsegfau.lt (shut down). kavin.rocks kept as last-resort
    // fallback even though it Cloudflare-blocks some requests, since it's still
    // the official instance and may work depending on IP/region.
    val PIPED_INSTANCES = listOf(
        "https://pipedapi.tokhmi.xyz",
        "https://pipedapi.rivo.lol",
        "https://api.piped.yt",
        "https://pipedapi.drgns.space",
        "https://api.piped.private.coffee",
        "https://pipedapi.owo.si",
        "https://pipedapi.ducks.party",
        "https://piped-api.codespace.cz",
        "https://pipedapi.kavin.rocks"
    )

    // ---- Live Radio: KiSS 92.5 Toronto (CKIS-FM, Rogers Sports & Media) ----
    const val KISS_925_STREAM_URL = "https://rogers-hls.leanstream.co/rogers/tor925.stream/48k/playlist.m3u8"
    const val KISS_925_NAME = "KiSS 92.5 Toronto"
    const val KISS_925_ARTWORK_URL = "https://www.kiss925.com/wp-content/uploads/2021/09/kiss925-logo.png"

    // ---- Roz & Mocha podcast (The Roz & Mocha Show, Frequency Podcast Network) ----
    const val ROZ_AND_MOCHA_RSS_URL = "https://feeds.simplecast.com/v_BVbu6v"
    const val ROZ_AND_MOCHA_TITLE = "Roz & Mocha"

    // WorkManager tag/name for the periodic new-episode check
    const val PODCAST_REFRESH_WORK_NAME = "podcast_refresh_work"

    // Notification
    const val PLAYBACK_CHANNEL_ID = "wavve_playback_channel"
    const val PODCAST_CHANNEL_ID = "wavve_podcast_channel"
    const val PODCAST_NOTIFICATION_ID = 2001

    // Equalizer
    const val EQ_BAND_COUNT_FALLBACK = 5
}