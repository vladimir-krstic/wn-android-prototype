package dev.ipf.whitenoise.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.MuteDuration

/** Selection applies immediately; every other dismissal leaves the preference unchanged. */
@Composable
fun MuteDurationDialog(
    onDismiss: () -> Unit,
    selectedDuration: MuteDuration? = null,
    onSelect: (MuteDuration) -> Unit,
) {
    WhiteNoiseAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.mute_for)) },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .selectableGroup(),
            ) {
                MuteDuration.entries.forEach { duration ->
                    WhiteNoiseDialogChoiceRow(
                        title = duration.label,
                        selected = duration == selectedDuration,
                        onClick = { onSelect(duration) },
                        modifier = Modifier.testTag("mute.duration.${duration.name}"),
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        modifier = Modifier.testTag("mute.duration.dialog"),
    )
}
