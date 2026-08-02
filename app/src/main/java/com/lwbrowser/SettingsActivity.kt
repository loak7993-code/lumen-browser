package com.lwbrowser

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lwbrowser.databinding.ActivitySettingsBinding
import com.lwbrowser.ext.ExtensionManager

class SettingsActivity : AppCompatActivity() {
    private lateinit var b: ActivitySettingsBinding
    private var url: String = ""
    private var title: String = ""

    private val pickExtension = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) installAddon(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(b.root)

        url = intent.getStringExtra(EXTRA_URL) ?: ""
        title = intent.getStringExtra(EXTRA_TITLE) ?: ""

        b.settingsToolbar.setNavigationOnClickListener { finish() }

        b.startPageInput.setText(
            if (Prefs.startPage == Prefs.DEFAULT_START_PAGE) "DuckDuckGo" else Prefs.startPage
        )
        b.swDark.isChecked = Prefs.forceDark
        b.swNightMode.isChecked = Prefs.nightMode
        b.swDesktop.isChecked = Prefs.desktopMode
        b.swImages.isChecked = Prefs.loadImages
        b.swJs.isChecked = Prefs.javaScriptEnabled
        b.swBlockAds.isChecked = Prefs.blockAds
        b.swBlockTrackers.isChecked = Prefs.blockTrackers
        b.swBlockWebRTC.isChecked = Prefs.blockWebRTC
        b.swHistory.isChecked = Prefs.saveHistory
        updateStats()

        val engines = arrayOf("Startpage", "DuckDuckGo", "Google", "Bing")
        val engineKeys = arrayOf("startpage", "duckduckgo", "google", "bing")
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, engines)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        b.spSearchEngine.adapter = adapter
        b.spSearchEngine.setSelection(engineKeys.indexOf(Prefs.searchEngine))
        b.spSearchEngine.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                Prefs.searchEngine = engineKeys[pos]
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        b.startPageInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val text = b.startPageInput.text.toString().trim()
                if (text.isEmpty() || text.equals("DuckDuckGo", ignoreCase = true)) {
                    Prefs.startPage = Prefs.DEFAULT_START_PAGE
                    b.startPageInput.setText("DuckDuckGo")
                } else {
                    Prefs.startPage = text
                }
            }
        }
        b.swDark.setOnCheckedChangeListener { _, v ->
            Prefs.forceDark = v
            BrowserApp.applyDarkMode(v)
        }
        b.swNightMode.setOnCheckedChangeListener { _, v ->
            Prefs.nightMode = v
        }
        b.swDesktop.setOnCheckedChangeListener { _, v ->
            Prefs.desktopMode = v
            returnResult(ACTION_RELOAD)
        }
        b.swImages.setOnCheckedChangeListener { _, v -> Prefs.loadImages = v }
        b.swJs.setOnCheckedChangeListener { _, v -> Prefs.javaScriptEnabled = v }
        b.swBlockAds.setOnCheckedChangeListener { _, v -> Prefs.blockAds = v }
        b.swBlockTrackers.setOnCheckedChangeListener { _, v -> Prefs.blockTrackers = v }
        b.swBlockWebRTC.setOnCheckedChangeListener { _, v -> Prefs.blockWebRTC = v }
        b.swHistory.setOnCheckedChangeListener { _, v -> Prefs.saveHistory = v }

        b.btnClearData.setOnClickListener {
            Dialogs.confirm(
                this,
                iconRes = R.drawable.ic_delete,
                title = getString(R.string.action_clear_data),
                message = getString(R.string.confirm_clear),
                positiveText = getString(R.string.yes),
                negativeText = getString(R.string.no)
            ) {
                HistoryStore.clear()
                AdBlocker.resetStats()
                updateStats()
                android.webkit.WebStorage.getInstance().deleteAllData()
                android.webkit.CookieManager.getInstance().removeAllCookies(null)
                android.webkit.WebViewDatabase.getInstance(this).clearFormData()
            }
        }

        setupRow(b.rowDownloads.root, R.drawable.ic_cloud_off, R.string.action_downloads) {
            val items = DownloadStore.all().map { LinkItem(it.name, it.url) }
            if (items.isEmpty()) {
                android.widget.Toast.makeText(this, R.string.downloads_empty, android.widget.Toast.LENGTH_SHORT).show()
            } else {
                val view = LayoutInflater.from(this).inflate(R.layout.dialog_links, null)
                view.findViewById<TextView>(R.id.linksTitle).text = getString(R.string.action_downloads)
                val recycler = view.findViewById<RecyclerView>(R.id.linksRecycler)
                recycler.layoutManager = LinearLayoutManager(this)
                recycler.adapter = LinkAdapter(items, R.drawable.ic_cloud_off, { item -> openUrl(item.url) }) {}
                val d = AlertDialog.Builder(this).setView(view).setNegativeButton(android.R.string.cancel, null).create()
                styleDialog(d); d.show()
            }
        }
        setupRow(b.rowHistory.root, R.drawable.ic_refresh, R.string.action_history) {
            showHistoryDialog()
        }
        setupRow(b.rowBookmarks.root, R.drawable.ic_bookmark, R.string.action_bookmarks) {
            showBookmarksDialog()
        }

        val bookmarked = BookmarkStore.isBookmarked(url)
        setupRow(b.rowBookmark.root, R.drawable.ic_bookmark,
            if (bookmarked) R.string.action_unbookmark else R.string.action_bookmark) { returnResult(ACTION_BOOKMARK) }
        setupRow(b.rowOpenExternal.root, R.drawable.ic_share, R.string.row_open_external) { returnResult(ACTION_OPEN_EXTERNAL) }
        setupRow(b.rowFind.root, R.drawable.ic_search, R.string.action_find) { returnResult(ACTION_FIND) }
        setupRow(b.rowExit.root, R.drawable.ic_close, R.string.row_exit) { returnResult(ACTION_EXIT) }

        b.btnInstallAddon.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                val extraMime = arrayOf("application/x-chrome-extension", "application/zip", "application/octet-stream", "application/x-zip-compressed")
                putExtra(Intent.EXTRA_MIME_TYPES, extraMime)
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            pickExtension.launch("*/*")
        }
        refreshAddons()
    }

    private fun setupRow(view: View, iconRes: Int, textRes: Int, onClick: () -> Unit) {
        view.findViewById<android.widget.ImageView>(R.id.rowIcon)
            .setImageResource(iconRes)
        view.findViewById<TextView>(R.id.rowText).setText(textRes)
        view.setOnClickListener { onClick() }
    }

    private fun installAddon(uri: Uri) {
        val ext = ExtensionManager.install(this, uri)
        if (ext != null) {
            android.widget.Toast.makeText(this, R.string.install_success, android.widget.Toast.LENGTH_SHORT).show()
            refreshAddons()
        } else {
            android.widget.Toast.makeText(this, R.string.install_failed, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun refreshAddons() {
        val extensions = ExtensionManager.all()
        b.addonsEmpty.visibility = if (extensions.isEmpty()) View.VISIBLE else View.GONE
        b.addonsList.removeAllViews()
        for (ext in extensions) {
            val row = LayoutInflater.from(this).inflate(R.layout.item_addon, b.addonsList, false)
            val iconView = row.findViewById<android.widget.ImageView>(R.id.addonIcon)
            val iconPath = ext.icons["128"] ?: ext.icons["64"] ?: ext.icons["48"] ?: ext.icons["32"] ?: ext.icons.values.firstOrNull()
            if (iconPath != null) {
                val base64 = ExtensionManager.loadIconBase64(ext.id, iconPath)
                if (base64 != null) {
                    runCatching {
                        val bytes = android.util.Base64.decode(base64.substringAfter("base64,"), android.util.Base64.DEFAULT)
                        val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        iconView.setImageBitmap(bmp)
                    }
                }
            }
            row.findViewById<TextView>(R.id.addonName).text = ext.name
            row.findViewById<TextView>(R.id.addonDesc).text = "${ext.version} · ${ext.description}"
            val allPerms = ext.permissions + ext.hostPermissions
            val permsText = if (allPerms.isEmpty()) "No permissions" else "Permissions: ${allPerms.take(4).joinToString(", ")}${if (allPerms.size > 4) " +${allPerms.size - 4}" else ""}"
            row.findViewById<TextView>(R.id.addonPerms).text = permsText
            val sw = row.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.addonSwitch)
            sw.isChecked = ext.enabled
            sw.setOnCheckedChangeListener { _, v -> ExtensionManager.setEnabled(ext.id, v) }
            row.setOnClickListener {
                showAddonDetails(ext)
            }
            row.findViewById<android.widget.ImageButton>(R.id.addonUninstall).setOnClickListener {
                Dialogs.confirm(
                    this,
                    iconRes = R.drawable.ic_delete,
                    title = "Uninstall ${ext.name}?",
                    message = "This will remove the add-on and its data.",
                    positiveText = getString(R.string.action_uninstall),
                    negativeText = getString(R.string.no)
                ) {
                    ExtensionManager.uninstall(ext.id)
                    refreshAddons()
                }
            }
            b.addonsList.addView(row)
        }
    }

    private fun showAddonDetails(ext: com.lwbrowser.ext.Extension) {
        val perms = ext.permissions.joinToString("\n") { "• $it" }
        val hostPerms = ext.hostPermissions.joinToString("\n") { "• $it" }
        val contentScriptCount = ext.contentScripts.size
        val hasBg = ext.background != null
        val hasOptions = ext.optionsPage != null
        val message = buildString {
            append("Version: ${ext.version}\n")
            append("ID: ${ext.id}\n\n")
            if (ext.description.isNotBlank()) {
                append("${ext.description}\n\n")
            }
            append("Content scripts: $contentScriptCount\n")
            append("Background script: ${if (hasBg) "Yes" else "No"}\n")
            append("Options page: ${if (hasOptions) "Yes" else "No"}\n\n")
            if (perms.isNotEmpty()) {
                append("Permissions:\n$perms\n\n")
            }
            if (hostPerms.isNotEmpty()) {
                append("Host permissions:\n$hostPerms\n\n")
            }
        }
        val neutralText = if (hasOptions) "Options" else null
        Dialogs.confirm(
            this,
            iconRes = R.drawable.ic_add,
            title = ext.name,
            message = message.trim(),
            positiveText = "Uninstall",
            negativeText = "Close",
            neutralText = neutralText,
            onPositive = {
                ExtensionManager.uninstall(ext.id)
                refreshAddons()
            },
            onNeutral = if (hasOptions && ext.optionsPage != null) {
                {
                    val optsPath = ext.optionsPage!!.page
                    val optsFile = java.io.File(filesDir, "extensions/${ext.id}/$optsPath")
                    if (optsFile.exists()) {
                        val intent = Intent(this, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            putExtra("url", "file://${optsFile.absolutePath}")
                        }
                        startActivity(intent)
                    }
                }
            } else null
        )
    }

    private fun styleDialog(d: AlertDialog) {
        d.setOnShowListener { d.window?.setBackgroundDrawableResource(R.drawable.dialog_bg_rounded) }
    }

    private fun showBookmarksDialog() {
        val items = BookmarkStore.all()
        if (items.isEmpty()) {
            android.widget.Toast.makeText(this, R.string.empty_bookmarks, android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_links, null)
        view.findViewById<TextView>(R.id.linksTitle).text = getString(R.string.action_bookmarks)
        val recycler = view.findViewById<RecyclerView>(R.id.linksRecycler)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = LinkAdapter(items, R.drawable.ic_bookmark, { item -> openUrl(item.url) }) { deleted ->
            BookmarkStore.remove(deleted.url)
            showBookmarksDialog()
        }
        val d = AlertDialog.Builder(this).setView(view).setNegativeButton(android.R.string.cancel, null).create()
        styleDialog(d); d.show()
    }

    private fun showHistoryDialog() {
        val items = HistoryStore.all().map { LinkItem(it.title, it.url) }
        if (items.isEmpty()) {
            android.widget.Toast.makeText(this, R.string.empty_history, android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_links, null)
        view.findViewById<TextView>(R.id.linksTitle).text = getString(R.string.action_history)
        val recycler = view.findViewById<RecyclerView>(R.id.linksRecycler)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = LinkAdapter(items, R.drawable.ic_refresh, { item -> openUrl(item.url) }) { deleted ->
            HistoryStore.remove(deleted.url)
            showHistoryDialog()
        }
        val d = AlertDialog.Builder(this).setView(view).setNegativeButton(android.R.string.cancel, null).create()
        styleDialog(d); d.show()
    }

    private fun openUrl(target: String) {
        returnResult(ACTION_OPEN_URL, target)
    }

    private fun returnResult(action: String, openUrl: String? = null) {
        val data = Intent().putExtra(EXTRA_ACTION, action)
        if (openUrl != null) data.putExtra(EXTRA_OPEN_URL, openUrl)
        setResult(RESULT_OK, data)
        finish()
    }

    override fun onResume() {
        super.onResume()
        updateStats()
    }

    private fun updateStats() {
        val (ads, trackers, cosmetic) = AdBlocker.stats()
        b.txtAdsCount.text = ads.toString()
        b.txtTrackersCount.text = trackers.toString()
    }

    companion object {
        const val EXTRA_URL = "extra_url"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_ACTION = "extra_action"
        const val EXTRA_OPEN_URL = "extra_open_url"
        const val ACTION_FIND = "find"
        const val ACTION_EXIT = "exit"
        const val ACTION_RELOAD = "reload"
        const val ACTION_BOOKMARK = "bookmark"
        const val ACTION_OPEN_EXTERNAL = "open_external"
        const val ACTION_OPEN_URL = "open_url"
    }
}
