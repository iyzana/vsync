package de.randomerror.ytsync

import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlHandler
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlParser
import java.net.URI
import java.net.URISyntaxException
import kotlin.time.Duration.Companion.seconds

private const val HTML_MAX_BYTES = 64 * 1024 // 64KiB

private val FAVICON_CACHE = mutableMapOf<String, String>()

fun getInitialFavicon(query: String, youtubeId: String?): String? {
    val isYoutube = youtubeId != null || !query.matches(Regex("^(ftp|https?)://.*"))
    val uri = if (isYoutube) {
        URI("https://www.youtube.com/")
    } else {
        try {
            URI(query)
        } catch (e: URISyntaxException) {
            return null
        }
    }

    return if (uri.authority in FAVICON_CACHE) {
        FAVICON_CACHE[uri.authority]
    } else {
        uri.resolve("/favicon.ico").toString()
    }
}
fun getFavicon(query: String, videoUrl: String): String? {
    val uri = try {
        URI(query)
    } catch (e: URISyntaxException) {
        URI(videoUrl)
    }
    if (uri.authority in FAVICON_CACHE) {
        return FAVICON_CACHE[uri.authority]
    }
    val favicon = fetchFavicon(uri)
    FAVICON_CACHE[uri.authority] = favicon
    return favicon
}

private fun fetchFavicon(uri: URI): String {
    try {
        val icons = mutableListOf<Pair<String, FaviconSizes>>()
        val handler = KsoupHtmlHandler.Builder()
            .onOpenTag { name, attributes, _ ->
                if (name != "link") {
                    return@onOpenTag
                }
                val href = attributes["href"]
                val rel = attributes["rel"]
                if (href == null || rel != "icon" && rel != "shortcut icon") {
                    return@onOpenTag
                }
                val icon = uri.resolve(href).toString()
                val type = attributes["type"]
                val sizesSpec = attributes["sizes"]
                val sizes = when {
                    type != null && type.startsWith("image/svg") -> FaviconSizes.Any
                    sizesSpec == "any" && !href.endsWith(".ico") -> FaviconSizes.Any
                    sizesSpec == "any" -> FaviconSizes.Unknown
                    sizesSpec == null -> FaviconSizes.Unknown
                    else -> {
                        val sizes = sizesSpec.split(' ')
                            .map { it.split('x', ignoreCase = true)[0] }
                            .map { it.toInt() }
                        FaviconSizes.Sizes(sizes)
                    }
                }
                icons.add(icon to sizes)
            }
            .build()
        val conn = uri.toURL().openConnection()
        conn.connectTimeout = 5.seconds.inWholeMilliseconds.toInt()
        conn.readTimeout = 5.seconds.inWholeMilliseconds.toInt()
        val bytes = conn.getInputStream().use { it.readNBytes(HTML_MAX_BYTES) }
        val parser = KsoupHtmlParser(handler)
        parser.write(String(bytes))
        parser.end()
        val favicon = icons.maxBy { it.second }.first
        FAVICON_CACHE[uri.authority] = favicon
        return favicon
    } catch (_: Exception) {
        return uri.resolve("/favicon.ico").toString()
    }
}

private const val IDEAL_FAVICON_SIZE = 16

sealed class FaviconSizes : Comparable<FaviconSizes> {
    object Unknown : FaviconSizes() {
        override fun compareTo(other: FaviconSizes): Int {
            return -1
        }
    }

    object Any : FaviconSizes() {
        override fun compareTo(other: FaviconSizes): Int {
            return 1
        }
    }

    class Sizes(val sizes: List<Int>) : FaviconSizes() {
        override fun compareTo(other: FaviconSizes): Int {
            if (other !is Sizes) {
                return -other.compareTo(this)
            }
            val best = bestSize()
            val otherBest = other.bestSize()
            return compareSize(best, otherBest)
        }

        fun bestSize(): Int {
            return sizes.maxWith(this::compareSize)
        }

        private fun compareSize(sizeA: Int, sizeB: Int): Int {
            return if (sizeA == IDEAL_FAVICON_SIZE && sizeB != IDEAL_FAVICON_SIZE) {
                1
            } else if (sizeA != IDEAL_FAVICON_SIZE && sizeB == IDEAL_FAVICON_SIZE) {
                -1
            } else if (sizeA < IDEAL_FAVICON_SIZE) {
                if (sizeB > IDEAL_FAVICON_SIZE) {
                    -1
                } else {
                    sizeA.compareTo(sizeB)
                }
            } else {
                if (sizeB < IDEAL_FAVICON_SIZE) {
                    1
                } else {
                    sizeB.compareTo(sizeA)
                }
            }
        }
    }
}

