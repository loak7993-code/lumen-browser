package com.lwbrowser.ext

import org.json.JSONArray
import org.json.JSONObject

data class ContentScript(
    val matches: List<MatchPattern>,
    val excludeMatches: List<MatchPattern>,
    val includeGlobs: List<String>,
    val excludeGlobs: List<String>,
    val js: List<String>,
    val css: List<String>,
    val runAt: String,
    val allFrames: Boolean
)

data class Extension(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val contentScripts: List<ContentScript>,
    val enabled: Boolean = true
) {
    fun scriptsFor(url: String, runAt: String): List<ContentScript> {
        if (!enabled) return emptyList()
        return contentScripts.filter { cs ->
            (cs.runAt.isEmpty() || cs.runAt == runAt) &&
                cs.matches.any { it.matches(url) } &&
                cs.excludeMatches.none { it.matches(url) } &&
                (cs.includeGlobs.isEmpty() || cs.includeGlobs.any { GlobPattern.matches(it, url) }) &&
                (cs.excludeGlobs.isEmpty() || cs.excludeGlobs.none { GlobPattern.matches(it, url) })
        }
    }

    companion object {
        fun fromManifest(id: String, manifestJson: String, enabled: Boolean = true): Extension {
            val manifest = JSONObject(manifestJson)
            val contentScripts = mutableListOf<ContentScript>()

            val csArray = manifest.optJSONArray("content_scripts")
            if (csArray != null) {
                for (i in 0 until csArray.length()) {
                    val cs = csArray.getJSONObject(i)
                    val matches = parsePatterns(cs.optJSONArray("matches"))
                    val excludeMatches = parsePatterns(cs.optJSONArray("exclude_matches"))
                    val includeGlobs = parseStringList(cs.optJSONArray("include_globs"))
                    val excludeGlobs = parseStringList(cs.optJSONArray("exclude_globs"))
                    val js = parseStringList(cs.optJSONArray("js"))
                    val css = parseStringList(cs.optJSONArray("css"))
                    val runAt = cs.optString("run_at", "document_idle")
                    val allFrames = cs.optBoolean("all_frames", false)
                    if (matches.isNotEmpty()) {
                        contentScripts.add(ContentScript(matches, excludeMatches, includeGlobs, excludeGlobs, js, css, runAt, allFrames))
                    }
                }
            }

            return Extension(
                id = id,
                name = manifest.optString("name", id),
                version = manifest.optString("version", "1.0"),
                description = manifest.optString("description", ""),
                contentScripts = contentScripts,
                enabled = enabled
            )
        }

        private fun parsePatterns(arr: JSONArray?): List<MatchPattern> {
            if (arr == null) return emptyList()
            val list = mutableListOf<MatchPattern>()
            for (i in 0 until arr.length()) {
                runCatching { MatchPattern(arr.getString(i)) }.onSuccess { list.add(it) }
            }
            return list
        }

        private fun parseStringList(arr: JSONArray?): List<String> {
            if (arr == null) return emptyList()
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) list.add(arr.getString(i))
            return list
        }
    }
}

object GlobPattern {
    fun matches(glob: String, url: String): Boolean {
        val regexStr = globToRegex(glob)
        val regex = Regex(regexStr, RegexOption.IGNORE_CASE)
        return regex.matches(url)
    }

    private fun globToRegex(glob: String): String {
        val sb = StringBuilder()
        for (c in glob) {
            when (c) {
                '*' -> sb.append(".*")
                '?' -> sb.append(".")
                '.', '+', '(', ')', '[', ']', '{', '}', '^', '$', '|', '\\' -> {
                    sb.append('\\').append(c)
                }
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }
}
