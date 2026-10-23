package com.forgetmenot.app.quickadd

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forgetmenot.app.ui.DueEditor
import com.forgetmenot.app.ui.FromRow
import com.forgetmenot.app.ui.ReminderCard
import com.forgetmenot.app.ui.RoundIconButton
import com.forgetmenot.app.ui.SheetActions
import com.forgetmenot.app.ui.SheetHeader
import com.forgetmenot.app.ui.TaskHeadline
import com.forgetmenot.app.ui.Warning

@Composable
fun QuickAddScreen(
    vm: QuickAddViewModel,
    onClose: () -> Unit,
    onEdit: (Long) -> Unit,
    inSheet: Boolean = false,
) {
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(vm::loadImage)
    }
    val onPickImage = {
        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    val title = when (vm.phase) {
        Phase.Review -> "Check & save"
        Phase.Pick -> "Pick a message"
        Phase.Confirm -> "New reminder"
        Phase.Saved -> "Saved"
        Phase.Reading -> "Reading"
        else -> "Quick add"
    }
    if (inSheet) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 10.dp)) {
            SheetHeader(title, onClose)
            Spacer(Modifier.size(14.dp))
            PhaseContent(vm, onClose, onEdit, onPickImage, Modifier)
        }
    } else {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Row(
                    Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RoundIconButton(Icons.Default.Close, "Close", onClose)
                    Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 14.dp))
                }
            },
        ) { padding ->
            PhaseContent(
                vm, onClose, onEdit, onPickImage,
                Modifier.fillMaxSize().padding(padding).imePadding().padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun PhaseContent(
    vm: QuickAddViewModel,
    onClose: () -> Unit,
    onEdit: (Long) -> Unit,
    onPickImage: () -> Unit,
    modifier: Modifier,
) {
    if (vm.phase == Phase.Pick) {
        PickPhase(vm, modifier)
        return
    }
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (vm.phase) {
            Phase.Input -> InputPhase(vm, onClose, onPickImage)
            Phase.Reading -> Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.size(16.dp))
                    Text(
                        when {
                            vm.screenText != null -> "Reading your screen…"
                            vm.image != null -> "Reading screenshot…"
                            else -> "Reading message…"
                        }
                    )
                }
            }
            Phase.Review -> ReviewPhase(vm, onClose)
            Phase.Confirm -> ConfirmPhase(vm, onClose)
            Phase.Saved -> SavedPhase(vm, onClose, onEdit)
            Phase.Pick, Phase.Halo -> Unit
        }
        Spacer(Modifier.size(8.dp))
    }
}

/** Power button without AI: the screen's text as tappable lines. */
@Composable
private fun PickPhase(vm: QuickAddViewModel, modifier: Modifier) {
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) {
        // Chats put the newest message at the bottom.
        if (vm.screenLines.isNotEmpty()) listState.scrollToItem(vm.screenLines.lastIndex)
    }
    Column(modifier) {
        Text(
            "Tap the message you want to remember.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f, fill = false).padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            itemsIndexed(vm.screenLines) { i, line ->
                val selected = i in vm.selectedLines
                Surface(
                    onClick = { vm.pick(i) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(line, Modifier.padding(horizontal = 12.dp, vertical = 10.dp), maxLines = 6, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        vm.error?.let { WarningCard(it) }
        if (vm.hasApiKey) {
            OutlinedButton(onClick = vm::read, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                Text("✨ Let Claude read it instead")
            }
        } else {
            Spacer(Modifier.size(12.dp))
        }
    }
}

/** A message picked from the screen: change anything, then Save reminder saves it. */
@Composable
private fun ConfirmPhase(vm: QuickAddViewModel, onClose: () -> Unit) {
    val task = vm.pendingTask ?: return
    val context = LocalContext.current
    TaskHeadline(task.title, { vm.updatePending(task.copy(title = it)) })
    task.notes?.let {
        Text(
            it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
    }
    FromRow(task.requester, task.source) { vm.updatePending(task.copy(requester = it.ifBlank { null })) }
    ReminderCard(
        dueAt = task.dueAt,
        onDue = { vm.updatePending(task.copy(dueAt = it)) },
        nagMinutes = task.nagMinutes,
        onNag = { vm.updatePending(task.copy(nagMinutes = it)) },
        highPriority = task.highPriority,
        onHighPriority = { vm.updatePending(task.copy(highPriority = it)) },
    )
    SheetActions(
        secondary = "Back",
        onSecondary = vm::backToScreen,
        primary = "Save reminder",
        onPrimary = {
            vm.confirmPending {
                Toast.makeText(context, "Saved to ForgetMeNot", Toast.LENGTH_SHORT).show()
                onClose()
            }
        },
        enabled = task.title.isNotBlank(),
    )
}

/** Shown after a one-tap save: change the due time or priority, edit, or undo. */
@Composable
private fun SavedPhase(vm: QuickAddViewModel, onClose: () -> Unit, onEdit: (Long) -> Unit) {
    val task = vm.savedTask ?: return
    TaskHeadline(task.title, null)
    task.notes?.let {
        Text(
            it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
    }
    if (task.requester != null) FromRow(task.requester, task.source, null)
    ReminderCard(
        dueAt = task.dueAt,
        onDue = { vm.updateSaved(task.copy(dueAt = it)) },
        nagMinutes = task.nagMinutes,
        onNag = { vm.updateSaved(task.copy(nagMinutes = it)) },
        highPriority = task.highPriority,
        onHighPriority = { vm.updateSaved(task.copy(highPriority = it)) },
    )
    SheetActions(
        secondary = "Undo",
        onSecondary = { vm.undoSaved(onClose) },
        primary = "Done",
        onPrimary = onClose,
        extra = {
            Text(
                "Edit",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onEdit(task.id) }.padding(horizontal = 6.dp, vertical = 12.dp),
            )
        },
    )
}

@Composable
private fun InputPhase(vm: QuickAddViewModel, onClose: () -> Unit, onPickImage: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (vm.image == null) runCatching { focus.requestFocus() } }

    vm.error?.let { WarningCard(it) }

    vm.clipboardSuggestion?.let { clip ->
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Column(Modifier.padding(12.dp)) {
                Text("You copied:", style = MaterialTheme.typography.labelMedium)
                Text(clip, maxLines = 4, style = MaterialTheme.typography.bodyMedium)
                Row {
                    TextButton(onClick = vm::useClipboard) { Text("Use this") }
                    TextButton(onClick = { vm.clipboardSuggestion = null }) { Text("Ignore") }
                }
            }
        }
    }

    vm.screenText?.let { text ->
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Captured ${text.lines().size} lines of text from your screen",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = vm::clearScreen) { Icon(Icons.Default.Close, "Discard screen text") }
            }
        }
    }

    vm.image?.let { bmp ->
        Box {
            Image(
                bmp.asImageBitmap(), "Screenshot",
                contentScale = ContentScale.Crop,
                alignment = Alignment.BottomCenter,
                modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp),
            )
            IconButton(onClick = vm::clearImage, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(Icons.Default.Close, "Remove screenshot")
            }
        }
    }

    OutlinedTextField(
        vm.input, { vm.input = it },
        label = { Text(if (vm.image != null) "Anything to add? (optional)" else "What do you need to remember?") },
        minLines = 2,
        modifier = Modifier.fillMaxWidth().focusRequester(focus),
    )

    if (vm.image == null && vm.screenText == null) DueEditor(vm.dueAt) { vm.dueAt = it }

    val hasScreen = vm.screenText != null
    val canRead = vm.hasApiKey && (vm.input.isNotBlank() || vm.image != null || hasScreen)
    Button(onClick = vm::read, enabled = canRead, modifier = Modifier.fillMaxWidth()) {
        Text(
            when {
                hasScreen -> "✨ Read my screen"
                vm.image != null -> "✨ Read screenshot"
                else -> "✨ Read with Claude"
            }
        )
    }
    if (!vm.hasApiKey) {
        Text(
            "Add an API key in Settings to let Claude read messages and screenshots.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            onClick = { vm.saveTyped(onClose) },
            enabled = vm.input.isNotBlank() && vm.image == null && !hasScreen,
            modifier = Modifier.weight(1f),
        ) { Text("Save as typed") }
        OutlinedButton(onClick = onPickImage, modifier = Modifier.weight(1f)) { Text("Pick screenshot") }
    }
}

@Composable
private fun ReviewPhase(vm: QuickAddViewModel, onClose: () -> Unit) {
    vm.drafts.forEachIndexed { i, d ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(d.include, { vm.drafts[i] = d.copy(include = it) })
                    Text(if (d.include) "Add this task" else "Skip", style = MaterialTheme.typography.labelLarge)
                }
                d.warnings.forEach { WarningCard(it) }
                OutlinedTextField(d.title, { vm.drafts[i] = d.copy(title = it) }, label = { Text("What") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    d.details, { vm.drafts[i] = d.copy(details = it) },
                    label = { Text("Details — check codes") },
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(d.requester, { vm.drafts[i] = d.copy(requester = it) }, label = { Text("Who asked") }, modifier = Modifier.fillMaxWidth())
                DueEditor(d.dueAt) { vm.drafts[i] = d.copy(dueAt = it) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("High priority", Modifier.weight(1f))
                    Switch(d.highPriority, { vm.drafts[i] = d.copy(highPriority = it) })
                }
            }
        }
    }
    if (vm.skipped.isNotEmpty()) {
        Text("Not added", style = MaterialTheme.typography.labelLarge)
        vm.skipped.forEach {
            Text("• $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    val count = vm.drafts.count { it.include }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = vm::backToInput, modifier = Modifier.weight(1f)) { Text("Back") }
        Button(onClick = { vm.saveDrafts(onClose) }, enabled = count > 0, modifier = Modifier.weight(1f)) {
            Text(if (count == 1) "Save task" else "Save $count tasks")
        }
    }
    Spacer(Modifier.size(16.dp))
}

@Composable
private fun WarningCard(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Warning.copy(alpha = 0.15f))) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, null, tint = Warning)
            Spacer(Modifier.size(8.dp))
            Text(text, style = MaterialTheme.typography.bodySmall)
        }
    }
}
