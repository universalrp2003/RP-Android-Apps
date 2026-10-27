package com.universalrp.launcher
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** SharedPreferences key recording that the first-run home-screen prompt has been shown. */
private const val KEY_ASKED_HOME = "asked_for_home"

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
    var favourites by remember { mutableStateOf(Favourites.load(context)) }
    var addingFavourite by remember { mutableStateOf(false) }

    // First run only: offer to become the home screen. A launcher that has to be found in Settings
    // is a launcher nobody sets, which is what the owner reported - they had to go to
    // Settings > Default Home screen by hand. This intent is the one the system answers with its own
    // home-app chooser, so there is nothing to configure here.
    var askedForHome by remember {
        mutableStateOf(
            context.getSharedPreferences("launcher", Context.MODE_PRIVATE)
                .getBoolean(KEY_ASKED_HOME, false),
        )
    }
    val homePrompt = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        // Whatever the user chose, do not ask again.
        context.getSharedPreferences("launcher", Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ASKED_HOME, true)
            .apply()
        askedForHome = true
    }

    val contactPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val uri = result.data?.data
        val picked = uri?.let { readPickedContact(context, it) }
        if (picked == null) {
            addingFavourite = false
            return@rememberLauncherForActivityResult
        }
        val favourite = picked.toFavourite()
        if (Favourites.add(context, favourite)) {
            favourites = Favourites.load(context)
        } else {
            notice = context.getString(
                if (Favourites.load(context).size >= Favourites.MAX) {
                    R.string.favourites_full
                } else {
                    R.string.favourites_duplicate
                },
            )
        }
        addingFavourite = false
    }

    // Offer the home screen once, after the first frame, so the screen is already drawn behind it.
    LaunchedEffect(askedForHome) {
        if (!askedForHome) {
            delay(600)
            homePrompt.launch(
                Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .addCategory(Intent.CATEGORY_DEFAULT)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

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
                    fontSize = 36.sp,
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
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    TextButton(onClick = { showAppList = false }) {
                        Text(
                            text = LocalContext.current.getString(R.string.back),
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
                        .height(BUTTON_GRID_HEIGHT_DP),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // A plain loop over item {}, not the items() extension. The grid's own member
                    // items(count: Int) shadows the List overload for the compiler's purposes, and
                    // resolving that costs a CI cycle to discover. item {} is a member with one
                    // signature and nothing to resolve.
                    for (button in BigButtons.home()) {
                        item {
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
            }
            item { FavouritesCard(favourites = favourites, onCall = { favourite ->
                    Favourites.call(context, favourite)
                }, onAdd = {
                    addingFavourite = true
                    contactPicker.launch(
                        Intent(Intent.ACTION_PICK)
                            .setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE),
                    )
                }, onRemove = { number ->
                    Favourites.remove(context, number)
                    favourites = Favourites.load(context)
                }) }
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
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = { Text(text = emergencyNumber, fontSize = 17.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        confirmEmergency = false
                        onEmergency(emergencyNumber)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyBg),
                ) { Text(text = yesLabel, fontSize = 15.sp) }
            },
            dismissButton = {
                TextButton(onClick = { confirmEmergency = false }) {
                    Text(
                        text = LocalContext.current.getString(R.string.emergency_no),
                        fontSize = 15.sp,
                    )
                }
            },
        )
    }

    notice?.let { text ->
        AlertDialog(
            onDismissRequest = { notice = null },
            title = { Text(text = text, fontSize = 15.sp) },
            confirmButton = {
                TextButton(onClick = { notice = null }) {
                    Text(text = yesLabel, fontSize = 14.sp)
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
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
        )
    }
}

@Composable
private fun FavouritesCard(
    favourites: List<Favourite>,
    onCall: (Favourite) -> Unit,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = context.getString(R.string.favourites),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            if (favourites.isEmpty()) {
                Text(
                    text = context.getString(R.string.favourites_empty),
                    fontSize = 13.sp,
                    color = Muted,
                    modifier = Modifier.padding(top = 6.dp),
                )
            } else {
                // Wrapping rather than a grid, so a long name does not push a button off the edge.
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (favourite in favourites) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Button(
                                onClick = { onCall(favourite) },
                                modifier = Modifier.height(64.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentBg),
                            ) {
                                Text(
                                    text = favourite.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 19.sp,
                                )
                            }
                            TextButton(onClick = { onRemove(favourite.number) }) {
                                Text(
                                    text = context.getString(R.string.favourites_remove),
                                    color = Muted,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            OutlinedButton(
                onClick = onAdd,
                modifier = Modifier.height(52.dp),
            ) {
                Text(
                    text = context.getString(R.string.favourites_add),
                    fontSize = 15.sp,
                    color = Accent,
                )
            }
        }
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

private val BUTTON_GRID_HEIGHT_DP = 340.dp

private val Bg = Color(0xFF101418)
private val CardBg = Color(0xFF1E262E)
private val AccentBg = Color(0xFF24402C)
private val EmergencyBg = Color(0xFFB3261E)
private val Accent = Color(0xFF7BD389)
private val Muted = Color(0xFFB6C2CC)
