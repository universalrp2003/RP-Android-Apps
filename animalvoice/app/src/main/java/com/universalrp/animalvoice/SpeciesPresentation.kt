package com.universalrp.animalvoice

/** Conservative display hints; no scores are changed and no other result is removed. */
object SpeciesPresentation {
    private val names = mapOf(
        "corvus splendens" to "வீட்டுக் காகம்",
        "felis catus" to "வீட்டுப் பூனை",
        "bos taurus" to "வீட்டு மாடு",
        "hemidactylus frenatus" to "வீட்டுப் பல்லி",
        "lithobates sylvaticus" to "மரக் காட்டுத் தவளை",
        "rana sylvatica" to "மரக் காட்டுத் தவளை"
    )
    fun display(scientific: String, common: String, tamil: Boolean): String {
        val prefix = if (tamil) names[scientific.trim().lowercase()] ?: common else common
        return if (scientific.isBlank()) prefix else "$prefix ($scientific)"
    }
    /** Species facts are curated descriptions, not AI judgments about this particular call. */
    fun description(scientific: String, tamil: Boolean): String? {
        val pair = when (scientific.trim().lowercase()) {
            "corvus splendens" -> "வீட்டுக் காகம் மனிதர் வாழும் பகுதிகளில் பொதுவாகக் காணப்படும். அதன் குரல் கரகரப்பான ‘கா கா’ என இருக்கலாம். இந்த ஒலியின் நோக்கம் தெரியாது." to "House crows often live near people. Their calls can sound like a harsh, repeated caw. This recording does not reveal the bird's intention."
            "felis catus" -> "வீட்டுப் பூனை பலவித ஒலிகளை எழுப்பும். ஒலியை மட்டும் வைத்து அதன் தேவையை உறுதியாக அறிய முடியாது." to "Domestic cats make several kinds of sounds. A sound alone cannot establish what this cat needs."
            "bos taurus" -> "வீட்டு மாடுகள் பலவித குரல்களை எழுப்பும். ஒலியை மட்டும் வைத்து அதன் தேவையை உறுதியாக அறிய முடியாது." to "Domestic cattle make a range of calls. A sound alone cannot establish what this animal needs."
            "hemidactylus frenatus" -> "வீட்டுப் பல்லி மனிதர் வாழும் இடங்களுக்கு அருகில் காணப்படும்; சில பல்லிகள் ஒலி எழுப்பும். இந்த முடிவு உறுதியான அடையாளம் அல்ல." to "Asian house geckos live near buildings, and some geckos vocalize. This guess does not confirm the animal."
            else -> return null
        }
        return if (tamil) pair.first else pair.second
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

    /** Publisher label-table group for a small badge; display only, never a filter. Unknown groups show nothing. */
    fun taxonGroup(raw: String, tamil: Boolean): String? {
        return when (raw.trim().lowercase()) {
            "aves" -> if (tamil) "பறவை" else "Bird"
            "insecta" -> if (tamil) "பூச்சி" else "Insect"
            "amphibia" -> if (tamil) "நீர்நில வாழ்வன்" else "Amphibian"
            "mammalia" -> if (tamil) "பாலூட்டி" else "Mammal"
            "actinopterygii" -> if (tamil) "மீன்" else "Fish"
            "reptilia" -> if (tamil) "ஊர்வன" else "Reptile"
            "arachnida" -> if (tamil) "சிலந்தி வகை" else "Arachnid"
            "anthropogenic" -> if (tamil) "மனிதரால் உருவான ஒலி" else "Human-made sound"
            "geophony" -> if (tamil) "இயற்கையான ஒலி (விலங்கு அல்ல)" else "Natural sound, not an animal"
            "music" -> if (tamil) "இசை" else "Music"
            else -> null
        }
    }

    /** Human speech is a genuine model output: explain it, never hide or demote it. */
    fun speechNote(scientific: String, tamil: Boolean): String? {
        return if (scientific.trim().equals("Homo sapiens", true)) {
            if (tamil) "மனிதப் பேச்சு கேட்கப்பட்டது. அருகில் பேசும் நபர்கள் இருக்கலாம்; அதே பதிவில் உள்ள மெல்லிய விலங்கு ஒலிகள் மறைக்கப்படலாம்."
            else "Human speech detected. People talking nearby may be masking quieter animal calls in the same recording."
        } else null
    }

    /** A transparent caveat, never a filter: the model can be wrong anywhere. */
    fun geographicCaveat(scientific: String, tamil: Boolean): String? {
        return when (scientific.trim().lowercase()) {
            "lithobates sylvaticus", "rana sylvatica" -> if (tamil) {
                "மரக் காட்டுத் தவளை வட அமெரிக்காவின் பூர்வீக உயிரினம். தமிழ்நாட்டில் இது தவறான ஊகமாக இருக்கலாம்; இதே ஒலியை எழுப்பும் உள்ளூர் தவளையும் இருக்கலாம். இடம் பயன்படுத்தப்படவில்லை."
            } else {
                "Wood frogs are native to North America. A guess in Tamil Nadu may be a false positive or a similar-sounding local frog. No location filter is used."
            }
            else -> null
        }
    }
}
