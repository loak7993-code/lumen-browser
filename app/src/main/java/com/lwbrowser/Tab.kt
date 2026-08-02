package com.lwbrowser

import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import java.util.UUID

class Tab(
    val id: String = UUID.randomUUID().toString(),
    var url: String = Prefs.startPage
) {
    lateinit var webView: WebView
    var title: String = ""
    var favicon: android.graphics.Bitmap? = null
    var isLoading: Boolean = false
    var canGoBack: Boolean = false
    var canGoForward: Boolean = false

    fun webViewReady(): Boolean = ::webView.isInitialized

    fun destroy() {
        if (this::webView.isInitialized) {
            webView.stopLoading()
            webView.webChromeClient = null
            webView.webViewClient = WebViewClient()
            // Detach the WebView from its parent before destroying; otherwise the
            // ViewGroup keeps a reference and the WebView leaks.
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.destroy()
        }
    }
}
