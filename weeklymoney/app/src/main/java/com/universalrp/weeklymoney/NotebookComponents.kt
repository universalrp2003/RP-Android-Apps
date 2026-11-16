package com.universalrp.weeklymoney

import android.app.DatePickerDialog
import android.view.WindowManager
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlin.math.absoluteValue
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dateFormat = DateTimeFormatter.ofPattern("dd-MM-uuuu")
private val blueInk = Color(0xff153c8b)
private val redInk = Color(0xffb3323c)
private val paper = Color(0xfffff8df)
@Composable fun DateChoice(label: String, value: LocalDate, allowed: Boolean, tag: String, change: (LocalDate) -> Unit) {
    val context = LocalContext.current
    Column {
        Text(label)
        TextButton(modifier = Modifier.testTag(tag), onClick = {
            DatePickerDialog(context, { _, y, m, d -> change(LocalDate.of(y, m + 1, d)) }, value.year, value.monthValue - 1, value.dayOfMonth).also { picker ->
                picker.setOnShowListener {
                    if (allowed) picker.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    else picker.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }.show()
        }) { Text(value.format(dateFormat)) }
    }
}
@Composable private fun EntryField(label: String, value: String, tag: String, numeric: Boolean = false, change: (String) -> Unit) {
    OutlinedTextField(value, change, label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text),
        modifier = Modifier.fillMaxWidth().testTag(tag))
}
@Composable fun LoanEntryDialog(state: NotebookState, book: Book, seed: Loan?, edit: Boolean,
    busy: Boolean, privacy: DialogProperties, allowed: Boolean, historyEnabled: Boolean, dismiss: () -> Unit,
    commit: (NotebookState, (String?) -> Unit) -> Unit, saved: (Loan, Boolean) -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    var place by remember { mutableStateOf(seed?.place ?: "") }
    var family by remember { mutableStateOf(seed?.familyHead ?: "") }
    var name by remember { mutableStateOf(seed?.customerName ?: "") }
    var amount by remember { mutableStateOf(if (edit && seed != null) rupees(seed.totalPaise) else "") }
    var date by remember { mutableStateOf(if (edit && seed != null) seed.started else LocalDate.now()) }
    var linked by remember { mutableStateOf(if (!edit) seed else null) }
    var resolved by remember { mutableStateOf(seed != null) }
    var choosing by remember { mutableStateOf(false) }
    var openingInk by remember { mutableStateOf(seed?.openingInk ?: Ink.BLUE) }
    var error by remember { mutableStateOf("") }
    val scroll = rememberScrollState()
    LaunchedEffect(error, choosing) { if (error.isNotEmpty() || choosing) scroll.animateScrollTo(0) }
    fun submit() {
        try {
            error = ""
            val matches = state.matchingCustomers(name)
            if (!edit && !resolved && matches.isNotEmpty()) { choosing = true; return }
            val created = if (edit && seed != null) seed.copy(place = place.trim(), familyHead = family.trim(), customerName = name.trim(), totalPaise = parseRupees(amount), started = date, openingInk = if (historyEnabled) openingInk else seed.openingInk)
                else state.nextLoan(book, place, family, name, amount, date, linked).copy(openingInk = if (historyEnabled) openingInk else Ink.BLUE)
            val next = if (edit && seed != null) state.correctLoan(seed, created) else state.copy(loans = state.loans + created)
            commit(next) { problem -> if (problem == null) { keyboard?.hide(); saved(created, false) } else error = problem }
        } catch (e: Exception) {
            error = (e as? InputProblem)?.explanation ?: "விவரங்கள், தொகை, தேதியைச் சரிபார்க்கவும் / Check details, total and date against saved payments"
        }
    }
    AlertDialog(properties = privacy, onDismissRequest = { if (!busy) dismiss() },
        title = { Text(if (edit) "விவரங்கள் திருத்து / Edit details" else "புதிய கடன் / New loan") }, text = {
            Column(Modifier.verticalScroll(scroll)) {
                if (error.isNotEmpty()) Text(error, color = redInk, modifier = Modifier.testTag("loanError"))
                if (choosing) {
                    Text("இதே பெயரில் பதிவு உள்ளது / Matching name already exists. Is this the same person?", modifier = Modifier.testTag("duplicatePrompt"))
                    state.matchingCustomers(name).forEach { candidate ->
                        val balance = state.customerLoans(candidate).sumOf { state.ledger().balance(it.id).remainingPaise }
                        Text("${candidate.place} → ${candidate.familyHead} → ${candidate.customerName}\n" +
                            if (balance > 0) "கடன் உள்ளது / Outstanding ₹${rupees(balance)}" else "முழுவதும் செலுத்தப்பட்டது / Fully paid. New loan needed?")
                        TextButton(modifier = Modifier.testTag("reuse-${candidate.id}"), onClick = {
                            linked = candidate; place = candidate.place; family = candidate.familyHead; name = candidate.customerName
                            resolved = true; choosing = false
                        }) { Text("ஆம், இவருக்குப் புதிய கடன் / Yes, new loan for this person") }
                    }
                    TextButton(modifier = Modifier.testTag("differentCustomer"), onClick = { linked = null; resolved = true; choosing = false }) {
                        Text("வேறு நபர் / Different person with the same name")
                    }
                } else {
                    linked?.let { prior ->
                        val remaining = state.customerLoans(prior).sumOf { state.ledger().balance(it.id).remainingPaise }
                        Text(if (remaining > 0) "கடன் உள்ளது / Existing balance ₹${rupees(remaining)}. This creates a separate loan."
                             else "முந்தைய கடன் முடிந்தது / Previous loan completed. Confirm new borrowing with Save.")
                    }
                    EntryField("இடம் / Place", place, "loanPlace") { place = it; if (seed == null) { resolved = false; linked = null } }
                    EntryField("குடும்பத் தலைவர் / Family head", family, "loanFamily") { family = it; if (seed == null) { resolved = false; linked = null } }
                    EntryField("வாடிக்கையாளர் / Customer", name, "loanName") { name = it; if (seed == null) { resolved = false; linked = null } }
                    EntryField("மொத்த ரூபாய் / Total rupees", amount, "loanAmount", true) { amount = it }
                    DateChoice("கடன் தேதி / Loan date", date, allowed, "loanDate") { date = it }
                    if (historyEnabled) {
                        Text("பழைய பதிவு நிறம் / Opening group")
                        Row {
                            FilterChip(selected = openingInk == Ink.BLUE, onClick = { openingInk = Ink.BLUE },
                                modifier = Modifier.testTag("openingBlue"), label = { Text("நீலம் · புதிய பதிவு", color = blueInk) })
                        }
                        Row {
                            FilterChip(selected = openingInk == Ink.RED, onClick = { openingInk = Ink.RED },
                                modifier = Modifier.testTag("openingRed"), label = { Text("சிவப்பு · 10 வாரம் கடந்த பாக்கி", color = redInk) })
                        }
                        Text("அசல் கடன் தேதி, அசல் மொத்தம் மற்றும் பழைய வசூல்களை உள்ளிடவும். நிறம் தொகை/தேதியை மாற்றாது.")
                    }
                    if (busy) Text("சேமிக்கிறது / Saving…")
                }
            }
        }, confirmButton = { if (!choosing) TextButton(enabled = !busy, modifier = Modifier.testTag("loanSave"), onClick = { submit() }) { Text("சேமி / Save") } },
        dismissButton = { TextButton(enabled = !busy, onClick = dismiss) { Text("ரத்து / Cancel") } })
}
@Composable fun WeeklyDialog(state: NotebookState, loan: Loan, initialWeek: Int?, allInitially: Boolean, auditInitially: Boolean,
    busy: Boolean, privacy: DialogProperties, allowed: Boolean, historyEnabled: Boolean, dismiss: () -> Unit,
    commit: (NotebookState, (String?) -> Unit) -> Unit, editDetails: () -> Unit, renew: () -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    val ledger = remember(state) { state.ledger() }
    val today = LocalDate.now()
    val book = state.books.single { it.id == loan.bookId }
    val old = ledger.latest().filter { it.loanId == loan.id }.associateBy { it.week }
    val target = if (historyEnabled) initialWeek ?: state.entryWeek(loan, today) else state.entryWeek(loan, today)
    var cycle by remember { mutableStateOf((target - 1) / 10) }
    var selected by remember { mutableStateOf((target - 1) % 10) }
    val all = false
    var audit by remember { mutableStateOf(auditInitially) }
    var error by remember { mutableStateOf("") }
    var todayAmount by remember { mutableStateOf("") }
    val amounts = remember(cycle) { mutableStateListOf<String>().apply { repeat(10) { i -> val p = old[cycle * 10 + i + 1]; add(if (p == null) "" else p.amountPaise?.let(::rupees) ?: "-") } } }
    val dates = remember(cycle) { mutableStateListOf<LocalDate>().apply { repeat(10) { i -> add(old[cycle * 10 + i + 1]?.receivedOn ?: state.dueDate(loan, cycle * 10 + i + 1)) } } }
    val scroll = rememberScrollState()
    val remaining = ledger.balance(loan.id).remainingPaise
    val entered = runCatching { if (todayAmount.trim() == "-") 0L else parseRupees(todayAmount) }.getOrNull()
    val canCollect = !state.isArchived(state.activeBookId(loan)) && state.collectionWeek(loan, today) != null && !ledger.balance(loan.id).completed
    LaunchedEffect(error) { if (error.isNotEmpty()) scroll.animateScrollTo(0) }
    AlertDialog(properties = privacy, onDismissRequest = { if (!busy) dismiss() },
        title = { Text("${loan.place} → ${loan.familyHead} → ${state.customerLabel(loan)}", style = MaterialTheme.typography.titleMedium) }, text = {
            Column(Modifier.verticalScroll(scroll)) {
                if (error.isNotEmpty()) Text(error, modifier = Modifier.testTag("receiptError"), color = redInk)
                Text("மீதி / Remaining ₹${rupees(remaining)}", modifier = Modifier.testTag("remainingBalance"))
                Text("கடன் தேதி / Loan ${loan.started.format(dateFormat)}")
                if (ledger.balance(loan.id).completed) {
                    Text("முழுவதும் செலுத்தப்பட்டது / Completed")
                    TextButton(modifier = Modifier.testTag("renewLoan"), onClick = renew) { Text("புதிய கடன் / New loan with same details?") }
                }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    TextButton(enabled = cycle > 0, onClick = { cycle--; error = "" }) { Text("‹") }
                    Text("10 வார அட்டவணை / Cycle ${cycle + 1}")
                    TextButton(enabled = cycle < maxOf((old.keys.maxOrNull() ?: 1) - 1, state.entryWeek(loan, today) - 1) / 10,
                        onClick = { cycle++; error = "" }) { Text("›") }
                }
                // Two rows of five: date above each amount; absent stays blank, missed is a dash.
                repeat(2) { row ->
                    Row(Modifier.fillMaxWidth()) { repeat(5) { column ->
                        val i = row * 5 + column
                        val week = cycle * 10 + i + 1
                        val p = old[week]
                        Column(Modifier.weight(1f).padding(2.dp).testTag("overviewWeek-${i + 1}")) {
                            Text("${i + 1}", style = MaterialTheme.typography.labelSmall)
                            Text(state.dueDate(loan, week).format(DateTimeFormatter.ofPattern("dd-MM-yy")),
                                modifier = Modifier.testTag("dueDate-${i + 1}"), style = MaterialTheme.typography.labelSmall)
                            Box(Modifier.fillMaxWidth().heightIn(min = 44.dp).border(1.dp,
                                if (historyEnabled && selected == i) blueInk else Color.Gray)
                                .then(if (historyEnabled && !state.isAfterSettlement(loan, week)) Modifier.clickable { selected = i } else Modifier).padding(3.dp)) {
                                Text(if (p == null && state.isAfterSettlement(loan, week)) "–" else if (p == null) "" else p.amountPaise?.let(::rupees) ?: "–",
                                    modifier = Modifier.testTag("overviewAmount-${i + 1}"), color = if (p?.amountPaise == null) Color(0xff805029) else blueInk,
                                    style = MaterialTheme.typography.bodySmall)
                            }
                            if (p != null && p.receivedOn != state.dueDate(loan, week))
                                Text("பெற்றது ${p.receivedOn.format(DateTimeFormatter.ofPattern("dd-MM"))}", style = MaterialTheme.typography.labelSmall)
                        }
                    } }
                }
                if (historyEnabled) {
                    Text("பழைய பதிவுகள் திருத்தம் ON / History entry ON", color = redInk)
                    TextButton(onClick = editDetails, modifier = Modifier.testTag("editLoanDetails")) { Text("பெயர், தொகை, தேதி திருத்து / Edit loan details") }
                    Text("மேலே தேவையான வாரக் கட்டத்தைத் தொட்டு பழைய பதிவை உள்ளிடவும் / Tap a week above to enter or correct it")
                    Text("வெற்று = மாற்றமில்லை / Blank leaves unchanged")
                    val shown = if (all) (0..9).toList() else listOf(selected)
                    shown.filter { !state.isAfterSettlement(loan, cycle * 10 + it + 1) }.forEach { i ->
                        Text("வாரம் / Week ${i + 1} · ${state.dueDate(loan, cycle * 10 + i + 1).format(dateFormat)}")
                        EntryField("ரூபாய் / Amount or -", amounts[i], "receiptAmount-${i + 1}", true) { amounts[i] = it }
                        TextButton(modifier = Modifier.testTag("markMissed-${i + 1}"), enabled = !busy, onClick = { amounts[i] = "-" }) { Text("வசூல் இல்லை / Mark missed –") }
                        DateChoice("பெற்ற தேதி / Actual collection date", dates[i], allowed, "receiptDate-${i + 1}") { dates[i] = it }
                    }
                } else {
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Text("இன்றைய வசூல் / Today's collection · ${today.format(dateFormat)}")
                    if (!canCollect) Text("அடுத்த வசூல் / Next ${state.dueDate(loan, state.entryWeek(loan, today)).format(dateFormat)}. இன்று வசூல் கட்டம் இல்லை.")
                    if (canCollect) EntryField("இன்று பெற்ற தொகை / Additional amount today", todayAmount, "todayAmount", true) { todayAmount = it }
                    else Text(if (remaining == 0L) "கடன் தீர்க்கப்பட்டது" else "இன்று வசூல் உள்ளீடு இல்லை", modifier = Modifier.testTag("todayAmount"), color = Color.Gray)
                    TextButton(modifier = Modifier.testTag("todayMissed"), enabled = canCollect && !busy, onClick = { todayAmount = "-" }) { Text("வசூல் இல்லை / Mark missed –") }
                    Text("புதிய மீதி / New balance ₹${rupees(if (entered == null) remaining else remaining - entered)}",
                        modifier = Modifier.testTag("newBalance"), color = if (entered != null && entered > remaining) redInk else blueInk)
                    Text("சேமித்த பிறகே புத்தகம் மாறும் / Preview only until Save", style = MaterialTheme.typography.labelSmall)
                }
                var fullHistory by remember { mutableStateOf(false) }
                TextButton(modifier = Modifier.testTag("fullPaymentHistory"), onClick = { fullHistory = !fullHistory }) {
                    Text("முழு வசூல் வரலாறு / All payments (${old.size})")
                }
                if (fullHistory) {
                    if (old.isEmpty()) Text("பதிவுகள் இல்லை / No recorded payments")
                    old.values.sortedWith(compareBy<Payment> { it.receivedOn }.thenBy { it.week }).forEach { payment ->
                        Text("W${payment.week} · ${payment.receivedOn.format(dateFormat)} · ${payment.amountPaise?.let { "₹${rupees(it)}" } ?: "–"}")
                    }
                    Text("பதிவு இல்லாத வாரங்கள் பணம் செலுத்தியதாகக் காட்டப்படாது / Unrecorded weeks are not payments", style = MaterialTheme.typography.labelSmall)
                }
                TextButton(onClick = { audit = !audit }) { Text("பதிவு / திருத்த வரலாறு · History") }
                if (audit) {
                    state.audits.filter { it.after.loanId == loan.id }.forEach { a ->
                        Text("W${a.after.week} · ${a.before?.amountPaise?.let(::rupees) ?: "–"} → ${a.after.amountPaise?.let(::rupees) ?: "–"}\n${a.after.receivedOn} · ${a.after.actorName} · ${a.after.recordedAt} · rev ${a.after.revision}")
                    }
                    state.loanChanges.filter { it.after.id == loan.id }.forEach { c -> Text("${c.before.customerName} ₹${rupees(c.before.totalPaise)} ${c.before.started} → ${c.after.customerName} ₹${rupees(c.after.totalPaise)} ${c.after.started}\n${state.owner.name} · ${c.at}") }
                }
            }
        }, confirmButton = { TextButton(enabled = !busy && (if (historyEnabled) !state.isAfterSettlement(loan, cycle * 10 + selected + 1) else canCollect), modifier = Modifier.testTag("receiptSave"), onClick = {
            try {
                error = ""
                val next = if (!historyEnabled) state.withTodayReceipt(loan, todayAmount, today) else {
                    val indices = if (all) (0..9).toList() else listOf(selected)
                    indices.filter { amounts[it].isNotBlank() }.forEach { i ->
                        val previous = old[cycle * 10 + i + 1]
                        if (dates[i] > today || (!state.isCollectionDate(book, dates[i]) && dates[i] != previous?.receivedOn))
                            throw InputProblem("வசூல் கிழமை / விடுமுறை / தேதியைச் சரிபார்க்கவும் / Choose an active collection date, not a future date")
                    }
                    state.withReceipts(loan, indices.map { ReceiptDraft(cycle * 10 + it + 1, amounts[it], dates[it]) })
                }
                commit(next) { problem -> if (problem == null) { keyboard?.hide(); dismiss() } else error = problem }
            } catch (e: Exception) { error = (e as? InputProblem)?.explanation ?: "தொகை, தேதியைச் சரிபார்க்கவும் / Check amounts and dates" }
        }) { Text("சேமி / Save") } }, dismissButton = { TextButton(enabled = !busy, modifier = Modifier.testTag("receiptClose"), onClick = dismiss) { Text("மூடு / Close") } })
}
@OptIn(ExperimentalFoundationApi::class)
@Composable fun LedgerBook(state: NotebookState, book: Book, search: String, modifier: Modifier,
    tamilDateEnabled: Boolean = false, requestedPageDate: LocalDate? = null,
    onArchivePage: (Loan, NotebookPage) -> Unit = { _, _ -> }, onOpen: (Loan, Int?, Boolean) -> Unit) {
    var zoom by remember(book.id) { mutableStateOf(1f) }
    var pinchEnabled by remember { mutableStateOf(false) }
    val baseDensity = LocalDensity.current
    val pinch = Modifier.pointerInput(book.id, pinchEnabled) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            var gestureZoom = zoom
            do {
                val event = awaitPointerEvent()
                if (pinchEnabled && event.changes.count { it.pressed } >= 2) {
                    val factor = event.calculateZoom()
                    if (factor.isFinite()) gestureZoom = (gestureZoom * factor).coerceIn(0.2f, 2.2f)
                    event.changes.forEach { it.consume() }
                }
            } while (event.changes.any { it.pressed })
            // Reflow the ruled table once on release, not for every two-finger event.
            if (pinchEnabled) zoom = gestureZoom
        }
    }
    // Pinch/zoom recomposes frequently: never replay the audited ledger for every cell/frame.
    val settlements = remember(state) { state.effectiveLoans().associate { it.id to state.settlementWeek(it) } }
    val labels = remember(state) { state.effectiveLoans().associate { it.id to state.customerLabel(it) } }
    val pages = remember(state, book.id) { state.pages(book.id) }
    val filteredIds = remember(state, search) { if (search.isBlank()) null else state.ledger().search(search).map { it.id }.toSet() }
    val pager = rememberPagerState(pageCount = { pages.size.coerceAtLeast(1) })
    val scope = rememberCoroutineScope()
    LaunchedEffect(search, pages, requestedPageDate) {
        val requestedIndex = pages.indexOfFirst { it.date == requestedPageDate }
        if (requestedIndex >= 0) { pager.scrollToPage(requestedIndex); return@LaunchedEffect }
        if (search.isBlank() && pages.isNotEmpty()) pager.scrollToPage(pages.lastIndex)
        if (search.isNotBlank()) {
            val index = pages.indexOfFirst { page -> state.pageLines(book.id, page).any { it.loan.id in filteredIds.orEmpty() } }
            if (index >= 0) pager.scrollToPage(index)
        }
    }
    Column(modifier.then(pinch).background(paper).border(1.dp, redInk).testTag("ledgerBook")) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${(zoom * 100).toInt()}%", modifier = Modifier.testTag("bookZoom"))
            TextButton(onClick = { zoom = (zoom - .15f).coerceAtLeast(.4f) }) { Text("−") }
            TextButton(onClick = { zoom = (zoom + .15f).coerceAtMost(1.6f) }) { Text("+") }
            TextButton(onClick = { zoom = 1f }, modifier = Modifier.testTag("resetBookZoom")) { Text("100%") }
            TextButton(onClick = { pinchEnabled = !pinchEnabled }, modifier = Modifier.testTag("pinchToggle")) { Text(if (pinchEnabled) "Pinch ON" else "Pinch OFF") }
        }
        Row(Modifier.fillMaxWidth()) {
            TextButton(enabled = pager.currentPage > 0, onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }) { Text("‹ முந்தைய / Previous") }
            Text("${pager.currentPage + 1}/${pages.size.coerceAtLeast(1)}", Modifier.padding(top = 14.dp))
            TextButton(enabled = pager.currentPage < pages.lastIndex, modifier = Modifier.testTag("nextLedgerPage"), onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }) { Text("அடுத்த / Next ›") }
        }
        if (pages.isEmpty()) Text("புதிய கடன் சேர்க்கவும் / Add a loan to start the ledger", Modifier.padding(12.dp))
        else HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth().weight(1f)) { index ->
            val page = pages[index]
            val rows = remember(state, book.id, page, filteredIds) {
                state.pageLines(book.id, page).filter { filteredIds == null || it.loan.id in filteredIds }
            }
            val payments = remember(state) { state.ledger().latest() }.associateBy { it.loanId to it.week }
            CompositionLocalProvider(LocalDensity provides Density(baseDensity.density * zoom, baseDensity.fontScale)) {
            val turn = ((pager.currentPage - index) + pager.currentPageOffsetFraction).coerceIn(-1f, 1f)
            Column(Modifier.fillMaxSize().graphicsLayer {
                transformOrigin = TransformOrigin(if (turn >= 0f) 0f else 1f, 0.5f)
                rotationY = turn * -65f
                cameraDistance = 24f * density
            }.shadow(3.dp, RoundedCornerShape(2.dp)).background(paper).drawWithContent {
                drawContent()
                drawRect(Color(0xff3b2b18).copy(alpha = turn.absoluteValue * 0.16f))
            }) {
                if (book.heading.isNotBlank()) Text(book.heading, Modifier.padding(horizontal = 8.dp), color = blueInk)
                rows.map { if (it.cycle == 0) it.loan.started else page.date }.distinct().sorted().forEach { date ->
                    Text(date.format(dateFormat), Modifier.padding(horizontal = 8.dp).testTag("pageStartDate"), color = blueInk)
                    if (tamilDateEnabled) TamilLoanDate.label(date)?.let { label ->
                        Text(label, Modifier.padding(horizontal = 8.dp).testTag("pageTamilDate"), color = blueInk, style = MaterialTheme.typography.bodySmall)
                    }
                }
                rows.filter { it.cycle > 0 }.mapNotNull { row ->
                    state.previousPage(row.loan, row.cycle)?.let { previous ->
                        Triple(previous, if (row.cycle == 1) row.loan.started else previous.date, row.loan)
                    }
                }.distinctBy { it.first to it.second }.forEach { (previous, date, loan) ->
                    val previousIndex = pages.indexOf(previous)
                    TextButton(modifier = Modifier.testTag("previousDate-${date}"), onClick = {
                        if (previousIndex >= 0) scope.launch { pager.animateScrollToPage(previousIndex) }
                        else onArchivePage(loan, previous)
                    }) { Text(date.format(dateFormat), color = redInk, style = MaterialTheme.typography.labelSmall) }
                }
                // Ruled two-page ledger, horizontally scrollable at readable text size on phones.
                Box(Modifier.fillMaxSize().horizontalScroll(rememberScrollState()).testTag("ledgerHorizontal")) {
                Column(Modifier.width(IntrinsicSize.Max).fillMaxHeight().verticalScroll(rememberScrollState()).testTag("ledgerVertical")) {
                    Row(Modifier.height(IntrinsicSize.Min)) {
                        listOf("இடம்\nPlace" to 140, "குடும்பத் தலைவர்\nFamily head" to 160, "வாடிக்கையாளர்\nCustomer" to 160, "மொத்தம்\nTotal" to 100).forEach { (text, width) -> LedgerCell(text, width, redInk) }
                        LedgerFold()
                        repeat(10) { i ->
                            Column(Modifier.width(90.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                Text("${i + 1}", color = redInk, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                Text((rows.firstOrNull()?.let { state.dueDate(it.loan, it.cycle * 10 + i + 1) } ?: state.collectionDates(book, page.date.minusDays(1), 10)[i]).format(DateTimeFormatter.ofPattern("dd-MM")), color = blueInk, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            }
                        }
                        LedgerCell("மீதி\nRemaining", 110, redInk)
                    }
                    rows.forEach { row ->
                        val loan = row.loan
                        val ink = if (row.isRed) redInk else blueInk
                        Row(Modifier.height(IntrinsicSize.Min).testTag("ledgerRow-${loan.id}")) {
                            LedgerCell(loan.place, 140, ink, Modifier.testTag("place-${loan.id}"), tap = { onOpen(loan, null, false) }, hold = { onOpen(loan, null, true) })
                            LedgerCell(loan.familyHead, 160, ink, tap = { onOpen(loan, null, false) }, hold = { onOpen(loan, null, true) })
                            LedgerCell("${labels.getValue(loan.id)}", 160, ink, Modifier.testTag("customer-${loan.id}"), tap = { onOpen(loan, null, false) }, hold = { onOpen(loan, null, true) })
                            LedgerCell(rupees(row.opening), 100, ink)
                            LedgerFold()
                            Box(Modifier.width(900.dp).height(IntrinsicSize.Min)) {
                                // Behind receipts, never on top of numbers; no duplicate spoken/clickable identity.
                                Text(if (settlements[loan.id] != null) "கடன் தீர்க்கப்பட்டது" else "${loan.place} · ${loan.familyHead} · ${labels.getValue(loan.id)}",
                                    modifier = Modifier.matchParentSize().wrapContentSize(androidx.compose.ui.Alignment.Center)
                                        .clearAndSetSemantics { },
                                    color = Color(0xffb8b8b8).copy(alpha = .65f),
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    maxLines = 1)
                                Row(Modifier.height(IntrinsicSize.Min)) {
                            repeat(10) { i ->
                                val week = row.cycle * 10 + i + 1
                                val p = payments[loan.id to week]
                                val blocked = (settlements[loan.id]?.let { week > it } == true) && p == null
                                val text = if (blocked) "—" else if (p == null) "" else p.amountPaise?.let(::rupees) ?: "—"
                                LedgerCell(text, 90, if (p != null && p.amountPaise == null) Color(0xff7b5c39) else ink,
                                    Modifier.testTag("cell-${loan.id}-$week"), tap = { if (!blocked) onOpen(loan, week, false) }, hold = { if (!blocked) onOpen(loan, week, true) })
                            }
                            }
                            }
                            LedgerCell(rupees(row.ending), 110, redInk)
                        }
                    }
                    Row(Modifier.height(IntrinsicSize.Min).background(Color(0xffe1f1e8))
                        .border(2.dp, Color(0xff23734a)).testTag("weeklyTotals")) {
                        val totalInk = Color(0xff145c38)
                        LedgerCell("வசூல் மொத்தம்", 140, totalInk)
                        LedgerCell("", 160, totalInk); LedgerCell("", 160, totalInk)
                        // Individual loan amounts stay above; no summed loan-amount cell.
                        LedgerCell("", 100, totalInk, Modifier.testTag("noLoanAmountTotal"))
                        LedgerFold()
                        repeat(10) { i -> LedgerCell(rupees(rows.sumOf { row -> payments[row.loan.id to (row.cycle * 10 + i + 1)]?.amountPaise ?: 0L }), 90, totalInk,
                            Modifier.testTag("weeklyTotal-${i + 1}")) }
                        LedgerCell(rupees(rows.sumOf { it.ending }), 110, totalInk)
                    }
                    repeat((8 - rows.size).coerceAtLeast(0)) {
                        Row(Modifier.height(IntrinsicSize.Min)) {
                            listOf(140, 160, 160, 100).forEach { LedgerCell("", it, blueInk) }
                            LedgerFold()
                            (List(10) { 90 } + 110).forEach { LedgerCell("", it, blueInk) }
                        }
                    }
                }
            }
        }
    }
}
}
}
/** The bound centre crease separates identity details and ten receipt columns. */
@Composable private fun LedgerFold() {
    Box(Modifier.width(18.dp).fillMaxHeight().background(Brush.horizontalGradient(listOf(
        Color.Transparent, Color(0xffc4b697), Color(0xff91846b), Color(0xffe6dcc5), Color.Transparent))))
}
@OptIn(ExperimentalFoundationApi::class)
@Composable private fun LedgerCell(text: String, width: Int, ink: Color, modifier: Modifier = Modifier,
    tap: (() -> Unit)? = null, hold: (() -> Unit)? = null) {
    val interaction = if (tap == null) modifier else modifier.combinedClickable(onClick = tap, onLongClick = hold)
    Box(interaction.width(width.dp).heightIn(min = 48.dp).fillMaxHeight().drawBehind {
        drawLine(redInk.copy(alpha = .65f), Offset(size.width - 1, 0f), Offset(size.width - 1, size.height), 1.dp.toPx())
        drawLine(blueInk.copy(alpha = .45f), Offset(0f, size.height - 1), Offset(size.width, size.height - 1), 1.dp.toPx())
    }.padding(8.dp)) {
        Text(text, color = ink, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable fun CollectionHolidayDialog(state: NotebookState, book: Book, allowed: Boolean, busy: Boolean,
    privacy: DialogProperties, dismiss: () -> Unit, commit: (NotebookState, (String?) -> Unit) -> Unit) {
    var date by remember { mutableStateOf(LocalDate.now().with(java.time.temporal.TemporalAdjusters.nextOrSame(book.collectionDay))) }
    var reason by remember { mutableStateOf("") }
    var skipped by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    AlertDialog(properties = privacy, onDismissRequest = { if (!busy) dismiss() }, title = { Text("புத்தக வசூல் விடுமுறை / Book holiday") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            if (error.isNotBlank()) Text(error, color = redInk, modifier = Modifier.testTag("holidayError"))
            Text("புத்தகம் ${book.number} · ${BookInput.weekday(book.collectionDay)}")
            DateChoice("வசூல் விடுமுறை தேதி / Date", date, allowed, "holidayDate") { date = it }
            Row { Checkbox(skipped, { skipped = it }, modifier = Modifier.testTag("holidaySkipped")); Text("விடுமுறை / Skip (OFF = restore duty)") }
            EntryField("காரணம் / Reason", reason, "holidayReason") { reason = it }
            Text("இந்த தேதியை வாரமாக எண்ண வேண்டாம். பின்வரும் திட்டத் தேதிகள் அடுத்த வசூல் கிழமைக்கு நகரும். ஏற்கெனவே பெற்ற தொகை / உண்மைத் தேதிகள் மாற்றப்படாது. அந்த நாளில் கடன் அல்லது வசூல் இருந்தால் விடுமுறை அனுமதி இல்லை.")
            Text("Planned dates shift for the whole book, not a customer. Existing actual receipts stay in history. No automatic festival calendar.")
            state.collectionDays.filter { it.bookId == book.id }.forEach { e ->
                Text("${e.date.format(dateFormat)} · ${if (e.skipped) "விடுமுறை" else "மீண்டும் வசூல்"} · ${e.reason}\n${state.owner.name} · ${e.at}", style = MaterialTheme.typography.bodySmall)
            }
        } }, confirmButton = { TextButton(enabled = !busy, modifier = Modifier.testTag("holidaySave"), onClick = {
            try { commit(state.changeCollectionDay(book, date, skipped, reason)) { problem -> if (problem == null) dismiss() else error = problem } }
            catch (e: Exception) { error = (e as? InputProblem)?.explanation ?: "தேதியைச் சரிபார்க்கவும் / Check date" }
        }) { Text("உறுதி / Confirm") } }, dismissButton = { TextButton(enabled = !busy, onClick = dismiss) { Text("ரத்து / Cancel") } })
}
