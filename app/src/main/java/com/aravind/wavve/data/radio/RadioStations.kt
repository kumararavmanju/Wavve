package com.aravind.wavve.data.radio

import com.aravind.wavve.util.Constants

data class RadioStation(
    val id: String,
    val name: String,
    val streamUrl: String,
    val artworkUrl: String?,
    val subtitle: String
)

/** Only one station requested for v1 — Kiss 92.5 Toronto — but the screen is list-based
 *  so adding more later is just adding another entry here. */
object RadioStations {
    val kiss925 = RadioStation(
        id = "kiss925",
        name = Constants.KISS_925_NAME,
        streamUrl = Constants.KISS_925_STREAM_URL,
        artworkUrl = Constants.KISS_925_ARTWORK_URL,
        subtitle = "Toronto's #1 Hit Music Station"
    )

    val all = listOf(kiss925)
}
