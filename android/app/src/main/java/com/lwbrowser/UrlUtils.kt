package com.lwbrowser

import android.util.Patterns
import java.net.URLEncoder

object UrlUtils {
    fun normalize(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""
        if (trimmed.startsWith("about:")) return trimmed
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
        if (trimmed.startsWith("file://") || trimmed.startsWith("data:")) return trimmed
        if (trimmed.contains(" ")) {
            return searchEngine(trimmed)
        }
        if (trimmed.contains(".") && !trimmed.contains(' ')) {
            val pattern = Patterns.WEB_URL.matcher(trimmed)
            if (pattern.matches()) return "https://$trimmed"
            return "https://$trimmed"
        }
        return searchEngine(trimmed)
    }

    fun searchEngine(query: String): String {
        return "file:///android_asset/search.html#q=" + URLEncoder.encode(query, "UTF-8").replace("+", "%20")
    }

    fun host(url: String?): String {
        if (url.isNullOrEmpty()) return ""
        return runCatching {
            android.net.Uri.parse(url).host ?: url
        }.getOrDefault(url)
    }

    fun isSecure(url: String?): Boolean {
        if (url.isNullOrEmpty()) return false
        return url.startsWith("https://") || url.startsWith("about:") || url.startsWith("file://")
    }
}
