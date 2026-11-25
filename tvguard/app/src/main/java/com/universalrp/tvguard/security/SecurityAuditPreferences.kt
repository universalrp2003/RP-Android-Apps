package com.universalrp.tvguard.security

import android.content.Context

/**
 * The security audit's own small state, separate from launcher data and from the Expert choices
 * so that neither ever clears the other.
 *
 *  - [overlayShield]: opt-in sixth cell of the status pill.
 *  - [lastHomePackage]: the default HOME app recorded at the previous launch, so a swap can be
 *    detected. One package name, nothing else.
 *  - [homeChanged]: set at launch when the HOME app differs from the recorded one; cleared when
 *    the user has seen the audit.
 */
class SecurityAuditPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    var overlayShield: Boolean
        get() = prefs.getBoolean(KEY_SHIELD, false)
        set(value) = prefs.edit().putBoolean(KEY_SHIELD, value).apply()

    /** Null until the first launch has recorded a HOME app. */
    val lastHomePackage: String?
        get() = if (prefs.contains(KEY_LAST_HOME)) prefs.getString(KEY_LAST_HOME, null) else null

    fun setLastHomePackage(packageName: String?) {
        prefs.edit().apply {
            if (packageName == null) remove(KEY_LAST_HOME) else putString(KEY_LAST_HOME, packageName)
        }.apply()
    }

    val homeChanged: Boolean get() = prefs.getBoolean(KEY_HOME_CHANGED, false)

    fun setHomeChanged(value: Boolean) = prefs.edit().putBoolean(KEY_HOME_CHANGED, value).apply()

    /** The user opened the audit and has seen the change. */
    fun acknowledgeHomeChange() = setHomeChanged(false)

    companion object {
        const val PREFERENCES_NAME = "tvguard_security_audit"
        private const val KEY_SHIELD = "overlay_shield"
        private const val KEY_LAST_HOME = "last_home_package"
        private const val KEY_HOME_CHANGED = "home_changed"
    }
}
