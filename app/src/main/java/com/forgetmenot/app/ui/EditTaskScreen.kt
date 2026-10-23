package com.forgetmenot.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.forgetmenot.app.data.Task

val NAG_OPTIONS = listOf(15, 30, 60, 120, 240)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskScreen(
    task: Task,
    onSave: (Task) -> Unit,
    onToggleDone: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    onBack: () -> Unit,
) {
    var title by rememberSaveable { mutableStateOf(task.title) }
    var requester by rememberSaveable { mutableStateOf(task.requester ?: "") }
    var details by rememberSaveable { mutableStateOf(task.details ?: "") }
    var notes by rememberSaveable { mutableStateOf(task.notes ?: "") }
    var dueAt by rememberSaveable { mutableStateOf(task.dueAt) }
    var high by rememberSaveable { mutableStateOf(task.highPriority) }
    var nag by rememberSaveable { mutableStateOf(task.nagMinutes) }

    fun edited() = task.copy(
        title = title.trim().ifEmpty { task.title },
        requester = requester.trim().ifEmpty { null },
        details = details.trim().ifEmpty { null },
        notes = notes.trim().ifEmpty { null },
        dueAt = dueAt,
        highPriority = high,
        nagMinutes = nag,
    )

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (task.id == 0L) "New task" else "Edit task") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            actions = {
                if (task.id != 0L) IconButton(onClick = { onDelete(task) }) { Icon(Icons.Default.Delete, "Delete") }
            },
        )
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(title, { title = it }, label = { Text("What") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                details, { details = it }, label = { Text("Details (codes, amounts)") },
                textStyle = TextStyle(fontFamily = FontFamily.Monospace),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(requester, { requester = it }, label = { Text("Who asked") }, modifier = Modifier.fillMaxWidth())
            DueEditor(dueAt) { dueAt = it }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("High priority", Modifier.weight(1f))
                Switch(high, { high = it })
            }
            Text("Nag every", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NAG_OPTIONS.forEach { m ->
                    FilterChip(selected = nag == m, onClick = { nag = m }, label = { Text(formatNag(m)) })
                }
            }
            OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            task.source?.let {
                Text("Added from $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { onSave(edited()) }, modifier = Modifier.weight(1f)) { Text("Save") }
                if (task.id != 0L) {
                    OutlinedButton(onClick = { onToggleDone(task) }, modifier = Modifier.weight(1f)) {
                        Text(if (task.done) "Reopen" else "Mark done")
                    }
                }
            }
            Spacer(Modifier.padding(8.dp))
        }
    }
}
