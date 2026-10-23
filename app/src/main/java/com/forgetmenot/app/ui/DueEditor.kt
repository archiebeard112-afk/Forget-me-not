package com.forgetmenot.app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** Shows the due time plus one-tap shortcuts and a date/time picker. */
@Composable
fun DueEditor(dueAt: Long?, onChange: (Long?) -> Unit) {
    val context = LocalContext.current
    val zone = ZoneId.systemDefault()
    Column {
        Text(
            if (dueAt == null) "No due time · no reminders" else "Due: ${formatDue(dueAt)}",
            style = MaterialTheme.typography.bodyMedium,
            color = if (dueAt != null && dueAt <= System.currentTimeMillis()) Urgent else MaterialTheme.colorScheme.onSurface,
        )
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = { onChange(System.currentTimeMillis()) }, label = { Text("ASAP") })
            AssistChip(onClick = { onChange(System.currentTimeMillis() + 60 * 60_000L) }, label = { Text("In 1h") })
            AssistChip(onClick = {
                onChange(LocalDate.now(zone).plusDays(1).atTime(9, 0).atZone(zone).toInstant().toEpochMilli())
            }, label = { Text("Tomorrow 9am") })
            AssistChip(onClick = {
                val start = dueAt?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDateTime() }
                    ?: LocalDateTime.now(zone).plusHours(1).withMinute(0)
                DatePickerDialog(context, { _, y, m, d ->
                    TimePickerDialog(context, { _, h, min ->
                        onChange(LocalDateTime.of(LocalDate.of(y, m + 1, d), LocalTime.of(h, min))
                            .atZone(zone).toInstant().toEpochMilli())
                    }, start.hour, start.minute, false).show()
                }, start.year, start.monthValue - 1, start.dayOfMonth).show()
            }, label = { Text("Pick…") })
            if (dueAt != null) AssistChip(onClick = { onChange(null) }, label = { Text("Clear") })
        }
    }
}
