package com.universalrp.animalvoice

/** A user choice filters the same generic classifier. It never establishes the animal's identity. */
data class AnimalOption(val id: String, val english: String, val tamil: String, val labels: Set<String>?, val supported: Boolean = true)
object AnimalOptions {
    val all = listOf(
        AnimalOption("all", "All supported sounds", "அனைத்து அறியக்கூடிய ஒலிகள்", null),
        AnimalOption("dog", "Dog", "நாய்", setOf("Dog", "Bark", "Yip", "Howl", "Bow-wow", "Growling", "Whimper (dog)")),
        AnimalOption("cat", "Cat", "பூனை", setOf("Cat", "Purr", "Meow", "Caterwaul")),
        AnimalOption("cow", "Cow / cattle", "மாடு", setOf("Cattle, bovinae", "Moo")),
        AnimalOption("crow", "Crow", "காகம்", setOf("Crow", "Caw")),
        AnimalOption("bird", "Bird (general)", "பறவை (பொது)", setOf("Bird", "Bird vocalization, bird call, bird song", "Chirp, tweet", "Squawk", "Pigeon, dove", "Coo", "Crow", "Caw", "Owl", "Hoot")),
        AnimalOption("pigeon", "Pigeon / dove", "புறா", setOf("Pigeon, dove", "Coo")),
        AnimalOption("owl", "Owl", "ஆந்தை", setOf("Owl", "Hoot")),
        AnimalOption("rodent", "Rat / mouse (audible sounds)", "எலி (கேட்கும் ஒலிகள்)", setOf("Rodents, rats, mice", "Mouse")),
        AnimalOption("horse", "Horse", "குதிரை", setOf("Horse", "Neigh, whinny")),
        AnimalOption("goat", "Goat / sheep", "ஆடு / செம்மறி ஆடு", setOf("Goat", "Bleat", "Sheep")),
        AnimalOption("pig", "Pig", "பன்றி", setOf("Pig", "Oink")),
        AnimalOption("chicken", "Chicken / rooster", "கோழி / சேவல்", setOf("Chicken, rooster", "Cluck", "Crowing, cock-a-doodle-doo")),
        AnimalOption("duck", "Duck / goose", "வாத்து", setOf("Duck", "Quack", "Goose")),
        AnimalOption("insect", "Insect", "பூச்சி", setOf("Insect", "Cricket", "Mosquito", "Fly, housefly", "Bee, wasp, etc.")),
        AnimalOption("frog", "Frog", "தவளை", setOf("Frog", "Croak")),
        AnimalOption("snake", "Snake", "பாம்பு", setOf("Snake", "Rattle")),
        AnimalOption("bigcat", "Lion / tiger (broad)", "சிங்கம் / புலி (பொது)", setOf("Roaring cats (lions, tigers)", "Roar")),
        AnimalOption("whale", "Whale sound", "திமிங்கல ஒலி", setOf("Whale vocalization")),
        AnimalOption("lizard", "Lizard / gecko (new species model)", "பல்லி (புதிய உயிரின மாடல்)", emptySet(), false)
    )
}
