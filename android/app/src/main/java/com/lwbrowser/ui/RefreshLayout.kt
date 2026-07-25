package com.lwbrowser.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.core.view.ViewCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class RefreshLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : SwipeRefreshLayout(context, attrs) {

    override fun canChildScrollUp(): Boolean {
        val scrollable = findScrollableWebView(getChildAt(0))
        if (scrollable != null) {
            return scrollable.canScrollVertically(-1) ||
                ViewCompat.canScrollVertically(scrollable, -1)
        }
        return super.canChildScrollUp()
    }

    private fun findScrollableWebView(v: View?): WebView? {
        if (v is WebView) return v
        if (v is ViewGroup) {
            for (i in 0 until v.childCount) {
                findScrollableWebView(v.getChildAt(i))?.let { return it }
            }
        }
        return null
    }
}
