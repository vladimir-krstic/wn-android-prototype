package dev.ipf.whitenoise.ui.settings

import android.os.Build
import android.view.inputmethod.EditorInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.PlatformTextInputInterceptor
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformTextInputSession

/** Preserve field actions and existing privacy flags; this is a request to the keyboard. */
// The caller supplies Build.VERSION.SDK_INT; this inlined flag is guarded before use.
@android.annotation.SuppressLint("InlinedApi")
internal fun incognitoImeOptions(options: Int, enabled: Boolean, apiLevel: Int): Int =
    if (enabled && apiLevel >= 26) options or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING else options

internal class IncognitoKeyboardInterceptor(private val enabled: Boolean, private val blocked: Boolean = false) : PlatformTextInputInterceptor {
    override suspend fun interceptStartInputMethod(request: PlatformTextInputMethodRequest, nextHandler: PlatformTextInputSession): Nothing {
        if (blocked) kotlinx.coroutines.awaitCancellation()
        nextHandler.startInputMethod(PlatformTextInputMethodRequest { info ->
            val connection = request.createInputConnection(info)
            info.imeOptions = incognitoImeOptions(info.imeOptions,enabled,Build.VERSION.SDK_INT)
            connection
        })
    }
}

@Composable
internal fun IncognitoKeyboardScope(enabled: Boolean, blocked: Boolean = false, content: @Composable () -> Unit) {
    val interceptor = remember(enabled,blocked) { IncognitoKeyboardInterceptor(enabled,blocked) }
    InterceptPlatformTextInput(interceptor,content)
}
