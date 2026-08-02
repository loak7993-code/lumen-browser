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

data class BackgroundScript(
    val scripts: List<String>,
    val persistent: Boolean,
    val type: String
)

data class OptionsPage(
    val page: String,
    val openInTab: Boolean
)

data class Extension(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val contentScripts: List<ContentScript>,
    val background: BackgroundScript?,
    val optionsPage: OptionsPage?,
    val permissions: List<String>,
    val hostPermissions: List<String>,
    val icons: Map<String, String>,
    val browserAction: Map<String, String>,
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

    fun hasPermission(perm: String): Boolean = permissions.contains(perm) || hostPermissions.contains(perm)

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

            val background: BackgroundScript? = parseBackground(manifest)
            val optionsPage = parseOptionsPage(manifest)
            val permissions = parseStringList(manifest.optJSONArray("permissions"))
            val hostPermissions = parseStringList(manifest.optJSONArray("host_permissions"))
            val icons = parseStringMap(manifest.optJSONObject("icons"))
            val browserAction = parseBrowserAction(manifest)

            return Extension(
                id = id,
                name = manifest.optString("name", id),
                version = manifest.optString("version", "1.0"),
                description = manifest.optString("description", ""),
                contentScripts = contentScripts,
                background = background,
                optionsPage = optionsPage,
                permissions = permissions,
                hostPermissions = hostPermissions,
                icons = icons,
                browserAction = browserAction,
                enabled = enabled
            )
        }

        private fun parseBackground(manifest: JSONObject): BackgroundScript? {
            val bg = manifest.optJSONObject("background")
                ?: manifest.optJSONObject("background") ?: return null
            val scripts = parseStringList(bg.optJSONArray("scripts"))
            val serviceWorker = bg.optString("service_worker", "")
            val allScripts = if (serviceWorker.isNotEmpty()) listOf(serviceWorker) else scripts
            if (allScripts.isEmpty()) return null
            val persistent = bg.optBoolean("persistent", false)
            val type = if (serviceWorker.isNotEmpty()) "service_worker" else "scripts"
            return BackgroundScript(allScripts, persistent, type)
        }

        private fun parseOptionsPage(manifest: JSONObject): OptionsPage? {
            val optsPage = manifest.optString("options_page", "")
            val optsUi = manifest.optJSONObject("options_ui")
            return when {
                optsUi != null -> OptionsPage(optsUi.optString("page", ""), optsUi.optBoolean("open_in_tab", false))
                optsPage.isNotEmpty() -> OptionsPage(optsPage, true)
                else -> null
            }
        }

        private fun parseBrowserAction(manifest: JSONObject): Map<String, String> {
            val ba = manifest.optJSONObject("browser_action")
                ?: manifest.optJSONObject("action")
                ?: return emptyMap()
            val map = mutableMapOf<String, String>()
            val icons = ba.optJSONObject("default_icon")
            if (icons != null) {
                for (key in icons.keys()) map["icon_$key"] = icons.getString(key)
            } else {
                val icon = ba.optString("default_icon", "")
                if (icon.isNotEmpty()) map["icon"] = icon
            }
            map["title"] = ba.optString("default_title", "")
            map["popup"] = ba.optString("default_popup", "")
            return map
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

        private fun parseStringMap(obj: JSONObject?): Map<String, String> {
            if (obj == null) return emptyMap()
            val map = mutableMapOf<String, String>()
            for (key in obj.keys()) map[key] = obj.getString(key)
            return map
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
