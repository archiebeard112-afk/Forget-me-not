package com.forgetmenot.app.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.forgetmenot.app.data.Prefs
import com.forgetmenot.app.reminders.Notifications

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("BatteryLife")
@Composable
fun SettingsScreen(prefs: Prefs, onBack: () -> Unit) {
    val context = LocalContext.current
    var apiKey by remember { mutableStateOf(prefs.apiKey) }
    var model by remember { mutableStateOf(prefs.model) }
    var nag by remember { mutableIntStateOf(prefs.defaultNagMinutes) }
    var quiet by remember { mutableStateOf(prefs.quietHoursEnabled) }
    var quietStart by remember { mutableIntStateOf(prefs.quietStart) }
    var quietEnd by remember { mutableIntStateOf(prefs.quietEnd) }
    var pinned by remember { mutableStateOf(prefs.pinnedQuickAdd) }
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
            Text("Reading messages & screenshots", style = MaterialTheme.typography.titleMedium)
            Text(
                "Paste an Anthropic API key (console.anthropic.com) to let Claude turn messages and screenshots into tasks. " +
                    "Without one, shared text is saved as-is and you fill in the rest.",
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = model == id, onClick = { model = id; prefs.model = id })
                    Text(label)
                }
            }

            HorizontalDivider()
            Text("Nagging", style = MaterialTheme.typography.titleMedium)
            Text("Default time between nags", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NAG_OPTIONS.forEach { m ->
                    FilterChip(selected = nag == m, onClick = { nag = m; prefs.defaultNagMinutes = m }, label = { Text(formatNag(m)) })
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Quiet hours")
                    Text("No nags ${hour(quietStart)} – ${hour(quietEnd)}", style = MaterialTheme.typography.bodySmall)
                }
                Switch(quiet, { quiet = it; prefs.quietHoursEnabled = it })
            }
            if (quiet) {
                HourPicker("Stop at", quietStart) { quietStart = it; prefs.quietStart = it }
                HourPicker("Resume at", quietEnd) { quietEnd = it; prefs.quietEnd = it }
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
private fun HourPicker(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        OutlinedButton(onClick = { onChange((value + 23) % 24) }) { Text("−") }
        Text(hour(value), Modifier.padding(horizontal = 12.dp))
        OutlinedButton(onClick = { onChange((value + 1) % 24) }) { Text("+") }
    }
}

private fun hour(h: Int): String = when {
    h == 0 -> "12am"
    h < 12 -> "${h}am"
    h == 12 -> "12pm"
    else -> "${h - 12}pm"
}
