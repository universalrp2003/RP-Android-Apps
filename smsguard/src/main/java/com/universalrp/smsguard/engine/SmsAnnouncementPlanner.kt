package com.universalrp.smsguard.engine

import kotlin.math.roundToLong

data class AnnouncementSettingsSnapshot(
    val tamil: Boolean,
    val voiceEnabled: Boolean,
    val shortEveryMessageEnabled: Boolean,
    val mutePromoVoice: Boolean,
)

/**
 * Pure decision & short script planner for SMS Guard voice announcements.
 *
 * Ironclad Rules:
 *  1. OTP messages NEVER speak aloud under ANY setting.
 *  2. Fake payment messages from personal numbers (+91...) ALWAYS warn clearly that NO money came from a bank.
 *  3. Dangerous scam links ALWAYS warn clearly NOT to touch the link.
 *  4. Offline / unverified links ALWAYS state honestly that verification is pending until internet returns.
 *  5. Verified bank/UPI messages announce exact institution, credit/debit direction & amount cleanly.
 */
object SmsAnnouncementPlanner {

    fun buildIncomingAnnouncement(
        body: String,
        sender: SenderIdentity,
        otp: OtpResult,
        links: List<LinkScanResult>,
        finance: FinanceParseResult,
        settings: AnnouncementSettingsSnapshot,
    ): String? {
        // RULE 1: NEVER read OTP messages aloud!
        if (!settings.voiceEnabled || otp.isOtp) {
            return null
        }

        val hasUnsafeLink = links.any { it.verdict == LinkVerdict.UNSAFE }
        val hasPendingOffline = links.any { it.verdict == LinkVerdict.PENDING_OFFLINE }
        val hasPendingInternetOff = links.any { it.verdict == LinkVerdict.PENDING_INTERNET_OFF }
        val allLinksSafe = links.isNotEmpty() && links.all { it.verdict == LinkVerdict.SAFE }

        // RULE 2: Promotional (-P) messages stay silent by default UNLESS they contain a dangerous scam link!
        if (sender.dltCategory == DltCategory.PROMOTIONAL && settings.mutePromoVoice && !hasUnsafeLink) {
            return null
        }

        val senderName = if (settings.tamil) sender.spokenNameTa else sender.spokenNameEn

        // RULE 3: Cyber Threat Red Alert — Unsafe / Phishing / Malware Link
        if (hasUnsafeLink) {
            return if (settings.tamil) {
                "ஆபத்து எச்சரிக்கை! $senderName அனுப்பிய குறுஞ்செய்தியில் போலியான ஆபத்தான இணைப்பு உள்ளது. தயவுசெய்து அந்த இணைப்பைத் தொடாதீர்கள்!"
            } else {
                "Cyber threat alert! Message from $senderName contains an unsafe fraudulent link. Please do not touch it!"
            }
        }

        // RULE 4: Fake Payment Prank / Scam from Personal Mobile Number (+91...)
        if (finance.classification == SpoofClassification.SPOOF_PERSONAL_NUMBER_FAKE_MONEY) {
            return if (settings.tamil) {
                "எச்சரிக்கை! $senderName என்ற தனிப்பட்ட எண்ணிலிருந்து போலியான பண வரவு செய்தி வந்துள்ளது. இது வங்கி அனுப்பிய செய்தி அல்ல — பணம் எதுவும் வரவில்லை!"
            } else {
                "Warning! Fake payment message sent from $senderName, NOT a verified bank. No money was received!"
            }
        }

        // RULE 5: Link Present while Offline or Internet Disabled — Honest Pending Notice
        if (hasPendingOffline) {
            return if (settings.tamil) {
                "$senderName இடமிருந்து இணைப்புடன் செய்தி வந்துள்ளது. இப்போது இணையம் இல்லாததால் என்னால் சரிபார்க்க முடியவில்லை — தயவுசெய்து தொடாதீர்கள். இணையம் கிடைத்தவுடன் சரிபார்த்து உண்மையான தகவலைச் சொல்கிறேன்."
            } else {
                "Message with a link from $senderName. No internet right now, so I cannot verify this link yet — do not touch it. Once internet returns, I will verify and tell you the real details."
            }
        }
        if (hasPendingInternetOff) {
            return if (settings.tamil) {
                "$senderName இடமிருந்து இணைப்புடன் செய்தி வந்துள்ளது. செயலியில் இணைய அணுகல் நிறுத்தப்பட்டுள்ளதால் இப்போது சரிபார்க்க முடியவில்லை."
            } else {
                "Message with a link from $senderName. Internet access is switched off in SMS Guard, so I cannot verify this link right now."
            }
        }

        // RULE 6: Verified Real Bank / UPI / Card / FASTag Announcement:
        // Professional, clean human announcement: ONLY Full Bank/Service Name + Received/Debited + Exact Amount!
        // Never speak account masks ("XX1453"), UTR digits, or noisy raw substrings.
        if (finance.classification == SpoofClassification.GENUINE_BANK_OR_UPI && finance.direction != null) {
            val amountSpoken = spokenRupees(finance.amountInr, settings.tamil)
            val instNameTa = finance.institutionTa.ifBlank { sender.spokenNameTa }
            val instNameEn = finance.institutionEn.ifBlank { sender.spokenNameEn }

            if (finance.instrumentType == FinanceInstrumentType.FASTAG_TOLL) {
                return when (finance.direction) {
                    TxDirection.CREDIT -> if (settings.tamil) {
                        "$instNameTa. $amountSpoken ரீசார்ஜ் செய்யப்பட்டது."
                    } else {
                        "$instNameEn. $amountSpoken recharged."
                    }
                    TxDirection.DEBIT -> if (settings.tamil) {
                        "$instNameTa. $amountSpoken சுங்கக் கட்டணம் கழிக்கப்பட்டது."
                    } else {
                        "$instNameEn. $amountSpoken toll deducted."
                    }
                }
            }

            return when (finance.direction) {
                TxDirection.CREDIT -> if (settings.tamil) {
                    "$instNameTa. $amountSpoken வரவு வந்துள்ளது."
                } else {
                    "$instNameEn. $amountSpoken received."
                }
                TxDirection.DEBIT -> if (settings.tamil) {
                    "$instNameTa. $amountSpoken செலவானது."
                } else {
                    "$instNameEn. $amountSpoken debited."
                }
            }
        }

        // RULE 7: Short Every-Message Summary (when user keeps Short Announcement ON, or when message had a verified safe link)
        if (!settings.shortEveryMessageEnabled && !allLinksSafe) {
            return null
        }

        val gist = conciseGist(body)
        return if (settings.tamil) {
            val linkPart = if (allLinksSafe) " இணைப்பு சரிபார்க்கப்பட்டது — பாதுகாப்பானது." else ""
            if (gist.isNotBlank()) {
                "$senderName இடமிருந்து செய்தி: $gist.$linkPart"
            } else {
                "$senderName இடமிருந்து புதிய குறுஞ்செய்தி வந்துள்ளது.$linkPart"
            }
        } else {
            val linkPart = if (allLinksSafe) " Link verified safe to open." else ""
            if (gist.isNotBlank()) {
                "Message from $senderName: $gist.$linkPart"
            } else {
                "New message from $senderName.$linkPart"
            }
        }
    }

    fun buildReconnectedLinkAnnouncement(
        senderNameEn: String,
        senderNameTa: String,
        results: List<LinkScanResult>,
        tamil: Boolean,
    ): String? {
        if (results.isEmpty()) return null
        val anyUnsafe = results.any { it.verdict == LinkVerdict.UNSAFE }
        val allSafe = results.all { it.verdict == LinkVerdict.SAFE }
        if (!anyUnsafe && !allSafe) return null

        val domain = results.first().displayDomain
        return if (anyUnsafe) {
            if (tamil) {
                "எச்சரிக்கை! இணையம் இணைந்தது. $senderNameTa அனுப்பிய செய்தியின் இணைப்பு ($domain) ஆபத்தான போலி இணைப்பு என உறுதி செய்யப்பட்டது — தயவுசெய்து தொடாதீர்கள்!"
            } else {
                "Danger alert! Internet reconnected. The link ($domain) in the message from $senderNameEn is unsafe and fraudulent — do not touch it!"
            }
        } else {
            if (tamil) {
                "இணையம் இணைந்தது. $senderNameTa அனுப்பிய செய்தியின் இணைப்பு ($domain) சரிபார்க்கப்பட்டது — பச்சை டிக் பாதுகாப்பானது, திறக்கலாம்."
            } else {
                "Internet reconnected. The link ($domain) from $senderNameEn has been verified — safe to open."
            }
        }
    }

    private fun spokenRupees(amount: Double, tamil: Boolean): String {
        val rounded = amount.roundToLong()
        val hasPaise = kotlin.math.abs(amount - rounded) >= 0.01
        val numText = if (hasPaise) String.format(java.util.Locale.US, "%.2f", amount) else rounded.toString()
        return if (tamil) "ரூபாய் $numText" else "Rupees $numText"
    }

    private fun conciseGist(rawBody: String): String {
        // Strip raw URLs, masked account/card digits (XX1453, *1453), and reference numbers so TTS sounds natural!
        val withoutUrls = rawBody
            .replace(Regex("https?://\\S+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\b(?:a/c(?:\\s*no\\.?)?|acct|account|card)\\s*[xX*#.\\-0-9]+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\b[xX*]{2,}[0-9]{2,8}\\b"), "")
            .replace(Regex("\\b(?:ref(?:\\s*no\\.?)?|utr|txn\\s*id)\\s*[:\\-#]?\\s*[A-Za-z0-9]+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\b[0-9]{5,}\\b"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (withoutUrls.isEmpty()) return ""
        val firstSentence = withoutUrls.split(Regex("[.!?\\n]"))
            .map { it.replace(Regex("(?i)\\s+\\b(at|on|via|visit|click|open)$"), "").trim() }
            .firstOrNull { it.length >= 3 }
            ?: withoutUrls
        return if (firstSentence.length <= 95) firstSentence else firstSentence.take(92).trimEnd()
    }
}
