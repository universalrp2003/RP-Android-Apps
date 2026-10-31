package com.universalrp.tamilnadufm.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.universalrp.tamilnadufm.data.Category
import com.universalrp.tamilnadufm.data.RadioTabOrder
import com.universalrp.updates.AppLanguage
import com.universalrp.updates.AppLanguageState
import com.universalrp.updates.AppStrings
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** Scroll normally; long-press a whole chip to drag. Only a completed drop writes preferences.
 * The detector belongs to the stationary viewport, not a recyclable/scrolling chip. This keeps
 * the pointer alive when edge auto-scroll takes the original chip out of the visible window.
 */
@Composable
fun DraggableCategoryTabs(
    ids: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onDrop: (id: String, target: String, after: Boolean) -> Unit,
    onMove: (id: String, delta: Int) -> Unit,
) {
    val tamil = AppLanguageState.current == AppLanguage.TAMIL
    val list = rememberLazyListState()
    val latestDrop by rememberUpdatedState(onDrop)
    val latestIds by rememberUpdatedState(ids)
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val edge = with(density) { 48.dp.toPx() }
    val step = with(density) { 12.dp.toPx() }
    var viewportWidth by remember { mutableIntStateOf(0) }
    var viewportHeight by remember { mutableIntStateOf(0) }
    var dragged by remember { mutableStateOf<String?>(null) }
    var finger by remember { mutableStateOf(Offset.Zero) }
    var grabOffset by remember { mutableFloatStateOf(0f) }
    var chipWidth by remember { mutableIntStateOf(0) }

    fun dropTarget(): RadioTabOrder.DropTarget? {
        val id = dragged ?: return null
        return RadioTabOrder.dropTarget(
            list.layoutInfo.visibleItemsInfo.map {
                RadioTabOrder.TabBounds(it.key as String, it.offset.toFloat(), it.size.toFloat())
            }, id, finger.x,
        )
    }

    // Bounded, cancellable edge scrolling exposes *all* 17 tabs during a single long drag.
    LaunchedEffect(dragged) {
        while (dragged != null) {
            if (finger.y in -edge..(viewportHeight + edge)) {
                val delta = when {
                    finger.x < edge -> -step * ((edge - finger.x) / edge).coerceIn(0f, 1f)
                    finger.x > viewportWidth - edge -> step * ((finger.x - viewportWidth + edge) / edge).coerceIn(0f, 1f)
                    else -> 0f
                }
                if (delta != 0f) list.scrollBy(delta)
            }
            delay(16)
        }
    }

    val target = if (dragged != null) dropTarget() else null
    Column {
        Text(
            if (tamil) "தாவலை அழுத்திப் பிடித்து இழுக்கவும் • வரிசை தானாகச் சேமிக்கப்படும்"
            else "Hold a tab and drag to reorder • saved automatically",
            color = TextSecondary,
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Box(Modifier.fillMaxWidth().clipToBounds().onSizeChanged {
            viewportWidth = it.width
            viewportHeight = it.height
        }.pointerInput(Unit) {
            detectDragGesturesAfterLongPress(
                onDragStart = { position ->
                    val item = list.layoutInfo.visibleItemsInfo.firstOrNull {
                        position.x >= it.offset && position.x < it.offset + it.size
                    }
                    if (item != null) {
                        dragged = item.key as String
                        finger = position
                        grabOffset = position.x - item.offset
                        chipWidth = item.size
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                },
                onDrag = { change, amount ->
                    if (dragged != null) {
                        change.consume()
                        finger += amount
                    }
                },
                onDragCancel = { dragged = null },
                onDragEnd = {
                    val id = dragged
                    val destination = dropTarget()
                    if (id != null && id in latestIds && destination != null &&
                        finger.y in -edge..(viewportHeight + edge)) {
                        latestDrop(id, destination.id, destination.after)
                    }
                    dragged = null
                },
            )
        }) {
            LazyRow(
                state = list,
                userScrollEnabled = dragged == null,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(ids, key = { it }) { id ->
                    val label = AppStrings.translate(Category.label(id))
                    Chip(
                        label = label,
                        selected = selected == id,
                        onClick = { onSelect(id) },
                        modifier = Modifier.heightIn(min = 48.dp)
                            .alpha(if (dragged == id) 0.25f else 1f)
                            .then(if (target?.id == id) Modifier.border(2.dp, Saffron, RoundedCornerShape(14.dp)) else Modifier)
                            .semantics {
                                customActions = listOf(
                                    CustomAccessibilityAction(if (tamil) "$label முன்னே நகர்த்து" else "Move $label earlier") {
                                        onMove(id, -1); true
                                    },
                                    CustomAccessibilityAction(if (tamil) "$label பின்னே நகர்த்து" else "Move $label later") {
                                        onMove(id, 1); true
                                    },
                                )
                            },
                    )
                }
            }
            dragged?.let { id ->
                Chip(
                    label = Category.label(id), selected = true, onClick = {},
                    modifier = Modifier.offset {
                        IntOffset((finger.x - grabOffset).coerceIn(0f, (viewportWidth - chipWidth).coerceAtLeast(0).toFloat()).roundToInt(), 0)
                    }.width(with(density) { chipWidth.toDp() }).heightIn(min = 48.dp)
                        .clearAndSetSemantics {},
                )
            }
        }
        if (target != null) {
            val label = AppStrings.translate(Category.label(target.id))
            Text(
                if (tamil) "$label ${if (target.after) "பின்னால்" else "முன்னால்"} வைக்க விடுங்கள்"
                else "Release to place ${if (target.after) "after" else "before"} $label",
                color = Saffron,
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            )
        }
    }
}
