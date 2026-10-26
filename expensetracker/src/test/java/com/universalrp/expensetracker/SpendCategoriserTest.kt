package com.universalrp.expensetracker

import com.universalrp.smsguard.engine.FinanceInstrumentType
import com.universalrp.smsguard.engine.FinanceParseResult
import com.universalrp.smsguard.engine.SpoofClassification
import com.universalrp.smsguard.engine.TxDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * B2 - what the money was spent on, and what must never be counted at all.
 *
 * Two failure modes here are invisible on a device and both are expensive. A spoofed "Rs.5000
 * credited" from a personal number that lands in the income total is money the owner believes they
 * have. And a self-transfer counted as spend is money the owner thinks left when it only moved
 * between their own accounts. Neither says anything on screen, so both are pinned here.
 */
class SpendCategoriserTest {

    private fun result(
        amount: Double = 500.0,
        direction: TxDirection = TxDirection.DEBIT,
        classification: SpoofClassification = SpoofClassification.GENUINE_BANK_OR_UPI,
        counterparty: String? = null,
        institutionEn: String = "",
        upiRailLabel: String? = null,
        accountDisplayLabel: String = "",
        accountKey: String = "SBI ••4821",
        isSelfTransfer: Boolean = false,
        instrumentType: FinanceInstrumentType = FinanceInstrumentType.BANK_ACCOUNT_OR_UPI,
        messageId: Long = 1L,
        timestampMs: Long = 1_700_000_000_000L,
    ) = FinanceParseResult(
        messageId = messageId,
        timestampMs = timestampMs,
        classification = classification,
        direction = direction,
        amountInr = amount,
        institutionEn = institutionEn,
        accountKey = accountKey,
        accountDisplayLabel = accountDisplayLabel,
        upiRailLabel = upiRailLabel,
        counterparty = counterparty,
        isSelfTransfer = isSelfTransfer,
        instrumentType = instrumentType,
    )

    // ------------------------------------------------------- what counts as spend

    @Test
    fun aGenuineDebitIsSpend() {
        assertTrue(SpendCategoriser.isSpend(result()))
    }

    @Test
    fun aCreditIsNotSpend() {
        assertFalse(SpendCategoriser.isSpend(result(direction = TxDirection.CREDIT)))
    }

    @Test
    fun aSelfTransferIsNotSpendEvenThoughItIsADebit() {
        // Money moving between the owner's own accounts is the single most common way an expense
        // tracker inflates itself. The parser flags it; the categoriser must honour the flag.
        assertFalse(SpendCategoriser.isSpend(result(isSelfTransfer = true)))
        assertFalse(SpendCategoriser.isIncome(result(isSelfTransfer = true)))
    }

    @Test
    fun aSpoofedPersonalNumberIsNeverSpendAndNeverIncome() {
        val spoof = result(classification = SpoofClassification.SPOOF_PERSONAL_NUMBER_FAKE_MONEY)
        assertFalse(SpendCategoriser.isSpend(spoof))
        assertFalse(SpendCategoriser.isIncome(spoof))
        assertEquals(SpendCategory.OTHER, SpendCategoriser.categorise(spoof))
    }

    @Test
    fun aPromotionalLoanOfferIsNeverSpendAndNeverIncome() {
        val promo = result(classification = SpoofClassification.REJECTED_PROMOTIONAL_OFFER)
        assertFalse(SpendCategoriser.isSpend(promo))
        assertFalse(SpendCategoriser.isIncome(promo))
    }

    @Test
    fun aZeroOrNegativeAmountIsNeverSpend() {
        assertFalse(SpendCategoriser.isSpend(result(amount = 0.0)))
        assertFalse(SpendCategoriser.isSpend(result(amount = -50.0)))
    }

    @Test
    fun anOutgoingBeneficiarySettlementIsNotIncome() {
        // The bank sends "credited to the beneficiary's account" hours after the owner's own debit.
        // Counting it as income would show the same money twice.
        val settlement = result(
            direction = TxDirection.CREDIT,
            classification = SpoofClassification.OUTGOING_TRANSFER_SETTLEMENT,
        )
        assertFalse(SpendCategoriser.isIncome(settlement))
        assertFalse(SpendCategoriser.isSpend(settlement))
    }

    @Test
    fun aGenuineCreditIsIncomeAndIsCategorisedAsIncomeNotOther() {
        val salary = result(direction = TxDirection.CREDIT, counterparty = "ACME CORP")
        assertTrue(SpendCategoriser.isIncome(salary))
        assertEquals(SpendCategory.INCOME, SpendCategoriser.categorise(salary))
    }

    // ------------------------------------------------------------------ categories

    @Test
    fun aMerchantIsMatchedRegardlessOfCase() {
        assertEquals(
            SpendCategory.FOOD,
            SpendCategoriser.categorise(result(counterparty = "swiggy")),
        )
        assertEquals(
            SpendCategory.FOOD,
            SpendCategoriser.categorise(result(counterparty = "SWIGGY")),
        )
        assertEquals(
            SpendCategory.FOOD,
            SpendCategoriser.categorise(result(counterparty = "Swiggy Instamart")),
        )
    }

    @Test
    fun theMoreSpecificRuleWinsOverTheMoreGeneralOne() {
        // "Indian Oil" contains "indian", and "Apollo Pharmacy" contains a word that would
        // otherwise read as a shop. The order of the rule table is the contract.
        assertEquals(
            SpendCategory.FUEL,
            SpendCategoriser.categorise(result(counterparty = "INDIAN OIL PETROL PUMP")),
        )
        assertEquals(
            SpendCategory.HEALTH,
            SpendCategoriser.categorise(result(counterparty = "APOLLO PHARMACY")),
        )
        assertEquals(
            SpendCategory.TOLL,
            SpendCategoriser.categorise(
                result(
                    counterparty = "NHAI FASTAG TOLL PLAZA",
                    instrumentType = FinanceInstrumentType.FASTAG_TOLL,
                ),
            ),
        )
    }

    @Test
    fun aTollStaysATollEvenWhenTheTextAlsoMentionsFuel() {
        // Some toll plazas sit at fuel stations, and the merchant text says so. A toll is the cost
        // an owner wants separated out because it is so predictable.
        assertEquals(
            SpendCategory.TOLL,
            SpendCategoriser.categorise(
                result(
                    counterparty = "HP PETROL FASTAG TOLL",
                    instrumentType = FinanceInstrumentType.FASTAG_TOLL,
                ),
            ),
        )
    }

    @Test
    fun aMerchantFoundInTheInstitutionFieldIsStillCategorised() {
        // Different banks put the merchant in different fields. Matching only the counterparty
        // would drop half of them into OTHER.
        assertEquals(
            SpendCategory.BILLS,
            SpendCategoriser.categorise(result(counterparty = null, institutionEn = "AIRTEL PREPAID")),
        )
        assertEquals(
            SpendCategory.TRAVEL,
            SpendCategoriser.categorise(result(counterparty = null, upiRailLabel = "IRCTC UPI")),
        )
    }

    @Test
    fun anUnknownMerchantFallsToOtherRatherThanBeingDropped() {
        assertEquals(
            SpendCategory.OTHER,
            SpendCategoriser.categorise(result(counterparty = "SOME LOCAL SHOP")),
        )
    }

    @Test
    fun everyCategoryHasBothLabels() {
        for (category in SpendCategory.entries) {
            assertTrue(category.labelEn.isNotBlank())
            assertTrue(category.labelTa.isNotBlank())
            assertTrue(category.id.isNotBlank())
        }
    }

    @Test
    fun aCategoryRoundTripsThroughItsId() {
        for (category in SpendCategory.entries) {
            assertEquals(category, SpendCategory.fromId(category.id))
        }
    }

    @Test
    fun anUnknownCategoryIdFallsBackToOtherRatherThanCrashing() {
        assertEquals(SpendCategory.OTHER, SpendCategory.fromId("not-a-category"))
        assertEquals(SpendCategory.OTHER, SpendCategory.fromId(""))
    }

    @Test
    fun labelsSwitchOnTheLanguageFlag() {
        assertEquals("உணவு & பானம்", SpendCategory.FOOD.label(tamil = true))
        assertEquals("Food & drink", SpendCategory.FOOD.label(tamil = false))
    }
}
