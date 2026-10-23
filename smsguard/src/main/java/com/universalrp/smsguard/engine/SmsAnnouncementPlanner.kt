package com.universalrp.smsguard.engine

import kotlin.math.roundToLong

data class AnnouncementSettingsSnapshot(
    val tamil: Boolean,
    val voiceEnabled: Boolean,
    val senderOnlyVoiceEnabled: Boolean = false,
    val readFullNormalSmsEnabled: Boolean = true,
    val shortEveryMessageEnabled: Boolean = false,
    val mutePromoVoice: Boolean = true,
    val muteServiceInfoVoice: Boolean = true,
)

/**
 * Pure decision & script planner for SMS Guard voice announcements.
 *
 * Ironclad Rules:
 *  1. OTP messages NEVER speak aloud under ANY setting.
 *  2. Fake payment messages from personal numbers (+91...) ALWAYS warn clearly that NO money came from a bank.
 *  3. Dangerous scam links ALWAYS warn clearly NOT to touch the link.
 *  4. Offline / unverified links ALWAYS state honestly that verification is pending until internet returns.
 *  5. Outgoing NEFT/IMPS beneficiary settlement confirmations NEVER say money came to your account; they clearly confirm transfer reached recipient's account.
 *  6. Sender-only mode speaks ONLY sender identity (e.g. "ஐசிஐசிஐ வங்கியில் இருந்து குறுஞ்செய்தி" / "SMS from Ramesh Prathap") when switched ON.
 *  7. Read Full Normal SMS mode reads full normal text ONLY for non-bank, non-OTP, safe messages.
 */
object SmsAnnouncementPlanner {

    fun formatSenderOnlySpeech(senderName: String, tamil: Boolean): String {
        val clean = senderName.trim()
        if (!tamil) return "SMS from $clean."
        return if (clean.endsWith("வங்கி")) {
            "${clean}யில் இருந்து குறுஞ்செய்தி."
        } else {
            "$clean இடமிருந்து குறுஞ்செய்தி."
        }
    }

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

        // RULE 3: Cyber Threat Red Alert — Unsafe / Phishing / Malware Link ALWAYS warns aloud!
        if (hasUnsafeLink) {
            return if (settings.tamil) {
                "ஆபத்து எச்சரிக்கை! $senderName அனுப்பிய குறுஞ்செய்தியில் போலியான ஆபத்தான இணைப்பு உள்ளது. தயவுசெய்து அந்த இணைப்பைத் தொடாதீர்கள்!"
            } else {
                "Cyber threat alert! Message from $senderName contains an unsafe fraudulent link. Please do not touch it!"
            }
        }

        // RULE 4: Fake Payment Prank / Scam from Personal Mobile Number (+91...) ALWAYS warns aloud!
        if (finance.classification == SpoofClassification.SPOOF_PERSONAL_NUMBER_FAKE_MONEY) {
            return if (settings.tamil) {
                "எச்சரிக்கை! $senderName என்ற தனிப்பட்ட எண்ணிலிருந்து போலியான பண வரவு செய்தி வந்துள்ளது. இது வங்கி அனுப்பிய செய்தி அல்ல — பணம் எதுவும் வரவில்லை!"
            } else {
                "Warning! Fake payment message sent from $senderName, NOT a verified bank. No money was received!"
            }
        }

        // RULE 5: Sender-Name-Only Mode (when switched ON by user)
        if (settings.senderOnlyVoiceEnabled) {
            val instNameTa = finance.institutionTa.ifBlank { sender.spokenNameTa }
            val instNameEn = finance.institutionEn.ifBlank { sender.spokenNameEn }
            val chosenName = if (settings.tamil) instNameTa else instNameEn
            return formatSenderOnlySpeech(chosenName, settings.tamil)
        }

        // RULE 6: Link Present while Offline or Internet Disabled — Honest Pending Notice
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

        // RULE 6B: Outgoing NEFT / IMPS Beneficiary Account Settlement Confirmation (NEVER user income!)
        if (finance.classification == SpoofClassification.OUTGOING_TRANSFER_SETTLEMENT && finance.amountInr > 0.0) {
            val amountSpoken = spokenRupees(finance.amountInr, settings.tamil)
            val instNameTa = finance.institutionTa.ifBlank { sender.spokenNameTa }
            val instNameEn = finance.institutionEn.ifBlank { sender.spokenNameEn }
            return if (settings.tamil) {
                "$instNameTa: நீங்கள் அனுப்பிய $amountSpoken பெறுநர் கணக்கில் சேர்ந்துவிட்டது."
            } else {
                "$instNameEn: Your $amountSpoken transfer has reached the beneficiary's account."
            }
        }

        // RULE 7: Verified Real Bank / UPI / Card / FASTag Announcement:
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

        // RULE 7.5: Routine telecom data / validity / remaining-SMS status dumps (e.g. "Pack Valid
        // till 10 Dec 2026. Remaining SMS:569 Bal:Rs.0.00.") stay SILENT when the owner turns this ON.
        // Placed AFTER the verified bank credit/debit announcer (RULE 7) so the exact received/spent
        // amount is NEVER silenced, and AFTER the scam-link / fake-payment alerts so danger still warns.
        if (settings.muteServiceInfoVoice && ServiceInfoDetector.isServiceInfo(body)) {
            return null
        }

        // RULE 8: Full Readout for Ordinary Messages (non-bank, non-OTP) OR Short Every-Message Summary
        if (!settings.readFullNormalSmsEnabled && !settings.shortEveryMessageEnabled && !allLinksSafe) {
            return null
        }

        val textToSpeak = if (settings.readFullNormalSmsEnabled) {
            cleanFullNormalBody(body, settings.tamil)
        } else {
            conciseGist(body)
        }

        return if (settings.tamil) {
            val linkPart = if (allLinksSafe) " இணைப்பு சரிபார்க்கப்பட்டது — பாதுகாப்பானது." else ""
            if (textToSpeak.isNotBlank()) {
                "$senderName இடமிருந்து செய்தி: $textToSpeak.$linkPart"
            } else {
                "$senderName இடமிருந்து புதிய குறுஞ்செய்தி வந்துள்ளது.$linkPart"
            }
        } else {
            val linkPart = if (allLinksSafe) " Link verified safe to open." else ""
            if (textToSpeak.isNotBlank()) {
                "Message from $senderName: $textToSpeak.$linkPart"
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

    private fun cleanFullNormalBody(rawBody: String, tamil: Boolean = false): String {
        var text = rawBody
            .replace(Regex("https?://\\S+", RegexOption.IGNORE_CASE), "")
            // NEVER read the total / available bank balance aloud — only the received or spent amount matters.
            .replace(
                Regex(
                    "\\b(?:avl\\.?\\s*bal(?:ance)?|available\\s+balance|clear\\s+bal(?:ance)?|closing\\s+bal(?:ance)?|bal(?:ance)?)\\s*[:\\-]?\\s*(?:rs\\.?|inr|₹)?\\s*[0-9][0-9,]*(?:\\.[0-9]{1,2})?",
                    RegexOption.IGNORE_CASE,
                ),
                "",
            )
            // Expand Ration Shop (PDS) & Indian currency / unit shorthand into natural human speech!
            .replace(Regex("\\bPDS\\s+Bill\\s+[0-9]{10,20}\\b", RegexOption.IGNORE_CASE), if (tamil) "பொருள் விவரம்:" else "Ration bill items:")
            .replace(Regex("\\bP\\.?\\s*Oil\\b", RegexOption.IGNORE_CASE), if (tamil) "பாமாயில்" else "Palm Oil")
            .replace(Regex("(?<=\\d)\\s*pkt\\b", RegexOption.IGNORE_CASE), if (tamil) " பாக்கெட்" else " packet")
            .replace(Regex("(?<=\\d)\\s*kg\\b", RegexOption.IGNORE_CASE), if (tamil) " கிலோ" else " kg")
            .replace(Regex("(?<=\\d)\\s*ltrs?\\b", RegexOption.IGNORE_CASE), if (tamil) " லிட்டர்" else " litre")
            .replace(
                Regex("\\bRs\\.?\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)\\s*(?:/-|/-\\.)?", RegexOption.IGNORE_CASE),
            ) { m ->
                val rawAmt = m.groupValues[1].replace(".00", "").replace(Regex("\\.0$"), "")
                if (tamil) "ரூபாய் $rawAmt" else "Rupees $rawAmt"
            }
            // Expand TN Board Exam Results (HSC / SSLC) abbreviations so every subject & mark speaks clearly!
            .replace(Regex("\\bSub\\s*[0-9]+\\s*:\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\bLAN\\s*:\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\bTAM\\s*-?\\s*(?=\\d)", RegexOption.IGNORE_CASE), if (tamil) "தமிழ் " else "Tamil ")
            .replace(Regex("\\bENG\\s*:?\\s*-?\\s*(?=\\d)", RegexOption.IGNORE_CASE), if (tamil) "ஆங்கிலம் " else "English ")
            .replace(Regex("\\bPHY\\s*-?\\s*(?=\\d)", RegexOption.IGNORE_CASE), if (tamil) "இயற்பியல் " else "Physics ")
            .replace(Regex("\\bCHE\\s*-?\\s*(?=\\d)", RegexOption.IGNORE_CASE), if (tamil) "வேதியியல் " else "Chemistry ")
            .replace(Regex("\\bCSC\\s*-?\\s*(?=\\d)", RegexOption.IGNORE_CASE), if (tamil) "கணினி அறிவியல் " else "Computer Science ")
            .replace(Regex("\\bBIO\\s*-?\\s*(?=\\d)", RegexOption.IGNORE_CASE), if (tamil) "உயிரியல் " else "Biology ")
            .replace(Regex("\\bMAT\\s*-?\\s*(?=\\d)", RegexOption.IGNORE_CASE), if (tamil) "கணிதம் " else "Maths ")
            .replace(Regex("\\bTOT\\s*-?\\s*(?=\\d)", RegexOption.IGNORE_CASE), if (tamil) "மொத்த மதிப்பெண் " else "Total ")
            // Strip trailing DLT tag sign-offs like `-TNSEDU` / `-TNEPDS` since sender name is already announced cleanly at start
            .replace(Regex("\\s*-[A-Z]{4,9}\\s*$"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (text.length > 950) {
            text = text.take(950).trimEnd()
        }
        return text
    }

    private fun conciseGist(rawBody: String): String {
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
