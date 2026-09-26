package com.aravind.wavve.data.piped

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PipedApi {

    /** Search for songs/albums by free-text query (song name, artist, "artist - album", etc). */
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("filter") filter: String = "music_songs"
    ): PipedSearchResponse

    /** Resolve a video id to its playable audio stream URLs. */
    @GET("streams/{videoId}")
    suspend fun getStreams(@Path("videoId") videoId: String): PipedStreamsResponse
}
