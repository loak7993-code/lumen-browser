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

    fun init(context: Context) {
        sp = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        migrateStartPage()
    }

    private fun migrateStartPage() {
        val saved = sp.getString("start_page", null)
        if (saved != null && saved != DEFAULT_START_PAGE && !saved.startsWith("file:///android_asset/home")) {
            sp.edit().putString("start_page", DEFAULT_START_PAGE).apply()
        }
    }
}
