package com.lwbrowser.ext

import android.content.Context
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

object ExtensionManager {

    private lateinit var extDir: File
    private val extensions = mutableListOf<Extension>()
    private val enabledIds = mutableSetOf<String>()

    fun init(context: Context) {
        extDir = File(context.filesDir, "extensions").apply { mkdirs() }
        loadEnabled()
        reload()
    }

    private fun loadEnabled() {
        val prefs = extDir.parentFile?.parentFile?.let {
            File(it, "shared_prefs/lwbrowser_prefs.xml")
        }
        enabledIds.clear()
        extDir.listFiles { f -> f.isDirectory }?.forEach { dir ->
            val manifest = File(dir, "manifest.json")
            if (manifest.exists()) {
                enabledIds.add(dir.name)
            }
        }
    }

    fun reload() {
        extensions.clear()
        extDir.listFiles { f -> f.isDirectory }?.forEach { dir ->
            val manifest = File(dir, "manifest.json")
            if (manifest.exists()) {
                runCatching {
                    val json = manifest.readText()
                    val ext = Extension.fromManifest(dir.name, json, enabled = enabledIds.contains(dir.name))
                    extensions.add(ext)
                }
            }
        }
    }

    fun all(): List<Extension> = extensions.toList()

    fun isEnabled(id: String): Boolean = enabledIds.contains(id)

    fun setEnabled(id: String, enabled: Boolean) {
        if (enabled) enabledIds.add(id) else enabledIds.remove(id)
        extensions.indexOfFirst { it.id == id }.let { idx ->
            if (idx >= 0) {
                val e = extensions[idx]
                extensions[idx] = e.copy(enabled = enabled)
            }
        }
    }

    fun uninstall(id: String) {
        extensions.removeAll { it.id == id }
        enabledIds.remove(id)
        File(extDir, id).deleteRecursively()
    }

    fun install(context: Context, fileUri: android.net.Uri): Extension? {
        val id = "ext_" + System.currentTimeMillis()
        val target = File(extDir, id).apply { mkdirs() }
        try {
            val bytes = context.contentResolver.openInputStream(fileUri)?.use { it.readBytes() }
                ?: return null
            val zipBytes = stripCrxHeader(bytes)
            ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val outFile = File(target, entry.name)
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            target.deleteRecursively()
            return null
        }

        val manifest = findManifest(target)
        if (manifest == null) {
            target.deleteRecursively()
            return null
        }

        return runCatching {
            val ext = Extension.fromManifest(id, manifest.readText(), enabled = true)
            extensions.add(ext)
            enabledIds.add(id)
            ext
        }.getOrNull()
    }

    private fun stripCrxHeader(data: ByteArray): ByteArray {
        if (data.size < 4) return data
        val magic = String(data, 0, 4, Charsets.US_ASCII)
        if (magic != "Cr24") return data
        if (data.size < 16) return data
        val version = bytesToInt(data, 4)
        if (version == 2) {
            val pubKeyLen = bytesToInt(data, 8)
            val sigLen = bytesToInt(data, 12)
            val offset = 16 + pubKeyLen + sigLen
            if (offset in 16..data.size) {
                return data.copyOfRange(offset, data.size)
            }
        } else if (version == 3) {
            val headerLen = bytesToInt(data, 8)
            val offset = 12 + headerLen
            if (offset in 12..data.size) {
                return data.copyOfRange(offset, data.size)
            }
        }
        return data
    }

    private fun bytesToInt(data: ByteArray, offset: Int): Int {
        return (data[offset].toInt() and 0xFF) or
            ((data[offset + 1].toInt() and 0xFF) shl 8) or
            ((data[offset + 2].toInt() and 0xFF) shl 16) or
            ((data[offset + 3].toInt() and 0xFF) shl 24)
    }

    private fun findManifest(root: File): File? {
        val direct = File(root, "manifest.json")
        if (direct.exists()) return direct
        return root.walkTopDown().firstOrNull { it.name == "manifest.json" && it.isFile }
    }

    fun scriptsFor(url: String, runAt: String): List<Extension> {
        return extensions.filter { it.enabled && it.scriptsFor(url, runAt).isNotEmpty() }
    }

    fun backgroundScripts(): List<Extension> {
        return extensions.filter { it.enabled && it.background != null }
    }

    fun optionsPage(extId: String): String? {
        val ext = extensions.find { it.id == extId } ?: return null
        return ext.optionsPage?.page
    }

    fun loadFile(extId: String, path: String): String? {
        val dir = File(extDir, extId)
        val file = File(dir, path)
        if (file.exists() && file.isFile) return file.readText()
        val manifest = findManifest(dir) ?: return null
        val base = manifest.parentFile ?: dir
        val resolved = File(base, path)
        return if (resolved.exists() && resolved.isFile) resolved.readText() else null
    }

    fun loadIconBase64(extId: String, iconPath: String): String? {
        val dir = File(extDir, extId)
        val file = File(dir, iconPath)
        val actualFile = if (file.exists() && file.isFile) file else {
            val manifest = findManifest(dir) ?: return null
            val base = manifest.parentFile ?: dir
            val resolved = File(base, iconPath)
            if (resolved.exists() && resolved.isFile) resolved else return null
        }
        val mime = when {
            iconPath.endsWith(".png") -> "png"
            iconPath.endsWith(".jpg") || iconPath.endsWith(".jpeg") -> "jpeg"
            iconPath.endsWith(".svg") -> "svg+xml"
            iconPath.endsWith(".gif") -> "gif"
            iconPath.endsWith(".webp") -> "webp"
            else -> "png"
        }
        val bytes = actualFile.readBytes()
        return "data:image/$mime;base64,${android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)}"
    }

    fun buildChromeApiShim(extId: String): String {
        return """
        (function() {
            var _cb = window._lumenChromeNative;
            var _id = "$extId";
            if (!window.chrome) window.chrome = {};
            if (!chrome.runtime) chrome.runtime = {};
            if (!chrome.storage) chrome.storage = {};
            if (!chrome.storage.local) chrome.storage.local = {};
            if (!chrome.tabs) chrome.tabs = {};
            if (!chrome.i18n) chrome.i18n = {};
            if (!chrome.cookies) chrome.cookies = {};

            function _cbResult(method, args) {
                try { return _cb[method] ? _cb[method].apply(_cb, [_id].concat(args || [])) : undefined; }
                catch(e) { console.log('chrome API error: ' + e); return undefined; }
            }

            chrome.storage.local.get = function(keys, cb) {
                var result = _cbResult('storageLocalGet', [keys ? (typeof keys === 'object' ? JSON.stringify(keys) : keys) : 'null']);
                var data = result ? JSON.parse(result) : {};
                if (cb) cb(data);
                return data;
            };
            chrome.storage.local.set = function(items, cb) {
                _cbResult('storageLocalSet', [JSON.stringify(items)]);
                if (cb) cb();
            };
            chrome.storage.local.remove = function(keys, cb) {
                _cbResult('storageLocalRemove', [Array.isArray(keys) ? JSON.stringify(keys) : JSON.stringify([keys])]);
                if (cb) cb();
            };
            chrome.storage.local.clear = function(cb) {
                _cbResult('storageLocalClear');
                if (cb) cb();
            };

            chrome.runtime.getURL = function(path) {
                return _cbResult('runtimeGetURL', [path]);
            };
            chrome.runtime.getManifest = function() {
                var m = _cbResult('runtimeGetManifest');
                return m ? JSON.parse(m) : {};
            };
            chrome.runtime.id = _id;
            chrome.runtime.sendMessage = function(msg, cb) {
                _cbResult('runtimeSendMessage', [JSON.stringify(msg)]);
                if (cb) cb({});
            };
            chrome.runtime.connect = function(name) {
                _cbResult('runtimeConnect', [name]);
                return { onMessage: { addListener: function(){} }, postMessage: function(){} };
            };
            chrome.runtime.onMessage = { addListener: function(){} };
            chrome.runtime.onInstalled = { addListener: function(){} };

            chrome.tabs.create = function(props, cb) {
                _cbResult('tabsCreate', [props.url]);
                if (cb) cb({id: 1});
                return 1;
            };
            chrome.tabs.update = function(tabId, props, cb) {
                _cbResult('tabsUpdate', [tabId, props.url]);
                if (cb) cb({id: tabId});
            };
            chrome.tabs.query = function(info, cb) {
                var result = _cbResult('tabsQuery', [JSON.stringify(info)]);
                var tabs = result ? JSON.parse(result) : [];
                if (cb) cb(tabs);
                return tabs;
            };

            chrome.i18n.getMessage = function(name) {
                return _cbResult('i18nGetMessage', [name]);
            };

            chrome.cookies.getAll = function(props, cb) {
                var domain = props.domain || '';
                var result = _cbResult('cookiesGetAll', [domain]);
                var cookies = result ? JSON.parse(result) : [];
                if (cb) cb(cookies);
                return cookies;
            };
            chrome.cookies.remove = function(url, name, cb) {
                _cbResult('cookiesRemove', [url, name]);
                if (cb) cb({});
            };

            window.chrome = chrome;
        })();
        """.trimIndent()
    }
}
