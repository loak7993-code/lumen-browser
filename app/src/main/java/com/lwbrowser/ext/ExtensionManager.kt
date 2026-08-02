package com.lwbrowser.ext

import android.content.Context
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

object ExtensionManager {

    private lateinit var extDir: File
    private lateinit var prefs: android.content.SharedPreferences
    private val extensions = mutableListOf<Extension>()
    private val enabledIds = mutableSetOf<String>()

    fun init(context: Context) {
        extDir = File(context.filesDir, "extensions").apply { mkdirs() }
        prefs = context.applicationContext.getSharedPreferences("lwbrowser_ext_state", Context.MODE_PRIVATE)
        loadEnabled()
        reload()
    }

    private fun loadEnabled() {
        enabledIds.clear()
        // Persist the *disabled* set (the minority case) so that freshly
        // installed extensions default to enabled, but user-disabled ones stay
        // disabled across restarts.
        val disabled = prefs.getStringSet("disabled_ids", emptySet()) ?: emptySet()
        extDir.listFiles { f -> f.isDirectory }?.forEach { dir ->
            val manifest = File(dir, "manifest.json")
            if (manifest.exists() && dir.name !in disabled) {
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
        // Persist the disabled set so disable/enable survives restarts.
        val disabled = extensions.filter { it.id !in enabledIds }.map { it.id }.toSet()
        prefs.edit().putStringSet("disabled_ids", disabled).apply()
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
        // Drop from the persisted disabled set too so we don't leak stale ids.
        val disabled = (prefs.getStringSet("disabled_ids", emptySet()) ?: emptySet()) - id
        prefs.edit().putStringSet("disabled_ids", disabled).apply()
        File(extDir, id).deleteRecursively()
    }

    fun install(context: Context, fileUri: android.net.Uri): Extension? {
        val id = "ext_" + java.util.UUID.randomUUID().toString()
        val target = File(extDir, id).apply { mkdirs() }
        val targetRoot = target.canonicalPath
        try {
            val bytes = context.contentResolver.openInputStream(fileUri)?.use { it.readBytes() }
                ?: return null
            val zipBytes = stripCrxHeader(bytes)
            ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val outFile = File(target, entry.name)
                        // Zip Slip guard: reject entries that escape the extension dir.
                        if (!outFile.canonicalPath.startsWith(targetRoot + File.separator)) {
                            entry = zis.nextEntry
                            continue
                        }
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
            // Reject negative/overflowing lengths before computing the offset.
            if (pubKeyLen < 0 || sigLen < 0) return data
            val offset = 16L + pubKeyLen.toLong() + sigLen.toLong()
            if (offset in 16..data.size.toLong()) {
                return data.copyOfRange(offset.toInt(), data.size)
            }
        } else if (version == 3) {
            val headerLen = bytesToInt(data, 8)
            if (headerLen < 0) return data
            val offset = 12L + headerLen.toLong()
            if (offset in 12..data.size.toLong()) {
                return data.copyOfRange(offset.toInt(), data.size)
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
        val dirRoot = dir.canonicalPath
        val file = File(dir, path)
        // Path traversal guard: reject paths escaping the extension dir.
        if (!file.canonicalPath.startsWith(dirRoot + File.separator) && file.canonicalPath != dirRoot) return null
        if (file.exists() && file.isFile) return file.readText()
        val manifest = findManifest(dir) ?: return null
        val base = manifest.parentFile ?: dir
        val baseRoot = base.canonicalPath
        val resolved = File(base, path)
        if (!resolved.canonicalPath.startsWith(baseRoot + File.separator) && resolved.canonicalPath != baseRoot) return null
        return if (resolved.exists() && resolved.isFile) resolved.readText() else null
    }

    fun loadIconBase64(extId: String, iconPath: String): String? {
        val dir = File(extDir, extId)
        val file = File(dir, iconPath)
        val traversalSafe: (File, String) -> File? = { base, p ->
            val baseRoot = base.canonicalPath
            val f = File(base, p)
            if (f.canonicalPath.startsWith(baseRoot + File.separator) || f.canonicalPath == baseRoot) f else null
        }
        val actualFile = if (file.exists() && file.isFile) file else {
            val manifest = findManifest(dir) ?: return null
            val base = manifest.parentFile ?: dir
            traversalSafe(base, iconPath)?.takeIf { it.exists() && it.isFile } ?: return null
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
