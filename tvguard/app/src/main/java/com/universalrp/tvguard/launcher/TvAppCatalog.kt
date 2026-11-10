package com.universalrp.tvguard.launcher

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings

data class LaunchableApp(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val isFavorite: Boolean,
    val isHidden: Boolean = false
)

object TvAppCatalog {

    private const val PREFS_NAME = "tvguard_launcher_prefs"
    private const val KEY_FAVORITES = "favorite_packages"
    private const val KEY_HIDDEN = "hidden_packages"

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

    fun getHiddenApps(context: Context): Set<String> {
        return getPrefs(context).getStringSet(KEY_HIDDEN, emptySet()) ?: emptySet()
    }

    fun toggleHideApp(context: Context, packageName: String): Boolean {
        val current = getHiddenApps(context).toMutableSet()
        val isNowHidden = if (current.contains(packageName)) {
            current.remove(packageName)
            false
        } else {
            current.add(packageName)
            true
        }
        getPrefs(context).edit().putStringSet(KEY_HIDDEN, current).apply()
        return isNowHidden
    }

    fun unhideApp(context: Context, packageName: String) {
        val current = getHiddenApps(context).toMutableSet()
        current.remove(packageName)
        getPrefs(context).edit().putStringSet(KEY_HIDDEN, current).apply()
    }

    fun loadApps(context: Context, includeHidden: Boolean = false): List<LaunchableApp> {
        val pm = context.packageManager
        val favorites = getFavorites(context)
        val hidden = getHiddenApps(context)
        val appMap = mutableMapOf<String, LaunchableApp>()

        // 1. Query TV Leanback Launcher apps
        val tvIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        }
        val tvResolve = pm.queryIntentActivities(tvIntent, 0)
        for (ri in tvResolve) {
            if (Thread.currentThread().isInterrupted) throw InterruptedException()
            val pkg = ri.activityInfo.packageName
            if (pkg == context.packageName) continue
            val label = ri.loadLabel(pm).toString()
            val icon = ri.loadIcon(pm)
            val isFav = favorites.contains(pkg)
            val isHid = hidden.contains(pkg)
            if (includeHidden || !isHid) {
                appMap[pkg] = LaunchableApp(pkg, label, icon, isFav, isHid)
            }
        }

        // 2. Query Standard Launcher apps (to show sideloaded apps that lack leanback banner)
        val standardIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val stdResolve = pm.queryIntentActivities(standardIntent, 0)
        for (ri in stdResolve) {
            if (Thread.currentThread().isInterrupted) throw InterruptedException()
            val pkg = ri.activityInfo.packageName
            if (pkg == context.packageName) continue
            if (!appMap.containsKey(pkg)) {
                val label = ri.loadLabel(pm).toString()
                val icon = ri.loadIcon(pm)
                val isFav = favorites.contains(pkg)
                val isHid = hidden.contains(pkg)
                if (includeHidden || !isHid) {
                    appMap[pkg] = LaunchableApp(pkg, label, icon, isFav, isHid)
                }
            }
        }

        return appMap.values.sortedWith(
            compareByDescending<LaunchableApp> { it.isFavorite }
                .thenBy { it.appName.lowercase() }
        )
    }

    /** Returns false if the package no longer exposes a launchable activity. */
    fun launchApp(context: Context, packageName: String): Boolean {
        val pm = context.packageManager
        val intent = pm.getLeanbackLaunchIntentForPackage(packageName)
            ?: pm.getLaunchIntentForPackage(packageName)
            ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return true
    }

    fun openAppDetails(context: Context, packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Throwable) {
            try {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (_: Throwable) {}
        }
    }
}
