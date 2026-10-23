package com.forgetmenot.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forgetmenot.app.R
import com.forgetmenot.app.data.DueDefaults
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** The app icon, small, for headers and the halo bar. */
@Composable
fun BrandMark(size: Dp = 18.dp, corner: Dp = 5.dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(corner)), contentAlignment = Alignment.Center) {
        // The icon art is drawn on a 108 grid with the flower in the middle 54; show just that part.
        val full = size * 1.9f
        Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.requiredSize(full))
        Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.requiredSize(full))
    }
}

/** Grab handle, brand label and close button at the top of every pop-up. */
@Composable
fun SheetHeader(label: String, onClose: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.padding(top = 8.dp).width(36.dp).height(4.dp)
                .clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.outlineVariant)
        )
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            BrandMark()
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp).weight(1f),
            )
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable(role = Role.Button, onClickLabel = "Close", onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Close, "Close", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** The task as large text you can tap and edit, no box around it. */
@Composable
fun TaskHeadline(
    text: String,
    onChange: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
    placeholder: String = "What do you need to remember?",
) {
    val style = MaterialTheme.typography.titleLarge.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 25.sp,
        color = MaterialTheme.colorScheme.onSurface,
    )
    if (onChange == null) {
        Text(text, style = style, modifier = modifier)
        return
    }
    BasicTextField(
        value = text,
        onValueChange = onChange,
        textStyle = style,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box {
                if (text.isEmpty()) {
                    Text(placeholder, style = style.copy(fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant))
                }
                inner()
            }
        },
    )
}

/** "AL  From Autumn Larson  [Teams]". The name can be edited in place. */
@Composable
fun FromRow(name: String?, source: String?, onNameChange: ((String) -> Unit)?) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                initials(name),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Text("From", style = MaterialTheme.typography.bodyMedium, color = muted)
        val nameStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        if (onNameChange == null) {
            Text(name ?: "Unknown", style = nameStyle, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        } else {
            BasicTextField(
                value = name ?: "",
                onValueChange = onNameChange,
                singleLine = true,
                textStyle = nameStyle,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f, fill = false).widthIn(min = 24.dp),
                decorationBox = { inner ->
                    Box {
                        if (name.isNullOrEmpty()) Text("add who asked", style = nameStyle.copy(fontWeight = FontWeight.Normal, color = muted))
                        inner()
                    }
                },
            )
        }
        sourceLabel(source)?.let {
            Text(
                it,
                fontSize = 11.sp,
                color = muted,
                modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}

private enum class DueChoice(val label: String) { Now("Now"), Hour("1 hour"), Morning("6 AM"), Custom("Custom") }

/**
 * One card with the reminder settings: due time (segmented Now / 1 hour / 6 AM / Custom),
 * how often to nag, and high priority. Pass null for [nagMinutes] to hide the nag row.
 */
@Composable
fun ReminderCard(
    dueAt: Long?,
    onDue: (Long?) -> Unit,
    nagMinutes: Int?,
    onNag: (Int) -> Unit,
    highPriority: Boolean,
    onHighPriority: (Boolean) -> Unit,
) {
    var custom by remember { mutableStateOf(false) }
    val now = System.currentTimeMillis()
    val choice = when {
        dueAt == null -> null
        dueAt <= now + 60_000 -> DueChoice.Now
        kotlin.math.abs(dueAt - (now + 3_600_000)) < 120_000 -> DueChoice.Hour
        dueAt == DueDefaults.tomorrowMorning() -> DueChoice.Morning
        else -> DueChoice.Custom
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 14.dp, vertical = 4.dp),
    ) {
        CardRow(icon = LineIcons.Clock, name = "Due") {
            Text(
                dueAt?.let { dueLabel(it) } ?: "No reminder",
                style = MaterialTheme.typography.bodyMedium,
                color = if (dueAt != null && dueAt <= now) Urgent else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Segmented(
            options = DueChoice.entries.map { it.label },
            selected = choice?.ordinal,
            onSelect = { i ->
                when (DueChoice.entries[i]) {
                    DueChoice.Now -> onDue(System.currentTimeMillis())
                    DueChoice.Hour -> onDue(System.currentTimeMillis() + 3_600_000)
                    DueChoice.Morning -> onDue(DueDefaults.tomorrowMorning())
                    DueChoice.Custom -> custom = true
                }
            },
        )
        if (nagMinutes != null) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            var menu by remember { mutableStateOf(false) }
            Box {
                CardRow(icon = LineIcons.Bell, name = "Remind me every", onClick = { menu = true }) {
                    Text(
                        "${nagLabel(nagMinutes)}  ›",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, modifier = Modifier.align(Alignment.CenterEnd)) {
                    NAG_OPTIONS.forEach { m ->
                        DropdownMenuItem(
                            text = { Text(nagLabel(m), fontWeight = if (m == nagMinutes) FontWeight.Bold else FontWeight.Normal) },
                            onClick = { onNag(m); menu = false },
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        CardRow(icon = LineIcons.Flag, name = "High priority", onClick = { onHighPriority(!highPriority) }) {
            Switch(
                checked = highPriority,
                onCheckedChange = onHighPriority,
                colors = SwitchDefaults.colors(checkedTrackColor = Urgent, checkedThumbColor = Color.White, checkedBorderColor = Urgent),
            )
        }
    }
    if (custom) {
        CustomDueDialog(initial = dueAt, onDismiss = { custom = false }, onConfirm = { custom = false; onDue(it) })
    }
}

@Composable
private fun CardRow(icon: ImageVector, name: String, onClick: (() -> Unit)? = null, trailing: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(48.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp).weight(1f))
        trailing()
    }
}

@Composable
private fun Segmented(options: List<String>, selected: Int?, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier.weight(1f).height(34.dp)
                    .then(if (on) Modifier.shadow(2.dp, RoundedCornerShape(9.dp)) else Modifier)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (on) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent)
                    .clickable(role = Role.RadioButton) { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (on) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Quiet text action on the left, one strong button filling the rest. */
@Composable
fun SheetActions(
    secondary: String,
    onSecondary: () -> Unit,
    primary: String,
    onPrimary: () -> Unit,
    enabled: Boolean = true,
    primaryIcon: ImageVector? = null,
    extra: (@Composable () -> Unit)? = null,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            secondary,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClick = onSecondary)
                .padding(horizontal = 6.dp, vertical = 12.dp),
        )
        extra?.invoke()
        Row(
            Modifier.weight(1f).height(50.dp)
                .alpha(if (enabled) 1f else 0.45f)
                .shadow(if (enabled) 8.dp else 0.dp, RoundedCornerShape(16.dp), spotColor = Color(0xFF3A5FD8))
                .clip(RoundedCornerShape(16.dp))
                .background(PrimaryButtonBrush)
                .clickable(enabled = enabled, role = Role.Button, onClick = onPrimary),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (primaryIcon != null) {
                Icon(primaryIcon, null, Modifier.size(18.dp), tint = Color.White)
                Box(Modifier.width(8.dp))
            }
            Text(primary, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

private val time = DateTimeFormatter.ofPattern("h:mm a")

/** "Today · 7:45 PM", "Tomorrow · 6:00 AM", "Fri · 9:00 AM", "Nov 3 · 9:00 AM". */
fun dueLabel(dueAt: Long): String {
    val zone = ZoneId.systemDefault()
    val t = Instant.ofEpochMilli(dueAt).atZone(zone)
    val today = java.time.LocalDate.now(zone)
    val day = when (t.toLocalDate()) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        else -> if (t.toLocalDate().isBefore(today.plusDays(7))) t.format(DateTimeFormatter.ofPattern("EEE")) else t.format(DateTimeFormatter.ofPattern("MMM d"))
    }
    return "$day · ${t.format(time)}"
}

private fun nagLabel(minutes: Int) = if (minutes < 60) "$minutes min" else if (minutes == 60) "1 hour" else "${minutes / 60} hours"

private fun initials(name: String?): String =
    name?.split(" ")?.filter { it.isNotBlank() }?.take(2)?.joinToString("") { it.first().uppercase() }?.ifEmpty { null } ?: "?"

/** "Screen (Teams)" → "Teams"; other sources as they are. */
private fun sourceLabel(source: String?): String? =
    source?.substringAfter("(", source)?.removeSuffix(")")?.takeIf { it.isNotBlank() && it != "Screen" && it != "Quick add" }

/** Small line icons used in the pop-ups (24-unit grid, drawn as strokes). */
object LineIcons {
    val Clock = stroke("M12 13m-8 0a8 8 0 1 0 16 0a8 8 0 1 0 -16 0 M12 9v4l2.5 2.5 M9 2h6")
    val Bell = stroke("M6 8a6 6 0 1 1 12 0c0 7 3 9 3 9H3s3-2 3-9 M10.3 21a1.9 1.9 0 0 0 3.4 0")
    val Flag = stroke("M5 21V4 M5 4h11l-2 4 2 4H5")
    val Fingerprint = stroke(
        "M12 11v4 M8.5 8.5a5 5 0 0 1 8.5 3.5c0 3-1 5-2 7 M7 12a5 5 0 0 1 .5-2.2 M7 15.5c.3 1.6.9 3 1.7 4.5 " +
            "M12 4a8 8 0 0 1 8 8c0 1.5-.2 3-.6 4.3 M4 12a8 8 0 0 1 2.6-5.9"
    )
    val List = stroke("M8 6h13 M8 12h13 M8 18h13 M3.5 6h.01 M3.5 12h.01 M3.5 18h.01")

    private fun stroke(d: String) = ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .addPath(
            pathData = PathParser().parsePathString(d).toNodes(),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        .build()
}
