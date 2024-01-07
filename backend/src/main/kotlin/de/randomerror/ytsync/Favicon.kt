package de.randomerror.ytsync

import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlHandler
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlParser
import java.net.MalformedURLException
import java.net.URL
import kotlin.time.Duration.Companion.seconds

private val FAVICON_CACHE = mutableMapOf<String, String>()

fun getInitialFavicon(query: String, youtubeId: String?): String? {
    val isYoutube = youtubeId != null || !query.matches(Regex("^(ftp|https?)://.*"))
    val url = if (isYoutube) {
        URL("https://www.youtube.com/")
    } else {
        try {
            URL(query)
        } catch (e: MalformedURLException) {
            return null
        }
    }

    return if (url.authority in FAVICON_CACHE) {
        FAVICON_CACHE[url.authority]
    } else {
        url.toURI().resolve("/favicon.ico").toString()
    }
}
fun getFavicon(query: String, videoUrl: String): String? {
    val url = try {
        URL(query)
    } catch (e: MalformedURLException) {
        URL(videoUrl)
    }
    if (url.authority in FAVICON_CACHE) {
        return FAVICON_CACHE[url.authority]
    }
    val favicon = fetchFavicon(url)
    FAVICON_CACHE[url.authority] = favicon
    return favicon
}

private fun fetchFavicon(url: URL): String {
    try {
        val icons = mutableListOf<Pair<String, FaviconSizes>>()
        val handler = KsoupHtmlHandler.Builder()
            .onOpenTag { name, attributes, _ ->
                val href = attributes["href"]
                val rel = attributes["rel"]
                if (name == "link" && (rel == "icon" || rel == "shortcut icon") && href != null) {
                    val icon = url.toURI().resolve(href).toString()
                    val type = attributes["type"]
                    val sizesSpec = attributes["sizes"]
                    val sizes = if (type != null && type.startsWith("image/svg") || sizesSpec == "any" && !href.endsWith(".ico")) {
                        FaviconSizes.Any
                    } else if (sizesSpec == null || sizesSpec == "any" && href.endsWith(".ico")) {
                        FaviconSizes.Unknown
                    } else {
                        val sizes = sizesSpec.split(' ')
                            .onEach { println("found size: $it") }
                            .map { it.split('x', ignoreCase = true)[0] }
                            .map { it.toInt() }
                        FaviconSizes.Sizes(sizes)
                    }
                    icons.add(icon to sizes)
                }
            }
            .build()
        val conn = url.openConnection()
        conn.connectTimeout = 5.seconds.inWholeMilliseconds.toInt()
        conn.readTimeout = 5.seconds.inWholeMilliseconds.toInt()
        val bytes = conn.getInputStream().use { it.readNBytes(1024 * 64) }
        val parser = KsoupHtmlParser(handler)
        parser.write(String(bytes))
        parser.end()
        val favicon = icons.maxBy { it.second }.first
        FAVICON_CACHE[url.authority] = favicon
        return favicon
    } catch (e: Exception) {
        return url.toURI().resolve("/favicon.ico").toString()
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

