package com.universalrp.cleansweep.widget

import kotlin.math.roundToInt

object CompactWidgetPolicy {
    /**
     * 2.61 — the four widget views cycled by the small "View" / "காட்சி" button on any card:
     * BATTERY = battery details plus the plan,
     * DATA    = plan data / days / today,
     * PHONE   = security review summary,
     * WEATHER = 1-minute rain & 24×7 storm radar summary.
     */
    enum class Profile { BATTERY, DATA, PHONE, WEATHER;
        fun next(): Profile = entries[(ordinal + 1) % entries.size]
    }

    data class WeatherCardText(
        val title: String,
        val hero: String,
        val state: String,
        val details: String,
        val brief: String,
    )

    private const val URGENT_FRESH_WINDOW_MS = 2L * 60L * 60L * 1000L // 2 hours

    fun defaultFor(component: String): Profile = when (component.substringAfterLast('.')) {
        "CleanSweepDataWidget" -> Profile.DATA
        "CleanSweepPhoneWidget" -> Profile.PHONE
        "CleanSweepWeatherWidget" -> Profile.WEATHER
        else -> Profile.BATTERY
    }

    fun targetScreenName(profile: Profile): String = when (profile) {
        Profile.WEATHER -> "WEATHER"
        Profile.DATA -> "MY_PLAN"
        Profile.PHONE -> "SECURITY"
        Profile.BATTERY -> "HEALTH"
    }

    fun parse(value: String?, default: Profile = Profile.BATTERY): Profile =
        Profile.entries.firstOrNull { it.name == value } ?: default

    fun meteredData(wifi: Boolean, userSetUnlimited5g: Boolean): Boolean = !wifi && !userSetUnlimited5g
    fun wholePercent(value: Int?): String = value?.takeIf { it in 0..100 }?.let { "$it%" } ?: "— %"

    fun formatWeatherCard(
        tamil: Boolean,
        enabled: Boolean,
        placeName: String,
        severityName: String,
        minutesUntilRain: Int?,
        temperatureC: Double?,
        windGustKmh: Double,
        sourceName: String,
        lastCheckedAtMs: Long,
        clockLabel: String,
    ): WeatherCardText {
        fun label(en: String, ta: String) = if (tamil) ta else en
        val title = label("Rain & storm", "மழை & புயல்")
        val cleanPlace = placeName.trim()
        val placePrefix = if (cleanPlace.isNotEmpty()) "$cleanPlace · " else ""
        val tempLabel = temperatureC?.takeIf { it.isFinite() }?.let { "${it.roundToInt()}°C" }

        if (!enabled) {
            return WeatherCardText(
                title = title,
                hero = tempLabel ?: "—",
                state = placePrefix + label("Alerts turned off", "எச்சரிக்கை அணைந்துள்ளது"),
                details = label("Tap Open to enable rain & storm alerts", "மழை & புயல் எச்சரிக்கையை இயக்க திற-ஐத் தட்டவும்"),
                brief = label("Zero background checks while switched off", "அணைத்திருக்கும்போது பின்னணி இயக்கம் இல்லை"),
            )
        }

        if (lastCheckedAtMs <= 0L) {
            return WeatherCardText(
                title = title,
                hero = "—",
                state = placePrefix + label("Tap Refresh or Open", "புதுப்பி அல்லது திற-ஐத் தட்டவும்"),
                details = if (cleanPlace.isEmpty()) {
                    label("Set GPS or PIN code inside Open", "திற-இல் ஊர் / PIN குறியீட்டை அமைக்கவும்")
                } else {
                    label("Ready to check 1-min radar", "1-நிமிட ரேடார் சரிபார்க்கத் தயார்")
                },
                brief = label("Normal rain: Quiet Hours · Storm: 24×7", "சாதாரண மழை: அமைதி நேரம் · புயல்: 24×7"),
            )
        }

        val mins = minutesUntilRain?.coerceAtLeast(0)
        val hero: String
        val statusLabel: String
        val brief: String
        when (severityName) {
            "CYCLONE_SEVERE" -> {
                hero = label("Storm!", "புயல்!")
                statusLabel = label("Cyclone / storm warning!", "அவசர புயல் எச்சரிக்கை!")
                brief = label("24×7 urgent safety voice alert active", "24×7 அவசர பாதுகாப்புக் குரல் இயங்குகிறது")
            }
            "VERY_HEAVY_RAIN" -> {
                hero = label("Heavy!", "கனமழை!")
                statusLabel = label("Very heavy rain ahead!", "மிகக் கனமழை எச்சரிக்கை!")
                brief = label("24×7 heavy-rain voice warning active", "24×7 கனமழை பாதுகாப்பு எச்சரிக்கை")
            }
            "RAIN_SOON" -> {
                if (mins != null && mins <= 2) {
                    hero = label("Rain", "மழை")
                    statusLabel = label("Rain starting now", "இப்போது மழை தொடங்குகிறது")
                } else {
                    val shown = mins ?: 30
                    hero = "~${shown}m"
                    statusLabel = label("Rain in ~${shown} min", "~${shown} நிமிடத்தில் மழை")
                }
                brief = label("~30 min heads-up · Quiet Hours respected", "~30 நிமிட முன்னறிவிப்பு · அமைதி நேரத்தில் ஓசை இல்லை")
            }
            else -> {
                hero = tempLabel ?: label("Clear", "அமைதி")
                statusLabel = label("No rain approaching", "மழை அறிகுறி இல்லை")
                brief = label("Staying quiet · Storm watch 24×7", "அமைதியாக உள்ளது · புயல் கண்காணிப்பு 24×7")
            }
        }

        val windRounded = windGustKmh.takeIf { it.isFinite() && it >= 0.5 }?.roundToInt()
        val providerShort = if (sourceName == "TOMORROW_IO_1M") "Tomorrow.io" else label("Radar", "ரேடார்")
        val parts = buildList {
            if (severityName != "CLEAR_OR_NORMAL" && tempLabel != null) add(tempLabel)
            if (windRounded != null) add(label("Wind ${windRounded} km/h", "காற்று ${windRounded} km/h"))
            add(providerShort)
            if (clockLabel.isNotBlank()) add(clockLabel)
        }
        return WeatherCardText(
            title = title,
            hero = hero,
            state = placePrefix + statusLabel,
            details = parts.joinToString(" · "),
            brief = brief,
        )
    }

    fun urgentBriefOverride(
        tamil: Boolean,
        enabled: Boolean,
        placeName: String,
        severityName: String,
        minutesUntilRain: Int?,
        lastCheckedAtMs: Long,
        nowMs: Long,
        clockLabel: String,
    ): String? {
        if (!enabled || severityName == "CLEAR_OR_NORMAL" || severityName.isBlank()) return null
        if (lastCheckedAtMs <= 0L || nowMs - lastCheckedAtMs > URGENT_FRESH_WINDOW_MS) return null
        fun label(en: String, ta: String) = if (tamil) ta else en
        val cleanPlace = placeName.trim()
        val prefix = if (cleanPlace.isNotEmpty()) "$cleanPlace: " else ""
        val alertText = when (severityName) {
            "CYCLONE_SEVERE" -> label("Cyclone alert!", "புயல் எச்சரிக்கை!")
            "VERY_HEAVY_RAIN" -> label("Very heavy rain!", "மிகக் கனமழை!")
            "RAIN_SOON" -> {
                val mins = minutesUntilRain?.coerceAtLeast(0)
                if (mins != null && mins <= 2) label("Rain starting now", "மழை தொடங்குகிறது")
                else label("Rain in ~${mins ?: 30} min", "~${mins ?: 30} நிமிடத்தில் மழை")
            }
            else -> return null
        }
        val suffix = if (clockLabel.isNotBlank()) " · $clockLabel" else ""
        return "⛈ $prefix$alertText$suffix"
    }
}
