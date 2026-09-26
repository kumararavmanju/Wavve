package com.aravind.wavve.data.local

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Scans on-device audio via MediaStore. Doesn't filter by IS_MUSIC, since
 * WhatsApp voice notes and call recordings are usually flagged as non-music
 * by the system anyway — instead classifies every audio file by folder path
 * so the caller (LocalMusicViewModel) can decide, per the user's refresh-time
 * choice, whether to include WhatsApp audio and/or recordings alongside
 * regular music.
 */
class LocalMusicScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val RECORDING_PATH_KEYWORDS = listOf(
            "call recording", "callrecording", "voice recorder",
            "voicerecorder", "/recordings/", "/sounds/record"
        )
    }

    suspend fun scan(includeWhatsApp: Boolean, includeRecordings: Boolean): List<LocalAudioTrack> =
        withContext(Dispatchers.IO) {
            queryMediaStore(selection = null, selectionArgs = null)
                .filter { track ->
                    if (track.isWhatsApp && !includeWhatsApp) return@filter false
                    if (track.isRecording && !includeRecordings) return@filter false
                    true
                }
        }

    /** Step 8: used by the search chain to check whether a song already exists
     *  on-device before falling back to YouTube. Matches on title OR artist
     *  containing the query text, always excluding WhatsApp/recordings — a
     *  search match should be actual music, not a stray voice note. */
    suspend fun searchLocal(query: String): List<LocalAudioTrack> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val likePattern = "%$query%"
        queryMediaStore(
            selection = "(${MediaStore.Audio.Media.TITLE} LIKE ? OR ${MediaStore.Audio.Media.ARTIST} LIKE ?)",
            selectionArgs = arrayOf(likePattern, likePattern)
        ).filter { !it.isWhatsApp && !it.isRecording }
    }

    private fun queryMediaStore(selection: String?, selectionArgs: Array<String>?): List<LocalAudioTrack> {
        val tracks = mutableListOf<LocalAudioTrack>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
        )

        context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val path = cursor.getString(dataCol).orEmpty()

                val isWhatsApp = path.contains("whatsapp", ignoreCase = true)
                val isRecording = RECORDING_PATH_KEYWORDS.any { path.contains(it, ignoreCase = true) }
                val contentUri = ContentUris.withAppendedId(collection, id).toString()

                tracks += LocalAudioTrack(
                    id = id,
                    title = cursor.getString(titleCol) ?: "Unknown title",
                    artist = cursor.getString(artistCol) ?: "Unknown artist",
                    album = cursor.getString(albumCol),
                    durationMs = cursor.getLong(durationCol),
                    contentUri = contentUri,
                    isWhatsApp = isWhatsApp,
                    isRecording = isRecording
                )
            }
        }

        return tracks
    }
}