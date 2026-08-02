package com.lwbrowser

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private const val FILE = "lwbrowser_prefs"
    private lateinit var sp: SharedPreferences

    const val DEFAULT_START_PAGE = "file:///android_asset/home.html"

    var startPage: String
        get() = sp.getString("start_page", DEFAULT_START_PAGE) ?: DEFAULT_START_PAGE
        set(value) = sp.edit().putString("start_page", value).apply()

    var blockAds: Boolean
        get() = sp.getBoolean("block_ads", true)
        set(value) = sp.edit().putBoolean("block_ads", value).apply()

    var blockTrackers: Boolean
        get() = sp.getBoolean("block_trackers", true)
        set(value) = sp.edit().putBoolean("block_trackers", value).apply()

    var loadImages: Boolean
        get() = sp.getBoolean("load_images", true)
        set(value) = sp.edit().putBoolean("load_images", value).apply()

    var javaScriptEnabled: Boolean
        get() = sp.getBoolean("js_enabled", true)
        set(value) = sp.edit().putBoolean("js_enabled", value).apply()

    var saveHistory: Boolean
        get() = sp.getBoolean("save_history", true)
        set(value) = sp.edit().putBoolean("save_history", value).apply()

    var forceDark: Boolean
        get() = sp.getBoolean("force_dark", false)
        set(value) = sp.edit().putBoolean("force_dark", value).apply()

    var desktopMode: Boolean
        get() = sp.getBoolean("desktop_mode", false)
        set(value) = sp.edit().putBoolean("desktop_mode", value).apply()

    var blockWebRTC: Boolean
        get() = sp.getBoolean("block_webrtc", true)
        set(value) = sp.edit().putBoolean("block_webrtc", value).apply()

    var searchEngine: String
        get() = sp.getString("search_engine", "startpage") ?: "startpage"
        set(value) = sp.edit().putString("search_engine", value).apply()

    var pageZoom: Int
        get() = sp.getInt("page_zoom", 100)
        set(value) = sp.edit().putInt("page_zoom", value).apply()

    var nightMode: Boolean
        get() = sp.getBoolean("night_mode", false)
        set(value) = sp.edit().putBoolean("night_mode", value).apply()

    var fontSize: Int
        get() = sp.getInt("font_size", 100)
        set(value) = sp.edit().putInt("font_size", value).apply()

    fun init(context: Context) {
        sp = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        migrateStartPage()
    }

    private fun migrateStartPage() {
        sp.edit().putString("start_page", DEFAULT_START_PAGE).apply()
    }

    fun searchUrl(query: String): String {
        val q = java.net.URLEncoder.encode(query, "UTF-8").replace("+", "%20")
        return when (searchEngine) {
            "duckduckgo" -> "https://duckduckgo.com/?q=$q"
            "google" -> "https://www.google.com/search?q=$q"
            "bing" -> "https://www.bing.com/search?q=$q"
            else -> "https://www.startpage.com/sp/search?query=$q"
        }
    }

    fun searchSuggestionUrl(query: String): String {
        val q = java.net.URLEncoder.encode(query, "UTF-8").replace("+", "%20")
        return when (searchEngine) {
            "duckduckgo" -> "https://duckduckgo.com/ac/?q=$q&type=list"
            "google" -> "https://suggestqueries.google.com/complete/search?client=firefox&q=$q"
            "bing" -> "https://www.bing.com/AS/Suggestions?pt=pagehan&mkt=en-us&qry=$q"
            else -> "https://duckduckgo.com/ac/?q=$q&type=list"
        }
    }
}
