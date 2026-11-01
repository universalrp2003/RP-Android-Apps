package com.universalrp.tamilanstore

import android.animation.ValueAnimator
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Decorative motion only: never drives downloads, installs, refresh or notification state. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StoreShowcase(tamil: Boolean, appCount: Int, motionEnabled: Boolean,
                  onMotionChanged: (Boolean) -> Unit, onLanguage: () -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val accessibility = remember(context) { context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager }
    var resumed by remember { mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    var systemMotion by remember { mutableStateOf(ValueAnimator.areAnimatorsEnabled()) }
    var touchExploration by remember { mutableStateOf(accessibility.isTouchExplorationEnabled) }
    DisposableEffect(owner, accessibility) {
        val lifecycle = LifecycleEventObserver { _, _ ->
            resumed = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            systemMotion = ValueAnimator.areAnimatorsEnabled()
            touchExploration = accessibility.isTouchExplorationEnabled
        }
        val listener = AccessibilityManager.TouchExplorationStateChangeListener { touchExploration = it }
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { systemMotion = ValueAnimator.areAnimatorsEnabled() }
        }
        owner.lifecycle.addObserver(lifecycle)
        accessibility.addTouchExplorationStateChangeListener(listener)
        context.contentResolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        onDispose {
            owner.lifecycle.removeObserver(lifecycle)
            accessibility.removeTouchExplorationStateChangeListener(listener)
            context.contentResolver.unregisterContentObserver(observer)
        }
    }
    val moving = StoreMotionPolicy.shouldScroll(motionEnabled, resumed, systemMotion, touchExploration)
    val message = if (tamil)
        "RP Ideas · தமிழ் மற்றும் ஆங்கில செயலிகள் · விளம்பரமும் கணக்கும் இல்லை · பதிவிறக்கக் கோப்பு சரிபார்ப்பு · நிறுவ Android ஒப்புதல் தேவை"
    else "RP Ideas · Tamil & English apps · No ads or accounts · Download file checks · Android confirmation before installation"
    Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF123B40), Color(0xFF162639))))) {
        Image(painterResource(R.drawable.tamil_landscape), contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(96.dp).align(Alignment.TopEnd).alpha(0.55f), contentScale = ContentScale.FillBounds)
        Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(if (tamil) "தமிழன்" else "Tamilan", style = MaterialTheme.typography.titleLarge)
                    Text(if (tamil) "RP Ideas · செயலி அங்காடி" else "RP Ideas · App store", style = MaterialTheme.typography.labelLarge)
                }
                TextButton(onClick = onLanguage) { Text(if (tamil) "English" else "தமிழ்") }
            }
            Text(if (tamil) "$appCount செயலிகள் · உங்கள் விருப்பப்படி" else "$appCount apps · Your choice",
                style = MaterialTheme.typography.labelSmall)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(message,
                modifier = (if (moving) Modifier.basicMarquee(iterations = Int.MAX_VALUE,
                    initialDelayMillis = 2000, repeatDelayMillis = 1800, velocity = 24.dp) else Modifier).weight(1f)
                    .clearAndSetSemantics { contentDescription = message },
                // Decorative summary only; TalkBack receives the full text above.
                maxLines = if (moving) 1 else 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { onMotionChanged(!motionEnabled) }) {
                Text(if (motionEnabled) {
                    if (tamil) "உரை நகர்வை நிறுத்து" else "Pause scrolling text"
                } else {
                    if (tamil) "உரை நகர்வை இயக்கு" else "Enable scrolling text"
                }, style = MaterialTheme.typography.labelSmall)
            }
            }
        }
        }
    }
}
