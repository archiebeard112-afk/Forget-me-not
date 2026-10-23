package com.forgetmenot.app.quickadd

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.forgetmenot.app.assist.ScreenLines

private val Petal = Color(0xFF4A7BE0)
private val Pollen = Color(0xFFF5C542)

/**
 * Power button, halo mode: the app underneath stays visible inside a glowing edge, and
 * tapping a message on it selects that message. Positions come from the assist capture.
 */
@Composable
fun HaloScreen(vm: QuickAddViewModel, onClose: () -> Unit) {
    val view = LocalView.current
    // Where this layout's top-left sits on the screen, to line up with captured positions.
    var origin by remember { mutableStateOf(Offset.Zero) }
    val pulse by rememberInfiniteTransition(label = "halo").animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "pulse",
    )
    val count = vm.selectedLines.size

    Box(
        Modifier.fillMaxSize()
            .onGloballyPositioned { coords ->
                val screen = IntArray(2).also(view::getLocationOnScreen)
                val inWindow = coords.positionInWindow()
                origin = Offset(screen[0] + inWindow.x, screen[1] + inWindow.y)
            }
            .pointerInput(Unit) {
                val slop = 10.dp.toPx()
                detectTapGestures { pos ->
                    val hit = ScreenLines.itemAt(vm.screenItems, pos.x + origin.x, pos.y + origin.y, slop)
                    when {
                        hit != null -> vm.toggleLine(hit)
                        vm.selectedLines.isEmpty() -> onClose()
                    }
                }
            }
            .drawBehind {
                val glow = Brush.linearGradient(
                    listOf(Petal, Pollen, Petal, Pollen, Petal),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height),
                )
                val corner = CornerRadius(40.dp.toPx())
                for ((width, alpha) in listOf(28.dp to 0.12f, 16.dp to 0.25f, 5.dp to 0.95f)) {
                    drawRoundRect(glow, cornerRadius = corner, style = Stroke(width.toPx()), alpha = alpha * pulse)
                }
                val pad = 6.dp.toPx()
                val radius = CornerRadius(12.dp.toPx())
                vm.selectedLines.forEach { i ->
                    val item = vm.screenItems.getOrNull(i) ?: return@forEach
                    val topLeft = Offset(item.left - origin.x - pad, item.top - origin.y - pad)
                    val box = Size(item.width + 2 * pad, item.height + 2 * pad)
                    drawRoundRect(Petal, topLeft, box, radius, alpha = 0.22f)
                    drawRoundRect(Petal, topLeft, box, radius, style = Stroke(3.dp.toPx()))
                }
            },
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            shadowElevation = 6.dp,
            modifier = Modifier.align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 12.dp)
                .swallowTaps(),
        ) {
            Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (count == 0) "Tap the message to save" else "Tap more to add, or Save",
                    style = MaterialTheme.typography.labelLarge,
                )
                TextButton(onClick = vm::showList) { Text("List") }
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") }
            }
        }

        if (count > 0) {
            val preview = vm.selectedLines.sorted().joinToString(" · ") { vm.screenLines[it] }
            Surface(
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 8.dp,
                modifier = Modifier.align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(16.dp)
                    .fillMaxWidth()
                    .swallowTaps(),
            ) {
                Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        preview,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                    )
                    Button(onClick = vm::savePicked) { Text(if (count == 1) "Save" else "Save $count") }
                }
            }
        }
    }
}

/** Keeps taps on a bar from reaching the screen behind it. */
private fun Modifier.swallowTaps(): Modifier = this.then(
    Modifier.clickable(MutableInteractionSource(), indication = null) {}
)
