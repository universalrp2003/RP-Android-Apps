package com.universalrp.smsguard.engine

import java.util.Locale

enum class DltCategory(val code: Char?, val labelEn: String, val labelTa: String) {
    TRANSACTIONAL('T', "Verified Transactional", "சரிபார்க்கப்பட்ட வங்கி/பரிவர்த்தனை"),
    SERVICE('S', "Verified Service", "சரிபார்க்கப்பட்ட சேவை"),
    GOVERNMENT('G', "Official Government", "அரசு அதிகாரப்பூர்வ செய்தி"),
    PROMOTIONAL('P', "Promotional / Marketing", "விளம்பரச் செய்தி"),
    LEGACY_DLT(null, "Registered Organization", "பதிவுசெய்யப்பட்ட நிறுவனம்"),
    PERSONAL(null, "Personal Mobile Number", "தனிப்பட்ட மொபைல் எண்"),
}

enum class SenderTrustLevel {
    VERIFIED_BANK_OR_UPI,
    VERIFIED_GOVERNMENT,
    VERIFIED_COMPANY,
    PROMOTIONAL,
    PERSONAL_CONTACT,
    PERSONAL_NUMBER,
}

data class BrandProfile(
    val code: String,
    val nameEn: String,
    val nameTa: String,
    val isFinancial: Boolean = false,
    val isGovernment: Boolean = false,
)

data class SenderIdentity(
    val rawAddress: String,
    val normalizedAddress: String,
    val isPersonalNumber: Boolean,
    val isOfficialDltHeader: Boolean,
    val headerCode: String?,
    val operatorEn: String?,
    val circleEn: String?,
    val dltCategory: DltCategory,
    val trustLevel: SenderTrustLevel,
    val displayNameEn: String,
    val displayNameTa: String,
    val spokenNameEn: String,
    val spokenNameTa: String,
    val isVerifiedFinancialSender: Boolean,
)

object SenderIdentityEngine {

    // Modern TRAI 2025/2026 scrubbed format: XY-HEADER-Z (e.g., AD-SBIINB-T, JT-HDFCBK-T, VM-612345-P)
    private val DLT_WITH_PREFIX_AND_SUFFIX =
        Regex("^([A-Z]{2})-([A-Z0-9]{3,11})-([TSPG])$")

    // Direct entity header with TRAI suffix (e.g., SBIINB-T, GOVTIN-G)
    private val DLT_DIRECT_WITH_SUFFIX =
        Regex("^([A-Z0-9]{4,11})-([TSPG])$")

    // Pre-May-2025 historical inbox SMS headers (e.g., VM-HDFCBK, AD-SBIINB, JD-651234)
    private val DLT_LEGACY_WITH_PREFIX =
        Regex("^([A-Z]{2})-([A-Z0-9]{3,11})$")

    // Carrier/official 5-to-6 digit shortcodes (never assignable to human SIM cards in India)
    private val CARRIER_SHORTCODE =
        Regex("^[1-9][0-9]{2,5}$")

    private val OPERATORS = mapOf(
        'A' to "Airtel",
        'B' to "BSNL",
        'D' to "Videocon / Enterprise",
        'I' to "Idea / Vi",
        'J' to "Jio",
        'M' to "MTNL",
        'Q' to "Quadgen / Enterprise",
        'R' to "Reliance",
        'T' to "Tata Tele",
        'V' to "Vodafone Idea (Vi)",
    )

    private val CIRCLES = mapOf(
        'A' to "Andhra Pradesh",
        'B' to "Bihar",
        'D' to "Delhi",
        'E' to "UP East",
        'G' to "Gujarat",
        'H' to "Haryana",
        'I' to "Himachal Pradesh",
        'J' to "Jammu & Kashmir",
        'K' to "Kolkata",
        'L' to "Kerala",
        'M' to "Mumbai",
        'N' to "North East",
        'O' to "Odisha",
        'P' to "Punjab",
        'R' to "Rajasthan",
        'S' to "Assam",
        'T' to "Tamil Nadu",
        'U' to "UP West",
        'V' to "West Bengal",
        'W' to "UP West",
        'X' to "Karnataka",
        'Y' to "Madhya Pradesh",
        'Z' to "Maharashtra",
    )

    private fun brand(code: String, en: String, ta: String, fin: Boolean = false, gov: Boolean = false) =
        code to BrandProfile(code, en, ta, isFinancial = fin, isGovernment = gov)

    private val BRANDS: Map<String, BrandProfile> = mapOf(
        // Public Sector & Regional Tamil Nadu / All-India Banks
        brand("SBIINB", "State Bank of India", "ஸ்டேட் பேங்க் ஆஃப் இந்தியா", fin = true),
        brand("SBIPSG", "State Bank of India", "ஸ்டேட் பேங்க் ஆஃப் இந்தியா", fin = true),
        brand("SBIUPI", "SBI UPI", "ஸ்டேட் பேங்க் UPI", fin = true),
        brand("CBSSBI", "State Bank of India", "ஸ்டேட் பேங்க் ஆஃப் இந்தியா", fin = true),
        brand("ATMSBI", "State Bank of India ATM", "ஸ்டேட் பேங்க் ஏடிஎம்", fin = true),
        brand("SBICRD", "State Bank of India Credit Card", "ஸ்டேட் பேங்க் கிரெடிட் கார்டு", fin = true),
        brand("SBICAR", "State Bank of India Credit Card", "ஸ்டேட் பேங்க் கிரெடிட் கார்டு", fin = true),
        brand("SBIFST", "State Bank of India FASTag", "ஸ்டேட் பேங்க் பாஸ்டேக்", fin = true),
        brand("SBIETG", "State Bank of India FASTag", "ஸ்டேட் பேங்க் பாஸ்டேக்", fin = true),
        brand("SBILIF", "SBI Life Insurance", "எஸ்பிஐ லைஃப் இன்சூரன்ஸ்", fin = false),
        brand("SBIGIC", "SBI General Insurance", "எஸ்பிஐ ஜெனரல் இன்சூரன்ஸ்", fin = false),
        brand("SBIMUT", "SBI Mutual Fund", "எஸ்பிஐ மியூச்சுவல் ஃபண்ட்", fin = false),
        brand("IHMCLS", "NHAI FASTag (IHMCL)", "தேசிய நெடுஞ்சாலை பாஸ்டேக்", fin = true),
        brand("NHAIIN", "NHAI FASTag", "தேசிய நெடுஞ்சாலை பாஸ்டேக்", fin = true),
        brand("IDFCFT", "IDFC FIRST FASTag", "IDFC பாஸ்டேக்", fin = true),
        brand("ICICFT", "ICICI FASTag", "ICICI பாஸ்டேக்", fin = true),
        brand("HDFCFT", "HDFC Bank FASTag", "HDFC பாஸ்டேக்", fin = true),
        brand("AXISFT", "Axis Bank FASTag", "ஆக்சிஸ் பாஸ்டேக்", fin = true),
        brand("PYTMFT", "Paytm FASTag", "பேடிஎம் பாஸ்டேக்", fin = true),
        brand("AIRTFT", "Airtel FASTag", "ஏர்டெல் பாஸ்டேக்", fin = true),
        brand("AIRINF", "Airtel", "ஏர்டெல்", fin = false),
        brand("YONOSB", "State Bank of India", "ஸ்டேட் பேங்க் ஆஃப் இந்தியா", fin = true),
        brand("IOBCHN", "Indian Overseas Bank", "இந்தியன் ஓவர்சீஸ் வங்கி", fin = true),
        brand("IOBBNK", "Indian Overseas Bank", "இந்தியன் ஓவர்சீஸ் வங்கி", fin = true),
        brand("IOBALT", "Indian Overseas Bank", "இந்தியன் ஓவர்சீஸ் வங்கி", fin = true),
        brand("IDIBNK", "Indian Bank", "இந்தியன் வங்கி", fin = true),
        brand("INDBNK", "Indian Bank", "இந்தியன் வங்கி", fin = true),
        brand("CANBNK", "Canara Bank", "கனரா வங்கி", fin = true),
        brand("KVBANK", "Karur Vysya Bank", "கரூர் வைஸ்யா வங்கி", fin = true),
        brand("KVBBNK", "Karur Vysya Bank", "கரூர் வைஸ்யா வங்கி", fin = true),
        brand("TMBANK", "Tamilnad Mercantile Bank", "தமிழ்நாடு மெர்க்கன்டைல் வங்கி", fin = true),
        brand("TMBOTP", "Tamilnad Mercantile Bank", "தமிழ்நாடு மெர்க்கன்டைல் வங்கி", fin = true),
        brand("CUBANK", "City Union Bank", "சிட்டி யூனியன் வங்கி", fin = true),
        brand("CUBBNK", "City Union Bank", "சிட்டி யூனியன் வங்கி", fin = true),
        brand("TNGBNK", "Tamil Nadu Grama Bank", "தமிழ்நாடு கிராம வங்கி", fin = true),
        brand("TNSCBK", "TN State Apex Co-op Bank", "தமிழ்நாடு மாநில கூட்டுறவு வங்கி", fin = true),
        brand("HDFCBK", "HDFC Bank", "HDFC வங்கி", fin = true),
        brand("HDFCBA", "HDFC Bank", "HDFC வங்கி", fin = true),
        brand("ICICIB", "ICICI Bank", "ICICI வங்கி", fin = true),
        brand("ICICIT", "ICICI Bank", "ICICI வங்கி", fin = true),
        brand("AXISBK", "Axis Bank", "ஆக்சிஸ் வங்கி", fin = true),
        brand("AXISMB", "Axis Bank", "ஆக்சிஸ் வங்கி", fin = true),
        brand("KOTAKB", "Kotak Mahindra Bank", "கோடக் மஹிந்திரா வங்கி", fin = true),
        brand("BOBTXN", "Bank of Baroda", "பேங்க் ஆஃப் பரோடா", fin = true),
        brand("BARODA", "Bank of Baroda", "பேங்க் ஆஃப் பரோடா", fin = true),
        brand("UNIONB", "Union Bank of India", "யூனியன் பேங்க் ஆஃப் இந்தியா", fin = true),
        brand("UBIBNK", "Union Bank of India", "யூனியன் பேங்க் ஆஃப் இந்தியா", fin = true),
        brand("PNBSMS", "Punjab National Bank", "பஞ்சாப் நேஷனல் வங்கி", fin = true),
        brand("BOIIND", "Bank of India", "பேங்க் ஆஃப் இந்தியா", fin = true),
        brand("MAHABK", "Bank of Maharashtra", "பேங்க் ஆஃப் மகாராஷ்டிரா", fin = true),
        brand("CENTBK", "Central Bank of India", "சென்ட்ரல் பேங்க் ஆஃப் இந்தியா", fin = true),
        brand("UCOBNK", "UCO Bank", "யூகோ வங்கி", fin = true),
        brand("IDBIBK", "IDBI Bank", "IDBI வங்கி", fin = true),
        brand("IDFCFB", "IDFC FIRST Bank", "IDFC ஃபர்ஸ்ட் வங்கி", fin = true),
        brand("INDUSB", "IndusInd Bank", "இண்டஸ்இண்ட் வங்கி", fin = true),
        brand("FEDBNK", "Federal Bank", "ஃபெடரல் வங்கி", fin = true),
        brand("SIBSMS", "South Indian Bank", "சவுத் இந்தியன் வங்கி", fin = true),
        brand("YESBNK", "YES Bank", "யெஸ் வங்கி", fin = true),
        brand("RBLBNK", "RBL Bank", "RBL வங்கி", fin = true),
        brand("DBSBNK", "DBS Bank India", "DBS வங்கி", fin = true),
        brand("CSBBNK", "CSB Bank", "CSB வங்கி", fin = true),
        brand("DHANBK", "Dhanlaxmi Bank", "தனலட்சுமி வங்கி", fin = true),
        brand("EQUTAS", "Equitas Small Finance Bank", "ஈக்விடாஸ் வங்கி", fin = true),
        brand("EQUITB", "Equitas Small Finance Bank", "ஈக்விடாஸ் வங்கி", fin = true),
        brand("UJJIVN", "Ujjivan Small Finance Bank", "உஜ்ஜீவன் வங்கி", fin = true),
        brand("AUBANK", "AU Small Finance Bank", "AU வங்கி", fin = true),
        brand("IPPBMB", "India Post Payments Bank", "இந்தியா போஸ்ட் பேமெண்ட்ஸ் வங்கி", fin = true),
        brand("IPPBNK", "India Post Payments Bank", "இந்தியா போஸ்ட் பேமெண்ட்ஸ் வங்கி", fin = true),
        brand("AIRBNK", "Airtel Payments Bank", "ஏர்டெல் பேமெண்ட்ஸ் வங்கி", fin = true),
        brand("JIOBNK", "Jio Payments Bank", "ஜியோ பேமெண்ட்ஸ் வங்கி", fin = true),
        brand("PAYTMB", "Paytm Payments / UPI", "பேடிஎம் (Paytm)", fin = true),
        // UPI / Payment Platforms
        brand("NPCIUP", "NPCI BHIM UPI", "NPCI பீம் UPI", fin = true),
        brand("BHIMUP", "BHIM UPI", "பீம் UPI", fin = true),
        brand("GOOGLP", "Google Pay (GPay)", "கூகுள் பே (GPay)", fin = true),
        brand("GPAYUP", "Google Pay (GPay)", "கூகுள் பே (GPay)", fin = true),
        brand("PHONPE", "PhonePe", "ஃபோன்பே (PhonePe)", fin = true),
        brand("PPAYUP", "PhonePe", "ஃபோன்பே (PhonePe)", fin = true),
        brand("PAYTMU", "Paytm", "பேடிஎம் (Paytm)", fin = true),
        brand("PYTMUP", "Paytm", "பேடிஎம் (Paytm)", fin = true),
        brand("AMZPAY", "Amazon Pay", "அமேசான் பே", fin = true),
        brand("CREDCL", "CRED", "CRED", fin = true),
        brand("JUPITR", "Jupiter Money", "ஜூபிட்டர்", fin = true),
        brand("SLICEP", "Slice", "ஸ்லைஸ்", fin = true),
        // Government & Public Utilities
        brand("UIDAIA", "UIDAI Aadhaar", "ஆதார் ஆணையம் (UIDAI)", gov = true),
        brand("ADHAAR", "UIDAI Aadhaar", "ஆதார் ஆணையம் (UIDAI)", gov = true),
        brand("ITDEPT", "Income Tax Department", "வருமான வரித் துறை", gov = true),
        brand("ITDEFL", "Income Tax e-Filing", "வருமான வரித் துறை", gov = true),
        brand("VAAHAN", "Parivahan Transport", "போக்குவரத்துத் துறை (Parivahan)", gov = true),
        brand("MORTHV", "Ministry of Road Transport", "சாலைப் போக்குவரத்து அமைச்சகம்", gov = true),
        brand("TANGED", "TNEB / TANGEDCO", "தமிழ்நாடு மின்சார வாரியம் (TNEB)", gov = true),
        brand("TNPDCL", "TNEB / TNPDCL", "தமிழ்நாடு மின்சார வாரியம் (TNEB)", gov = true),
        brand("TNEBSV", "TNEB Electricity", "தமிழ்நாடு மின்சார வாரியம்", gov = true),
        brand("TNGOVT", "Government of Tamil Nadu", "தமிழ்நாடு அரசு", gov = true),
        brand("TNEGAH", "TN e-Sevai / TNeGA", "தமிழ்நாடு இ-சேவை", gov = true),
        brand("EPFOHO", "EPFO Provident Fund", "வருங்கால வைப்பு நிதி (EPFO)", gov = true),
        brand("INDPST", "India Post", "இந்திய அஞ்சல் துறை", gov = true),
        brand("RBISAY", "Reserve Bank of India", "இந்திய ரிசர்வ் வங்கி (RBI)", gov = true),
        brand("DIGILK", "DigiLocker", "டிஜிலாக்கர் (DigiLocker)", gov = true),
        brand("CYBERD", "Cyber Crime Helpline (1930)", "சைபர் கிரைம் பாதுகாப்பு (1930)", gov = true),
        brand("PMKISN", "PM-KISAN Portal", "பி.எம். கிசான் திட்டம்", gov = true),
        // Telecom, Shopping, Travel & Couriers
        brand("JIOINF", "Reliance Jio", "ரிலையன்ஸ் ஜியோ"),
        brand("JIOPAY", "Jio Recharge & Pay", "ஜியோ ரீசார்ஜ்", fin = true),
        brand("MYJIOA", "MyJio", "மைஜியோ (MyJio)"),
        brand("AIRTEL", "Airtel", "ஏர்டெல்"),
        brand("AIRTLX", "Airtel", "ஏர்டெல்"),
        brand("VICARE", "Vi (Vodafone Idea)", "வோடபோன் ஐடியா (Vi)"),
        brand("BSNLMO", "BSNL Mobile", "பிஎஸ்என்எல் (BSNL)"),
        brand("AMAZON", "Amazon India", "அமேசான் இந்தியா"),
        brand("AMZNIN", "Amazon India", "அமேசான் இந்தியா"),
        brand("FLPKRT", "Flipkart", "ஃப்ளிப்கார்ட்"),
        brand("EKARTL", "Ekart Logistics", "ஈகார்ட் கூரியர்"),
        brand("DLHVRY", "Delhivery Courier", "டெலிவரி கூரியர்"),
        brand("BLUDRT", "Blue Dart Courier", "புளூ டார்ட் கூரியர்"),
        brand("SWIGGY", "Swiggy", "ஸ்விக்கி"),
        brand("ZOMATO", "Zomato", "சொமாட்டோ"),
        brand("IRCTCi", "IRCTC Railways", "இந்திய ரயில்வே (IRCTC)", gov = true),
        brand("IRCTCS", "IRCTC Railways", "இந்திய ரயில்வே (IRCTC)", gov = true),
        brand("REDBUS", "redBus", "ரெட்பஸ் (redBus)"),
        brand("LICIND", "LIC of India", "எல்.ஐ.சி (LIC)", fin = true),
        brand("GOOGLE", "Google", "கூகுள்"),
        brand("GOVTIN", "Government of India", "இந்திய அரசு", gov = true),
    )

    private val TEXT_BANK_HINTS = listOf(
        "state bank of india" to ("State Bank of India" to "ஸ்டேட் பேங்க் ஆஃப் இந்தியா"),
        "-sbi" to ("State Bank of India" to "ஸ்டேட் பேங்க் ஆஃப் இந்தியா"),
        "hdfc bank" to ("HDFC Bank" to "HDFC வங்கி"),
        "indian overseas bank" to ("Indian Overseas Bank" to "இந்தியன் ஓவர்சீஸ் வங்கி"),
        "indian bank" to ("Indian Bank" to "இந்தியன் வங்கி"),
        "canara bank" to ("Canara Bank" to "கனரா வங்கி"),
        "karur vysya" to ("Karur Vysya Bank" to "கரூர் வைஸ்யா வங்கி"),
        "tamilnad mercantile" to ("Tamilnad Mercantile Bank" to "தமிழ்நாடு மெர்க்கன்டைல் வங்கி"),
        "city union bank" to ("City Union Bank" to "சிட்டி யூனியன் வங்கி"),
        "tamil nadu grama bank" to ("Tamil Nadu Grama Bank" to "தமிழ்நாடு கிராம வங்கி"),
        "icici bank" to ("ICICI Bank" to "ICICI வங்கி"),
        "axis bank" to ("Axis Bank" to "ஆக்சிஸ் வங்கி"),
        "kotak bank" to ("Kotak Mahindra Bank" to "கோடக் வங்கி"),
        "bank of baroda" to ("Bank of Baroda" to "பேங்க் ஆஃப் பரோடா"),
        "union bank" to ("Union Bank of India" to "யூனியன் வங்கி"),
        "punjab national bank" to ("Punjab National Bank" to "பஞ்சாப் நேஷனல் வங்கி"),
        "idfc first" to ("IDFC FIRST Bank" to "IDFC ஃபர்ஸ்ட் வங்கி"),
        "indusind bank" to ("IndusInd Bank" to "இண்டஸ்இண்ட் வங்கி"),
        "federal bank" to ("Federal Bank" to "ஃபெடரல் வங்கி"),
        "south indian bank" to ("South Indian Bank" to "சவுத் இந்தியன் வங்கி"),
        "equitas" to ("Equitas Bank" to "ஈக்விடாஸ் வங்கி"),
    )

    fun normalizeThreadKey(rawAddress: String): String {
        val trimmed = rawAddress.trim()
        val digitsOnly = trimmed.replace(Regex("[\\s()\\-]"), "")
        if (digitsOnly.matches(Regex("^\\+?91[6-9][0-9]{9}$"))) {
            return "+91" + digitsOnly.takeLast(10)
        }
        if (digitsOnly.matches(Regex("^0?[6-9][0-9]{9}$"))) {
            return "+91" + digitsOnly.takeLast(10)
        }
        if (digitsOnly.matches(Regex("^\\+[0-9]{7,15}$"))) {
            return digitsOnly
        }
        return trimmed.uppercase(Locale.US)
    }

    fun identify(
        rawAddress: String,
        messageBody: String = "",
        savedContactName: String? = null,
    ): SenderIdentity {
        val cleaned = rawAddress.trim()
        val normalized = normalizeThreadKey(cleaned)
        val upper = cleaned.uppercase(Locale.US).replace(" ", "")

        // 1. Check strictly if this is a personal phone number (human SIM / international number)
        val compactPhone = cleaned.replace(Regex("[\\s()\\-]"), "")
        val isPhoneLike = compactPhone.startsWith("+") ||
            compactPhone.matches(Regex("^0?[6-9][0-9]{9}$")) ||
            compactPhone.matches(Regex("^[0-9]{7,15}$"))

        if (isPhoneLike) {
            val masked = if (normalized.length >= 6) {
                normalized
            } else {
                cleaned
            }
            val contact = savedContactName?.trim()?.takeIf { it.isNotEmpty() }
            return SenderIdentity(
                rawAddress = cleaned,
                normalizedAddress = normalized,
                isPersonalNumber = true,
                isOfficialDltHeader = false,
                headerCode = null,
                operatorEn = null,
                circleEn = null,
                dltCategory = DltCategory.PERSONAL,
                trustLevel = if (contact != null) SenderTrustLevel.PERSONAL_CONTACT else SenderTrustLevel.PERSONAL_NUMBER,
                displayNameEn = contact ?: masked,
                displayNameTa = contact ?: masked,
                spokenNameEn = contact ?: "personal mobile number ${masked.takeLast(4)}",
                spokenNameTa = contact ?: "தனிப்பட்ட மொபைல் எண் ${masked.takeLast(4)}",
                isVerifiedFinancialSender = false,
            )
        }

        // 2. Parse TRAI DLT Header structures
        var prefix: String? = null
        var headerCode: String? = null
        var suffixChar: Char? = null

        DLT_WITH_PREFIX_AND_SUFFIX.matchEntire(upper)?.let { m ->
            prefix = m.groupValues[1]
            headerCode = m.groupValues[2]
            suffixChar = m.groupValues[3].first()
        } ?: DLT_DIRECT_WITH_SUFFIX.matchEntire(upper)?.let { m ->
            headerCode = m.groupValues[1]
            suffixChar = m.groupValues[2].first()
        } ?: DLT_LEGACY_WITH_PREFIX.matchEntire(upper)?.let { m ->
            prefix = m.groupValues[1]
            headerCode = m.groupValues[2]
        } ?: run {
            if (upper.matches(Regex("^[A-Z][A-Z0-9]{2,10}$")) || CARRIER_SHORTCODE.matches(upper)) {
                headerCode = upper
            }
        }

        val isNumericPromoHeader = headerCode?.all { it.isDigit() } == true && prefix != null
        val category = when {
            suffixChar == 'T' -> DltCategory.TRANSACTIONAL
            suffixChar == 'S' -> DltCategory.SERVICE
            suffixChar == 'G' -> DltCategory.GOVERNMENT
            suffixChar == 'P' || isNumericPromoHeader -> DltCategory.PROMOTIONAL
            headerCode != null -> DltCategory.LEGACY_DLT
            else -> DltCategory.PERSONAL
        }

        if (headerCode == null) {
            return SenderIdentity(
                rawAddress = cleaned,
                normalizedAddress = normalized,
                isPersonalNumber = true,
                isOfficialDltHeader = false,
                headerCode = null,
                operatorEn = null,
                circleEn = null,
                dltCategory = DltCategory.PERSONAL,
                trustLevel = SenderTrustLevel.PERSONAL_NUMBER,
                displayNameEn = cleaned.ifBlank { "Unknown Sender" },
                displayNameTa = cleaned.ifBlank { "அறியப்படாத அனுப்புநர்" },
                spokenNameEn = "unknown sender",
                spokenNameTa = "அறியப்படாத எண்",
                isVerifiedFinancialSender = false,
            )
        }

        val code = headerCode!!
        val operatorEn = prefix?.getOrNull(0)?.let { OPERATORS[it] }
        val circleEn = prefix?.getOrNull(1)?.let { CIRCLES[it] }
        val brand = BRANDS[code] ?: matchFuzzyBrand(code)
        val lowerBody = messageBody.lowercase(Locale.US)
        val bodyBankHint = if (brand == null && category != DltCategory.PROMOTIONAL) {
            TEXT_BANK_HINTS.firstOrNull { (token, _) -> token in lowerBody }?.second
        } else {
            null
        }

        // Determine if sender is genuinely eligible to record real financial debits/credits:
        // NEVER promotional (-P), NEVER personal SIM, ONLY known bank/UPI OR carrier-verified -T transactional bank header.
        val isVerifiedFinancial = category != DltCategory.PROMOTIONAL && (
            (brand?.isFinancial == true) ||
                bodyBankHint != null ||
                (category == DltCategory.TRANSACTIONAL && looksLikeBankHeaderOrTemplate(code, lowerBody))
            )

        val trust = when {
            category == DltCategory.PROMOTIONAL -> SenderTrustLevel.PROMOTIONAL
            brand?.isGovernment == true || category == DltCategory.GOVERNMENT -> SenderTrustLevel.VERIFIED_GOVERNMENT
            isVerifiedFinancial -> SenderTrustLevel.VERIFIED_BANK_OR_UPI
            else -> SenderTrustLevel.VERIFIED_COMPANY
        }

        val nameEn = brand?.nameEn
            ?: bodyBankHint?.first
            ?: when (category) {
                DltCategory.PROMOTIONAL -> "Promo Sender ($code)"
                DltCategory.GOVERNMENT -> "Government Official ($code)"
                else -> code
            }
        val nameTa = brand?.nameTa
            ?: bodyBankHint?.second
            ?: when (category) {
                DltCategory.PROMOTIONAL -> "விளம்பர அனுப்புநர் ($code)"
                DltCategory.GOVERNMENT -> "அரசு அறிவிப்பு ($code)"
                else -> "$code நிறுவனம்"
            }

        return SenderIdentity(
            rawAddress = cleaned,
            normalizedAddress = code,
            isPersonalNumber = false,
            isOfficialDltHeader = true,
            headerCode = code,
            operatorEn = operatorEn,
            circleEn = circleEn,
            dltCategory = category,
            trustLevel = trust,
            displayNameEn = nameEn,
            displayNameTa = nameTa,
            spokenNameEn = brand?.nameEn ?: bodyBankHint?.first ?: code,
            spokenNameTa = brand?.nameTa ?: bodyBankHint?.second ?: code,
            isVerifiedFinancialSender = isVerifiedFinancial,
        )
    }

    private fun matchFuzzyBrand(code: String): BrandProfile? {
        return when {
            code.contains("FAST") || code.endsWith("FT") && (
                code.startsWith("SBI") || code.startsWith("ICIC") || code.startsWith("HDFC") ||
                    code.startsWith("AXIS") || code.startsWith("IDFC") || code.startsWith("PYTM") ||
                    code.startsWith("KVB") || code.startsWith("IOB")
                ) -> BrandProfile(code, "${code.take(4)} FASTag", "${code.take(4)} பாஸ்டேக்", isFinancial = true)
            code == "SBIETG" || code == "SBIFST" -> BrandProfile(code, "State Bank of India FASTag", "ஸ்டேட் பேங்க் பாஸ்டேக்", isFinancial = true)
            code.startsWith("SBILIF") || code.startsWith("SBIG") || code.startsWith("SBIMF") ->
                BrandProfile(code, "SBI Insurance / Investments", "எஸ்பிஐ காப்பீடு / முதலீடு", isFinancial = false)
            code.startsWith("HDFCL") || code.startsWith("HDFCM") || code.startsWith("HDFCE") ->
                BrandProfile(code, "HDFC Insurance / Mutual Fund", "HDFC காப்பீடு / முதலீடு", isFinancial = false)
            code.startsWith("ICICIP") || code.startsWith("ICICIL") || code.startsWith("ICICIM") ->
                BrandProfile(code, "ICICI Insurance / Mutual Fund", "ICICI காப்பீடு / முதலீடு", isFinancial = false)
            code.startsWith("SBI") -> BrandProfile(code, "State Bank of India", "ஸ்டேட் பேங்க் ஆஃப் இந்தியா", isFinancial = true)
            code.startsWith("HDFC") -> BrandProfile(code, "HDFC Bank", "HDFC வங்கி", isFinancial = true)
            code.startsWith("ICICI") -> BrandProfile(code, "ICICI Bank", "ICICI வங்கி", isFinancial = true)
            code.startsWith("AXIS") -> BrandProfile(code, "Axis Bank", "ஆக்சிஸ் வங்கி", isFinancial = true)
            code.startsWith("KOTAK") -> BrandProfile(code, "Kotak Bank", "கோடக் வங்கி", isFinancial = true)
            code.startsWith("IOB") -> BrandProfile(code, "Indian Overseas Bank", "இந்தியன் ஓவர்சீஸ் வங்கி", isFinancial = true)
            code.startsWith("KVB") -> BrandProfile(code, "Karur Vysya Bank", "கரூர் வைஸ்யா வங்கி", isFinancial = true)
            code.startsWith("TMB") -> BrandProfile(code, "Tamilnad Mercantile Bank", "தமிழ்நாடு மெர்க்கன்டைல் வங்கி", isFinancial = true)
            code.startsWith("CUB") -> BrandProfile(code, "City Union Bank", "சிட்டி யூனியன் வங்கி", isFinancial = true)
            code.startsWith("CANB") -> BrandProfile(code, "Canara Bank", "கனரா வங்கி", isFinancial = true)
            code.startsWith("PNB") -> BrandProfile(code, "Punjab National Bank", "பஞ்சாப் நேஷனல் வங்கி", isFinancial = true)
            code.startsWith("IDFC") -> BrandProfile(code, "IDFC FIRST Bank", "IDFC ஃபர்ஸ்ட் வங்கி", isFinancial = true)
            code.startsWith("PAYTM") || code.startsWith("PYTM") -> BrandProfile(code, "Paytm", "பேடிஎம் (Paytm)", isFinancial = true)
            code.startsWith("PHONP") -> BrandProfile(code, "PhonePe", "ஃபோன்பே (PhonePe)", isFinancial = true)
            code.startsWith("GPAY") -> BrandProfile(code, "Google Pay", "கூகுள் பே (GPay)", isFinancial = true)
            code.startsWith("AMZN") || code.startsWith("AMAZN") -> BrandProfile(code, "Amazon", "அமேசான்")
            code.startsWith("FLPK") -> BrandProfile(code, "Flipkart", "ஃப்ளிப்கார்ட்")
            code.startsWith("JIO") -> BrandProfile(code, "Jio", "ஜியோ")
            code.startsWith("AIRT") -> BrandProfile(code, "Airtel", "ஏர்டெல்")
            code.startsWith("BSNL") -> BrandProfile(code, "BSNL", "பிஎஸ்என்எல்")
            code.startsWith("IRCTC") -> BrandProfile(code, "IRCTC Railways", "இந்திய ரயில்வே (IRCTC)", isGovernment = true)
            code.startsWith("TNEB") || code.startsWith("TANG") -> BrandProfile(code, "TNEB Electricity", "தமிழ்நாடு மின்சார வாரியம்", isGovernment = true)
            code == "1930" -> BrandProfile(code, "National Cyber Crime Portal", "சைபர் பாதுகாப்பு (1930)", isGovernment = true)
            else -> null
        }
    }

    private fun looksLikeBankHeaderOrTemplate(code: String, lowerBody: String): Boolean {
        val headerLooksBank = code.endsWith("BK") || code.endsWith("BNK") || code.contains("BANK") || code.contains("UPI")
        val bodyLooksBank = ("a/c" in lowerBody || "acct" in lowerBody || "account" in lowerBody || "upi" in lowerBody) &&
            ("credited" in lowerBody || "debited" in lowerBody || "utr" in lowerBody || "avl bal" in lowerBody)
        return headerLooksBank || bodyLooksBank
    }
}
