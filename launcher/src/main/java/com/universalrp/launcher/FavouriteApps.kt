package com.universalrp.launcher

import android.content.Context
import org.json.JSONArray

/** Separate from contact favourites and home-role preferences; ordinary updates retain both. */
object FavouriteApps {
    private const val FILE = "launcher_favourite_apps"
    private const val KEY = "packages_v1"
    fun decode(raw: String?): List<String> = runCatching {
        if (raw == null || raw.length > 20_000) return emptyList()
        val array = JSONArray(raw)
        FavouriteAppPolicy.clean((0 until minOf(array.length(), 100)).mapNotNull {
            array.opt(it) as? String
        })
    }.getOrDefault(emptyList())
    fun load(context: Context): List<String> = decode(context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, null))
    private fun save(context: Context, packages: List<String>) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putString(KEY, JSONArray(FavouriteAppPolicy.clean(packages)).toString()).apply()
    }
    fun add(context: Context, packageName: String): Boolean {
        // Picker lists launchable apps only; recheck in case the app was uninstalled meanwhile.
        if (context.packageManager.getLaunchIntentForPackage(packageName) == null) return false
        val current = load(context)
        val next = FavouriteAppPolicy.add(current, packageName)
        if (next == current) return false
        save(context, next)
        return true
    }
    fun remove(context: Context, packageName: String) = save(context, load(context).filterNot { it == packageName })
}
