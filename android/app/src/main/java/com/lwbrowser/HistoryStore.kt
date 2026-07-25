package com.lwbrowser

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class HistoryItem(val title: String, val url: String, val time: Long)

object HistoryStore {
    private const val FILE = "history.json"
    private const val MAX = 500
    private lateinit var ctx: Context

    fun init(context: Context) {
        ctx = context.applicationContext
    }

    private fun read(): MutableList<HistoryItem> {
        val list = mutableListOf<HistoryItem>()
        val raw = ctx.getSharedPreferences("files", Context.MODE_PRIVATE).getString(FILE, null)
            ?: return list
        runCatching {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(HistoryItem(o.getString("title"), o.getString("url"), o.getLong("time")))
            }
        }
        return list
    }

    private fun write(list: List<HistoryItem>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("title", it.title).put("url", it.url).put("time", it.time))
        }
        ctx.getSharedPreferences("files", Context.MODE_PRIVATE)
            .edit().putString(FILE, arr.toString()).apply()
    }

    fun all(): List<HistoryItem> = read().reversed()

    fun add(item: HistoryItem) {
        if (!Prefs.saveHistory) return
        val list = read()
        list.removeAll { it.url == item.url }
        list.add(item)
        while (list.size > MAX) list.removeAt(0)
        write(list)
    }

    fun remove(url: String) {
        val list = read()
        list.removeAll { it.url == url }
        write(list)
    }

    fun clear() {
        write(emptyList())
    }
}
