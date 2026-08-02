package com.lwbrowser

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.webkit.DownloadListener
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lwbrowser.databinding.ActivityMainBinding
import com.lwbrowser.databinding.ErrorPageBinding
import com.lwbrowser.databinding.FindBarBinding
import com.lwbrowser.databinding.ItemTabBinding
import com.lwbrowser.ext.ExtensionManager

class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding
    private val tabs = TabsManager()
    private var findBinding: FindBarBinding? = null
    private var errorBinding: ErrorPageBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        applyInsets()

        val initialUrl = resolveInitialUrl(intent)
        val tab = tabs.newTab(initialUrl)
        attachTab(tab, restore = false)
        if (intent?.action != ACTION_FOCUS_SEARCH) tab.webView.loadUrl(initialUrl)

        b.swipe.setOnRefreshListener {
            currentWebView()?.reload()
        }

        setupUrlBar()
        setupNav()
        setupMenu()

        if (intent?.action == ACTION_FOCUS_SEARCH) focusUrlBar()
    }

    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(b.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            b.topBar.setPadding(0, bars.top, 0, 0)
            b.swipe.setPadding(0, 0, 0, bars.bottom)
            insets
        }
    }

    private fun resolveInitialUrl(intent: Intent): String {
        if (intent.action == Intent.ACTION_VIEW) {
            intent.data?.toString()?.let { return it }
        }
        return Prefs.startPage
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        when (intent.action) {
            Intent.ACTION_VIEW -> intent.data?.toString()?.let { openInNewTab(it) }
            ACTION_FOCUS_SEARCH -> focusUrlBar()
        }
    }

    private fun focusUrlBar() {
        b.urlField.setText("")
        b.urlField.requestFocus()
        b.urlField.post {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(b.urlField, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun setupUrlBar() {
        b.urlField.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                navigateTo(UrlUtils.normalize(v.text.toString()))
                true
            } else false
        }
        b.urlField.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                b.urlField.selectAll()
                b.btnClear.visibility = View.VISIBLE
            } else {
                b.btnClear.visibility = View.GONE
            }
        }
        b.urlField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                b.btnClear.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
            }
        })
        b.btnClear.setOnClickListener {
            b.urlField.setText("")
            b.urlField.requestFocus()
        }
    }

    private fun setupNav() {
        b.btnBack.setOnClickListener {
            val wv = currentWebView() ?: return@setOnClickListener
            if (wv.canGoBack()) wv.goBack()
        }
        b.btnForward.setOnClickListener {
            val wv = currentWebView() ?: return@setOnClickListener
            if (wv.canGoForward()) wv.goForward()
        }
        b.btnRefresh.setOnClickListener {
            val tab = tabs.current
            if (tab?.isLoading == true) tab.webView.stopLoading() else tab?.webView?.reload()
        }
    }

    private fun setupMenu() {
        b.btnMenu.setOnClickListener { showOverflowMenu() }
    }

    private fun showOverflowMenu() {
        val view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_menu, null)
        val sheet = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        sheet.setContentView(view)

        val url = tabs.current?.url ?: ""
        val bookmarked = BookmarkStore.isBookmarked(url)

        view.findViewById<TextView>(R.id.txtBookmark).text =
            if (bookmarked) getString(R.string.action_unbookmark) else getString(R.string.action_bookmark)

        view.findViewById<View>(R.id.qaBack).setOnClickListener {
            val wv = currentWebView()
            if (wv?.canGoBack() == true) wv.goBack()
            sheet.dismiss()
        }
        view.findViewById<View>(R.id.qaForward).setOnClickListener {
            val wv = currentWebView()
            if (wv?.canGoForward() == true) wv.goForward()
            sheet.dismiss()
        }
        view.findViewById<View>(R.id.qaShare).setOnClickListener { sheet.dismiss(); shareCurrent() }
        view.findViewById<View>(R.id.qaRefresh).setOnClickListener {
            sheet.dismiss()
            val tab = tabs.current
            if (tab?.isLoading == true) tab.webView.stopLoading() else tab?.webView?.reload()
        }
        view.findViewById<View>(R.id.rowBookmark).setOnClickListener { sheet.dismiss(); toggleBookmark() }
        view.findViewById<View>(R.id.rowFind).setOnClickListener { sheet.dismiss(); showFindBar() }

        val swDesktop = view.findViewById<Switch>(R.id.swDesktopMenu)
        swDesktop.isChecked = Prefs.desktopMode
        swDesktop.setOnCheckedChangeListener { _, v ->
            Prefs.desktopMode = v
            toggleDesktopMode()
            sheet.dismiss()
        }

        view.findViewById<View>(R.id.rowExtensions).setOnClickListener {
            sheet.dismiss()
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        view.findViewById<View>(R.id.gridNewTab).setOnClickListener { sheet.dismiss(); openInNewTab(Prefs.startPage) }
        view.findViewById<View>(R.id.gridTabs).setOnClickListener { sheet.dismiss(); showTabsDialog() }
        view.findViewById<View>(R.id.gridHistory).setOnClickListener { sheet.dismiss(); showHistory() }
        view.findViewById<View>(R.id.gridDownloads).setOnClickListener { sheet.dismiss(); showDownloads() }
        view.findViewById<View>(R.id.rowSettings).setOnClickListener {
            sheet.dismiss()
            openSettings()
        }

        sheet.show()
    }

    private var pendingSearch: String? = null

    private fun navigateTo(url: String) {
        if (url.isEmpty()) return
        val wv = currentWebView() ?: return

        if (url.startsWith("file:///android_asset/search.html#q=")) {
            val query = android.net.Uri.parse(url).fragment
                ?.removePrefix("q=")
                ?.let { java.net.URLDecoder.decode(it, "UTF-8").replace("+", " ") }
                ?: ""
            val currentUrl = wv.url ?: ""
            if (currentUrl.startsWith("file:///android_asset/search.html")) {
                val escaped = query.replace("\\", "\\\\").replace("'", "\\'")
                wv.evaluateJavascript("doSearch('$escaped')", null)
            } else {
                pendingSearch = query
                wv.loadUrl("file:///android_asset/search.html")
            }
            b.urlField.clearFocus()
            return
        }

        wv.loadUrl(url)
        b.urlField.clearFocus()
    }

    private fun injectWebRTCBlock(wv: WebView) {
        val js = """
            (function(){
                if (window.__lumen_webrtc_blocked) return;
                window.__lumen_webrtc_blocked = true;
                try {
                    window.RTCPeerConnection = undefined;
                    window.webkitRTCPeerConnection = undefined;
                    window.mozRTCPeerConnection = undefined;
                    if (navigator && navigator.mediaDevices) {
                        navigator.mediaDevices.getUserMedia = function(){return Promise.reject(new Error('WebRTC disabled'));};
                        navigator.mediaDevices.enumerateDevices = function(){return Promise.resolve([]);};
                    }
                    Object.defineProperty(navigator,'mediaDevices',{get:function(){return {getUserMedia:function(){return Promise.reject(new Error('WebRTC disabled'));},enumerateDevices:function(){return Promise.resolve([]);}};},configurable:false});
                } catch(e) {}
            })();
        """.trimIndent()
        wv.evaluateJavascript(js, null)
    }

    private fun injectCosmeticFilters(wv: WebView) {
        val css = CosmeticFilters.cssHideRules()
        val js = """
            (function(){
                if(document.getElementById('__lumen_cosmetic'))return;
                var s=document.createElement('style');
                s.id='__lumen_cosmetic';
                s.textContent=${"\"\"\""}$css${"\"\"\""};
                (document.head||document.documentElement).appendChild(s);
                var observer=new MutationObserver(function(){
                    document.querySelectorAll('${"\"\"\""}${CosmeticFilters.cssSelectors.joinToString(",")}${"\"\"\""}').forEach(function(el){
                        if(getComputedStyle(el).display!=='none'){el.style.display='none';}
                    });
                });
                observer.observe(document.body||document.documentElement,{childList:true,subtree:true});
            })();
        """.trimIndent()
        wv.evaluateJavascript(js, null)
    }

    private fun injectAntiFingerprint(wv: WebView) {
        wv.evaluateJavascript(AntiFingerprint.js(), null)
    }

    private fun injectContentScripts(wv: WebView, url: String, runAt: String) {
        for (ext in ExtensionManager.scriptsFor(url, runAt)) {
            for (cs in ext.scriptsFor(url, runAt)) {
                for (cssFile in cs.css) {
                    val css = ExtensionManager.loadFile(ext.id, cssFile) ?: continue
                    val escaped = css.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n")
                    wv.evaluateJavascript(
                        "(function(){var s=document.createElement('style');s.textContent='$escaped';document.head.appendChild(s);})();",
                        null
                    )
                }
                for (jsFile in cs.js) {
                    val js = ExtensionManager.loadFile(ext.id, jsFile) ?: continue
                    wv.evaluateJavascript(js, null)
                }
            }
        }
    }

    private fun openInNewTab(url: String) {
        val tab = tabs.newTab(url)
        attachTab(tab, restore = false)
        tab.webView.loadUrl(url)
    }

    private fun openSettings() {
        val tab = tabs.current
        val intent = Intent(this, SettingsActivity::class.java).apply {
            putExtra(SettingsActivity.EXTRA_URL, tab?.url ?: "")
            putExtra(SettingsActivity.EXTRA_TITLE, tab?.title ?: "")
        }
        startActivityForResult(intent, REQ_SETTINGS)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_SETTINGS && resultCode == RESULT_OK) {
            when (data?.getStringExtra(SettingsActivity.EXTRA_ACTION)) {
                SettingsActivity.ACTION_FIND -> showFindBar()
                SettingsActivity.ACTION_EXIT -> finishAffinity()
                SettingsActivity.ACTION_RELOAD -> currentWebView()?.reload()
                SettingsActivity.ACTION_BOOKMARK -> toggleBookmark()
                SettingsActivity.ACTION_OPEN_EXTERNAL -> openInExternal()
                SettingsActivity.ACTION_OPEN_URL -> {
                    val url = data.getStringExtra(SettingsActivity.EXTRA_OPEN_URL)
                    if (!url.isNullOrEmpty()) navigateTo(url)
                }
            }
        }
    }

    companion object {
        private const val REQ_SETTINGS = 1001
        const val ACTION_FOCUS_SEARCH = "com.lwbrowser.FOCUS_SEARCH"
    }

    private fun attachTab(tab: Tab, restore: Boolean) {
        if (::createdWebView.isInitialized && createdWebView.parent === b.webContainer) {
            b.webContainer.removeView(createdWebView)
        }
        if (!tab.webViewReady()) {
            tab.webView = createWebView()
        }
        if (tab.webView.parent != null) (tab.webView.parent as ViewGroup).removeView(tab.webView)
        b.webContainer.addView(tab.webView)
        createdWebView = tab.webView
        updateChromeFromTab(tab)
        b.urlField.setText(displayUrl(tab.url))
    }

    private lateinit var createdWebView: WebView

    private fun createWebView(): WebView {
        val wv = WebView(this)
        val s = wv.settings
        s.javaScriptEnabled = Prefs.javaScriptEnabled
        s.domStorageEnabled = true
        s.databaseEnabled = true
        s.loadsImagesAutomatically = Prefs.loadImages
        s.blockNetworkImage = !Prefs.loadImages
        s.cacheMode = WebSettings.LOAD_DEFAULT
        s.userAgentString = s.userAgentString + " Lumen/1.0"
        s.useWideViewPort = true
        s.loadWithOverviewMode = true
        s.setSupportZoom(true)
        s.builtInZoomControls = true
        s.displayZoomControls = false
        s.allowFileAccess = true
        s.allowContentAccess = true
        s.javaScriptCanOpenWindowsAutomatically = false
        s.mediaPlaybackRequiresUserGesture = true
        s.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            s.safeBrowsingEnabled = true
        }
        if (Prefs.desktopMode) {
            s.userAgentString = s.userAgentString.replace("Mobile", "eliboM")
            s.useWideViewPort = true
        }
        wv.webViewClient = BrowserWebViewClient()
        wv.webChromeClient = BrowserChromeClient()
        wv.setDownloadListener(BrowserDownloadListener())
        wv.setOnLongClickListener { handleLongClick(); true }
        wv.addJavascriptInterface(
            LumenSearchEngine { kind, json ->
                runOnUiThread {
                    val escaped = json.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n")
                    val fn = if (kind == "suggestions") "window.__lumenSuggestions" else "window.__lumenResults"
                    wv.evaluateJavascript("$fn('$escaped')", null)
                }
            },
            "LumenBridge"
        )
        return wv
    }

    private fun handleLongClick() {
        val result = currentWebView()?.hitTestResult ?: return
        val url = result.extra ?: return
        when (result.type) {
            WebView.HitTestResult.IMAGE_TYPE,
            WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE,
            WebView.HitTestResult.SRC_ANCHOR_TYPE -> {
                val items = listOf(
                    MenuItem(R.drawable.ic_add, "Open in new tab") { openInNewTab(url) },
                    MenuItem(R.drawable.ic_share, "Copy link") { copyToClipboard(url) },
                    MenuItem(R.drawable.ic_cloud_off, "Download image") { triggerDownload(url, null) }
                )
                val view = LayoutInflater.from(this).inflate(R.layout.dialog_menu, null)
                val recycler = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.menuRecycler)
                val dialog = AlertDialog.Builder(this).setView(view).create()
                dialog.setOnShowListener {
                    dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_bg_rounded)
                }
                recycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
                recycler.adapter = MenuAdapter(items) { dialog.dismiss() }
                dialog.show()
            }
            else -> {}
        }
    }

    private fun copyToClipboard(text: String) {
        val cm = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        cm.setPrimaryClip(android.content.ClipData.newPlainText("link", text))
    }

    private fun currentWebView(): WebView? = tabs.current?.takeIf { it.webViewReady() }?.webView

    private fun updateChromeFromTab(tab: Tab) {
        b.btnBack.isEnabled = tab.canGoBack
        b.btnForward.isEnabled = tab.canGoForward
        b.btnBack.alpha = if (tab.canGoBack) 1f else 0.4f
        b.btnForward.alpha = if (tab.canGoForward) 1f else 0.4f
        b.securityIcon.visibility = if (UrlUtils.isSecure(tab.url)) View.VISIBLE else View.GONE
        if (!b.urlField.isFocused) b.urlField.setText(displayUrl(tab.url))
    }

    private fun displayUrl(url: String?): String {
        if (url.isNullOrEmpty()) return ""
        if (url.startsWith("file:///android_asset/search.html")) return ""
        return url
    }

    private fun updateProgress(p: Int) {
        b.progress.progress = p
        b.progress.visibility = if (p in 1..99) View.VISIBLE else View.GONE
        b.swipe.isRefreshing = p >= 0 && p < 100 && p > 80 && false
    }

    private fun setRefreshIcon(loading: Boolean) {
        b.btnRefresh.setImageResource(if (loading) R.drawable.ic_stop else R.drawable.ic_refresh)
    }

    inner class BrowserWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val url = request?.url?.toString() ?: return false
            if (url.startsWith("intent:")) {
                runCatching {
                    val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent)
                        return true
                    }
                }
                return true
            }
            if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("about:") || url.startsWith("data:") || url.startsWith("file://")) {
                return false
            }
            return try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                true
            } catch (e: Exception) {
                true
            }
        }

        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
            val result = AdBlocker.shouldBlock(request?.url?.toString())
            if (result != AdBlocker.BlockResult.Allow) {
                return WebResourceResponse("text/plain", "utf-8",
                    java.io.ByteArrayInputStream(ByteArray(0)))
            }
            return null
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            tabs.current?.isLoading = true
            setRefreshIcon(true)
            hideError()
            updateProgress(1)
            b.securityIcon.visibility = if (UrlUtils.isSecure(url)) View.VISIBLE else View.GONE
            if (!b.urlField.isFocused) b.urlField.setText(displayUrl(url))
            tabs.current?.url = url ?: ""
            tabs.current?.favicon = favicon
            tabs.current?.title = ""
            if (view != null) {
                injectAntiFingerprint(view)
                if (Prefs.blockAds) injectCosmeticFilters(view)
                if (Prefs.blockWebRTC) injectWebRTCBlock(view)
                if (url != null) injectContentScripts(view, url, "document_start")
            }
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            tabs.current?.isLoading = false
            setRefreshIcon(false)
            updateProgress(100)
            b.swipe.isRefreshing = false
            if (view != null) {
                if (Prefs.blockAds) injectCosmeticFilters(view)
                if (Prefs.blockWebRTC) injectWebRTCBlock(view)
                if (url != null) {
                    injectContentScripts(view, url, "document_end")
                    injectContentScripts(view, url, "document_idle")
                }
            }
            if (pendingSearch != null && view != null && url?.startsWith("file:///android_asset/search.html") == true) {
                val q = pendingSearch!!
                pendingSearch = null
                val escaped = q.replace("\\", "\\\\").replace("'", "\\'")
                view.evaluateJavascript("doSearch('$escaped')", null)
            }
            val tab = tabs.current
            if (tab != null && view != null) {
                tab.canGoBack = view.canGoBack()
                tab.canGoForward = view.canGoForward()
                tab.url = url ?: tab.url
                updateChromeFromTab(tab)
                val title = view.title ?: ""
                tab.title = title
                if (!url.isNullOrEmpty() && !url.startsWith("file:///android_asset/")) {
                    HistoryStore.add(HistoryItem(title.ifEmpty { UrlUtils.host(url) }, url, System.currentTimeMillis()))
                }
            }
        }

        override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
            showError(description ?: getString(R.string.empty_history))
        }
    }

    inner class BrowserChromeClient : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            updateProgress(newProgress)
        }

        override fun onReceivedTitle(view: WebView?, title: String?) {
            tabs.current?.title = title ?: ""
            tabs.current?.let { updateChromeFromTab(it) }
        }

        override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
            tabs.current?.favicon = icon
        }

        override fun onCreateWindow(
            view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?
        ): Boolean {
            val newWv = WebView(this@MainActivity)
            newWv.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    openInNewTab(request?.url?.toString() ?: "")
                    return true
                }
            }
            val transport = resultMsg?.obj as? WebView.WebViewTransport
            transport?.webView = newWv
            resultMsg?.sendToTarget()
            return true
        }
    }

    inner class BrowserDownloadListener : DownloadListener {
        override fun onDownloadStart(url: String?, userAgent: String?, contentDisposition: String?, mimetype: String?, contentLength: Long) {
            triggerDownload(url, mimetype)
        }
    }

    private fun triggerDownload(url: String?, mimetype: String?) {
        if (url == null) return
        val name = Uri.parse(url).lastPathSegment ?: "download"
        try {
            val req = android.app.DownloadManager.Request(Uri.parse(url))
            req.setMimeType(mimetype ?: "*/*")
            req.setTitle(name)
            req.setDescription(getString(R.string.app_name))
            req.allowScanningByMediaScanner()
            req.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            req.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, name)
            val dm = getSystemService(DOWNLOAD_SERVICE) as android.app.DownloadManager
            dm.enqueue(req)
            DownloadStore.add(DownloadItem(name, url, System.currentTimeMillis()))
            toast(getString(R.string.download_started))
        } catch (e: Exception) {
            toast(getString(R.string.permission_denied))
        }
    }

    private fun toast(msg: String) = android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()

    private fun showError(message: String) {
        if (errorBinding == null) {
            val stub = b.errorStub
            if (stub.parent == null) return
            errorBinding = ErrorPageBinding.bind(stub.inflate())
            errorBinding?.btnRetry?.setOnClickListener { currentWebView()?.reload() }
        }
        errorBinding?.errorTitle?.text = getString(R.string.app_name)
        errorBinding?.errorMessage?.text = message
        errorBinding?.root?.visibility = View.VISIBLE
    }

    private fun hideError() {
        errorBinding?.root?.visibility = View.GONE
    }

    private fun toggleBookmark() {
        val tab = tabs.current ?: return
        val now = BookmarkStore.toggle(LinkItem(tab.title.ifEmpty { UrlUtils.host(tab.url) }, tab.url))
        toast(if (now) getString(R.string.action_bookmark) else getString(R.string.action_unbookmark))
    }

    private fun shareCurrent() {
        val url = tabs.current?.url ?: return
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        startActivity(Intent.createChooser(send, getString(R.string.action_share)))
    }

    private fun openInExternal() {
        val url = tabs.current?.url ?: return
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun toggleDesktopMode() {
        Prefs.desktopMode = !Prefs.desktopMode
        val wv = currentWebView() ?: return
        val s = wv.settings
        if (Prefs.desktopMode) {
            s.userAgentString = s.userAgentString.replace("Mobile", "eliboM").replace("Android", "X11")
            s.useWideViewPort = true
        } else {
            s.userAgentString = s.userAgentString.replace("eliboM", "Mobile").replace("X11", "Android")
        }
        wv.reload()
    }

    private fun showFindBar() {
        if (findBinding != null) return
        val container = b.root as ViewGroup
        val binding = FindBarBinding.inflate(LayoutInflater.from(this), container, false)
        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        container.addView(binding.root, container.indexOfChild(b.swipe), params)
        findBinding = binding
        binding.findField.setOnEditorActionListener { v, _, _ ->
            doFind(v.text.toString())
            true
        }
        binding.findNext.setOnClickListener { currentWebView()?.findNext(true) }
        binding.findPrev.setOnClickListener { currentWebView()?.findNext(false) }
        binding.findClose.setOnClickListener { closeFindBar() }
        binding.findField.requestFocus()
        showKeyboard(binding.findField)
    }

    private fun doFind(query: String) {
        currentWebView()?.findAllAsync(query)
    }

    private fun closeFindBar() {
        currentWebView()?.clearMatches()
        val container = b.root as ViewGroup
        container.removeView(findBinding?.root)
        findBinding = null
        hideKeyboard()
    }

    private fun showKeyboard(v: View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(v, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(b.root.windowToken, 0)
    }

    private fun showTabsDialog() {
        if (tabs.count == 0) {
            toast(getString(R.string.empty_tabs))
            return
        }
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_tabs, null)
        val recycler = view.findViewById<RecyclerView>(R.id.tabsRecycler)
        view.findViewById<TextView>(R.id.tabsTitle).text =
            "${tabs.count} ${getString(R.string.action_tabs).lowercase()}"
        view.findViewById<android.widget.ImageButton>(R.id.btnNewTab).setOnClickListener {
            dismissActiveDialog()
            openInNewTab(Prefs.startPage)
        }
        recycler.layoutManager = androidx.recyclerview.widget.GridLayoutManager(this, 2)
        recycler.adapter = TabAdapter(tabs.all, tabs.indexOf(tabs.current!!), { idx ->
            tabs.select(idx)
            attachTab(tabs.current!!, restore = true)
            dismissActiveDialog()
        }, { idx ->
            tabs.closeAt(idx)
            if (tabs.count == 0) {
                dismissActiveDialog()
                val tab = tabs.newTab(Prefs.startPage)
                attachTab(tab, restore = false)
                tab.webView.loadUrl(Prefs.startPage)
            } else {
                attachTab(tabs.current!!, restore = true)
                dismissActiveDialog()
                showTabsDialog()
            }
        })
        val dialog = AlertDialog.Builder(this).setView(view)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        styleDialog(dialog)
        activeDialog = dialog
        dialog.show()
    }

    private var activeDialog: AlertDialog? = null
    private fun dismissActiveDialog() { activeDialog?.dismiss(); activeDialog = null }

    private fun styleDialog(d: AlertDialog) {
        d.setOnShowListener { d.window?.setBackgroundDrawableResource(R.drawable.dialog_bg_rounded) }
    }

    private fun showBookmarks() {
        val items = BookmarkStore.all()
        if (items.isEmpty()) { toast(getString(R.string.empty_bookmarks)); return }
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_links, null)
        view.findViewById<android.widget.TextView>(R.id.linksTitle).text = getString(R.string.action_bookmarks)
        val recycler = view.findViewById<RecyclerView>(R.id.linksRecycler)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = LinkAdapter(items, R.drawable.ic_bookmark, { item ->
            dismissActiveDialog()
            navigateTo(item.url)
        }, { item ->
            BookmarkStore.remove(item.url)
            showBookmarks()
            dismissActiveDialog()
        })
        activeDialog = AlertDialog.Builder(this).setView(view).setNegativeButton(android.R.string.cancel, null).create()
        styleDialog(activeDialog!!)
        activeDialog?.show()
    }

    private fun showHistory() {
        val items = HistoryStore.all()
        if (items.isEmpty()) { toast(getString(R.string.empty_history)); return }
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_links, null)
        view.findViewById<android.widget.TextView>(R.id.linksTitle).text = getString(R.string.action_history)
        val recycler = view.findViewById<RecyclerView>(R.id.linksRecycler)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = LinkAdapter(items.map { LinkItem(it.title, it.url) }, R.drawable.ic_refresh, { item ->
            dismissActiveDialog()
            navigateTo(item.url)
        }, { item ->
            HistoryStore.remove(item.url)
            showHistory()
            dismissActiveDialog()
        })
        activeDialog = AlertDialog.Builder(this).setView(view).setNegativeButton(android.R.string.cancel, null).create()
        styleDialog(activeDialog!!)
        activeDialog?.show()
    }

    private fun showDownloads() {
        val items = DownloadStore.all()
        if (items.isEmpty()) { toast(getString(R.string.downloads_empty)); return }
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_links, null)
        view.findViewById<android.widget.TextView>(R.id.linksTitle).text = getString(R.string.action_downloads)
        val recycler = view.findViewById<RecyclerView>(R.id.linksRecycler)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = LinkAdapter(items.map { LinkItem(it.name, it.url) }, R.drawable.ic_cloud_off, { item ->
            dismissActiveDialog()
            toast(item.url)
        }, { _ -> })
        activeDialog = AlertDialog.Builder(this).setView(view).setNegativeButton(android.R.string.cancel, null).create()
        styleDialog(activeDialog!!)
        activeDialog?.show()
    }

    private fun confirmClear() {
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
            currentWebView()?.clearCache(true)
            currentWebView()?.clearHistory()
            toast("Cleared")
        }
    }

    override fun onBackPressed() {
        val wv = currentWebView()
        when {
            findBinding != null -> closeFindBar()
            wv != null && wv.canGoBack() -> wv.goBack()
            tabs.count > 1 -> {
                tabs.close(tabs.current!!)
                if (tabs.current != null) attachTab(tabs.current!!, restore = true)
                else finish()
            }
            else -> {
                super.onBackPressed()
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) { onBackPressed(); return true }
        return super.onKeyDown(keyCode, event)
    }

    override fun onPause() {
        super.onPause()
        tabs.all.forEach { if (it.webViewReady()) it.webView.onPause() }
    }

    override fun onResume() {
        super.onResume()
        tabs.all.forEach { if (it.webViewReady()) it.webView.onResume() }
    }

    override fun onDestroy() {
        tabs.closeAll()
        super.onDestroy()
    }
}
