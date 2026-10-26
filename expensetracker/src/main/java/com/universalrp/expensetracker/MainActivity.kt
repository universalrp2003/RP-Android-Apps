package com.universalrp.expensetracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.content.ContextCompat
import com.universalrp.updates.AppLanguage
import com.universalrp.updates.AppLanguageState
import java.time.YearMonth
import java.time.ZoneId

/**
 * B2 - செலவு கண்காணிப்பு / Expense Tracker.
 *
 * Reads the owner's bank and UPI SMS, keeps only what survived the anti-spoof parser, and shows the
 * month as categories. There is no INTERNET permission in this app at all, so the ledger cannot
 * leave the phone even by accident - that is the whole privacy posture and the reason the app exists
 * separately from SMS Guard.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLanguageState.initialize(this)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Background) {
                    ExpenseScreen(onRequestSms = ::ensureSmsPermission)
                }
            }
        }
    }

    private fun ensureSmsPermission(onGranted: () -> Unit) {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            onGranted()
            return
        }
        smsPermissionCallback = onGranted
        smsPermissionLauncher.launch(
            arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS),
        )
    }

    private var smsPermissionCallback: (() -> Unit)? = null

    private val smsPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
            val callback = smsPermissionCallback
            smsPermissionCallback = null
            if (granted[Manifest.permission.READ_SMS] == true) callback?.invoke()
        }
}

/** The palette. Green for money, and not the purple of Voice Notes. */
private val Background = Color(0xFF0A0F0B)
private val Card = Color(0xFF152018)
private val CardHigh = Color(0xFF1D2B22)
private val Accent = Color(0xFF4ADE80)
private val AccentWarm = Color(0xFFFBBF24)
private val TextPrimary = Color(0xFFECFDF5)
private val TextSecondary = Color(0xFF9CB3A5)
private val Danger = Color(0xFFF87171)

@Composable
fun ExpenseScreen(onRequestSms: (() -> Unit) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var transactions by remember { mutableStateOf(LedgerWriter.load(context)) }
    var month by remember { mutableStateOf(YearMonth.now(ZoneId.systemDefault())) }
    var scanning by remember { mutableStateOf(false) }
    var hasPermission by remember {
        mutableStateOf(SmsInbox.hasPermission(context))
    }
    var excludedSpoofs by remember { mutableStateOf(0) }

    val tamil = AppLanguageState.current == AppLanguage.TAMIL

    fun tr(en: String, ta: String) = if (tamil) ta else en

    // Re-read the inbox whenever the screen comes back, so a scan run from the permission dialog
    // or from another screen shows up here without a restart.
    val scope = rememberCoroutineScope()
    DisposableEffect(Unit) {
        val watcher = SmsInbox.observe(context) {
            if (!SmsInbox.hasPermission(context)) return@observe
            // The scan is a ContentResolver walk over up to two years of messages, so it happens
            // off the main thread. Doing it inline is what turns this screen into a frozen screen
            // on a phone with a large inbox.
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                scanning = true
                val results = SmsInbox.scan(context)
                LedgerWriter.replaceAll(context, results)
                val reloaded = LedgerWriter.load(context)
                excludedSpoofs = results.count {
                    it.classification ==
                        com.universalrp.smsguard.engine.SpoofClassification
                            .SPOOF_PERSONAL_NUMBER_FAKE_MONEY ||
                        it.classification ==
                        com.universalrp.smsguard.engine.SpoofClassification
                            .REJECTED_PROMOTIONAL_OFFER
                }
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    transactions = reloaded
                    scanning = false
                }
            }
        }
        onDispose { SmsInbox.unobserver(context, watcher) }
    }

    val totals = remember(transactions, month) {
        TransactionStore.monthTotals(transactions, month)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            // targetSdk 36 means edge-to-edge is enforced on Android 15+, so the title would
            // otherwise be drawn under the clock and the footer under the navigation bar.
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // ------------------------------------------------------------------ header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tr("செலவு கண்காணிப்பு", "Expense Tracker"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
                Text(
                    text = tr(
                        "உங்கள் SMS-லிருந்து, தொலைபேசியிலேயே",
                        "From your SMS, on this phone only",
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
            }
            Text(
                text = if (tamil) "English" else "தமிழ்",
                color = Accent,
                fontSize = 13.sp,
                modifier = Modifier.clickable { AppLanguageState.toggle(context) },
            )
        }

        if (!hasPermission) {
            PermissionCard(
                tamil = tamil,
                onGrant = { onRequestSms { hasPermission = true } },
            )
        } else {
            // -------------------------------------------------------------- the month
            MonthCard(totals = totals, tamil = tamil, month = month, onChangeMonth = { month = it })

            if (scanning) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        color = Accent,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = tr("SMS-ஐப் படிக்கிறது…", "Reading your SMS…"),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                }
            }

            // ------------------------------------------------------------ categories
            if (totals.perCategory.isNotEmpty()) {
                CategoryBreakdown(totals = totals, tamil = tamil)
            }

            // ----------------------------------------------------------- transactions
            val monthRows = transactions.filter {
                MonthlyLedger.monthOf(it.timestampMs) == month
            }
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (monthRows.isEmpty()) {
                    item {
                        Text(
                            text = tr(
                                "இந்த மாதத்தில் எந்த சலுகையும் இல்லை.",
                                "No transactions recorded for this month.",
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
                items(monthRows, key = { it.messageId }) { transaction ->
                    TransactionRow(
                        transaction = transaction,
                        tamil = tamil,
                        onDelete = {
                            transactions = TransactionStore.remove(transactions, transaction.messageId)
                            LedgerWriter.save(context, transactions)
                        },
                    )
                }
            }

            // -------------------------------------------------------------- footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = tr(
                        "இணையம் தேவையில்லை · கணக்கு இல்லை · SMS மட்டும்",
                        "No internet needed · no account · SMS only",
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.weight(1f),
                )
                if (excludedSpoofs > 0) {
                    Text(
                        text = tr(
                            "$excludedSpoofs பொய் SMS நிராகரிக்கப்பட்டது",
                            "$excludedSpoofs fake messages refused",
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentWarm,
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(tamil: Boolean, onGrant: () -> Unit) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (tamil) "SMS அனுமதி தேவை" else "SMS permission needed",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (tamil) {
                "உங்கள் வங்கி & UPI SMS-ஐப் படிக்க இந்தச் செயலிக்கு அனுமதி தேவை. " +
                    "SMS-ஐ படிக்கும் படி இணையம் தேவையில்லை, மற்றல் எங்கும் அனுப்பவே இல்லை."
            } else {
                "This app needs permission to read your bank and UPI SMS. " +
                    "It reads them on this phone only and never sends them anywhere."
            },
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
        Spacer(Modifier.height(16.dp))
        Surface(
            color = Accent,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.clickable { onGrant() },
        ) {
            Text(
                text = if (tamil) "அனுமதி கொடு" else "Grant permission",
                color = Color(0xFF04120A),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun MonthCard(
    totals: MonthTotals,
    tamil: Boolean,
    month: YearMonth,
    onChangeMonth: (YearMonth) -> Unit,
) {
    Surface(
        color = Card,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = monthLabel(month, tamil),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "‹",
                    color = TextSecondary,
                    fontSize = 20.sp,
                    modifier = Modifier
                        .clickable { onChangeMonth(month.minusMonths(1)) }
                        .padding(horizontal = 10.dp),
                )
                Text(
                    text = "›",
                    color = TextSecondary,
                    fontSize = 20.sp,
                    modifier = Modifier
                        .clickable { onChangeMonth(month.plusMonths(1)) }
                        .padding(horizontal = 10.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = MonthlyLedger.formatInr(totals.spend),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Text(
                text = if (tamil) "இந்த மாதம் செலவு" else "spent this month",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
            Spacer(Modifier.height(12.dp))
            Row {
                StatChip(
                    label = if (tamil) "வருவாய்" else "Income",
                    value = MonthlyLedger.formatInr(totals.income),
                    color = Accent,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                StatChip(
                    label = if (tamil) "மீதம்" else "Net",
                    value = MonthlyLedger.formatInr(totals.net),
                    color = if (totals.net < 0) Danger else Accent,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                StatChip(
                    label = if (tamil) "பரிவர்த்தனைகள்" else "Transactions",
                    value = totals.spendCount.toString(),
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun StatChip(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(color = CardHigh, shape = RoundedCornerShape(12.dp), modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color,
            )
        }
    }
}

@Composable
private fun CategoryBreakdown(totals: MonthTotals, tamil: Boolean) {
    Surface(
        color = Card,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (tamil) "எங்கே செலவு ஆனது" else "Where it went",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )
            Spacer(Modifier.height(10.dp))
            for (total in totals.perCategory) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = total.category.label(tamil),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${(total.shareOf(totals.spend) * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = MonthlyLedger.formatInr(total.amountInr),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                    )
                }
                // The bar is the share of the month, so the biggest category is visible at a glance
                // without reading any number.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(CardHigh, RoundedCornerShape(2.dp)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(total.shareOf(totals.spend).coerceAtLeast(0.02f))
                            .height(4.dp)
                            .background(Accent, RoundedCornerShape(2.dp)),
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: Transaction,
    tamil: Boolean,
    onDelete: () -> Unit,
) {
    Surface(
        color = if (transaction.isSpend) Card else CardHigh,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = if (transaction.isSpend) Color(0xFF22332A) else Color(0xFF1E3A2A),
                shape = CircleShape,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (transaction.isSpend) "−" else "+",
                        color = if (transaction.isSpend) Danger else Accent,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.displayName(tamil),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
                Text(
                    text = "${transaction.category.label(tamil)} · ${transaction.institutionLabel(tamil)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
            }
            Text(
                text = MonthlyLedger.formatInr(transaction.amountInr),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (transaction.isSpend) TextPrimary else Accent,
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.Filled.Delete,
                contentDescription = if (tamil) "அழி" else "Delete",
                tint = TextSecondary,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onDelete() },
            )
        }
    }
}

/** "அக்டோ 2025" / "Oct 2025" - the month the owner is looking at. */
private fun monthLabel(month: YearMonth, tamil: Boolean): String {
    val en = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
    )[month.monthValue - 1]
    val ta = listOf(
        "ஜன", "பிப்", "மார்", "ஏப்", "மே", "ஜூன்",
        "ஜூலை", "ஆக", "செப்", "அக்", "நவ", "டிச",
    )[month.monthValue - 1]
    return if (tamil) "$ta ${month.year}" else "$en ${month.year}"
}
