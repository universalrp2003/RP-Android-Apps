package com.universalrp.cleansweep.data

import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Deterministic authority-specific parser. Forecast rows never supply an observed intensity.
 * No free provider prose is spoken: only bounded names, numbers, direction and landfall phrase.
 */
data class OfficialCycloneBulletin(
    val authority: String, val name: String, val issuedAt: Long, val observedAt: Long,
    val lat: Double, val lon: Double, val windLow: Int, val windHigh: Int, val gust: Int?,
    val movementKmh: Int?, val direction: String?, val landfallForecast: String?, val url: String,
) {
    fun fresh(now: Long) = now - issuedAt in 0..18*3_600_000L && now - observedAt in 0..18*3_600_000L && issuedAt >= observedAt
    fun matches(event: NewsDisasterPolicy.Event): Boolean = event.type == "TC" &&
        name.equals(event.name.replace(Regex("-[0-9]{2}$"), ""), true)
    fun speech(tamil: Boolean): String {
        fun time(at: Long) = DateTimeFormatter.ofPattern("d MMM h:mm a z", if(tamil) Locale.forLanguageTag("ta-IN") else Locale.ENGLISH)
            .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(at))
        val wind = if(windLow == windHigh) "$windHigh" else "$windLow–$windHigh"
        val dir = direction?.let { if (!tamil) (it.toIntOrNull()?.let { degrees -> "$degrees degrees" } ?: it) else mapOf("north" to "வடக்கு", "south" to "தெற்கு", "east" to "கிழக்கு", "west" to "மேற்கு", "northwest" to "வடமேற்கு", "northeast" to "வடகிழக்கு", "southwest" to "தென்மேற்கு", "southeast" to "தென்கிழக்கு", "westnorthwest" to "மேற்கு வடமேற்கு", "northnorthwest" to "வடக்கு வடமேற்கு", "northnortheast" to "வடக்கு வடகிழக்கு", "eastnortheast" to "கிழக்கு வடகிழக்கு", "eastsoutheast" to "கிழக்கு தென்கிழக்கு", "southsoutheast" to "தெற்கு தென்கிழக்கு", "southsouthwest" to "தெற்கு தென்மேற்கு", "westsouthwest" to "மேற்கு தென்மேற்கு")[it] ?: "$it டிகிரி திசை" }
        return if(tamil) "அறிக்கை வெளியீடு: ${time(issuedAt)}. புயல் நிலை நேரம்: ${time(observedAt)}. நிலையான காற்று${if(authority=="JTWC") "; ஒரு நிமிட சராசரி" else ""} மணிக்கு $wind கிலோமீட்டர்; இது உள்ளூர் காற்றல்ல." +
            (gust?.let { " திடீர் பலத்த காற்று மணிக்கு $it கிலோமீட்டர்." } ?: " திடீர் பலத்த காற்றின் வேகம் கிடைக்கவில்லை.") +
            (movementKmh?.let { " கடந்த ஆறு மணி நேர இயக்கம்: மணிக்கு $it கிலோமீட்டர்${dir?.let { d -> "; $d" }.orEmpty()}." } ?: " சரிபார்த்த இயக்க வேகம் கிடைக்கவில்லை.") +
            (landfallForecast?.let { OfficialCyclonePolicy.tamilLandfall(it) ?: " கரையைக் கடக்கும் முன்கணிப்பு ஐ எம் டி அறிக்கையில் உள்ளது; இந்தத் தமிழ் வாசிப்பிற்கான சரிபார்த்த மொழிபெயர்ப்பு கிடைக்கவில்லை. இது உறுதிசெய்த நிகழ்வு அல்ல." } ?: " சரிபார்த்த கரையைக் கடக்கும் இடம், நேரம் கிடைக்கவில்லை; ஐ எம் டி அறிக்கையைப் பார்க்கவும்.") +
            " மூலம்: ${if(authority == "IMD") "ஐ எம் டி" else "ஜே டி டபிள்யூ சி"}."
        else "Bulletin issued ${time(issuedAt)}. Observed position at ${time(observedAt)}. Sustained wind${if(authority=="JTWC") " (one-minute average)" else ""} $wind kilometres per hour, not local wind." +
            (gust?.let { " Gusts $it kilometres per hour." } ?: " Verified gust speed unavailable.") +
            (movementKmh?.let { " Movement over the past six hours: $it kilometres per hour${dir?.let { d -> ", $d" }.orEmpty()}." } ?: " Verified movement speed unavailable.") +
            (landfallForecast?.let { " Forecast landfall, not confirmed: ${OfficialCyclonePolicy.forecastTimes(it)}" } ?: " Verified landfall area and time unavailable; consult IMD.") +
            " Source: $authority."
    }
}

object OfficialCyclonePolicy {
    private fun normalize(raw: String): String = raw.replace(Regex("[\\p{Cc}\\p{Cf}]"), " ").replace(Regex("\\s+"), " ").trim()
    private fun range(low: Int, high: Int, gust: Int?) = low in 1..350 && high in low..350 && (gust == null || gust in high..450)
    private fun utcStamp(code: String, reference: Long): Long? {
        val ref = Instant.ofEpochMilli(reference).atZone(ZoneOffset.UTC)
        return (-1L..1L).mapNotNull { offset -> runCatching {
            val m = ref.plusMonths(offset)
            m.withDayOfMonth(code.take(2).toInt()).withHour(code.substring(2,4).toInt()).withMinute(code.takeLast(2).toInt()).withSecond(0).withNano(0).toInstant().toEpochMilli()
        }.getOrNull() }.minByOrNull { kotlin.math.abs(it-reference) }
    }
    fun forecastTimes(value: String): String = Regex("([0-9]{4})(?:\\s*[-–]\\s*([0-9]{4}))?\\s*(?:Hrs|hours)\\s*IST",RegexOption.IGNORE_CASE).replace(value) { m ->
        fun time(code: String): String = runCatching { LocalTime.of(code.take(2).toInt(),code.takeLast(2).toInt()).format(DateTimeFormatter.ofPattern("h:mm a",Locale.ENGLISH)) }.getOrDefault("time unavailable")
        time(m.groupValues[1]) + m.groupValues[2].takeIf { it.isNotBlank() }?.let { "–"+time(it) }.orEmpty()+" IST"
    }
    /** Fixed grammar translation only; proper place names remain as issued, no model/API. */
    fun tamilLandfall(quote: String): String? {
        val m = Regex("cross (.+?)(?: as .+?)? (?:during|by) (.+)\\.",RegexOption.IGNORE_CASE).find(quote) ?: return null
        val words = mapOf("north" to "வடக்கு", "south" to "தெற்கு", "coasts" to "கடற்கரை", "coast" to "கடற்கரை", "between" to "இடையே", "and" to "மற்றும்", "close" to "அருகில்", "to" to "வரை", "near" to "அருகில்", "of" to "", "the" to "", "early" to "தொடக்க", "hours" to "மணி", "hrs" to "மணி", "ist" to "இந்திய நேரம்", "am" to "முற்பகல்", "pm" to "பிற்பகல்", "night" to "இரவு", "midnight" to "நள்ளிரவு", "morning" to "காலை", "forenoon" to "முற்பகல்", "afternoon" to "பிற்பகல்", "evening" to "மாலை", "january" to "ஜனவரி", "february" to "பிப்ரவரி", "march" to "மார்ச்", "april" to "ஏப்ரல்", "may" to "மே", "june" to "ஜூன்", "july" to "ஜூலை", "august" to "ஆகஸ்ட்", "september" to "செப்டம்பர்", "october" to "அக்டோபர்", "november" to "நவம்பர்", "december" to "டிசம்பர்")
        fun translate(value: String): String? {
            val plain = forecastTimes(value).replace(Regex("(?<=[0-9])(?:st|nd|rd|th)\\b",RegexOption.IGNORE_CASE), "")
            if (Regex("[A-Za-z]+").findAll(plain).any { it.value.lowercase(Locale.ROOT) !in words && !it.value.first().isUpperCase() }) return null
            return Regex("[A-Za-z]+").replace(plain) { words[it.value.lowercase(Locale.ROOT)] ?: it.value }.replace('&',' ').trim()
        }
        val area = translate(m.groupValues[1]) ?: return null
        val window = translate(m.groupValues[2]) ?: return null
        return " கரையைக் கடக்கும் முன்கணிப்பு; உறுதி அல்ல. பகுதி: $area. நேரச் சாளரம்: $window."
    }
    fun jtwcLinks(html: String): List<String> = Regex("(?:https://www\\.metoc\\.navy\\.mil)?/jtwc/products/io[0-9]{4}web\\.txt")
        .findAll(html.take(400_000)).map { "https://www.metoc.navy.mil" + it.value.substringAfter("https://www.metoc.navy.mil") }.distinct().take(3).toList()
    fun imdLinks(html: String): List<String> = Regex("(?:https://rsmcnewdelhi\\.imd\\.gov\\.in)?/uploads/archive/(?:1|47)/(?:[A-Za-z0-9_. -]|%20){1,160}\\.pdf")
        .findAll(html.take(400_000)).map { "https://rsmcnewdelhi.imd.gov.in" + it.value.substringAfter("https://rsmcnewdelhi.imd.gov.in") }.distinct().take(2).toList()
    fun allowed(url: String): Boolean = url in setOf("https://www.metoc.navy.mil/jtwc/jtwc.html", "https://rsmcnewdelhi.imd.gov.in/") ||
        Regex("https://www\\.metoc\\.navy\\.mil/jtwc/products/io[0-9]{4}web\\.txt").matches(url) ||
        Regex("https://rsmcnewdelhi\\.imd\\.gov\\.in/uploads/archive/(?:1|47)/(?:[A-Za-z0-9_. -]|%20){1,160}\\.pdf").matches(url)
    fun jtwc(raw: String, url: String, now: Long): OfficialCycloneBulletin? = runCatching {
        require(raw.length <= 80_000 && allowed(url) && url.endsWith("web.txt"))
        val s = normalize(raw)
        require(s.contains("MAX SUSTAINED WINDS BASED ON ONE-MINUTE AVERAGE"))
        val identity = Regex("SUBJ/TROPICAL CYCLONE ([0-9]{2}[AB]) \\(([A-Z][A-Z-]{1,29})\\) WARNING NR [0-9]{1,3}//").find(s) ?: return null
        val issue = Regex("WTIO[0-9]{2} PGTW ([0-9]{6})").find(s)?.groupValues?.get(1) ?: return null
        // Tie the year/month to the dated remarks and product filename, not a guessed month.
        val date = Regex(" ([0-9]{2}[A-Z]{3}[0-9]{2})\\. TROPICAL CYCLONE " + Regex.escape(identity.groupValues[1])).find(s)?.groupValues?.get(1) ?: return null
        val day = java.time.LocalDate.parse(date, java.time.format.DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("ddMMMyy").toFormatter(Locale.ENGLISH))
        require(url.substringAfterLast('/').substring(4,6) == (day.year % 100).toString().padStart(2,'0'))
        require(url.substringAfterLast('/').substring(2,4) == identity.groupValues[1].take(2))
        val issued = utcStamp(issue, day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()) ?: return null
        require(Instant.ofEpochMilli(issued).atZone(ZoneOffset.UTC).toLocalDate() == day)
        val observed = s.substringAfter("WARNING POSITION:", "").substringBefore("FORECASTS:").substringBefore("EXTENDED OUTLOOK:")
        val pos = Regex("([0-9]{6})Z --- NEAR ([0-9.]+)([NS]) ([0-9.]+)([EW])").find(observed) ?: return null
        val at = utcStamp(pos.groupValues[1], issued) ?: return null
        val lat = pos.groupValues[2].toDouble() * if(pos.groupValues[3] == "S") -1 else 1
        val lon = pos.groupValues[4].toDouble() * if(pos.groupValues[5] == "W") -1 else 1
        require(SavedRainWatches.validCoordinates(lat,lon))
        val w = Regex("MAX SUSTAINED WINDS - ([0-9]{1,3}) KT, GUSTS ([0-9]{1,3}) KT").find(observed) ?: return null
        fun km(k: String) = kotlin.math.round(k.toInt()*1.852).toInt()
        val wind = km(w.groupValues[1]); val gust = km(w.groupValues[2]); require(range(wind,wind,gust))
        val move = Regex("MOVEMENT PAST SIX HOURS - ([0-9]{1,3}) DEGREES AT ([0-9]{1,3}) KTS").find(observed)
        val direction = move?.groupValues?.get(1)?.toInt()?.takeIf { it in 0..360 }?.toString()
        val speed = move?.groupValues?.get(2)?.let(::km)?.takeIf { it in 0..150 }
        OfficialCycloneBulletin("JTWC", identity.groupValues[2], issued, at, lat,lon,wind,wind,gust, speed.takeIf { direction != null },direction,null,url).takeIf { it.fresh(now) }
    }.getOrNull()
    fun imd(raw: String, url: String, now: Long): OfficialCycloneBulletin? = runCatching {
        require(raw.length <= 60_000 && allowed(url) && url.endsWith(".pdf"))
        val s = normalize(raw).replace('“','"').replace('”','"').replace('‘','"').replace('’','"')
        require(s.contains("India Meteorological Department",true) && s.contains("NATIONAL BULLETIN",true))
        val name = Regex("[Cc]yclonic [Ss]torm\\s+\"([A-Za-z][A-Za-z-]{1,29})\"").find(s)?.groupValues?.get(1)?.uppercase(Locale.ROOT) ?: return null
        val issue = Regex("TIME OF ISSUE\\s*:\\s*([0-9]{4}) HOURS IST\\s+DATED\\s*:\\s*([0-9]{2}\\.[0-9]{2}\\.[0-9]{4})",RegexOption.IGNORE_CASE).find(s) ?: return null
        val zone = ZoneId.of("Asia/Kolkata")
        val date = java.time.LocalDate.parse(issue.groupValues[2],DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(java.time.format.ResolverStyle.STRICT))
        val issued = date.atTime(issue.groupValues[1].take(2).toInt(),issue.groupValues[1].takeLast(2).toInt()).atZone(zone).toInstant().toEpochMilli()
        // Read only the FIRST location/intensity table row; later rows are forecasts.
        val table = s.substringAfter("Forecast location and intensity", "").ifBlank { s.substringAfter("Forecast track and intensity", "") }
        val first = Regex("([0-9]{2}\\.[0-9]{2}\\.[0-9]{2})\\s*/\\s*([0-9]{4})\\s+([0-9.]+)\\s*/\\s*([0-9.]+)\\s+([0-9]{1,3})\\s*[-–]\\s*([0-9]{1,3})\\s+gusting\\s+to\\s+([0-9]{1,3})",RegexOption.IGNORE_CASE).find(table) ?: return null
        val obsDate = java.time.LocalDate.parse(first.groupValues[1],DateTimeFormatter.ofPattern("dd.MM.uu").withResolverStyle(java.time.format.ResolverStyle.STRICT))
        val at = obsDate.atTime(first.groupValues[2].take(2).toInt(),first.groupValues[2].takeLast(2).toInt()).atZone(zone).toInstant().toEpochMilli()
        val lat = first.groupValues[3].toDouble(); val lon = first.groupValues[4].toDouble(); require(SavedRainWatches.validCoordinates(lat,lon))
        val lo = first.groupValues[5].toInt(); val hi = first.groupValues[6].toInt(); val gust = first.groupValues[7].toInt(); require(range(lo,hi,gust))
        val intro = s.substringBefore("Forecast location and intensity").substringBefore("Forecast track and intensity")
        val m = Regex("moved (?:nearly )?([a-z-]+?)(?:wards|ward)? with a speed of ([0-9]{1,3}) kmph during (?:the )?past 6 hours",RegexOption.IGNORE_CASE).find(intro)
        val d = m?.groupValues?.get(1)?.lowercase(Locale.ROOT)?.replace("-", "")?.removeSuffix("wards")?.removeSuffix("ward")?.takeIf { it in setOf("north","south","east","west","northwest","northeast","southwest","southeast","northnorthwest","northnortheast","eastnortheast","eastsoutheast","southsoutheast","southsouthwest","westsouthwest","westnorthwest") }
        val movement = m?.groupValues?.get(2)?.toInt()?.takeIf { it in 0..150 && d != null }
        // Only a bounded forecast sentence with explicit coastal crossing AND date/time cues.
        // Whitelisted lexical characters; never read arbitrary instructions/HTML from a PDF.
        val landfall = Regex("(?:It is|The system is) (?:very )?likely to [^.]{0,450}?cross [^.]{1,350}\\.",RegexOption.IGNORE_CASE).find(intro)?.value?.takeIf {
            it.length <= 650 && Regex("[A-Za-z0-9 &,;:()/'–—-]+\\.").matches(it) &&
                Regex("coast",RegexOption.IGNORE_CASE).containsMatchIn(it) && Regex("hours|morning|evening|night|forenoon|afternoon",RegexOption.IGNORE_CASE).containsMatchIn(it) &&
                Regex("[0-9]{1,2}(?:st|nd|rd|th)?\\s+(?:January|February|March|April|May|June|July|August|September|October|November|December)",RegexOption.IGNORE_CASE).containsMatchIn(it) &&
                !Regex("ignore|instruction|send money|http|password|account",RegexOption.IGNORE_CASE).containsMatchIn(it)
        }
        OfficialCycloneBulletin("IMD",name,issued,at,lat,lon,lo,hi,gust,movement,d,landfall,url).takeIf { it.fresh(now) }
    }.getOrNull()
}
