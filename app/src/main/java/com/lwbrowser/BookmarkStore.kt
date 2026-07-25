package com.lwbrowser

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class LinkItem(val title: String, val url: String)

object BookmarkStore {
    private const val FILE = "bookmarks.json"
    private lateinit var ctx: Context

    fun init(context: Context) {
        ctx = context.applicationContext
    }

    private fun read(): MutableList<LinkItem> {
        val list = mutableListOf<LinkItem>()
        val raw = ctx.getSharedPreferences("files", Context.MODE_PRIVATE).getString(FILE, null)
            ?: return list
        runCatching {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(LinkItem(o.getString("title"), o.getString("url")))
            }
        }
        return list
    }

    private fun write(list: List<LinkItem>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("title", it.title).put("url", it.url))
        }
        ctx.getSharedPreferences("files", Context.MODE_PRIVATE)
            .edit().putString(FILE, arr.toString()).apply()
    }

    fun all(): List<LinkItem> = read()

    fun isBookmarked(url: String): Boolean = read().any { it.url == url }

    fun add(item: LinkItem) {
        val list = read()
        if (list.none { it.url == item.url }) {
            list.add(0, item)
            write(list)
        }
    }

    fun remove(url: String) {
        val list = read()
        list.removeAll { it.url == url }
        write(list)
    }

    fun toggle(item: LinkItem): Boolean {
        val exists = isBookmarked(item.url)
        if (exists) remove(item.url) else add(item)
        return !exists
    }
}
