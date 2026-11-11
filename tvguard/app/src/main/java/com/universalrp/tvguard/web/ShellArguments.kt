package com.universalrp.tvguard.web

/** Encode one POSIX shell argument without allowing substitutions or extra commands. */
internal fun shellQuote(value: String): String = "'" + value.replace("'", "'\\''") + "'"
