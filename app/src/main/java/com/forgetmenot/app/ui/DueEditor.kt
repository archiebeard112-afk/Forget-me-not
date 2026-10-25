package com.forgetmenot.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.forgetmenot.app.data.DueDefaults
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Pick a date, then a time. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDueDialog(initial: Long?, onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    val zone = ZoneId.systemDefault()
    val start = initial?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDateTime() }
        ?: LocalDateTime.now(zone).plusHours(1).withMinute(0)
    val today = LocalDate.now(zone)
    // The date picker works in UTC midnights.
    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = start.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isBefore(today)
        },
    )
    val timeState = rememberTimePickerState(initialHour = start.hour, initialMinute = start.minute, is24Hour = false)
    var date by remember { mutableStateOf<LocalDate?>(null) }

    val picked = date
    if (picked == null) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        date = dateState.selectedDateMillis
                            ?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() } ?: today
                    },
                ) { Text("Next: time") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        ) {
            DatePicker(state = dateState)
        }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(picked.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))) },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    onConfirm(picked.atTime(timeState.hour, timeState.minute).atZone(zone).toInstant().toEpochMilli())
                }) { Text("Set") }
            },
            dismissButton = { TextButton(onClick = { date = null }) { Text("Back") } },
        )
    }
}
