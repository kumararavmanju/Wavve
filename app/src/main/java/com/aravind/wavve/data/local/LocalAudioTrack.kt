package com.aravind.wavve.data.local

/** A song scanned from on-device storage via MediaStore. */
data class LocalAudioTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String?,
    val durationMs: Long,
    val contentUri: String,
    val isWhatsApp: Boolean,
    val isRecording: Boolean
)