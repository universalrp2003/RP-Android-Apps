package com.universalrp.tvguard.web

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** Accept one existing file name, never a path. Upload names are sanitized separately. */
internal fun existingHubFileName(raw: String): String? = raw.takeIf {
    it.isNotBlank() && it != "." && it != ".." && '/' !in it && '\u0000' !in it
}

internal fun htmlEscape(value: String): String = buildString(value.length) {
    value.forEach { character ->
        append(when (character) {
            '&' -> "&amp;"
            '<' -> "&lt;"
            '>' -> "&gt;"
            '"' -> "&quot;"
            '\'' -> "&#39;"
            else -> character.toString()
        })
    }
}

/** URL query and RFC 5987 filename* value; no raw HTML/attribute delimiters. */
internal fun hubUrlComponent(value: String): String =
    URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20").replace("*", "%2A")
