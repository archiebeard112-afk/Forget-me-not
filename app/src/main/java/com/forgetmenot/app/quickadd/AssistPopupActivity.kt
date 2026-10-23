package com.forgetmenot.app.quickadd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.forgetmenot.app.assist.AssistInbox
import com.forgetmenot.app.ui.ForgetMeNotTheme

/** The pop-up shown over the current app when the power button is held. */
class AssistPopupActivity : ComponentActivity() {
    private val vm: QuickAddViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            AssistInbox.take()?.let { vm.loadFromScreen(it.screenText, it.screenshot, it.appPackage) }
        }
        setContent {
            ForgetMeNotTheme {
                Box(
                    Modifier.fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable(remember { MutableInteractionSource() }, indication = null) { finish() },
                ) {
                    Surface(
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.88f)
                            .clickable(remember { MutableInteractionSource() }, indication = null) {},
                    ) {
                        QuickAddScreen(vm, onClose = { finish() }, inSheet = true)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) AssistInbox.finishSession()
    }
}
