package com.universalrp.animalvoice

/** The model is a generic sound classifier, not a species, intention or emotion translator. */
data class SoundScore(val label: String, val score: Float)
data class SoundGuess(val label: String, val tamil: String, val score: Float)

object Interpretation {
    // AudioSet class names only; do not reinterpret a generic chirp as a gecko or crow.
    private val names = mapOf(
        "Animal" to "விலங்கின் ஒலி", "Domestic animals, pets" to "வீட்டு விலங்கு",
        "Dog" to "நாய்", "Bark" to "நாய் குரைத்தல்", "Yip" to "நாய் குறுங்குரல்",
        "Howl" to "ஊளையிடுதல்", "Bow-wow" to "நாய் குரல்", "Growling" to "உறுமல்", "Whimper (dog)" to "நாயின் மெல்லிய குரல்",
        "Cat" to "பூனை", "Purr" to "பூனை முணுமுணுப்பு", "Meow" to "பூனை குரல்", "Caterwaul" to "பூனையின் உரத்த குரல்",
        "Livestock, farm animals, working animals" to "பண்ணை விலங்கு", "Horse" to "குதிரை", "Neigh, whinny" to "குதிரைக் குரல்",
        "Cattle, bovinae" to "மாடு", "Moo" to "மாட்டுக் குரல்", "Pig" to "பன்றி", "Oink" to "பன்றிக் குரல்",
        "Goat" to "ஆடு", "Bleat" to "ஆட்டுக் குரல்", "Sheep" to "செம்மறி ஆடு", "Fowl" to "கோழி வகை",
        "Chicken, rooster" to "கோழி / சேவல்", "Cluck" to "கோழிக் குரல்", "Crowing, cock-a-doodle-doo" to "சேவல் கூவல்",
        "Duck" to "வாத்து", "Quack" to "வாத்துக் குரல்", "Goose" to "வாத்து வகை",
        "Bird" to "பறவை", "Bird vocalization, bird call, bird song" to "பறவைக் குரல்", "Chirp, tweet" to "சிறு பறவைக் குரல்",
        "Squawk" to "பறவையின் உரத்த குரல்", "Pigeon, dove" to "புறா", "Coo" to "புறாக் குரல்", "Crow" to "காகம்", "Caw" to "காகக் குரல்",
        "Owl" to "ஆந்தை", "Hoot" to "ஆந்தைக் குரல்", "Rodents, rats, mice" to "எலி வகை", "Mouse" to "சிறிய எலி",
        "Insect" to "பூச்சி", "Cricket" to "சிள்வண்டு", "Mosquito" to "கொசு", "Fly, housefly" to "ஈ", "Bee, wasp, etc." to "தேனீ வகை",
        "Frog" to "தவளை", "Croak" to "கரகரப்பான குரல்", "Snake" to "பாம்பு", "Rattle" to "சலசலப்பு",
        "Wild animals" to "காட்டு விலங்கு", "Roaring cats (lions, tigers)" to "புலி / சிங்கம் வகை", "Roar" to "உரத்த கர்ஜனை",
        "Whale vocalization" to "திமிங்கல ஒலி"
    )
    // This is a conservative display gate, NOT a probability, scientific calibration or proof.
    const val DISPLAY_GATE = 0.25f
    fun choose(scores: List<SoundScore>, rms: Double, selected: Set<String>? = null): List<SoundGuess> {
        if (!rms.isFinite() || rms < 0.003) return emptyList()
        val valid = scores.filter { it.score.isFinite() && it.score in 0f..1f }
        val bestAnimal = valid.filter { it.label in names && (selected == null || it.label in selected) && it.score >= DISPLAY_GATE }.sortedByDescending { it.score }
        if (bestAnimal.isEmpty()) return emptyList()
        val bestOther = valid.filterNot { it.label in names && (selected == null || it.label in selected) }.maxOfOrNull { it.score } ?: 0f
        if (bestOther >= 0.45f && bestOther > bestAnimal.first().score) return emptyList()
        return bestAnimal.take(3).map { SoundGuess(it.label, names.getValue(it.label), it.score) }
    }
}
