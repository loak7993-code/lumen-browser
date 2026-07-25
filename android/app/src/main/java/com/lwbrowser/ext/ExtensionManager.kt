package com.lwbrowser.ext

import android.content.Context
import org.json.JSONObject
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

    fun install(context: Context, xpiUri: android.net.Uri): Extension? {
        val id = "ext_" + System.currentTimeMillis()
        val target = File(extDir, id).apply { mkdirs() }
        try {
            context.contentResolver.openInputStream(xpiUri)?.use { input ->
                ZipInputStream(input).use { zis ->
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

    private fun findManifest(root: File): File? {
        val direct = File(root, "manifest.json")
        if (direct.exists()) return direct
        return root.walkTopDown().firstOrNull { it.name == "manifest.json" && it.isFile }
    }

    fun scriptsFor(url: String, runAt: String): List<Extension> {
        return extensions.filter { it.enabled && it.scriptsFor(url, runAt).isNotEmpty() }
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
}
