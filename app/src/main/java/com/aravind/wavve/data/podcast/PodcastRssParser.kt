package com.aravind.wavve.data.podcast

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Minimal, dependency-free RSS 2.0 + iTunes-namespace podcast feed parser.
 * Good enough for Simplecast/Omny/Libsyn-style feeds (covers Roz & Mocha's feed shape).
 */
object PodcastRssParser {

    private val rfc822Formats = listOf(
        "EEE, dd MMM yyyy HH:mm:ss Z",
        "EEE, dd MMM yyyy HH:mm:ss zzz",
        "dd MMM yyyy HH:mm:ss Z"
    ).map { SimpleDateFormat(it, Locale.US) }

    fun parse(input: InputStream): PodcastFeed {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(input, null)

        var feedTitle = ""
        var feedImage: String? = null
        val episodes = mutableListOf<PodcastEpisode>()

        var inItem = false
        var curTitle = ""
        var curDesc = ""
        var curAudioUrl = ""
        var curGuid = ""
        var curImage: String? = null
        var curPubDate = ""
        var curDurationSec = 0L

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name.substringAfter(":")) {
                        "item" -> {
                            inItem = true
                            curTitle = ""; curDesc = ""; curAudioUrl = ""; curGuid = ""
                            curImage = null; curPubDate = ""; curDurationSec = 0L
                        }
                        "title" -> {
                            val text = readText(parser)
                            if (inItem) curTitle = text else if (feedTitle.isBlank()) feedTitle = text
                        }
                        "description" -> if (inItem) curDesc = readText(parser)
                        "enclosure" -> if (inItem) {
                            curAudioUrl = parser.getAttributeValue(null, "url") ?: ""
                        }
                        "guid" -> if (inItem) curGuid = readText(parser)
                        "pubDate" -> if (inItem) curPubDate = readText(parser)
                        "image" -> {
                            // <itunes:image href="..."/> is self-closing with an attribute;
                            // channel-level <image><url>...</url></image> has a child.
                            val href = parser.getAttributeValue(null, "href")
                            if (href != null) {
                                if (inItem) curImage = href else feedImage = href
                            }
                        }
                        "url" -> if (!inItem && feedImage == null) feedImage = readText(parser)
                        "duration" -> if (inItem) curDurationSec = parseDuration(readText(parser))
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name.substringAfter(":") == "item") {
                        inItem = false
                        if (curAudioUrl.isNotBlank()) {
                            episodes += PodcastEpisode(
                                guid = curGuid.ifBlank { curAudioUrl },
                                title = curTitle,
                                description = curDesc,
                                audioUrl = curAudioUrl,
                                imageUrl = curImage ?: feedImage,
                                pubDateMs = parseRfc822(curPubDate),
                                durationSec = curDurationSec
                            )
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return PodcastFeed(title = feedTitle, imageUrl = feedImage, episodes = episodes)
    }

    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text ?: ""
            parser.nextTag()
        }
        return result.trim()
    }

    private fun parseDuration(raw: String): Long {
        if (raw.isBlank()) return 0
        if (raw.contains(":")) {
            val parts = raw.split(":").map { it.toLongOrNull() ?: 0L }
            return when (parts.size) {
                3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
                2 -> parts[0] * 60 + parts[1]
                else -> parts.getOrElse(0) { 0L }
            }
        }
        return raw.toLongOrNull() ?: 0
    }

    private fun parseRfc822(raw: String): Long {
        if (raw.isBlank()) return 0
        for (fmt in rfc822Formats) {
            try {
                return fmt.parse(raw)?.time ?: continue
            } catch (_: Exception) { /* try next format */ }
        }
        return 0
    }
}
