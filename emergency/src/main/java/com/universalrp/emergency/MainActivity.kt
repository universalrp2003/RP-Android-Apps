package com.universalrp.emergency

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity

/**
 * The one screen.
 *
 * Three cards, in the order a worried person needs them: is it listening, is it reaching anybody,
 * and what has it heard. Nothing else. The first release of Vasanthi AI was sent back for "big
 * unwanted text", and that lesson is applied here from the start rather than after the owner says
 * so again.
 */
class MainActivity : ComponentActivity() {

    /**
     * Declared as a field, not created inside a method: `registerForActivityResult` may only be
     * called before the activity is started, and creating it on demand is an immediate crash.
     */
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* best effort */ }

    private val micPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                EmergencyService.start(this)
            } else {
                EmergencyState.setStatus(getString(R.string.status_permission_needed))
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Bg) {
                    EmergencyScreen(
                        onStart = { startOrRequest() },
                        onStop = { EmergencyService.stop(this) },
                        onClear = { EmergencyState.clear() },
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Ask the family phones who is out there every time the screen comes forward, so the list
        // is fresh rather than whatever happened to be on the network ten minutes ago.
        if (EmergencyState.listening) AlertBroadcaster.ping()
    }

    private fun startOrRequest() {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            AlertReceiver.setEnabled(this, true)
            EmergencyService.start(this)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestNotificationPermission()
            }
        } else {
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

}

@Composable
private fun EmergencyScreen(
    onStart: () -> Unit,
    onStop: () -> Unit,
    onClear: () -> Unit,
) {
    // The state object is a plain singleton fed by a foreground service, so the only way Compose
    // knows to redraw is a counter it can read. Bumping it on every change and reading it here is
    // the whole mechanism: a snapshot flow would be cleaner but would need a coroutine scope tied
    // to the service's lifetime, and this has none.
    var revision by remember { mutableIntStateOf(EmergencyState.revision) }
    LaunchedEffect(Unit) {
        while (true) {
            revision = EmergencyState.revision
            delay(400)
        }
    }

    val local = EmergencyState.localSnapshot()
    val received = EmergencyState.receivedSnapshot()
    val sources = EmergencyState.sourcesSnapshot()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Header() }
        item {
            ListenCard(
                listening = EmergencyState.listening,
                status = EmergencyState.statusText,
                onStart = onStart,
                onStop = onStop,
            )
        }
        item { FamilyCard(sources = sources) }
        item {
            AlertsCard(
                local = local,
                received = received,
                onClear = onClear,
            )
        }
        item { LimitsCard() }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun Header() {
    Column(modifier = Modifier.padding(top = 20.dp)) {
        Text(
            text = LocalContext.current.getString(R.string.app_name),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Text(
            text = LocalContext.current.getString(R.string.family_phones_body),
            fontSize = 13.sp,
            color = Muted,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun ListenCard(listening: Boolean, status: String?, onStart: () -> Unit, onStop: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (listening) {
                    LocalContext.current.getString(R.string.listening)
                } else {
                    LocalContext.current.getString(R.string.stopped)
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (listening) Live else Color.White,
            )
            Text(
                text = status ?: LocalContext.current.getString(R.string.status_idle),
                fontSize = 14.sp,
                color = Color.White,
                modifier = Modifier.padding(top = 6.dp),
            )
            Spacer(Modifier.height(12.dp))
            if (listening) {
                Button(
                    onClick = onStop,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = StopRed),
                ) {
                    Text(LocalContext.current.getString(R.string.stop_listening))
                }
            } else {
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Live),
                ) {
                    Text(LocalContext.current.getString(R.string.start_listening))
                }
            }
        }
    }
}

@Composable
private fun FamilyCard(sources: List<String>) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = context.getString(R.string.family_phones),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                text = context.getString(R.string.what_it_does),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = context.getString(R.string.what_it_does_body),
                fontSize = 13.sp,
                color = Muted,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = context.getString(R.string.connected_phones),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            if (sources.isEmpty()) {
                Text(
                    text = context.getString(R.string.no_phones_yet),
                    fontSize = 13.sp,
                    color = Muted,
                    modifier = Modifier.padding(top = 4.dp),
                )
            } else {
                sources.forEach { name ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = "•", color = Live, fontSize = 16.sp)
                        Text(
                            text = name,
                            fontSize = 14.sp,
                            color = Color.White,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertsCard(
    local: List<EmergencyAlert>,
    received: List<EmergencyAlert>,
    onClear: () -> Unit,
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = context.getString(R.string.alerts_received),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                if (local.isNotEmpty() || received.isNotEmpty()) {
                    TextButton(onClick = onClear) {
                        Text(context.getString(R.string.clear_alerts), color = Live, fontSize = 13.sp)
                    }
                }
            }
            if (local.isEmpty() && received.isEmpty()) {
                Text(
                    text = context.getString(R.string.no_alerts_yet),
                    fontSize = 13.sp,
                    color = Muted,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            if (received.isNotEmpty()) {
                Text(
                    text = context.getString(R.string.alerts_from_family),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Live,
                    modifier = Modifier.padding(top = 8.dp),
                )
                received.forEach { AlertRow(it, true) }
            }
            if (local.isNotEmpty()) {
                Text(
                    text = context.getString(R.string.alerts_from_here),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Muted,
                    modifier = Modifier.padding(top = 8.dp),
                )
                local.forEach { AlertRow(it, false) }
            }
        }
    }
}

@Composable
private fun AlertRow(alert: EmergencyAlert, fromFamily: Boolean) {
    val percent = (alert.confidence * 100).toInt()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = alert.tamilLabel,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (fromFamily) Color.White else Muted,
            )
            Text(
                text = LocalContext.current.getString(R.string.confidence, percent),
                fontSize = 11.sp,
                color = Muted,
            )
        }
        if (fromFamily) {
            Text(
                text = alert.sourceName,
                fontSize = 11.sp,
                color = Live,
            )
        }
    }
}

@Composable
private fun LimitsCard() {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = context.getString(R.string.cannot_hear_call),
                fontSize = 13.sp,
                color = Muted,
            )
            Text(
                text = context.getString(R.string.false_alerts),
                fontSize = 13.sp,
                color = Muted,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

private val Bg = Color(0xFF120606)
private val CardBg = Color(0xFF1E1010)
private val Live = Color(0xFFFF6B5A)
private val StopRed = Color(0xFF8C2B22)
private val Muted = Color(0xFFBFA8A6)
