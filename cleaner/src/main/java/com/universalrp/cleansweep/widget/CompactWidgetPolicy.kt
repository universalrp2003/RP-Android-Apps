package com.universalrp.cleansweep.widget

object CompactWidgetPolicy {
    /**
     * 2.53 — the three widget views, and what each is for:
     * BATTERY = battery details plus the plan, DATA = plan data / days / today, PHONE = security.
     * `next()` walks them in that order for the small "View" button on the card.
     */
    enum class Profile { BATTERY, DATA, PHONE;
        fun next(): Profile = entries[(ordinal + 1) % entries.size]
    }
    fun defaultFor(component: String): Profile = when (component.substringAfterLast('.')) {
        "CleanSweepDataWidget" -> Profile.DATA
        "CleanSweepPhoneWidget" -> Profile.PHONE
        else -> Profile.BATTERY
    }
    fun parse(value: String?, default: Profile = Profile.BATTERY): Profile = Profile.entries.firstOrNull { it.name == value } ?: default
    fun meteredData(wifi: Boolean, userSetUnlimited5g: Boolean): Boolean = !wifi && !userSetUnlimited5g
    fun wholePercent(value: Int?): String = value?.takeIf { it in 0..100 }?.let { "$it%" } ?: "— %"
}
