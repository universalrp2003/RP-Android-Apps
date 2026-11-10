package com.universalrp.tvguard.voice

import com.universalrp.tvguard.hardware.FullHardwareReport
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Small local, rule-based TV helper; no language model or network lookup. */
object TvAssistantAnswers {
    fun answer(question: String, report: FullHardwareReport?, tamil: Boolean = false): String {
        val q = question.lowercase(Locale.ROOT).trim()
        fun matches(vararg words: String) = words.any { q.contains(it) }
        fun text(en: String, ta: String) = if (tamil) ta else en
        fun gib(bytes: Long) = String.format(Locale.US, "%.1f", bytes.toDouble() / 1_073_741_824.0)
        fun decimalGb(bytes: Long) = String.format(Locale.US, "%.1f", bytes.toDouble() / 1_000_000_000.0)
        fun missing() = text("The hardware audit is not available yet. Try again after refresh.", "வன்பொருள் ஆய்வு இன்னும் கிடைக்கவில்லை. புதுப்பித்த பின் மீண்டும் கேளுங்கள்.")

        if (matches("who are you", "what can you do", "help", "நீ யார்", "என்ன செய்ய")) {
            return text(
                "I am a local TV Guard helper. I can explain the available storage, RAM, Android and processor readings, and run supported voice commands. I am not a general AI model.",
                "நான் டிவி கார்டின் உள்ளூர் உதவியாளர். சேமிப்பகம், RAM, ஆண்ட்ராய்டு, செயலி தகவல்களை விளக்கவும் சில குரல் கட்டளைகளை இயக்கவும் முடியும். நான் பொதுவான AI மாதிரி அல்ல."
            )
        }
        if (matches("hello", "hi ", "வணக்கம்", "vanakkam") || q.trimEnd('?', '!', '.') == "hi") return text("Hello! Ask me about your TV or say a command.", "வணக்கம்! டிவி பற்றி கேளுங்கள் அல்லது கட்டளை சொல்லுங்கள்.")
        if (matches("thank", "நன்றி")) return text("You are welcome.", "மகிழ்ச்சி.")
        if (matches("time", "நேரம்")) {
            val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
            return text("The current time is $time.", "இப்போதைய நேரம் $time.")
        }
        if (matches("storage", "flash", "space", "சேமிப்பகம்", "இடம்")) {
            val s = report?.storage ?: return missing()
            val device = s.blockDeviceBytes?.let { decimalGb(it) }
            val candidate = if (device == null) text("A candidate block device was not readable. ", "தொகுதி சாதன அளவு கிடைக்கவில்லை. ")
                else text("A candidate block device reports $device GB. ", "ஒரு தொகுதி சாதனம் $device GB என்று தெரிவிக்கிறது. ")
            val data = if (s.dataTotalBytes > 0 && s.dataFreeBytes in 0L..s.dataTotalBytes) text(
                "The data filesystem reports ${gib(s.dataTotalBytes)} GiB total and ${gib(s.dataFreeBytes)} GiB available. ",
                "Data கோப்பு அமைப்பு மொத்தம் ${gib(s.dataTotalBytes)} GiB என்றும் காலி ${gib(s.dataFreeBytes)} GiB என்றும் தெரிவிக்கிறது. "
            ) else text("The data filesystem reading is unavailable. ", "Data கோப்பு அமைப்பின் தகவல் கிடைக்கவில்லை. ")
            val caution = if (s.isStorageSpoofed) text(
                "These sizes disagree. The disk may not back data, or the reports may be altered. ",
                "இந்த அளவுகள் முரண்படுகின்றன. வேறு வட்டாக இருக்கலாம் அல்லது தகவல்கள் மாற்றப்பட்டிருக்கலாம். "
            ) else ""
            val estimate = s.estimatedPhysicalRemainderBytes?.let {
                text("A rough accounting estimate is ${gib(it)} GiB; this is not verified writable space.",
                    "தோராய மீதி ${gib(it)} GiB; இது உறுதிப்படுத்தப்பட்ட காலி இடம் அல்ல.")
            } ?: text("Writable physical free space is not verified.", "எழுதக்கூடிய காலி இடம் உறுதிப்படுத்தப்படவில்லை.")
            return candidate + data + caution + estimate
        }
        if (matches("ram", "memory", "நினைவகம்")) {
            val ram = report?.ram ?: return missing()
            return text(
                "The kernel reports ${gib(ram.kernelMemTotalBytes)} GiB of RAM, with ${gib(ram.kernelMemAvailableBytes)} GiB available.",
                "கர்னலின்படி RAM மொத்தம் ${gib(ram.kernelMemTotalBytes)} GiB; கிடைப்பது ${gib(ram.kernelMemAvailableBytes)} GiB."
            )
        }
        if (matches("android", "os version", "version", "ஆண்ட்ராய்டு", "பதிப்பு")) {
            val os = report?.os ?: return missing()
            val warning = if (os.isOsSpoofed) text(" The reported version and probes disagree.", " பதிப்பும் சோதனை முடிவும் முரண்படுகின்றன.") else ""
            return text(
                "The device reports Android ${os.reportedRelease} and API ${os.apiLevel}. ${os.detectedBaseOs}.$warning These readings cannot authenticate the exact base version.",
                "சாதனம் ஆண்ட்ராய்டு ${os.reportedRelease}, API ${os.apiLevel} எனத் தெரிவிக்கிறது.$warning இத்தகவல்களால் உண்மையான பதிப்பை உறுதிப்படுத்த முடியாது."
            )
        }
        if (matches("processor", "chipset", "rk3528", "cpu", "செயலி", "சிப்")) {
            val soc = report?.soc ?: return missing()
            return text(
                "The reported processor profile is ${soc.likelyChipset}, with ${soc.cpuCores} cores. This identification is an estimate from system data.",
                "அறிக்கையிட்ட செயலி: ${soc.likelyChipset}; கருக்கள் ${soc.cpuCores}. இது கணினித் தகவலின் அடிப்படையிலான மதிப்பீடு."
            )
        }
        if (matches("root", "superuser", "ரூட்")) {
            val root = report?.root ?: return missing()
            return if (root.canExecuteRoot) text("The last audit could execute a root command.", "கடைசி ஆய்வில் ரூட் கட்டளையை இயக்க முடிந்தது.")
            else text("The last audit did not confirm root access.", "கடைசி ஆய்வில் ரூட் அனுமதி உறுதியாகவில்லை.")
        }
        if (matches("debloat", "bloatware", "uninstall", "நீக்கி")) {
            return text(
                "TV Debloater lists categorized apps. Review each app before removal; some choices can affect the system.",
                "செயலி நீக்கி செயலிகளை வகைப்படுத்திக் காட்டும். நீக்குமுன் ஒவ்வொன்றையும் சரிபார்க்கவும்; சில நீக்கங்கள் அமைப்பைப் பாதிக்கலாம்."
            )
        }
        return text(
            "I cannot answer that with this local helper. I can answer basic questions about the available TV readings, but I am not connected to a general AI service.",
            "இந்த உள்ளூர் உதவியாளரால் அதற்குப் பதில் அளிக்க முடியவில்லை. டிவி தகவல்கள் பற்றிய அடிப்படைக் கேள்விகளுக்குப் பதில் அளிக்க முடியும்; பொதுவான AI சேவை இணைக்கப்படவில்லை."
        )
    }
}

/** Minimal, in-memory follow-up context; it does not provide open-ended AI. */
class TvAssistantConversation {
    private enum class Topic { STORAGE, RAM, ANDROID, PROCESSOR, ROOT, DEBLOAT }
    private var previousTopic: Topic? = null

    fun reset() { previousTopic = null }

    fun answer(question: String, report: FullHardwareReport?, tamil: Boolean = false): String {
        val q = question.lowercase(Locale.ROOT).trim()
        fun containsAny(vararg words: String) = words.any { q.contains(it) }
        val topic = when {
            containsAny("storage", "flash", "space", "சேமிப்பகம்", "இடம்") -> Topic.STORAGE
            containsAny("ram", "memory", "நினைவகம்") -> Topic.RAM
            containsAny("android", "os version", "version", "ஆண்ட்ராய்டு", "பதிப்பு") -> Topic.ANDROID
            containsAny("processor", "chipset", "rk3528", "cpu", "செயலி", "சிப்") -> Topic.PROCESSOR
            containsAny("root", "superuser", "ரூட்") -> Topic.ROOT
            containsAny("debloat", "bloatware", "uninstall", "நீக்கி") -> Topic.DEBLOAT
            else -> null
        }
        if (topic != null) {
            previousTopic = topic
            return TvAssistantAnswers.answer(question, report, tamil)
        }
        val followUp = q.trimEnd('?', '!', '.', ' ') in setOf(
            "why", "why is that", "how do you know", "explain", "explain that", "tell me more",
            "is it real", "is it fake", "ஏன்", "ஏன் அப்படி", "மேலும் சொல்லு", "விளக்கவும்"
        )
        val lastTopic = previousTopic
        if (!followUp || lastTopic == null) return TvAssistantAnswers.answer(question, report, tamil)
        fun text(en: String, ta: String) = if (tamil) ta else en
        return when (lastTopic) {
            Topic.STORAGE -> text(
                "The filesystem and candidate disk are system reports and may describe different devices. Firmware can alter them. The rough remainder does not verify writable space; confirm the backing device independently.",
                "கோப்பு அமைப்பும் கண்டறிந்த வட்டும் வெவ்வேறு சாதனங்களாக இருக்கலாம்; firmware தகவல்களை மாற்றலாம். தோராய மீதி எழுதக்கூடிய காலி இடத்தை உறுதி செய்யாது; தொடர்புடைய வட்டைத் தனியாகச் சரிபார்க்கவும்."
            )
            Topic.ANDROID -> text(
                "The Android release, SDK and framework class probes are reported by the system. A mismatch may have several causes and cannot prove the original Android version.",
                "Android பதிப்பு, SDK மற்றும் framework சோதனைகள் கணினி தெரிவிக்கும் தகவல்கள். முரண்பாட்டுக்குப் பல காரணங்கள் இருக்கலாம்; அவை அசல் பதிப்பை உறுதி செய்யாது."
            )
            Topic.RAM -> text(
                "The kernel reports memory available to it, not necessarily the installed chip size. Reserved memory or modified system reports may affect the reading.",
                "கர்னல் தனக்குக் கிடைக்கும் நினைவகத்தைத் தெரிவிக்கிறது; அது சிப்பின் மொத்த அளவாக இருக்க வேண்டியதில்லை. ஒதுக்கிய நினைவகமோ மாற்றப்பட்ட தகவல்களோ அளவைப் பாதிக்கலாம்."
            )
            Topic.PROCESSOR -> text(
                "The processor name is inferred from reported hardware and board strings, not verified by inspecting the chip.",
                "செயலி பெயர் hardware, board தகவல்களிலிருந்து ஊகிக்கப்படுகிறது; சிப்பை ஆய்வு செய்து உறுதிப்படுத்தப்படவில்லை."
            )
            Topic.ROOT -> text(
                "The audit checks common su paths and whether a root command succeeded at that time. A negative result does not rule out every possible root setup.",
                "பொதுவான su பாதைகளையும் ஆய்வின்போது ரூட் கட்டளை வெற்றியடைந்ததா என்பதையும் சோதிக்கிறது. எதிர்மறை முடிவு அனைத்து ரூட் அமைப்புகளையும் நிராகரிக்காது."
            )
            Topic.DEBLOAT -> text(
                "The catalog is a guide, not a guarantee that an app is safe to remove. Check its purpose before removal; system changes may affect the TV.",
                "பட்டியல் ஒரு வழிகாட்டி மட்டுமே; செயலியை நீக்குவது பாதுகாப்பானது என்று உறுதியளிக்காது. நோக்கத்தை அறிந்து நீக்கவும்; கணினி மாற்றங்கள் டிவியைப் பாதிக்கலாம்."
            )
        }
    }
}
