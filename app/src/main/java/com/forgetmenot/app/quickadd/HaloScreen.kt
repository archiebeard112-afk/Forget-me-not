package com.forgetmenot.app.quickadd

import android.os.Build
import android.view.RoundedCorner
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
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
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
    val density = LocalDensity.current
    // The status bar and navigation buttons are drawn over every app, and while the halo is
    // see-through they take Teams' (opaque) colours. Keep the glow inside them so all edges show.
    val bars = WindowInsets.systemBars.union(WindowInsets.displayCutout)
    val inset = Rect(
        bars.getLeft(density, LayoutDirection.Ltr).toFloat(),
        bars.getTop(density).toFloat(),
        bars.getRight(density, LayoutDirection.Ltr).toFloat(),
        bars.getBottom(density).toFloat(),
    )
    // Follow the phone's rounded screen corners where the glow meets the screen edge.
    val screenCorner = remember {
        val reported = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            view.rootWindowInsets?.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.radius
        } else null
        reported?.toFloat() ?: with(density) { 40.dp.toPx() }
    }

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
                    if (hit != null) vm.pick(hit) else if (vm.selectedLines.isEmpty()) onClose()
                }
            }
            .drawBehind {
                val glow = Brush.linearGradient(
                    listOf(Petal, Pollen, Petal, Pollen, Petal),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height),
                )
                // Each stroke sits fully inside the visible area so all four edges show. Below a
                // status bar the corner is a gentler curve than the screen's own.
                val area = Rect(inset.left, inset.top, size.width - inset.right, size.height - inset.bottom)
                val corner = if (inset.top > 0f || inset.bottom > 0f) 28.dp.toPx() else screenCorner
                for ((width, alpha) in listOf(28.dp to 0.12f, 16.dp to 0.25f, 5.dp to 0.95f)) {
                    val half = width.toPx() / 2
                    drawRoundRect(
                        glow,
                        topLeft = Offset(area.left + half, area.top + half),
                        size = Size(area.width - 2 * half, area.height - 2 * half),
                        cornerRadius = CornerRadius((corner - half).coerceAtLeast(0f)),
                        style = Stroke(width.toPx()),
                        alpha = alpha * pulse,
                    )
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
                Text("Tap the message to save", style = MaterialTheme.typography.labelLarge)
                TextButton(onClick = vm::showList) { Text("List") }
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") }
            }
        }
    }
}

/** Keeps taps on a bar from reaching the screen behind it. */
private fun Modifier.swallowTaps(): Modifier = this.then(
    Modifier.clickable(MutableInteractionSource(), indication = null) {}
)
