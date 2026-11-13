package com.universalrp.appdisabler.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object BackupManager {

    fun exportDisabled(apps: List<AppInfo>): String {
        val arr = JSONArray()
        apps.filter { !it.isEnabled }.forEach { arr.put(it.packageName) }
        val obj = JSONObject()
        obj.put("version", 1)
        obj.put("created", System.currentTimeMillis())
        obj.put("app", "System App Disabler")
        obj.put("count", arr.length())
        obj.put("packages", arr)
        return obj.toString(2)
    }

    fun importPackages(json: String): List<String> {
        return try {
            val obj = JSONObject(json)
            val arr = obj.optJSONArray("packages") ?: JSONArray(json)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (_: Exception) {
            // fallback: line-separated
            json.lines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }
        }
    }

    fun saveToFile(context: Context, json: String, filename: String = "disabled-backup-${System.currentTimeMillis()}.json"): java.io.File {
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val file = java.io.File(dir, filename)
        file.writeText(json)
        return file
    }
}
