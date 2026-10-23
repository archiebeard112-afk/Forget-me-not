package com.forgetmenot.app.ui

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val time = DateTimeFormatter.ofPattern("h:mm a")
private val dayTime = DateTimeFormatter.ofPattern("EEE h:mm a")
private val dateTime = DateTimeFormatter.ofPattern("MMM d, h:mm a")

/** "Overdue 2h", "Today 3:00 PM", "Tomorrow 9:00 AM", "Fri 9:00 AM", "Nov 3, 9:00 AM". */
fun formatDue(dueAt: Long, now: Long = System.currentTimeMillis()): String {
    val zone = ZoneId.systemDefault()
    val due = Instant.ofEpochMilli(dueAt).atZone(zone)
    if (dueAt <= now) {
        val late = Duration.ofMillis(now - dueAt)
        return when {
            late.toMinutes() < 1 -> "Due now"
            late.toHours() < 1 -> "Overdue ${late.toMinutes()}m"
            late.toDays() < 1 -> "Overdue ${late.toHours()}h"
            else -> "Overdue ${late.toDays()}d"
        }
    }
    val today = LocalDate.now(zone)
    val day = due.toLocalDate()
    return when {
        day == today -> "Today ${due.format(time)}"
        day == today.plusDays(1) -> "Tomorrow ${due.format(time)}"
        day.isBefore(today.plusDays(7)) -> due.format(dayTime)
        else -> due.format(dateTime)
    }
}

fun formatNag(minutes: Int): String = if (minutes < 60) "${minutes}m" else "${minutes / 60}h"
