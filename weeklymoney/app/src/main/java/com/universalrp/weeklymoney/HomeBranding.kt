package com.universalrp.weeklymoney

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private val gold = Color(0xff9a701f)
private val forest = Color(0xff225b42)

/** Local vector artwork, no images/network; deliberately faint underneath readable controls. */
fun Modifier.moneyPaper(): Modifier = background(Brush.verticalGradient(listOf(Color(0xfffffcf2), Color(0xfff1f6ed))))
    .drawBehind {
        val ink = gold.copy(alpha = .07f)
        val radius = 24.dp.toPx()
        for (row in 0..5) {
            val x = if (row % 2 == 0) size.width * .82f else size.width * .12f
            val y = size.height * (.23f + row * .14f)
            drawCircle(ink, radius, Offset(x, y), style = Stroke(2.dp.toPx()))
            drawCircle(ink, radius * .75f, Offset(x, y), style = Stroke(1.dp.toPx()))
            drawRoundRect(forest.copy(alpha = .035f), Offset(size.width * .32f, y + radius),
                Size(96.dp.toPx(), 44.dp.toPx()), CornerRadius(6.dp.toPx()), style = Stroke(2.dp.toPx()))
        }
    }

@Composable fun MoneyHomeHeading() {
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(48.dp).background(Color(0xffffedb9), CircleShape).border(1.dp, gold, CircleShape)
            .clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
            Text("₹", color = gold, style = MaterialTheme.typography.headlineMedium)
        }
        Text("வார வசூல்", modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("homeTitle"),
            textAlign = TextAlign.Center, color = forest, fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineSmall)
        if (BuildConfig.IS_SAMPLE) Text("DEMO · மாதிரி மட்டும் · ${BuildConfig.VERSION_NAME}", color = gold)
    }
}

/** User-initiated copy only. No dial intent, phone/contacts permission, or silent clipboard reads. */
@Composable fun ContactNumber(number: String) {
    if (number.isBlank()) return
    val context = LocalContext.current
    TextButton(modifier = Modifier.testTag("copyContact"), onClick = {
        val clip = ClipData.newPlainText("Customer contact", number)
        if (Build.VERSION.SDK_INT >= 33) clip.description.extras = PersistableBundle().apply {
            putBoolean("android.content.extra.IS_SENSITIVE", true)
        }
        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
        Toast.makeText(context, "எண் நகலெடுக்கப்பட்டது / Number copied. Paste in Phone to dial.", Toast.LENGTH_SHORT).show()
    }) { Text("$number · நகலெடு / Copy", modifier = Modifier.testTag("savedContact")) }
}
