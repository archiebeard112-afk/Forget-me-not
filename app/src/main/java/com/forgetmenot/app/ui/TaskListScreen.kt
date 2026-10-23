package com.forgetmenot.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forgetmenot.app.data.Task
import java.time.LocalDate
import java.time.ZoneId

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
    val overdue = open.filter { it.dueAt != null && it.dueAt <= now }
    val sections = listOf(
        "Overdue" to overdue,
        "Today" to open.filter { it.dueAt != null && it.dueAt > now && it.dueAt < endOfToday },
        "Upcoming" to open.filter { it.dueAt != null && it.dueAt >= endOfToday },
        "No date" to open.filter { it.dueAt == null },
    )
    val done = tasks.filter { it.done }.sortedByDescending { it.completedAt ?: 0 }.take(30)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ListHeader(open.size, overdue.size, onSettings) },
        floatingActionButton = { GradientFab("New reminder", onAdd) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (open.isEmpty()) item { EmptyState() }
            for ((name, list) in sections) {
                if (list.isEmpty()) continue
                item(key = "h-$name") {
                    SectionLabel(name, list.size, color = if (name == "Overdue") Urgent else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(list, key = { it.id }) { TaskRow(it, now, onOpen, onToggleDone) }
            }
            if (done.isNotEmpty()) {
                item(key = "h-done") {
                    Row(
                        Modifier.padding(top = 8.dp).clip(CircleShape).clickable { showDone = !showDone }
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SectionLabel(if (showDone) "Hide done" else "Show done", done.size, modifier = Modifier.padding(0.dp))
                    }
                }
                if (showDone) items(done, key = { it.id }) { TaskRow(it, now, onOpen, onToggleDone) }
            }
        }
    }
}

@Composable
private fun ListHeader(openCount: Int, overdueCount: Int, onSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 20.dp, end = 16.dp, top = 14.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandMark(size = 40.dp, corner = 12.dp)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text("ForgetMeNot", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            val summary = when {
                openCount == 0 -> "All clear"
                overdueCount > 0 -> "$openCount open · $overdueCount overdue"
                else -> "$openCount open"
            }
            Text(
                summary,
                style = MaterialTheme.typography.bodySmall,
                color = if (overdueCount > 0) Urgent else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        RoundIconButton(LineIcons.Sliders, "Settings", onSettings)
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier.fillMaxWidth().padding(top = 72.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BrandMark(size = 72.dp, corner = 20.dp)
        Spacer(Modifier.size(4.dp))
        Text("Nothing to remember", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(
            "Hold the power button on a Teams message, use the + Task tile, or tap New reminder.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TaskRow(task: Task, now: Long, onOpen: (Task) -> Unit, onToggleDone: (Task) -> Unit) {
    val overdue = !task.done && task.dueAt != null && task.dueAt <= now
    Surface(
        onClick = { onOpen(task) },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)) {
            RoundCheck(task.done, overdue) { onToggleDone(task) }
            Column(Modifier.weight(1f).padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (task.highPriority && !task.done) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(Urgent))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        task.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 21.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (task.done) TextDecoration.LineThrough else null,
                        color = if (task.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    )
                }
                task.details?.let {
                    Text(
                        it,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val due = task.dueAt?.takeIf { !task.done }
                if (due != null || task.requester != null) {
                    Row(
                        Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (due != null) DueChip(formatDue(due, now), overdue)
                        task.requester?.let { Requester(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DueChip(label: String, overdue: Boolean) {
    val color = if (overdue) Urgent else MaterialTheme.colorScheme.primary
    Row(
        Modifier.clip(CircleShape).background(color.copy(alpha = 0.14f)).padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(LineIcons.Clock, null, Modifier.size(13.dp), tint = color)
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = color, modifier = Modifier.padding(start = 5.dp))
    }
}

@Composable
private fun Requester(name: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(20.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() },
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Text(
            name,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}
