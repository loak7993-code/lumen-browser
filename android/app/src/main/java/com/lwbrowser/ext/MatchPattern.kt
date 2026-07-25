package com.lwbrowser.ext

data class MatchPattern(private val pattern: String) {

    private val scheme: String
    private val host: String
    private val path: String

    init {
        val parts = pattern.split("://", limit = 2)
        if (parts.size != 2) throw IllegalArgumentException("Invalid match pattern: $pattern")
        scheme = parts[0]
        val hostPath = parts[1].split("/", limit = 2)
        host = hostPath[0]
        path = "/" + (hostPath.getOrNull(1) ?: "")
    }

    fun matches(url: String): Boolean {
        val u = try { android.net.Uri.parse(url) } catch (e: Exception) { return false }
        val urlScheme = u.scheme ?: return false
        val urlHost = u.host ?: ""
        val urlPath = u.path ?: "/"

        if (scheme != "*" && urlScheme != scheme) return false
        if (urlScheme == "file" && scheme != "file") return false

        if (host == "*") return true
        if (host.startsWith("*.")) {
            val base = host.substring(2)
            if (urlHost == base) return true
            if (urlHost.endsWith(".$base")) return true
            return false
        }
        if (urlHost != host) return false

        return matchPath(path, urlPath)
    }

    private fun matchPath(patternPath: String, urlPath: String): Boolean {
        if (patternPath == "/" || patternPath.isEmpty()) return true
        val regex = patternPath
            .replace(".", "\\.")
            .replace("*", ".*")
            .toRegex(RegexOption.IGNORE_CASE)
        return regex.matches(urlPath) || urlPath.startsWith(patternPath)
    }
}
