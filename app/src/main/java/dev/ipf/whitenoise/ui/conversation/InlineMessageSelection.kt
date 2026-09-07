package dev.ipf.whitenoise.ui.conversation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.rememberSelectionState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/** One message's text only. Metadata and attachment controls stay outside this container. */
@Composable
internal fun InlineMessageSelection(
    messageId: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val selection = rememberSelectionState()
    var hadSelection by remember { mutableStateOf(false) }
    LaunchedEffect(messageId) {
        // Text must be registered and laid out before Foundation can select its first word.
        repeat(2) { withFrameNanos { } }
        if (selection.selectedTexts.isEmpty()) selection.extendSelectionByWord()
    }
    LaunchedEffect(selection.selectedTexts.isNotEmpty()) {
        if (selection.selectedTexts.isNotEmpty()) hadSelection = true
        else if (hadSelection) onDismiss()
    }
    BackHandler {
        selection.clear()
        onDismiss()
    }
    SelectionContainer(
        state = selection,
        modifier = Modifier.testTag("message.inlineSelection.$messageId"),
        content = content,
    )
}
