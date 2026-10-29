package com.universalrp.launcher
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import android.app.role.RoleManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import com.universalrp.updates.AppGuides
import com.universalrp.updates.GuideScreen
import com.universalrp.updates.SuiteAbout
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
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
            pendingEmergencyNumber?.let { number ->
                if (granted) LauncherActions.placeDirectCall(this, number)
                else runCatching { startActivity(Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:$number"))) }
            }
            pendingEmergencyNumber = null
        }

    private var pendingEmergencyNumber: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingEmergencyNumber = savedInstanceState?.getString("pending_call")
        com.universalrp.updates.AppLanguageState.initialize(this)
        setContent {
            MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme(
                primary = Accent,
                onPrimary = Bg,
                secondary = Accent,
                onSecondary = Bg,
                background = Bg,
                onBackground = Color.White,
                surface = CardBg,
                onSurface = Color.White,
                onSurfaceVariant = Muted,
                error = EmergencyBg,
                onError = Color.White,
            )) {
                com.universalrp.updates.AppUpdates.Control("Elderly-Launcher", automatic = true)
                Surface(modifier = Modifier.fillMaxSize(), color = Bg) {
                    LauncherScreen(
                        onEmergency = { number -> requestEmergencyCall(number) },
                    )
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("pending_call", pendingEmergencyNumber)
        super.onSaveInstanceState(outState)
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
            LauncherActions.placeDirectCall(this, number)
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
    var page by remember { mutableStateOf("") }
    BackHandler {
        when {
            page.isNotEmpty() -> page = ""
            confirmEmergency -> confirmEmergency = false
            notice != null -> notice = null
            addingFavourite -> addingFavourite = false
            showAppList -> showAppList = false
            else -> Unit // A home screen must not finish on Back.
        }
    }
    if (page == "guide") {
        GuideScreen(AppGuides.launcher, com.universalrp.updates.AppLanguageState.current == com.universalrp.updates.AppLanguage.TAMIL, { page = "" })
        return
    }
    if (page == "about") {
        SuiteAbout("launcher", "Elderly-Launcher") { page = "" }
        return
    }

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

    fun chooseHome() {
        val manager = if (Build.VERSION.SDK_INT >= 29) context.getSystemService(RoleManager::class.java) else null
        val intent = if (Build.VERSION.SDK_INT >= 29 && manager != null &&
            manager.isRoleAvailable(RoleManager.ROLE_HOME) && !manager.isRoleHeld(RoleManager.ROLE_HOME)) {
            manager.createRequestRoleIntent(RoleManager.ROLE_HOME)
        } else Intent(Settings.ACTION_HOME_SETTINGS)
        runCatching { homePrompt.launch(intent) }.onFailure {
            notice = "முகப்புத் திரையை அமைப்புகளில் தேர்ந்தெடுக்கவும். / Choose the home app in Settings."
        }
    }
    LaunchedEffect(askedForHome) {
        if (!askedForHome) {
            delay(600)
            chooseHome()
        }
    }

    val contactsPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            contactPicker.launch(Intent(Intent.ACTION_PICK).setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE))
        } else {
            addingFavourite = false
            notice = "தொடர்பைச் சேர்க்க தொடர்புகள் அனுமதி தேவை. அமைப்புகளில் மாற்றலாம். / Contacts permission is needed to add a favourite; you can change it in Settings."
        }
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val (result, next) = LauncherActions.toggleTorch(context, torchOn)
            if (result is LauncherActions.Result.Done) torchOn = next
            else notice = "விளக்கை இயக்க முடியவில்லை. / Torch unavailable."
        } else notice = "விளக்கிற்கு கேமரா அனுமதி தேவை. / Camera permission is needed for the torch."
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
                    text = batteryText(),
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
                        AppRow(app = app, onClick = {
                            val result = LauncherActions.launchApp(context, app.packageName)
                            if (result is LauncherActions.Result.Failed) {
                                notice = context.getString(result.reasonRes)
                            }
                        })
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
                                    if (button.action == BigButtons.Action.TORCH &&
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                                        cameraPermission.launch(Manifest.permission.CAMERA)
                                        return@BigButton
                                    }
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
                    onEmergency(favourite.number)
                }, onAdd = {
                    addingFavourite = true
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                        contactPicker.launch(Intent(Intent.ACTION_PICK).setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE))
                    } else contactsPermission.launch(Manifest.permission.READ_CONTACTS)
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

        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { chooseHome() }, modifier = Modifier.fillMaxWidth()) {
                    Text("முகப்புத் திரையாக அமை / Set default home")
                }
                OutlinedButton(onClick = { page = "guide" }, modifier = Modifier.fillMaxWidth()) { Text("வழிகாட்டி / Guide") }
                OutlinedButton(onClick = { page = "about" }, modifier = Modifier.fillMaxWidth()) { Text("இந்தச் செயலி / புதுப்பிப்பு / About & update") }
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmergencyBg,
                        contentColor = ButtonText,
                        disabledContainerColor = CardBg,
                        disabledContentColor = Muted,
                    ),
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

/** Pixel size an app icon is decoded at, which is plenty for a 44dp row. */
private const val ICON_PIXELS = 96

/**
 * One app in the list: its own icon, its name, and a chevron.
 *
 * The icon comes from the phone, so it is the same artwork the app uses everywhere else and the
 * list is recognisable at a glance rather than a column of names to read.
 */
@Composable
private fun AppRow(app: AppEntry, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = CardBg,
            contentColor = ButtonText,
            disabledContainerColor = CardBg,
            disabledContentColor = Muted,
        ),
        contentPadding = PaddingValues(horizontal = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(46.dp),
                contentAlignment = Alignment.Center,
            ) {
                val icon = app.icon
                if (icon != null) {
                    Image(
                        bitmap = icon.toBitmap(ICON_PIXELS, ICON_PIXELS).asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                    )
                } else {
                    // An app that publishes no usable icon still gets a tile with its first letter.
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Accent.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = app.label.take(1),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Accent,
                        )
                    }
                }
            }
            Text(
                text = app.label,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
                maxLines = 2,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f),
            )
            Text(
                text = "\u203a",
                fontSize = 26.sp,
                color = Muted,
            )
        }
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
        colors = ButtonDefaults.buttonColors(
            containerColor = background,
            contentColor = ButtonText,
            disabledContainerColor = CardBg,
            disabledContentColor = Muted,
        ),
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
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AccentBg,
                                    contentColor = ButtonText,
                                    disabledContainerColor = CardBg,
                                    disabledContentColor = Muted,
                                ),
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

private val Bg = Color(LauncherPalette.BACKGROUND)
private val CardBg = Color(LauncherPalette.CARD)
private val AccentBg = Color(LauncherPalette.FAVOURITE)
private val EmergencyBg = Color(LauncherPalette.EMERGENCY)
private val Accent = Color(LauncherPalette.ACCENT)
private val Muted = Color(LauncherPalette.MUTED)

private val ButtonText = Color(LauncherPalette.BUTTON_TEXT)
