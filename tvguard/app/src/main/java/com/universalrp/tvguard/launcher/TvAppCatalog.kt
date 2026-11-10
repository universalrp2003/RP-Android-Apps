package com.universalrp.tvguard.launcher

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

data class LaunchableApp(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val isFavorite: Boolean
)

object TvAppCatalog {

    private const val PREFS_NAME = "tvguard_launcher_prefs"
    private const val KEY_FAVORITES = "favorite_packages"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getFavorites(context: Context): Set<String> {
        return getPrefs(context).getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
    }

    fun toggleFavorite(context: Context, packageName: String): Boolean {
        val current = getFavorites(context).toMutableSet()
        val isNowFav = if (current.contains(packageName)) {
            current.remove(packageName)
            false
        } else {
            current.add(packageName)
            true
        }
        getPrefs(context).edit().putStringSet(KEY_FAVORITES, current).apply()
        return isNowFav
    }

    fun loadApps(context: Context): List<LaunchableApp> {
        val pm = context.packageManager
        val favorites = getFavorites(context)
        val appMap = mutableMapOf<String, LaunchableApp>()

        // 1. Query TV Leanback Launcher apps
        val tvIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        }
        val tvResolve = pm.queryIntentActivities(tvIntent, 0)
        for (ri in tvResolve) {
            val pkg = ri.activityInfo.packageName
            if (pkg == context.packageName) continue
            val label = ri.loadLabel(pm).toString()
            val icon = ri.loadIcon(pm)
            appMap[pkg] = LaunchableApp(pkg, label, icon, favorites.contains(pkg))
        }

        // 2. Query Standard Launcher apps (to show sideloaded apps that lack leanback banner)
        val standardIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val stdResolve = pm.queryIntentActivities(standardIntent, 0)
        for (ri in stdResolve) {
            val pkg = ri.activityInfo.packageName
            if (pkg == context.packageName) continue
            if (!appMap.containsKey(pkg)) {
                val label = ri.loadLabel(pm).toString()
                val icon = ri.loadIcon(pm)
                appMap[pkg] = LaunchableApp(pkg, label, icon, favorites.contains(pkg))
            }
        }

        return appMap.values.sortedWith(
            compareByDescending<LaunchableApp> { it.isFavorite }
                .thenBy { it.appName.lowercase() }
        )
    }

    fun launchApp(context: Context, packageName: String) {
        val pm = context.packageManager
        val intent = pm.getLeanbackLaunchIntentForPackage(packageName)
            ?: pm.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
