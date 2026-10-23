package com.forgetmenot.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.forgetmenot.app.data.Task
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    tasks: List<Task>,
    onOpen: (Task) -> Unit,
    onToggleDone: (Task) -> Unit,
    onAdd: () -> Unit,
    onSettings: () -> Unit,
) {
    var showDone by remember { mutableStateOf(false) }
    val now = System.currentTimeMillis()
    val zone = ZoneId.systemDefault()
    val endOfToday = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val open = tasks.filter { !it.done }
    val sections = listOf(
        "Overdue" to open.filter { it.dueAt != null && it.dueAt <= now },
        "Today" to open.filter { it.dueAt != null && it.dueAt > now && it.dueAt < endOfToday },
        "Upcoming" to open.filter { it.dueAt != null && it.dueAt >= endOfToday },
        "No date" to open.filter { it.dueAt == null },
    )
    val done = tasks.filter { it.done }.sortedByDescending { it.completedAt ?: 0 }.take(30)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Forget-me-not") },
                actions = {
                    IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Settings") }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Add") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (open.isEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Nothing to remember 🌼", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.size(8.dp))
                        Text(
                            "Tap Add, use the \"+ Task\" tile, or share a Teams screenshot to Forget-me-not.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            for ((name, list) in sections) {
                if (list.isEmpty()) continue
                item(key = "h-$name") { SectionHeader(name, list.size, urgent = name == "Overdue") }
                items(list, key = { it.id }) { TaskRow(it, now, onOpen, onToggleDone) }
            }
            if (done.isNotEmpty()) {
                item(key = "h-done") {
                    TextButton(onClick = { showDone = !showDone }) {
                        Text(if (showDone) "Hide done (${done.size})" else "Show done (${done.size})")
                    }
                }
                if (showDone) items(done, key = { it.id }) { TaskRow(it, now, onOpen, onToggleDone) }
            }
        }
    }
}

@Composable
private fun SectionHeader(name: String, count: Int, urgent: Boolean) {
    Text(
        "$name · $count",
        style = MaterialTheme.typography.labelLarge,
        color = if (urgent) Urgent else MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
    )
}

@Composable
private fun TaskRow(task: Task, now: Long, onOpen: (Task) -> Unit, onToggleDone: (Task) -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onOpen(task) }) {
        Row(Modifier.padding(end = 12.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = task.done, onCheckedChange = { onToggleDone(task) })
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (task.highPriority && !task.done) {
                        Box(Modifier.size(8.dp).background(Urgent, CircleShape))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        task.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (task.done) TextDecoration.LineThrough else null,
                    )
                }
                task.details?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val meta = listOfNotNull(
                    task.dueAt?.takeIf { !task.done }?.let { formatDue(it, now) },
                    task.requester,
                ).joinToString(" · ")
                if (meta.isNotEmpty()) {
                    val overdue = !task.done && task.dueAt != null && task.dueAt <= now
                    Text(
                        meta,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (overdue) Urgent else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
