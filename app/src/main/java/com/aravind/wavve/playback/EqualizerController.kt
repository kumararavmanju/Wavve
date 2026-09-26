package com.aravind.wavve.playback

import android.media.audiofx.Equalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class EqBandInfo(val index: Short, val centerFreqHz: Int, val minMb: Short, val maxMb: Short)

/**
 * Thin wrapper around android.media.audiofx.Equalizer, attached to ExoPlayer's audio
 * session. Survives player/track changes since it's keyed to the session id, not the
 * media item — so gains persist as songs change, exactly like a hardware EQ would.
 */
@Singleton
class EqualizerController @Inject constructor() {

    private var equalizer: Equalizer? = null

    private val _enabled = MutableStateFlow(true)
    val enabled: StateFlow<Boolean> = _enabled

    private val _bands = MutableStateFlow<List<EqBandInfo>>(emptyList())
    val bands: StateFlow<List<EqBandInfo>> = _bands

    private val _gains = MutableStateFlow<Map<Short, Short>>(emptyMap())
    val gains: StateFlow<Map<Short, Short>> = _gains

    fun attach(audioSessionId: Int) {
        release()
        val eq = runCatching { Equalizer(0, audioSessionId) }.getOrNull() ?: return
        equalizer = eq
        eq.enabled = _enabled.value

        val bandCount = eq.numberOfBands
        val bandInfos = (0 until bandCount).map { i ->
            val idx = i.toShort()
            val range = eq.getBandLevelRange()
            EqBandInfo(
                index = idx,
                centerFreqHz = eq.getCenterFreq(idx) / 1000,
                minMb = range[0],
                maxMb = range[1]
            )
        }
        _bands.value = bandInfos
        _gains.value = bandInfos.associate { it.index to eq.getBandLevel(it.index) }
    }

    fun setEnabled(on: Boolean) {
        _enabled.value = on
        equalizer?.enabled = on
    }

    fun setBandLevel(band: Short, levelMb: Short) {
        equalizer?.setBandLevel(band, levelMb)
        _gains.value = _gains.value.toMutableMap().apply { put(band, levelMb) }
    }

    /** Applies a saved preset (list of millibel gains, one per band, in band order). */
    fun applyPreset(gainsMb: List<Int>) {
        val bandList = _bands.value
        gainsMb.forEachIndexed { i, gain ->
            bandList.getOrNull(i)?.let { setBandLevel(it.index, gain.toShort()) }
        }
    }

    fun currentGainsAsList(): List<Int> =
        _bands.value.map { (_gains.value[it.index] ?: 0).toInt() }

    fun release() {
        equalizer?.release()
        equalizer = null
    }
}

/** A handful of familiar starting points, applied as offsets over a flat 5â€“10 band EQ. */
object BuiltInEqPresets {
    val FLAT = "Flat" to List(10) { 0 }
    val BASS_BOOST = "Bass Boost" to listOf(600, 500, 400, 200, 0, 0, 0, 0, 0, 0)
    val TREBLE_BOOST = "Treble Boost" to listOf(0, 0, 0, 0, 0, 100, 300, 500, 600, 600)
    val VOCAL = "Vocal" to listOf(-200, -100, 0, 300, 500, 500, 300, 0, -100, -200)
    val ELECTRONIC = "Electronic" to listOf(400, 300, 0, -200, -300, 0, 200, 300, 400, 500)

    val all = listOf(FLAT, BASS_BOOST, TREBLE_BOOST, VOCAL, ELECTRONIC)
}
