package com.forgetmenot.app.ui

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forgetmenot.app.assist.AssistService
import com.forgetmenot.app.data.Prefs
import com.forgetmenot.app.reminders.Notifications
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as DayStyle
import java.util.Locale

@SuppressLint("BatteryLife")
@Composable
fun SettingsScreen(prefs: Prefs, onBack: () -> Unit) {
    val context = LocalContext.current
    var nag by remember { mutableIntStateOf(prefs.defaultNagMinutes) }
    var quiet by remember { mutableStateOf(prefs.quietHoursEnabled) }
    var alarm by remember { mutableStateOf(prefs.alarmVibration) }
    var quietStart by remember { mutableIntStateOf(prefs.quietStartMinute) }
    var quietEnd by remember { mutableIntStateOf(prefs.quietEndMinute) }
    var nagDays by remember { mutableStateOf(prefs.nagDays) }
    var pinned by remember { mutableStateOf(prefs.pinnedQuickAdd) }
    var lockScreen by remember { mutableStateOf(prefs.lockScreenAdd) }
    var instant by remember { mutableStateOf(prefs.instantSave) }
    var instantDue by remember { mutableStateOf(prefs.instantDue) }
    var powerMode by remember { mutableStateOf(prefs.powerButtonMode) }
    val power = context.getSystemService(PowerManager::class.java)
    var unrestricted by remember { mutableStateOf(power.isIgnoringBatteryOptimizations(context.packageName)) }
    val assistantActive = remember { AssistService.isActive(context) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundIconButton(LineIcons.Back, "Back", onBack)
                Text("Settings", fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 14.dp))
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp).windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionLabel("Power button")
            SettingsGroup {
                SelectRow(LineIcons.Halo, "Halo", "Tap the message right on the screen", powerMode == Prefs.POWER_HALO) {
                    powerMode = Prefs.POWER_HALO; prefs.powerButtonMode = Prefs.POWER_HALO
                }
                GroupDivider()
                SelectRow(LineIcons.List, "List", "Pick from the text on screen", powerMode == Prefs.POWER_PICK) {
                    powerMode = Prefs.POWER_PICK; prefs.powerButtonMode = Prefs.POWER_PICK
                }
                GroupDivider()
                SettingRow(
                    LineIcons.Power,
                    if (assistantActive) "ForgetMeNot is your assistant" else "Set up the power button",
                    if (assistantActive) "Tap to change" else "Choose ForgetMeNot as digital assistant",
                    onClick = {
                        runCatching { context.startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)) }
                            .onFailure { context.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)) }
                    },
                ) { Chevron() }
            }
            Footnote(
                "Also turn on \"Analyze text on screen\" in that screen. On Samsung: Settings → Advanced features → " +
                    "Side button → Press and hold → Digital assistant."
            )

            SectionLabel("Lock screen")
            SettingsGroup {
                SwitchRow(
                    LineIcons.Lock, "Add from the lock screen",
                    "The + Task tile opens a pop-up without unlocking. Save needs your fingerprint.",
                    lockScreen,
                ) { lockScreen = it; prefs.lockScreenAdd = it }
                GroupDivider()
                SwitchRow(LineIcons.Pin, "Pinned \"+ Add task\" notification", "Always one tap away", pinned) {
                    pinned = it; prefs.pinnedQuickAdd = it
                    if (it) Notifications.showQuickAdd(context) else Notifications.hideQuickAdd(context)
                }
            }
            Footnote("To add the tile: swipe down from the top right, tap the pencil, and drag + Task into your tiles.")

            SectionLabel("Shared and selected text")
            SettingsGroup {
                SwitchRow(LineIcons.Text, "Save in one tap", "\"Add to ForgetMeNot\" saves straight away, with undo", instant) {
                    instant = it; prefs.instantSave = it
                }
                GroupDivider()
                SettingRow(LineIcons.Clock, "One-tap saves are due")
                PillChoices(Prefs.INSTANT_DUE_OPTIONS, instantDue) { instantDue = it; prefs.instantDue = it }
            }

            SectionLabel("Nagging")
            SettingsGroup {
                SwitchRow(
                    LineIcons.Bell, "Vibrate like an alarm",
                    "Strong vibration for up to 30 seconds, even on mute. Stops when you answer it.",
                    alarm,
                ) { alarm = it; prefs.alarmVibration = it }
                GroupDivider()
                SettingRow(LineIcons.Bell, "Remind me every", "Default for new reminders")
                PillChoices(NAG_OPTIONS.map { it to formatNag(it) }, nag) { nag = it; prefs.defaultNagMinutes = it }
                GroupDivider()
                SettingRow(LineIcons.Calendar, "Nag on these days", "Other days stay quiet all day")
                Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    DayOfWeek.entries.forEach { day ->
                        val on = day in nagDays
                        DayDot(day.getDisplayName(DayStyle.NARROW, Locale.getDefault()), on) {
                            val next = if (on) nagDays - day else nagDays + day
                            // At least one day, or reminders would never fire.
                            if (next.isNotEmpty()) { nagDays = next; prefs.nagDays = next }
                        }
                    }
                }
                GroupDivider()
                SwitchRow(LineIcons.Moon, "Quiet hours", "No nags ${clock(quietStart)} – ${clock(quietEnd)}", quiet) {
                    quiet = it; prefs.quietHoursEnabled = it
                }
                if (quiet) {
                    GroupDivider()
                    TimeRow("Stop at", quietStart) { quietStart = it; prefs.quietStartMinute = it }
                    GroupDivider()
                    TimeRow("Resume at", quietEnd) { quietEnd = it; prefs.quietEndMinute = it }
                }
            }

            SectionLabel("Reliable reminders")
            SettingsGroup {
                SettingRow(
                    LineIcons.Battery,
                    if (unrestricted) "Running in background" else "Allow running in background",
                    if (unrestricted) "Battery optimization is off" else "Samsung may otherwise silently stop reminders",
                    onClick = if (unrestricted) null else {
                        {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
                            )
                            unrestricted = true
                        }
                    },
                ) {
                    if (unrestricted) Icon(LineIcons.Check, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary) else Chevron()
                }
            }
        }
    }
}

@Composable
private fun Footnote(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 6.dp),
    )
}

@Composable
private fun Chevron() {
    Icon(LineIcons.Chevron, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun DayDot(label: String, on: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(38.dp).clip(CircleShape)
            .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLowest)
            .clickable(role = Role.Checkbox, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TimeRow(label: String, minuteOfDay: Int, onChange: (Int) -> Unit) {
    val context = LocalContext.current
    SettingRow(null, label) {
        Pill(clock(minuteOfDay), selected = true) {
            TimePickerDialog(context, { _, h, m -> onChange(h * 60 + m) }, minuteOfDay / 60, minuteOfDay % 60, false).show()
        }
    }
}

private val clockFormat = DateTimeFormatter.ofPattern("h:mm a")

private fun clock(minuteOfDay: Int): String = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60).format(clockFormat)
