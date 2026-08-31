package dev.ipf.whitenoise.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing

/** Ordinary sheets only. Material owns the handle, shape, width, IME, motion and dismissal. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhiteNoiseModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden),
    containerColor: Color? = null,
    contentWindowInsets: @Composable () -> WindowInsets = { WindowInsets.safeDrawing },
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    CompositionLocalProvider(
        LocalWhiteNoiseTextFieldContainerColor provides scheme.surfaceContainerLowest,
    ) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            modifier = modifier.testTag("sheet.surface"),
            sheetState = sheetState,
            containerColor = containerColor ?: scheme.surfaceContainerLow,
            contentColor = scheme.onSurface,
            contentWindowInsets = contentWindowInsets,
            dragHandle = {
                BottomSheetDefaults.DragHandle(Modifier.testTag("sheet.dragHandle"))
            },
            content = content,
        )
    }
}

/** Begins immediately after Material's drag-handle slot. No repeated status-bar or top padding. */
@Composable
fun WhiteNoiseSheetHeader(
    title: String,
    onClose: (() -> Unit)? = null,
    closeEnabled: Boolean = true,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .testTag("sheet.header")
            .padding(
                start = WhiteNoiseSpacing.Section,
                end = if (onClose == null) WhiteNoiseSpacing.Section else WhiteNoiseSpacing.Related,
                bottom = WhiteNoiseSpacing.Related,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        if (onClose != null) {
            IconButton(onClick = onClose, enabled = closeEnabled) {
                Icon(painterResource(R.drawable.ic_close), stringResource(R.string.close))
            }
        }
    }
}
