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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.forgetmenot.app.app
import com.forgetmenot.app.data.DueDefaults
import com.forgetmenot.app.data.Task
import com.forgetmenot.app.ui.ForgetMeNotTheme
import com.forgetmenot.app.ui.LineIcons
import com.forgetmenot.app.ui.ReminderCard
import com.forgetmenot.app.ui.SheetActions
import com.forgetmenot.app.ui.SheetHeader
import com.forgetmenot.app.ui.TaskHeadline
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
        var nag by rememberSaveable { mutableStateOf(app.prefs.defaultNagMinutes) }
        var high by rememberSaveable { mutableStateOf(false) }
        val focus = remember { FocusRequester() }
        LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 18.dp, end = 18.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SheetHeader("New reminder") { finish() }
            TaskHeadline(text, { text = it }, modifier = Modifier.focusRequester(focus))
            ReminderCard(
                dueAt = dueAt,
                onDue = { dueAt = it },
                nagMinutes = nag,
                onNag = { nag = it },
                highPriority = high,
                onHighPriority = { high = it },
            )
            SheetActions(
                secondary = "Cancel",
                onSecondary = { finish() },
                primary = "Save with fingerprint",
                onPrimary = { confirmThenSave(text, dueAt, high, nag) },
                enabled = text.isNotBlank(),
                primaryIcon = LineIcons.Fingerprint,
            )
            Text(
                "Your phone stays locked",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    private fun confirmThenSave(text: String, dueAt: Long?, high: Boolean, nag: Int) {
        val save = { save(text, dueAt, high, nag) }
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
                .setTitle("Save to ForgetMeNot")
                .setSubtitle(text.lineSequence().first().take(60))
                .setAllowedAuthenticators(authenticators)
                .build()
        )
    }

    private fun save(text: String, dueAt: Long?, high: Boolean, nag: Int) {
        val trimmed = text.trim()
        val title = trimmed.lineSequence().first().take(120)
        val task = Task(
            title = title,
            notes = trimmed.takeIf { it != title },
            dueAt = dueAt,
            highPriority = high,
            nagMinutes = nag,
            source = "Lock screen",
        )
        lifecycleScope.launch {
            app.repository.save(task)
            Toast.makeText(this@LockScreenAddActivity, "Saved to ForgetMeNot", Toast.LENGTH_SHORT).show()
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
