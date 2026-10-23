package com.forgetmenot.app.ui

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.forgetmenot.app.assist.AssistService
import com.forgetmenot.app.data.Prefs
import com.forgetmenot.app.reminders.Notifications
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@SuppressLint("BatteryLife")
@Composable
fun SettingsScreen(prefs: Prefs, onBack: () -> Unit) {
    val context = LocalContext.current
    var apiKey by remember { mutableStateOf(prefs.apiKey) }
    var model by remember { mutableStateOf(prefs.model) }
    var nag by remember { mutableIntStateOf(prefs.defaultNagMinutes) }
    var quiet by remember { mutableStateOf(prefs.quietHoursEnabled) }
    var quietStart by remember { mutableIntStateOf(prefs.quietStartMinute) }
    var quietEnd by remember { mutableIntStateOf(prefs.quietEndMinute) }
    var nagDays by remember { mutableStateOf(prefs.nagDays) }
    var pinned by remember { mutableStateOf(prefs.pinnedQuickAdd) }
    var instant by remember { mutableStateOf(prefs.instantSave) }
    var instantDue by remember { mutableStateOf(prefs.instantDue) }
    var powerMode by remember { mutableStateOf(prefs.powerButtonMode) }
    val power = context.getSystemService(PowerManager::class.java)
    var unrestricted by remember { mutableStateOf(power.isIgnoringBatteryOptimizations(context.packageName)) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
        )
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Hold the power button", style = MaterialTheme.typography.titleMedium)
            val active = remember { AssistService.isActive(context) }
            Text(
                if (active) {
                    "✅ Forget-me-not is your digital assistant. Hold the power button on any screen, such as a Teams chat."
                } else {
                    "Make Forget-me-not your digital assistant app, and holding the power button opens a pop-up with " +
                        "the text on screen. This replaces Google Assistant / Gemini on that button; you can switch back any time."
                },
                style = MaterialTheme.typography.bodySmall,
            )
            Choice("Halo: tap the message right on the screen (no AI)", powerMode == Prefs.POWER_HALO) {
                powerMode = Prefs.POWER_HALO; prefs.powerButtonMode = Prefs.POWER_HALO
            }
            Choice("List of the text on screen (no AI)", powerMode == Prefs.POWER_PICK) {
                powerMode = Prefs.POWER_PICK; prefs.powerButtonMode = Prefs.POWER_PICK
            }
            Choice("Claude reads the screen and drafts the task (needs API key)", powerMode == Prefs.POWER_CLAUDE) {
                powerMode = Prefs.POWER_CLAUDE; prefs.powerButtonMode = Prefs.POWER_CLAUDE
            }
            OutlinedButton(onClick = {
                runCatching { context.startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)) }
                    .onFailure { context.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)) }
            }) { Text(if (active) "Change digital assistant" else "Choose digital assistant") }
            Text(
                "In that screen, also turn on \"Analyze text on screen\" (names vary by phone). " +
                    "On Samsung, also set Settings → Advanced features → Side button → Press and hold → Digital assistant.",
                style = MaterialTheme.typography.bodySmall,
            )

            HorizontalDivider()
            Text("Saving shared or selected text", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Save in one tap")
                    Text(
                        "Picking \"Add to Forget-me-not\" saves straight away, then lets you adjust or undo.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(instant, { instant = it; prefs.instantSave = it })
            }
            Text("One-tap saves are due", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Prefs.INSTANT_DUE_OPTIONS.forEach { (id, label) ->
                    FilterChip(selected = instantDue == id, onClick = { instantDue = id; prefs.instantDue = id }, label = { Text(label) })
                }
            }

            HorizontalDivider()
            Text("Nagging", style = MaterialTheme.typography.titleMedium)
            Text("Default time between nags", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NAG_OPTIONS.forEach { m ->
                    FilterChip(selected = nag == m, onClick = { nag = m; prefs.defaultNagMinutes = m }, label = { Text(formatNag(m)) })
                }
            }
            Text("Nag on these days", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DayOfWeek.entries.forEach { day ->
                    val on = day in nagDays
                    FilterChip(
                        selected = on,
                        onClick = {
                            val next = if (on) nagDays - day else nagDays + day
                            // At least one day, or reminders would never fire.
                            if (next.isNotEmpty()) { nagDays = next; prefs.nagDays = next }
                        },
                        label = { Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault())) },
                    )
                }
            }
            Text(
                "Other days are quiet all day; nags wait for the next nag day.",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Quiet hours")
                    Text("No nags ${clock(quietStart)} – ${clock(quietEnd)}", style = MaterialTheme.typography.bodySmall)
                }
                Switch(quiet, { quiet = it; prefs.quietHoursEnabled = it })
            }
            if (quiet) {
                TimeRow("Stop at", quietStart) { quietStart = it; prefs.quietStartMinute = it }
                TimeRow("Resume at", quietEnd) { quietEnd = it; prefs.quietEndMinute = it }
            }

            HorizontalDivider()
            Text("Reading messages & screenshots", style = MaterialTheme.typography.titleMedium)
            Text(
                "Optional. Paste an Anthropic API key (console.anthropic.com) to let Claude turn messages and " +
                    "screenshots into tidy tasks. Everything else works without it.",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedTextField(
                apiKey, { apiKey = it; prefs.apiKey = it },
                label = { Text("API key") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            Prefs.MODELS.forEach { (id, label) ->
                Choice(label, model == id) { model = id; prefs.model = id }
            }

            HorizontalDivider()
            Text("Quick capture", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Pinned \"+ Add task\" notification")
                    Text("Always one tap away, including from the lock screen", style = MaterialTheme.typography.bodySmall)
                }
                Switch(pinned, {
                    pinned = it; prefs.pinnedQuickAdd = it
                    if (it) Notifications.showQuickAdd(context) else Notifications.hideQuickAdd(context)
                })
            }
            Text(
                "Quick Settings tile: swipe down twice, tap the pencil icon, and drag \"+ Task\" into your tiles.",
                style = MaterialTheme.typography.bodySmall,
            )

            HorizontalDivider()
            Text("Reliable reminders", style = MaterialTheme.typography.titleMedium)
            Text(
                if (unrestricted) "✅ Battery optimization is off for Forget-me-not."
                else "Samsung and other phones may silently kill reminders. Turn off battery optimization for this app.",
                style = MaterialTheme.typography.bodySmall,
            )
            if (!unrestricted) {
                OutlinedButton(onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
                    )
                    unrestricted = true
                }) { Text("Allow running in background") }
            }
        }
    }
}

@Composable
private fun Choice(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(label)
    }
}

@Composable
private fun TimeRow(label: String, minuteOfDay: Int, onChange: (Int) -> Unit) {
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        OutlinedButton(onClick = {
            TimePickerDialog(context, { _, h, m -> onChange(h * 60 + m) }, minuteOfDay / 60, minuteOfDay % 60, false).show()
        }) { Text(clock(minuteOfDay)) }
    }
}

private val clockFormat = DateTimeFormatter.ofPattern("h:mm a")

private fun clock(minuteOfDay: Int): String = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60).format(clockFormat)
