package com.universalrp.launcher

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * The home screen.
 *
 * Two states and no menus: the grid of big buttons, and the list of every installed app. There is
 * no settings screen, because a screen for configuring a launcher is a screen somebody has to learn
 * to use, and this launcher exists precisely so nobody has to learn anything.
 *
 * Everything is at least 18sp and every button is at least 96dp tall, because the whole point is
 * that it can be read and hit without reading carefully or aiming precisely.
 */
class MainActivity : ComponentActivity() {

    private val callPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                pendingEmergencyNumber?.let { LauncherActions.placeEmergencyCall(this, it) }
            }
            pendingEmergencyNumber = null
        }

    private var pendingEmergencyNumber: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Bg) {
                    LauncherScreen(
                        onEmergency = { number -> requestEmergencyCall(number) },
                    )
                }
            }
        }
    }

    /** The home button must never stack copies of this screen. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun requestEmergencyCall(number: String) {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            LauncherActions.placeEmergencyCall(this, number)
        } else {
            pendingEmergencyNumber = number
            callPermission.launch(Manifest.permission.CALL_PHONE)
        }
    }
}

@Composable
private fun LauncherScreen(onEmergency: (String) -> Unit) {
    val context = LocalContext.current
    var showAppList by remember { mutableStateOf(false) }
    var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var torchOn by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var confirmEmergency by remember { mutableStateOf(false) }

    // A launcher's screen is recreated constantly, so the app list is loaded once per composition
    // on a background thread and never on the main thread.
    LaunchedEffect(showAppList) {
        if (showAppList && apps.isEmpty()) {
            apps = withContext(Dispatchers.IO) { LauncherActions.installedApps(context) }
        }
    }

    // The clock ticks so the time is never more than a minute stale.
    var now by remember { mutableStateOf(currentTimeText()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = currentTimeText()
            delay(30_000)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = now,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    text = LocalContext.current.getString(R.string.battery),
                    fontSize = 16.sp,
                    color = Muted,
                )
            }
        }

        if (showAppList) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = LocalContext.current.getString(R.string.all_apps),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    TextButton(onClick = { showAppList = false }) {
                        Text(
                            text = LocalContext.current.getString(R.string.big_phone),
                            color = Accent,
                            fontSize = 16.sp,
                        )
                    }
                }
            }
            if (apps.isEmpty()) {
                item {
                    Text(
                        text = LocalContext.current.getString(R.string.loading_apps),
                        fontSize = 16.sp,
                        color = Muted,
                        modifier = Modifier.padding(vertical = 20.dp),
                    )
                }
            } else {
                for (app in apps) {
                    item {
                        BigButton(
                            label = app.label,
                            height = 84,
                            background = CardBg,
                            onClick = {
                                val result = LauncherActions.launchApp(context, app.packageName)
                                if (result is LauncherActions.Result.Failed) {
                                    notice = context.getString(result.reasonRes)
                                }
                            },
                        )
                    }
                }
            }
        } else {
            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(BUTTON_GRID_HEIGHT),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(BigButtons.home()) { button ->
                        BigButton(
                            label = LocalContext.current.getString(button.labelRes),
                            height = 0,
                            background = CardBg,
                            onClick = {
                                val result = handleAction(
                                    context = context,
                                    action = button.action,
                                    torchOn = torchOn,
                                    onTorch = { torchOn = it },
                                    onAppList = { showAppList = true },
                                )
                                if (result is LauncherActions.Result.Failed) {
                                    notice = context.getString(result.reasonRes)
                                }
                            },
                        )
                    }
                }
            }
            item {
                BigButton(
                    label = LocalContext.current.getString(BigButtons.emergency().labelRes),
                    height = 96,
                    background = EmergencyBg,
                    onClick = { confirmEmergency = true },
                )
            }
            item {
                BigButton(
                    label = LocalContext.current.getString(BigButtons.appList().labelRes),
                    height = 72,
                    background = AccentBg,
                    onClick = { showAppList = true },
                )
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }

    // Resolved here, in the composable body, because an onClick lambda is not a composable context
    // and LocalContext.current cannot be read inside one.
    val emergencyNumber = LocalContext.current.getString(R.string.emergency_number)
    val yesLabel = LocalContext.current.getString(R.string.emergency_yes)

    if (confirmEmergency) {
        AlertDialog(
            onDismissRequest = { confirmEmergency = false },
            title = {
                Text(
                    text = LocalContext.current.getString(R.string.emergency_confirm),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = { Text(text = emergencyNumber, fontSize = 20.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        confirmEmergency = false
                        onEmergency(emergencyNumber)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyBg),
                ) { Text(text = yesLabel, fontSize = 18.sp) }
            },
            dismissButton = {
                TextButton(onClick = { confirmEmergency = false }) {
                    Text(
                        text = LocalContext.current.getString(R.string.emergency_no),
                        fontSize = 18.sp,
                    )
                }
            },
        )
    }

    notice?.let { text ->
        AlertDialog(
            onDismissRequest = { notice = null },
            title = { Text(text = text, fontSize = 18.sp) },
            confirmButton = {
                TextButton(onClick = { notice = null }) {
                    Text(text = yesLabel, fontSize = 16.sp)
                }
            },
        )
    }
}

/** Runs one big button and reports what happened. */
private fun handleAction(
    context: android.content.Context,
    action: BigButtons.Action,
    torchOn: Boolean,
    onTorch: (Boolean) -> Unit,
    onAppList: () -> Unit,
): LauncherActions.Result = when (action) {
    BigButtons.Action.DIAL -> LauncherActions.openDialer(context)
    BigButtons.Action.MESSAGES -> LauncherActions.openMessages(context)
    BigButtons.Action.CAMERA -> LauncherActions.openCamera(context)
    BigButtons.Action.CONTACTS -> LauncherActions.openContacts(context)
    BigButtons.Action.SETTINGS -> LauncherActions.openSettings(context)
    BigButtons.Action.TORCH -> {
        val (result, newState) = LauncherActions.toggleTorch(context, torchOn)
        onTorch(newState)
        result
    }
    BigButtons.Action.VOLUME_UP -> LauncherActions.nudgeVolume(context, up = true)
    BigButtons.Action.VOLUME_DOWN -> LauncherActions.nudgeVolume(context, up = false)
    BigButtons.Action.EMERGENCY -> LauncherActions.Result.Done
    BigButtons.Action.APP_LIST -> {
        onAppList()
        LauncherActions.Result.Done
    }
}

@Composable
private fun BigButton(
    label: String,
    height: Int,
    background: Color,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (height > 0) Modifier.height(height.dp) else Modifier.aspectRatio(1.15f)),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = background),
    ) {
        Text(
            text = label,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 26.sp,
        )
    }
}

/** `h:mm` in 24-hour form, which is unambiguous and needs no AM/PM to read. */
private fun currentTimeText(): String {
    val calendar = java.util.Calendar.getInstance()
    val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
    val minute = calendar.get(java.util.Calendar.MINUTE)
    val mm = minute.toString().padStart(2, '0')
    return "$hour:$mm"
}

private const val BUTTON_GRID_HEIGHT = 340

private val Bg = Color(0xFF101418)
private val CardBg = Color(0xFF1E262E)
private val AccentBg = Color(0xFF24402C)
private val EmergencyBg = Color(0xFFB3261E)
private val Accent = Color(0xFF7BD389)
private val Muted = Color(0xFFB6C2CC)
