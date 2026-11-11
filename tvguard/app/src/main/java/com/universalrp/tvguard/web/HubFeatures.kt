package com.universalrp.tvguard.web

/** A disabled control is rejected by the server, even for a direct HTTP request. */
data class HubFeatures(
    val remote: Boolean = false,
    val screenshot: Boolean = false,
    val ramBoost: Boolean = false
) {
    fun allows(path: String): Boolean = when (path) {
        "/api/key", "/api/text", "/api/launch", "/api/install" -> remote
        "/api/screenshot" -> remote && screenshot
        "/api/boost" -> remote && ramBoost
        else -> true
    }
}
