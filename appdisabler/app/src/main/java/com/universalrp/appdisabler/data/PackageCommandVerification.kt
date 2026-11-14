package com.universalrp.appdisabler.data

/** Parse only an exact package state reported by the package manager; null means unknown. */
internal object PackageCommandVerification {
    fun matchesSuspension(exit: Int, liveState: Boolean?, output: String, packageName: String, expected: Boolean): Boolean {
        if (exit != 0) return false
        return (liveState ?: suspendedState(output, packageName)) == expected
    }

    fun suspendedState(output: String, packageName: String): Boolean? {
        val prefix = "Package $packageName new suspended state: "
        val answers = output.lineSequence().map { it.trim() }
            .filter { it.startsWith(prefix) }
            .map { it.removePrefix(prefix) }.toList()
        if (answers.size != 1) return null
        return when (answers.single()) {
            "true" -> true
            "false" -> false
            else -> null
        }
    }
}
