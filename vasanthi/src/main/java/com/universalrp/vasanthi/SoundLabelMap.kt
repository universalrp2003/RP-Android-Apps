package com.universalrp.vasanthi

/**
 * Tamil names for the YAMNet classes that matter to someone who cannot hear.
 *
 * YAMNet predicts 521 classes, all of them named in English because AudioSet was labelled in
 * English. Handing a deaf Tamil reader the string "Hubbub, speech noise, speech babble" would be
 * worse than handing her nothing, so the classes that actually turn up in a home are translated
 * and everything else is held back until it earns a translation.
 *
 * Two rules keep this honest:
 *
 * 1. A label is only ever keyed by its exact AudioSet display name, never by its index. The index
 *    order is an implementation detail of one model file; the display name is the contract. A map
 *    keyed by index silently points at the wrong sound the day the model is updated, and a wrong
 *    sound is worse than an unknown one.
 * 2. A label that has no Tamil entry is never shown as English. The caller drops it. An
 *    untranslated sound name is a guess dressed up as information.
 */
object SoundLabelMap {

    /**
     * Exact AudioSet display name to Tamil. Every key here was read out of the shipped
     * yamnet_labels.txt, so a typo in a key shows up as a permanently silent sound rather than as
     * a wrong sound.
     */
    private val TAMIL: Map<String, String> = mapOf(
        // Someone is talking. The single most useful thing this app can say.
        "Speech" to "பேச்சு",
        "Conversation" to "உரையாடல்",
        "Child speech, kid speaking" to "குழந்தை பேச்சு",
        "Whispering" to "கிசுகிசு",
        "Chatter" to "பேச்சு கலவை",
        "Hubbub, speech noise, speech babble" to "பலர் பேசும் சத்தம்",
        "Narration, monologue" to "ஒருவர் பேசுதல்",
        "Babbling" to "பாப்பல்",
        "Shout" to "கூக்குரல்",
        "Yell" to "கூச்சல்",
        "Children shouting" to "குழந்தைகள் கூவுதல்",
        "Screaming" to "அலறல்",
        "Bellow" to "மொரயல்",
        "Whoop" to "கெஞ்சும் சத்தம்",
        "Groan" to "கதிரல்",
        "Grunt" to "கர்ரு",
        "Sigh" to "மூச்சு விடுதல்",
        "Giggle" to "சிரல்",
        "Snicker" to "பரிகாச சிரிப்பு",
        "Laughter" to "சிரிப்பு",
        "Baby laughter" to "குழந்தை சிரிப்பு",
        "Child singing" to "குழந்தை பாட்டு",
        "Singing" to "பாடல்",
        "Chant" to "மந்திர ஓசை",
        "Beatboxing" to "ரித்மிக் சத்தம்",
        "Speech synthesizer" to "இயந்திர பேச்சு",

        // A person making a sound with their body. She needs these as much as words.
        "Crying, sobbing" to "அழுகை",
        "Baby cry, infant cry" to "குழந்தை அழுகை",
        "Cough" to "இருமல்",
        "Sneeze" to "தும்மல்",
        "Snoring" to "குறுக்கு",
        "Breathing" to "மூச்சு",
        "Wheeze" to "நெஞ்சு சுழல்",
        "Sniff" to "மூக்கு சப்பான்",
        "Hiccup" to "விக்கி",
        "Burping, eructation" to "வாய்ப்பு",
        "Humming" to "கொந்தளிப்பு",
        "Whistling" to "விசில் ஓசை",

        // Feet and hands. Who is moving, and where.
        "Walk, footsteps" to "அடிகள்",
        "Clapping" to "கைதட்டல்",
        "Typing" to "தட்டச்சு",

        // Doors, cupboards and drawers: someone entering or leaving the room.
        "Doorbell" to "கதவு மணி",
        "Door" to "கதவு",
        "Cupboard open or close" to "அலமாரி",
        "Drawer open or close" to "அறை",
        "Slam" to "மோதல்",
        "Squeak" to "சிலிற் ஓசை",
        "Rattle" to "கடகட ஓசை",
        "Knock" to "தட்டு",

        // Things breaking. Loud, sudden, and she will not hear any of it.
        "Shatter" to "கீறல்",
        "Breaking" to "உடைதல்",
        "Glass" to "கண்ணாடி",
        "Smash, crash" to "படிவம்",
        "Bang" to "பொருந்து",
        "Explosion" to "வெடிப்பு",
        "Gunshot, gunfire" to "துப்பாக்கி சத்தம்",
        "Fireworks" to "பட்டாசு",

        // The kitchen. Cooking, water, and the danger of a stove left on.
        "Water" to "தண்ணீர்",
        "Water tap, faucet" to "குழாய் தண்ணீர்",
        "Sink (filling or washing)" to "சிங்க்",
        "Toilet flush" to "கழிப்பு",
        "Dishes, pots, and pans" to "பாத்திரங்கள்",
        "Cutlery, silverware" to "கத்திகள்",
        "Frying (food)" to "வறுதல்",
        "Microwave oven" to "மைக்ரோவேவ் அடுப்பு",
        "Liquid" to "திரவம்",
        "Pour" to "ஒழுகுதல்",
        "Trickle, dribble" to "சொட்டு",
        "Splash, splatter" to "செலவு",
        "Gush" to "பாய்தல்",
        "Stream" to "ஆறு",
        "Waterfall" to "அருவி",

        // Weather, which she can see but not hear.
        "Rain" to "மழை",
        "Thunderstorm" to "இடியுடன் மழை",
        "Wind" to "காற்று",
        "Rustling leaves" to "இலை சலசலப்பு",
        "Rustle" to "சலசலப்பு",
        "Whoosh, swoosh, swish" to "வீசல்",

        // Bells and alarms. The things a deaf person most needs to be told about.
        "Telephone" to "தொலைபேசி",
        "Ringtone" to "ரிங்டோன்",
        "Alarm clock" to "அலாரம் கடிகாரம்",
        "Car alarm" to "கார் அலாரம்",
        "Chime" to "மணி ஓசை",
        "Church bell" to "தேவாலய மணி",
        "Bicycle bell" to "சைக்கிள் மணி",
        "Reversing beeps" to "பின்னோக்கி மணி",
        "Toot" to "ஹார்ன்",
        "Train horn" to "ரயில் ஹார்ன்",
        "Smoke detector, smoke alarm" to "புகை எச்சரிக்கை",
        "Fire engine, fire truck (siren)" to "தீயணைப்பு வாகன சிரன்",
        "Police car (siren)" to "காவல் வாகன சிரன்",
        "Siren" to "சிரன்",
        "Civil defense siren" to "எச்சரிக்கை சிரன்",
        "Train wheels squealing" to "ரயில் சடசட",

        // Traffic outside the window.
        "Vehicle" to "வாகனம்",
        "Car passing by" to "கார் வருதல்",
        "Bus" to "பஸ்",
        "Motorcycle" to "மோட்டார் சைக்கிள்",
        "Bicycle" to "சைக்கிள்",
        "Train" to "ரயில்",
        "Rail transport" to "ரயில் போக்குவரத்து",
        "Railroad car, train wagon" to "ரயில் பெட்டி",
        "Subway, metro, underground" to "மெட்ரோ",
        "Aircraft" to "விமானம்",
        "Fixed-wing aircraft, airplane" to "விமானம்",
        "Sailboat, sailing ship" to "படகு",
        "Rowboat, canoe, kayak" to "தோணி",
        "Honk" to "ஹார்ன் ஒலி",
        "Idling" to "வெற்று ஓட்டம்",
        "Engine knocking" to "எஞ்சின் தட்டு",
        "Engine" to "எஞ்சின்",

        // Animals. A dog at the gate, a bird at the window.
        "Bark" to "நாய் ஓட்டம்",
        "Howl" to "ஊளியது",
        "Growling" to "குரைதல்",
        "Whimper (dog)" to "நாய் களிப்பு",
        "Bow-wow" to "பவ் பவ்",
        "Bird" to "பறவை",
        "Bird vocalization, bird call, bird song" to "பறவை கூவல்",
        "Chirp, tweet" to "சிறுது",
        "Crow" to "காகம்",
        "Caw" to "கா கா",
        "Hoot" to "ஆந்தை ஓசை",
        "Owl" to "ஆந்தை",
        "Pigeon, dove" to "புறா",
        "Chicken, rooster" to "கோழி",
        "Crowing, cock-a-doodle-doo" to "கோழி கூவல்",
        "Cluck" to "கொக் கொக்",
        "Goat" to "வெள்ளாடு",
        "Bleat" to "மேகி ஓசை",
        "Sheep" to "செம்மறி ஆடு",
        "Cattle, bovinae" to "கன்று",
        "Moo" to "ஆம்மா",
        "Horse" to "குதிரை",
        "Neigh, whinny" to "குதிரை கிறை",
        "Pig" to "பன்றி",
        "Oink" to "பன்றி ஓசை",
        "Meow" to "மியாவ்",
        "Purr" to "புருரு",
        "Cat" to "பூனை",
        "Rodents, rats, mice" to "எலி",
        "Mouse" to "எலி",
        "Insect" to "பூச்சி",
        "Buzz" to "உண்ணு",
        "Cricket" to "டிக்கடிக்",
        "Mosquito" to "கொசு",
        "Fly, housefly" to "ஈ",
        "Frog" to "தவளை",
        "Croak" to "தவளை ஓசை",
        "Snake" to "பாம்பு",

        // The room itself.
        "Silence" to "அமைதி",
        "Music" to "இசை",
        "Musical instrument" to "இசைக்கருவி",
        "Guitar" to "கிட்டார்",
        "Tabla" to "தபலா",
        "Drum" to "முரிசு",
        "Drum kit" to "முரிசு தொகுப்பு",
        "Bass drum" to "பெரிய முரிசு",
        "Snare drum" to "சிறிய முரிசு",
        "Cymbal" to "தாளம்",
        "Tambourine" to "கைதட்டுத் தாளம்",
        "Violin, fiddle" to "பெய்லன்",
        "Piano" to "பியானோ",
        "Organ" to "நாடஸ்வரம்",
        "Harmonica" to "சிறுகை",
        "Flute" to "புல்லாங்குழல்",
        "Trumpet" to "காளை ஊதுக் கருவி",
        "Saxophone" to "சாக்சோஃபோன்",
        "Electronic organ" to "மின்னணு நாதஸ்வரம்",
        "Synthesizer" to "சின்தசைசர்",
        "Sampler" to "மாதிரி எடுப்பான்",
        "Keyboard (musical)" to "இசை விசைப்பலகை",
        "Plucked string instrument" to "தாள் இசைக்கருவி",
        "Bowed string instrument" to "விலோச்சு இசைக்கருவி",
        "Stomach rumble" to "வயிறு ஓசை",

        // Machines in the house.
        "Air conditioning" to "ஏர் கண்டீஷனர்",
        "Vacuum cleaner" to "வாக்கூம் கிளீனர்",
        "Printer" to "அச்சுப்பொறி",
        "Sawing" to "வெட்டுதல்",
        "Hammer" to "சுத்தி",
        "Drill" to "துளைக் கருவி",
        "Dental drill, dentist's drill" to "பல் குழை கருவி",
        "Power tool" to "மின்சாதன கருவி",
        "Sanding" to "மெ தேய்த்தல்",
        "Sewing machine" to "தையல் இயந்திரம்",
        "Mechanical fan" to "சக்தி விசிறி",
        "Clock" to "கடிகாரம்",
        "Tick" to "டிக் டிக்",
        "Alarm" to "எச்சரிக்கை",
        "Telephone bell ringing" to "தொலைபேசி மணி",
        "Beep, bleep" to "பீப் சத்தம்",
        "Ding-dong" to "டிங் டாங்",
        "Coin (dropping)" to "நாணயம் விழுதல்",
        "Cash register" to "பணப் பட்டறை",
        "Sliding door" to "நழுவு கதவு",
    )

    /**
     * Sounds worth interrupting the user for. Everything else is recorded but never vibrates the
     * phone, because a phone that buzzes at every passing car is a phone that gets turned off,
     * and a turned-off phone protects nobody.
     */
    private val URGENT: Set<String> = setOf(
        "Doorbell",
        "Telephone",
        "Ringtone",
        "Alarm clock",
        "Smoke detector, smoke alarm",
        "Fire engine, fire truck (siren)",
        "Police car (siren)",
        "Siren",
        "Civil defense siren",
        "Car alarm",
        "Screaming",
        "Crying, sobbing",
        "Baby cry, infant cry",
        "Shatter",
        "Breaking",
        "Glass",
        "Smash, crash",
        "Explosion",
        "Gunshot, gunfire",
        "Bark",
        "Door",
        "Slam",
        "Bang",
        "Train horn",
        "Chime",
        "Church bell",
    )

    /** The Tamil name for an AudioSet class, or null when this class is not translated. */
    fun tamilFor(englishLabel: String): String? = TAMIL[englishLabel]

    /** True when the class has a Tamil name and may be shown. */
    fun isShown(englishLabel: String): Boolean = englishLabel in TAMIL

    /** True when the sound should vibrate the phone as well as appear on screen. */
    fun isUrgent(englishLabel: String): Boolean = englishLabel in URGENT

    /** Every translated class, for tests and for the module README's coverage list. */
    fun translatedLabels(): Set<String> = TAMIL.keys
}
