package com.universalrp.expensetracker

import com.universalrp.smsguard.engine.FinanceParseResult
import com.universalrp.smsguard.engine.FinanceInstrumentType
import com.universalrp.smsguard.engine.SpoofClassification
import com.universalrp.smsguard.engine.TxDirection
import java.util.Locale

/**
 * B2 - where the money went.
 *
 * SMS Guard's ledger already answers *how much* left the account and whether the message was real.
 * What it does not answer, and what this app is for, is *what kind* of spending it was. A month that
 * reads "₹18,400 spent across 31 transactions" is a number; the same month read as "₹6,100 food,
 * ₹4,800 fuel, ₹3,200 bills" is something the owner can act on.
 *
 * The categoriser is deliberately a pure function of the strings the parser already extracted. It
 * never re-reads the SMS body, so it cannot disagree with the parser about what the amount was, and
 * it can be tested without a phone, an inbox or a network.
 */
enum class SpendCategory(val id: String, val labelEn: String, val labelTa: String) {
    FOOD("food", "Food & drink", "உணவு & பானம்"),
    GROCERIES("groceries", "Groceries", "மளிகை"),
    FUEL("fuel", "Fuel", "எரிவை"),
    BILLS("bills", "Bills & recharge", "பில்கள் & ரீச்சார்ஜ்"),
    TRAVEL("travel", "Travel", "பயணம்"),
    SHOPPING("shopping", "Shopping", "ஷாப்பிங்"),
    HEALTH("health", "Health", "மருத்துவம்"),
    ENTERTAINMENT("entertainment", "Entertainment", "பொழுதுபோக்கு"),
    EDUCATION("education", "Education", "கல்வி"),
    CASH("cash", "Cash", "பணம்"),
    TOLL("toll", "Toll & FASTag", "டோல் & ஃபாஸ்டேக்"),
    INCOME("income", "Income", "வருவாய்"),
    OTHER("other", "Other", "மற்றவை"),
    ;

    fun label(tamil: Boolean): String = if (tamil) labelTa else labelEn

    companion object {
        fun fromId(id: String): SpendCategory =
            entries.firstOrNull { it.id == id } ?: OTHER
    }
}

/**
 * Keyword table, first match wins.
 *
 * The order is the contract. "Indian Oil" must reach FUEL before "Indian" reaches anything else, and
 * "Apollo Pharmacy" must reach HEALTH before "Apollo" reaches SHOPPING, so the more specific rows
 * sit above the more general ones. Every entry is matched against the lowercased counterparty and
 * institution joined together, because different banks put the merchant in different fields.
 */
private val CATEGORY_RULES: List<Pair<SpendCategory, List<String>>> = listOf(
    // --- tolls and FASTag first: the parser already recognises them as their own instrument, and a
    // --- toll is the one "travel" cost an owner wants separated out because it is so predictable.
    SpendCategory.TOLL to listOf(
        "fastag", "fast tag", "toll", "nhai", "ihmcl", "netc", "toll plaza",
    ),
    SpendCategory.FUEL to listOf(
        "petrol", "diesel", "indian oil", "iocl", "hp petrol", "hindustan petroleum", "bpcl",
        "bharat petroleum", "shell", "fuel", "filling station", "essar", "nayara", "cng",
    ),
    SpendCategory.HEALTH to listOf(
        "apollo", "pharmacy", "medical", "medicals", "hospital", "clinic", "dental", "dentist",
        "lab", "diagnostic", "dr ", "doctor", "pharma", "medplus", "wellness forever", "optical",
        "spectacles", "ayush",
    ),
    SpendCategory.GROCERIES to listOf(
        "big basket", "bigbasket", "big bazaar", "dmart", "d mart", "reliance fresh", "reliance smart",
        "more supermarket", "supermarket", "grocery", "groceries", "kirana", "vegetable", "veg store",
        "nilgiri", "spar", "zepto", "blinkit", "instamart", "jio mart", "jiomart", "farmers market",
        "milk", "dairy", "sathyam supermarket",
    ),
    SpendCategory.FOOD to listOf(
        "swiggy", "zomato", "restaurant", "hotel", "cafe", "cafeteria", "tea", "tiffin", "mess",
        "biryani", "pizza", "domino", "mcdonald", "kfc", "subway", "burger", "bakes", "bakery",
        "sweet", "juice", "food", "eats", "dine", "kitchen", "canteen", "saravana bhavan",
        "annapoorna", "kumarakom", "grill", "dhaba",
    ),
    SpendCategory.BILLS to listOf(
        "electricity", "tangedco", "tnem", "bescom", "mseb", "wbsedcl", "pspcl", "bill payment",
        "broadband", "act fibernet", "airtel", "jio", "vodafone", "vi ", "bsnl", "recharge",
        "prepaid", "postpaid", "dth", "tata play", "dish tv", "sun direct", "gas", "indane",
        "hp gas", "bharat gas", "water", "cmwssb", "municipal", "property tax", "credit card bill",
        "card payment", "insurance premium", "lic ", "emi", "loan repayment",
    ),
    SpendCategory.TRAVEL to listOf(
        "irctc", "indian railway", "railway", "train", "uber", "ola", "rapido", "redbus", "abhibus",
        "bus ticket", "setc", "tnstc", "ksrtc", "msrtc", "metro", "flight", "indigo", "air india",
        "spicejet", "akasa", "makemytrip", "goibibo", "yatra", "parking", "cab", "auto",
    ),
    SpendCategory.ENTERTAINMENT to listOf(
        "netflix", "amazon prime", "prime video", "hotstar", "disney", "spotify", "youtube premium",
        "movie", "cinema", "pvr", "inox", "bookmyshow", "paytm insider", "gaana", "wynk", "game",
        "steam", "playstation", "xbox", "sports club", "gym",
    ),
    SpendCategory.EDUCATION to listOf(
        "school", "college", "university", "fees", "tuition", "course", "udemy", "coursera",
        "byju", "unacademy", "books", "stationery", "exam", "institute",
    ),
    SpendCategory.SHOPPING to listOf(
        "amazon", "flipkart", "myntra", "ajio", "meesho", "nykaa", "decathlon", "lifestyle",
        "pantaloons", "max fashion", "mall", "store", "shop", "mart", "tanishq", "kalyan",
        "textile", "silks", "footwear", "bata", "reliance digital", "croma", "poorvika",
    ),
    SpendCategory.CASH to listOf(
        "atm", "withdrawal", "cash withdrawal", "cash deposit",
    ),
)

object SpendCategoriser {

    /**
     * The category for [result].
     *
     * Only a genuine, non-self-transfer debit is a spend. Everything else has a category that is
     * really a statement about what it is: income, a transfer between the owner's own accounts, or
     * a message the parser refused to believe. Putting those in OTHER would quietly inflate the
     * spend total with money that never left.
     */
    fun categorise(result: FinanceParseResult): SpendCategory {
        if (result.classification != SpoofClassification.GENUINE_BANK_OR_UPI) return SpendCategory.OTHER
        if (result.isSelfTransfer) return SpendCategory.OTHER
        if (result.direction == TxDirection.CREDIT) return SpendCategory.INCOME
        if (result.amountInr <= 0.0) return SpendCategory.OTHER
        return fromMerchant(result)
    }

    /**
     * Matches the merchant text, most specific rule first.
     *
     * The search text is the counterparty plus the institution plus the UPI rail label, because the
     * same merchant arrives as "SWIGGY" in one bank's SMS and as "Bangalore" in another's. Matching
     * against all three is what stops a well-known merchant falling through to OTHER.
     */
    fun fromMerchant(result: FinanceParseResult): SpendCategory {
        val haystack = listOf(
            result.counterparty.orEmpty(),
            result.institutionEn,
            result.upiRailLabel.orEmpty(),
            result.accountDisplayLabel,
        ).joinToString(" ").lowercase(Locale.ROOT)

        if (haystack.isBlank()) return SpendCategory.OTHER
        for ((category, keywords) in CATEGORY_RULES) {
            if (keywords.any { haystack.contains(it) }) return category
        }
        return SpendCategory.OTHER
    }

    /** True when [result] is a real spend that belongs in the month's total. */
    fun isSpend(result: FinanceParseResult): Boolean {
        if (result.classification != SpoofClassification.GENUINE_BANK_OR_UPI) return false
        if (result.direction != TxDirection.DEBIT) return false
        if (result.isSelfTransfer) return false
        return result.amountInr > 0.0
    }

    /** True when [result] is real income. Self-transfers are not income, in either direction. */
    fun isIncome(result: FinanceParseResult): Boolean {
        if (result.classification != SpoofClassification.GENUINE_BANK_OR_UPI) return false
        if (result.direction != TxDirection.CREDIT) return false
        if (result.isSelfTransfer) return false
        return result.amountInr > 0.0
    }

    /**
     * True when the parser recognised a FASTag toll. Kept as its own predicate rather than inferred
     * from the category so a toll stays a toll even if the merchant text also mentions a fuel pump.
     */
    fun isToll(result: FinanceParseResult): Boolean =
        result.instrumentType == FinanceInstrumentType.FASTAG_TOLL
}
