package com.forgetmenot.app.quickadd

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.forgetmenot.app.ui.DueEditor
import com.forgetmenot.app.ui.Warning

@OptIn(ExperimentalMaterial3Api::class)
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
        Phase.Pick -> "Tap what to save"
        Phase.Saved -> "Saved ✓"
        else -> "Quick add"
    }
    if (inSheet) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(start = 4.dp, end = 16.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") }
                Text(title, style = MaterialTheme.typography.titleLarge)
            }
            PhaseContent(vm, onClose, onEdit, onPickImage, Modifier.padding(horizontal = 16.dp))
        }
    } else {
        Scaffold(topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") } },
            )
        }) { padding ->
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
            "Tap the message you want to remember. Tap more lines to add them to the same task.",
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
                    onClick = { vm.toggleLine(i) },
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
        Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (vm.hasApiKey) {
                OutlinedButton(onClick = vm::read, modifier = Modifier.weight(1f)) { Text("✨ Let Claude read it") }
            }
            val n = vm.selectedLines.size
            Button(onClick = vm::savePicked, enabled = n > 0, modifier = Modifier.weight(1f)) {
                Text(if (n <= 1) "Save" else "Save $n lines")
            }
        }
    }
}

/** Shown after a one-tap save: change the due time or priority, edit, or undo. */
@Composable
private fun SavedPhase(vm: QuickAddViewModel, onClose: () -> Unit, onEdit: (Long) -> Unit) {
    val task = vm.savedTask ?: return
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(task.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            task.notes?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 4, overflow = TextOverflow.Ellipsis)
            }
            task.requester?.let {
                Text("From $it", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    DueEditor(task.dueAt) { vm.updateSaved(task.copy(dueAt = it)) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("High priority", Modifier.weight(1f))
        Switch(task.highPriority, { vm.updateSaved(task.copy(highPriority = it)) })
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { vm.undoSaved(onClose) }) { Text("Undo") }
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = { onEdit(task.id) }) { Text("Edit") }
        Button(onClick = onClose) { Text("Done") }
    }
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
