package com.forgetmenot.app.quickadd

import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.forgetmenot.app.app
import com.forgetmenot.app.data.DueDefaults
import com.forgetmenot.app.data.Task
import com.forgetmenot.app.ui.DueEditor
import com.forgetmenot.app.ui.ForgetMeNotTheme
import kotlinx.coroutines.launch

/**
 * Add a task from the lock screen without unlocking. It can only add (never shows existing
 * tasks), and Save needs a fingerprint, face or the phone's PIN. Opened by the "+ Task" tile and
 * the pinned notification; when the phone is unlocked it hands over to the normal quick add.
 */
class LockScreenAddActivity : FragmentActivity() {
    private val keyguard by lazy { getSystemService(KeyguardManager::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) setShowWhenLocked(true)
        super.onCreate(savedInstanceState)
        if (!keyguard.isKeyguardLocked) {
            openQuickAdd()
            return
        }
        if (!app.prefs.lockScreenAdd) {
            // Turned off in Settings: unlock first, then the normal quick add.
            unlockThen { openQuickAdd() }
            return
        }
        enableEdgeToEdge()
        setContent {
            ForgetMeNotTheme {
                PopupSheet(onDismiss = { finish() }) { LockScreenForm() }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun LockScreenForm() {
        var text by rememberSaveable { mutableStateOf("") }
        var dueAt by rememberSaveable { mutableStateOf(DueDefaults.instant(app.prefs)) }
        var high by rememberSaveable { mutableStateOf(false) }
        val focus = remember { FocusRequester() }
        LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("New task", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = { finish() }) { Icon(Icons.Default.Close, "Cancel") }
            }
            OutlinedTextField(
                text, { text = it },
                label = { Text("What do you need to remember?") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
            DueEditor(dueAt) { dueAt = it }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("High priority", Modifier.weight(1f))
                Switch(high, { high = it })
            }
            Text(
                "You'll confirm with your fingerprint or face. Your phone stays locked.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { finish() }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    onClick = { confirmThenSave(text, dueAt, high) },
                    enabled = text.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) { Text("Save") }
            }
        }
    }

    private fun confirmThenSave(text: String, dueAt: Long?, high: Boolean) {
        val save = { save(text, dueAt, high) }
        val authenticators = BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        if (BiometricManager.from(this).canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            unlockThen(save)
            return
        }
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = save()

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                when (errorCode) {
                    // The person backed out: keep the pop-up open, save nothing.
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    BiometricPrompt.ERROR_CANCELED -> Unit
                    // Some phones can't show the prompt over the lock screen: use the normal unlock.
                    else -> unlockThen(save)
                }
            }
        })
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Save to Forget-me-not")
                .setSubtitle(text.lineSequence().first().take(60))
                .setAllowedAuthenticators(authenticators)
                .build()
        )
    }

    private fun save(text: String, dueAt: Long?, high: Boolean) {
        val trimmed = text.trim()
        val title = trimmed.lineSequence().first().take(120)
        val task = Task(
            title = title,
            notes = trimmed.takeIf { it != title },
            dueAt = dueAt,
            highPriority = high,
            nagMinutes = app.prefs.defaultNagMinutes,
            source = "Lock screen",
        )
        lifecycleScope.launch {
            app.repository.save(task)
            Toast.makeText(this@LockScreenAddActivity, "Saved to Forget-me-not", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun unlockThen(action: () -> Unit) {
        keyguard.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
            override fun onDismissSucceeded() = action()
            override fun onDismissCancelled() {
                if (!app.prefs.lockScreenAdd) finish()
            }
            override fun onDismissError() {
                if (!app.prefs.lockScreenAdd) finish()
            }
        })
    }

    private fun openQuickAdd() {
        startActivity(Intent(this, QuickAddActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }
}
