package com.lwbrowser

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.lwbrowser.ext.ExtensionManager

class BrowserApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        BookmarkStore.init(this)
        HistoryStore.init(this)
        DownloadStore.init(this)
        ExtensionManager.init(this)
        applyDarkMode(Prefs.forceDark)
    }

    companion object {
        fun applyDarkMode(force: Boolean) {
            AppCompatDelegate.setDefaultNightMode(
                if (force) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            )
        }
    }
}
