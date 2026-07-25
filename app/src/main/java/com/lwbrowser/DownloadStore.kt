package com.lwbrowser

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class DownloadItem(val name: String, val url: String, val time: Long)

object DownloadStore {
    private const val FILE = "downloads.json"
    private const val MAX = 100
    private lateinit var ctx: Context

    fun init(context: Context) {
        ctx = context.applicationContext
    }

    private fun read(): MutableList<DownloadItem> {
        val list = mutableListOf<DownloadItem>()
        val raw = ctx.getSharedPreferences("files", Context.MODE_PRIVATE).getString(FILE, null)
            ?: return list
        runCatching {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(DownloadItem(o.getString("name"), o.getString("url"), o.getLong("time")))
            }
        }
        return list
    }

    private fun write(list: List<DownloadItem>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("name", it.name).put("url", it.url).put("time", it.time))
        }
        ctx.getSharedPreferences("files", Context.MODE_PRIVATE)
            .edit().putString(FILE, arr.toString()).apply()
    }

    fun all(): List<DownloadItem> = read().reversed()

    fun add(item: DownloadItem) {
        val list = read()
        list.add(item)
        while (list.size > MAX) list.removeAt(0)
        write(list)
    }
}
