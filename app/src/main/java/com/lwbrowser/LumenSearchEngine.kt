package com.lwbrowser

import android.net.Uri
import android.util.JsonWriter
import android.webkit.JavascriptInterface
import java.io.StringWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import kotlin.concurrent.thread

class LumenSearchEngine(private val callback: (String, String) -> Unit) {

    enum class Type { WEB, IMAGES, VIDEOS, NEWS }

    @JavascriptInterface
    fun fetchResults(query: String, type: String) {
        thread {
            val t = runCatching { Type.valueOf(type.uppercase()) }.getOrDefault(Type.WEB)
            val json = try {
                when (t) {
                    Type.WEB -> fetchWeb(query)
                    Type.IMAGES -> fetchImages(query)
                    Type.VIDEOS -> fetchVideos(query)
                    Type.NEWS -> fetchNews(query)
                }
            } catch (e: Exception) {
                errorJson(query, e.message ?: "Network error")
            }
            callback("results", json)
        }
    }

    @JavascriptInterface
    fun fetchSuggestions(query: String) {
        thread {
            val json = try {
                val raw = fetchText("https://duckduckgo.com/ac/?q=" + Uri.encode(query) + "&type=list")
                val list = parseSuggestionList(raw)
                val sw = StringWriter()
                JsonWriter(sw).use { w ->
                    w.beginObject()
                    w.name("query").value(query)
                    w.name("suggestions")
                    w.beginArray()
                    for (s in list) w.value(s)
                    w.endArray()
                    w.endObject()
                }
                sw.toString()
            } catch (e: Exception) {
                """{"query":"$query","suggestions":[]}"""
            }
            callback("suggestions", json)
        }
    }

    @JavascriptInterface
    fun getProtectionStats(): String {
        val (ads, trackers, cosmetic) = AdBlocker.stats()
        return "Protected · ${ads + trackers} blocked"
    }

    private data class Result(
        val title: String,
        val url: String,
        val display: String,
        val snippet: String,
        val source: String,
        val thumbnail: String = "",
        val score: Double = 0.0
    )

    private fun fetchWeb(query: String): String {
        val ddg = mutableListOf<Result>()
        val bing = mutableListOf<Result>()
        val wiki = mutableListOf<Result>()
        val related = mutableListOf<String>()

        val t1 = thread { ddg.addAll(fetchDuckDuckGo(query)); related.addAll(fetchRelated(query)) }
        val t2 = thread { bing.addAll(fetchBing(query)) }
        val t3 = thread { wiki.addAll(fetchWikipedia(query)) }
        t1.join(9000); t2.join(9000); t3.join(9000)

        val all = mutableListOf<Result>()
        all += ddg; all += bing; all += wiki
        val ranked = aggregate(all)

        return buildWebJson(query, ranked, related)
    }

    private fun fetchImages(query: String): String {
        val results = mutableListOf<Result>()
        try {
            val html = fetchText("https://www.bing.com/images/search?q=" + Uri.encode(query) + "&form=HDRSC2&first=1&count=30")
            val pattern = Regex("""m="(\{[^"]*\})"""", RegexOption.DOT_MATCHES_ALL)
            for (m in pattern.findAll(html)) {
                val json = unescapeHtml(m.groupValues[1])
                val imgUrl = extractJsonField(json, "murl") ?: continue
                val thumb = extractJsonField(json, "turl") ?: ""
                val title = extractJsonField(json, "t") ?: query
                if (imgUrl.startsWith("http")) {
                    results.add(Result(title, imgUrl, imgUrl, "", "bing", thumb))
                }
            }
        } catch (e: Exception) {}
        return buildMediaJson(query, results, "images")
    }

    private fun fetchVideos(query: String): String {
        val results = mutableListOf<Result>()
        try {
            val html = fetchText("https://www.bing.com/videos/search?q=" + Uri.encode(query) + "&form=HDRSC3&first=1&count=20")
            val pattern = Regex("""m="(\{[^"]*\})"""", RegexOption.DOT_MATCHES_ALL)
            for (m in pattern.findAll(html)) {
                val json = unescapeHtml(m.groupValues[1])
                val vidUrl = extractJsonField(json, "murl") ?: continue
                val thumb = extractJsonField(json, "smturl") ?: extractJsonField(json, "turl") ?: ""
                val title = extractJsonField(json, "t") ?: query
                if (vidUrl.startsWith("http")) {
                    results.add(Result(title, vidUrl, vidUrl, "", "bing", thumb))
                }
            }
        } catch (e: Exception) {}
        return buildMediaJson(query, results, "videos")
    }

    private fun fetchNews(query: String): String {
        val results = mutableListOf<Result>()
        try {
            val json = fetchText("https://news.google.com/rss/search?q=" + Uri.encode(query) + "&hl=en-US&gl=US&ceid=US:en")
            val itemPattern = Regex("""<item>.*?<title>(.*?)</title>.*?<link>(.*?)</link>.*?<pubDate>(.*?)</pubDate>.*?<source[^>]*>(.*?)</source>.*?</item>""", RegexOption.DOT_MATCHES_ALL)
            for (m in itemPattern.findAll(json)) {
                val title = stripTags(m.groupValues[1])
                val url = m.groupValues[2]
                val date = m.groupValues[3]
                val source = stripTags(m.groupValues[4]).ifEmpty { "news" }
                results.add(Result(title, url, source, date, "news"))
            }
        } catch (e: Exception) {}
        return buildMediaJson(query, results, "news")
    }

    private fun fetchDuckDuckGo(q: String): List<Result> {
        return try {
            val html = fetchText("https://html.duckduckgo.com/html/?q=" + Uri.encode(q))
            parseDuckDuckGo(html)
        } catch (e: Exception) { emptyList() }
    }

    private fun fetchRelated(q: String): List<String> {
        return try {
            val html = fetchText("https://html.duckduckgo.com/html/?q=" + Uri.encode(q))
            val pattern = Regex("""<a[^>]*class="[^"]*related-search[^"]*"[^>]*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL)
            pattern.findAll(html).map { stripTags(it.groupValues[1]).trim() }.filter { it.isNotEmpty() }.toList()
        } catch (e: Exception) { emptyList() }
    }

    private fun fetchBing(q: String): List<Result> {
        return try {
            val html = fetchText("https://www.bing.com/search?q=" + Uri.encode(q) + "&count=30&setlang=en")
            parseBing(html)
        } catch (e: Exception) { emptyList() }
    }

    private fun fetchWikipedia(q: String): List<Result> {
        return try {
            val json = fetchText("https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=" +
                Uri.encode(q) + "&format=json&srlimit=8&utf8=1")
            parseWikipedia(json)
        } catch (e: Exception) { emptyList() }
    }

    private fun fetchText(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Lumen/1.0")
            setRequestProperty("Accept", "text/html,application/xhtml+xml,application/json,application/rss+xml")
            setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            connectTimeout = 8000
            readTimeout = 8000
            instanceFollowRedirects = true
        }
        return conn.inputStream.bufferedReader().use { it.readText() }
    }

    private fun parseDuckDuckGo(html: String): List<Result> {
        val results = mutableListOf<Result>()
        val titlePattern = Regex("""<a[^>]*class="[^"]*result__a[^"]*"[^>]*href="([^"]*)"[^>]*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL)
        val snippetPattern = Regex("""<a[^>]*class="[^"]*result__snippet[^"]*"[^>]*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL)
        val titles = titlePattern.findAll(html).toList()
        val snippets = snippetPattern.findAll(html).toList()
        val count = minOf(titles.size, snippets.size)
        for (i in 0 until count) {
            val rawHref = titles[i].groupValues[1]
            val actualUrl = decodeDdgUrl(rawHref)
            val title = stripTags(titles[i].groupValues[2]).trim()
            val snippet = stripTags(snippets.getOrNull(i)?.groupValues?.get(1) ?: "").trim()
            if (title.isNotEmpty() && actualUrl.startsWith("http")) {
                results.add(Result(title, actualUrl, hostOf(actualUrl), snippet, "ddg"))
            }
        }
        return results
    }

    private fun parseBing(html: String): List<Result> {
        val results = mutableListOf<Result>()
        val liPattern = Regex("""<li class="b_algo">(.*?)</li>""", RegexOption.DOT_MATCHES_ALL)
        for (li in liPattern.findAll(html)) {
            val block = li.groupValues[1]
            val linkPattern = Regex("""<a[^>]*href="(https?://[^"]*)"[^>]*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL)
            val link = linkPattern.find(block) ?: continue
            val url = link.groupValues[1]
            val title = stripTags(link.groupValues[2]).trim()
            val snippet = stripTags(Regex("""<p[^>]*>(.*?)</p>""", RegexOption.DOT_MATCHES_ALL).find(block)?.groupValues?.get(1) ?: "").trim()
            if (title.isNotEmpty() && url.startsWith("http")) {
                results.add(Result(title, url, hostOf(url), snippet, "bing"))
            }
        }
        return results
    }

    private fun parseWikipedia(json: String): List<Result> {
        val results = mutableListOf<Result>()
        val itemPattern = Regex("""\{"title":"([^"]+)","pageid":\d+,"snippet":"(.*?)"\}""", RegexOption.DOT_MATCHES_ALL)
        for (item in itemPattern.findAll(json)) {
            val title = unescape(item.groupValues[1])
            val snippet = stripTags(unescape(item.groupValues[2])).trim()
            val url = "https://en.wikipedia.org/wiki/" + Uri.encode(title.replace(" ", "_"))
            results.add(Result(title, url, "wikipedia.org", snippet, "wiki"))
        }
        return results
    }

    private fun parseSuggestionList(raw: String): List<String> {
        val list = mutableListOf<String>()
        val pattern = Regex("""\["([^"]+)"\]""")
        for (m in pattern.findAll(raw)) {
            if (m.groupValues[1].isNotEmpty()) list.add(m.groupValues[1])
        }
        if (list.isEmpty()) {
            val alt = Regex(""""([^"]+)"""")
            for (m in alt.findAll(raw)) {
                val v = m.groupValues[1]
                if (v.isNotEmpty() && v != "query" && v.length < 100) list.add(v)
            }
        }
        return list.distinct().take(8)
    }

    private fun aggregate(all: List<Result>): List<Result> {
        val byUrl = LinkedHashMap<String, Result>()
        val snippets = mutableMapOf<String, String>()
        for (r in all) {
            val key = normalizeUrl(r.url)
            val existing = byUrl[key]
            if (existing == null) {
                byUrl[key] = r.copy(url = key, score = 0.0)
                snippets[key] = r.snippet
            } else {
                byUrl[key] = existing.copy(score = existing.score + 0.5)
                if (snippets[key].isNullOrEmpty() && r.snippet.isNotEmpty()) snippets[key] = r.snippet
            }
        }
        val ranked = byUrl.values.map { r ->
            val s = r.score + sourceWeight(r.source) +
                (if (r.url.contains("wikipedia.org")) 0.3 else 0.0) +
                (if (r.url.startsWith("https://")) 0.1 else 0.0) +
                (if (!snippets[r.url].isNullOrEmpty()) 0.1 else 0.0)
            r.copy(score = s, snippet = snippets[r.url] ?: r.snippet)
        }
        return ranked.sortedByDescending { it.score }
    }

    private fun sourceWeight(source: String): Double = when (source) {
        "ddg" -> 1.0; "bing" -> 0.9; "wiki" -> 0.7; "news" -> 0.6; else -> 0.5
    }

    private fun normalizeUrl(u: String): String {
        var n = u
        if (n.startsWith("http://")) n = "https://" + n.substring(7)
        n = n.replace("https://www.", "https://")
        if (n.endsWith("/")) n = n.dropLast(1)
        n = n.substringBefore("#")
        return n.lowercase()
    }

    private fun decodeDdgUrl(href: String): String {
        val uddg = Uri.parse(href).getQueryParameter("uddg")
        return if (uddg != null) URLDecoder.decode(uddg, "UTF-8") else href
    }

    private fun hostOf(url: String): String = runCatching { Uri.parse(url).host ?: url }.getOrDefault(url)

    private fun stripTags(html: String): String {
        return html
            .replace(Regex("<[^>]*>"), "")
            .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
            .replace("&quot;", "\"").replace("&#x27;", "'").replace("&#39;", "'").replace("&nbsp;", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun unescapeHtml(s: String): String {
        return s.replace("&quot;", "\"").replace("&#x27;", "'").replace("&#39;", "'").replace("&amp;", "&")
    }

    private fun extractJsonField(json: String, field: String): String? {
        val pattern = Regex(""""$field"\s*:\s*"((?:[^"\\]|\\.)*)"""")
        val match = pattern.find(json) ?: return null
        return match.groupValues[1]
            .replace("\\/", "/")
            .replace("\\\"", "\"")
    }

    private fun unescape(s: String): String = s
        .replace("\\\"", "\"").replace("\\\\", "\\").replace("\\n", " ").replace("\\/", "/").replace("\\u0026", "&")

    private fun buildWebJson(query: String, results: List<Result>, related: List<String>): String {
        val sw = StringWriter()
        JsonWriter(sw).use { w ->
            w.beginObject()
            w.name("query").value(query)
            w.name("error").nullValue()
            w.name("results"); w.beginArray()
            for (r in results) { writeResult(w, r) }
            w.endArray()
            w.name("related"); w.beginArray()
            for (s in related) w.value(s)
            w.endArray()
            w.endObject()
        }
        return sw.toString()
    }

    private fun buildMediaJson(query: String, results: List<Result>, type: String): String {
        val sw = StringWriter()
        JsonWriter(sw).use { w ->
            w.beginObject()
            w.name("query").value(query)
            w.name("type").value(type)
            w.name("error").nullValue()
            w.name("results"); w.beginArray()
            for (r in results) { writeResult(w, r) }
            w.endArray()
            w.name("related"); w.beginArray(); w.endArray()
            w.endObject()
        }
        return sw.toString()
    }

    private fun writeResult(w: JsonWriter, r: Result) {
        w.beginObject()
        w.name("title").value(r.title)
        w.name("url").value(r.url)
        w.name("display").value(r.display)
        w.name("snippet").value(r.snippet)
        w.name("source").value(r.source)
        w.name("thumbnail").value(r.thumbnail)
        w.name("favicon").value("https://icons.duckduckgo.com/ip3/" + hostOf(r.url) + ".ico")
        w.endObject()
    }

    private fun errorJson(query: String, error: String): String {
        val sw = StringWriter()
        JsonWriter(sw).use { w ->
            w.beginObject()
            w.name("query").value(query)
            w.name("error").value(error)
            w.name("results"); w.beginArray(); w.endArray()
            w.name("related"); w.beginArray(); w.endArray()
            w.endObject()
        }
        return sw.toString()
    }
}
