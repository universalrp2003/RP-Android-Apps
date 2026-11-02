package com.universalrp.cleansweep.ui

/** Top-level navigation only. Provider subresources remain website-owned; no app bridge. */
internal object DisasterWebPolicy {
    enum class Provider(val label: String, val home: String) {
        GDACS("GDACS", "https://www.gdacs.org/"),
        IMD("IMD", "https://rsmcnewdelhi.imd.gov.in/"),
        TOMORROW("Tomorrow.io", "https://weather.tomorrow.io/IN/radar/")
    }
    private val hosts = setOf("www.gdacs.org", "gdacs.org", "rsmcnewdelhi.imd.gov.in",
        "sachet.ndma.gov.in", "weather.tomorrow.io", "www.tomorrow.io")
    fun allowed(url: String): Boolean = runCatching {
        val uri = java.net.URI(url)
        uri.scheme == "https" && uri.rawUserInfo == null && uri.port in setOf(-1, 443) && uri.host in hosts
    }.getOrDefault(false)
    fun provider(url: String): Provider = when (java.net.URI(url).host) {
        "rsmcnewdelhi.imd.gov.in" -> Provider.IMD
        "weather.tomorrow.io", "www.tomorrow.io" -> Provider.TOMORROW
        else -> Provider.GDACS
    }
    /** GDACS emits HTTP report links inside its HTTPS home. Upgrade, never load cleartext. */
    fun secureNavigation(url: String): String? = runCatching {
        if (allowed(url)) return@runCatching url
        val u = java.net.URI(url)
        if (u.scheme != "http" || u.rawUserInfo != null || u.port !in setOf(-1, 80) ||
            u.host !in setOf("www.gdacs.org", "gdacs.org")) return@runCatching null
        ("https://" + u.host + (u.rawPath ?: "") +
            (u.rawQuery?.let { "?$it" } ?: "") + (u.rawFragment?.let { "#$it" } ?: ""))
            .takeIf(::allowed)
    }.getOrNull()
    // Public provider directory entry verified 2 Nov 2026, NOT a geocoded guess or live GPS link.
    // Only this explicitly verified town is linked; other places stay an honestly labelled country page.
    const val ALANGUDI_RADAR = "https://weather.tomorrow.io/IN/TN/%C4%80langudi/134455/radar/"
    fun tomorrowForPlace(place: com.universalrp.weather.WeatherPlaceCandidate?): String {
        if (place == null) return Provider.TOMORROW.home
        val name = place.name.trim().lowercase(java.util.Locale.ROOT).substringBefore(',').trim()
        val townName = name in setOf("alangudi", "ālangudi", "ஆலங்குடி")
        // Disambiguate from similarly named villages around Lalgudi and elsewhere. Never alter home.
        val townArea = place.latitude in 10.30..10.42 && place.longitude in 78.90..79.05
        return if (townName && townArea) ALANGUDI_RADAR else Provider.TOMORROW.home
    }

    // WebView.setInitialScale ignores display density; onScaleChanged includes it.
    fun initialScale(percent: Int, density: Float): Int = (savedPageZoom(percent) * density).toInt().coerceAtLeast(1)
    val pageZoomChoices = listOf(50, 75, 100, 125, 150, 200)
    fun savedPageZoom(value: Int): Int = value.coerceIn(25, 500)
}

internal fun disasterWebAllowed(url: String): Boolean = DisasterWebPolicy.allowed(url)
