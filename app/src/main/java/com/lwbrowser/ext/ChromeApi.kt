package com.lwbrowser.ext

import android.webkit.JavascriptInterface
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ChromeApi(
    private val extDir: File,
    private val onOpenUrl: (String) -> Unit,
    private val onQueryTabs: () -> String,
    private val onActiveTabUrl: () -> String
) {
    @JavascriptInterface
    fun storageLocalGet(extId: String, keys: String): String {
        val data = loadStorage(extId)
        val keysArray = parseKeys(keys)
        if (keysArray.isEmpty()) return data.toString()
        val result = JSONObject()
        for (key in keysArray) {
            if (data.has(key)) result.put(key, data.get(key))
        }
        return result.toString()
    }

    @JavascriptInterface
    fun storageLocalSet(extId: String, items: String): Boolean {
        val data = loadStorage(extId)
        val itemsObj = JSONObject(items)
        for (key in itemsObj.keys()) data.put(key, itemsObj.get(key))
        saveStorage(extId, data)
        return true
    }

    @JavascriptInterface
    fun storageLocalRemove(extId: String, keys: String): Boolean {
        val data = loadStorage(extId)
        for (key in parseKeys(keys)) data.remove(key)
        saveStorage(extId, data)
        return true
    }

    @JavascriptInterface
    fun storageLocalClear(extId: String): Boolean {
        saveStorage(extId, JSONObject())
        return true
    }

    @JavascriptInterface
    fun runtimeGetURL(extId: String, path: String): String {
        val cleanPath = if (path.startsWith("/")) path.substring(1) else path
        return "file://${extDir.absolutePath}/$extId/$cleanPath"
    }

    @JavascriptInterface
    fun runtimeGetManifest(extId: String): String {
        val manifestFile = File(File(extDir, extId), "manifest.json")
        return if (manifestFile.exists()) manifestFile.readText() else "{}"
    }

    @JavascriptInterface
    fun runtimeGetId(extId: String): String = extId

    @JavascriptInterface
    fun runtimeSendMessage(extId: String, message: String): Boolean = true

    @JavascriptInterface
    fun runtimeConnect(extId: String, name: String): String = "port_$extId"

    @JavascriptInterface
    fun tabsCreate(extId: String, url: String): Boolean {
        onOpenUrl(url)
        return true
    }

    @JavascriptInterface
    fun tabsUpdate(extId: String, tabId: Int, url: String): Boolean {
        onOpenUrl(url)
        return true
    }

    @JavascriptInterface
    fun tabsQuery(extId: String, queryInfo: String): String = onQueryTabs()

    @JavascriptInterface
    fun tabsGetActiveUrl(extId: String): String = onActiveTabUrl()

    @JavascriptInterface
    fun i18nGetMessage(extId: String, messageName: String): String {
        val localeDir = File(File(extDir, extId), "_locales")
        if (!localeDir.exists()) return messageName
        val messagesFile = findMessagesFile(localeDir) ?: return messageName
        return parseMessage(messagesFile.readText(), messageName)
    }

    @JavascriptInterface
    fun cookiesGetAll(extId: String, domain: String): String {
        val cookieManager = android.webkit.CookieManager.getInstance()
        val cookies = cookieManager.getCookie(domain) ?: return "[]"
        val arr = JSONArray()
        for (cookie in cookies.split(";")) {
            val parts = cookie.trim().split("=", limit = 2)
            if (parts.size == 2) {
                val obj = JSONObject()
                obj.put("name", parts[0].trim())
                obj.put("value", parts[1].trim())
                obj.put("domain", domain)
                arr.put(obj)
            }
        }
        return arr.toString()
    }

    @JavascriptInterface
    fun cookiesRemove(extId: String, url: String, name: String): Boolean = true

    private fun findMessagesFile(localeDir: File): File? {
        val enDir = File(localeDir, "en")
        if (enDir.exists()) {
            val f = File(enDir, "messages.json")
            if (f.exists()) return f
        }
        val dirs = localeDir.listFiles { f -> f.isDirectory } ?: return null
        if (dirs.isNotEmpty()) {
            val f = File(dirs[0], "messages.json")
            if (f.exists()) return f
        }
        return null
    }

    private fun parseMessage(messagesJson: String, name: String): String {
        return try {
            val messages = JSONObject(messagesJson)
            if (messages.has(name)) messages.getJSONObject(name).optString("message", name) else name
        } catch (e: Exception) { name }
    }

    private fun storageFile(extId: String): File {
        val dir = File(File(extDir, extId), "storage")
        dir.mkdirs()
        return File(dir, "local.json")
    }

    private fun loadStorage(extId: String): JSONObject {
        val f = storageFile(extId)
        return if (f.exists()) {
            try { JSONObject(f.readText()) } catch (e: Exception) { JSONObject() }
        } else JSONObject()
    }

    private fun saveStorage(extId: String, data: JSONObject) {
        storageFile(extId).writeText(data.toString())
    }

    private fun parseKeys(keys: String): List<String> {
        if (keys.isEmpty() || keys == "null") return emptyList()
        return try {
            val arr = JSONArray(keys)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (e: Exception) { listOf(keys) }
    }
}
