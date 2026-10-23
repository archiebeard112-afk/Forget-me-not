package com.forgetmenot.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forgetmenot.app.data.Task

val NAG_OPTIONS = listOf(15, 30, 60, 120, 240)

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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundIconButton(LineIcons.Back, "Back", onBack)
                Text(
                    if (task.done) "Done reminder" else "Edit reminder",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(start = 14.dp),
                )
                if (task.id != 0L) RoundIconButton(LineIcons.Trash, "Delete", { onDelete(task) }, tint = Urgent)
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TaskHeadline(title, { title = it }, modifier = Modifier.padding(top = 8.dp))
            FromRow(requester.ifEmpty { null }, task.source) { requester = it }
            TextCard(LineIcons.Tag, "Details") {
                CardTextField(details, { details = it }, "Customer, amounts, product codes", monospace = true)
            }
            ReminderCard(
                dueAt = dueAt,
                onDue = { dueAt = it },
                nagMinutes = nag,
                onNag = { nag = it },
                highPriority = high,
                onHighPriority = { high = it },
            )
            if (dueAt != null) {
                Text(
                    "Turn off reminders for this task",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End).clickable { dueAt = null }.padding(4.dp),
                )
            }
            TextCard(LineIcons.Notes, "Notes") {
                CardTextField(notes, { notes = it }, "Anything else to remember", minLines = 3)
            }
            task.source?.let {
                Text(
                    "Added from $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SheetActions(
                secondary = if (task.done) "Reopen" else "Mark done",
                onSecondary = { onToggleDone(task) },
                primary = "Save changes",
                onPrimary = { onSave(edited()) },
                enabled = title.isNotBlank(),
            )
        }
    }
}

@Composable
private fun CardTextField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    monospace: Boolean = false,
    minLines: Int = 1,
) {
    val style = TextStyle(
        fontSize = 15.sp,
        lineHeight = 21.sp,
        fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
        color = MaterialTheme.colorScheme.onSurface,
    )
    BasicTextField(
        value = value,
        onValueChange = onChange,
        textStyle = style,
        minLines = minLines,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            androidx.compose.foundation.layout.Box {
                if (value.isEmpty()) Text(placeholder, style = style.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                inner()
            }
        },
    )
}
