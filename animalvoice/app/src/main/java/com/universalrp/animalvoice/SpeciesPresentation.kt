package com.universalrp.animalvoice

/** Conservative display hints; no scores are changed and no other result is removed. */
object SpeciesPresentation {
    private val names = mapOf(
        "corvus splendens" to "வீட்டுக் காகம்",
        "felis catus" to "வீட்டுப் பூனை",
        "bos taurus" to "வீட்டு மாடு",
        "hemidactylus frenatus" to "வீட்டுப் பல்லி"
    )
    fun display(scientific: String, common: String, tamil: Boolean): String {
        val prefix = if (tamil) names[scientific.trim().lowercase()] ?: common else common
        return if (scientific.isBlank()) prefix else "$prefix ($scientific)"
    }
    fun focusMatches(option: String, scientific: String): Boolean? {
        val name = scientific.trim().lowercase()
        return when (option) {
            "crow" -> name.startsWith("corvus ")
            "cat" -> name == "felis catus"
            "cow" -> name == "bos taurus"
            "lizard" -> name == "hemidactylus frenatus"
            else -> null // Other animal groups have not been mapped; don't imply exclusion.
        }
    }
}
