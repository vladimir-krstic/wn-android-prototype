package dev.ipf.whitenoise.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.ProfileExitAttempt
import dev.ipf.whitenoise.model.ProfileExitStep
import dev.ipf.whitenoise.model.ProfileExitStepResult
import dev.ipf.whitenoise.ui.components.WhiteNoiseAlertDialog
import dev.ipf.whitenoise.ui.components.whiteNoiseVerticalScroll
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing

@StringRes
internal fun ProfileExitStep.progressLabel(): Int = when (this) {
    ProfileExitStep.LeaveGroups -> R.string.exit_leaving_groups
    ProfileExitStep.RelayCleanup -> R.string.exit_clearing_relays
    ProfileExitStep.LocalCleanup -> R.string.exit_cleaning_local
}

@Composable
internal fun ProfileExitStatus(attempt: ProfileExitAttempt, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().whiteNoiseVerticalScroll(rememberScrollState())
            .padding(WhiteNoiseSpacing.CompactScreenMargin),
        verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.FormField),
    ) {
        attempt.currentStep?.let {
            CircularProgressIndicator()
            Text(stringResource(it.progressLabel()), Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        if (!attempt.isRunning) {
            Text(
                stringResource(when {
                    !attempt.localCleanupCompleted -> R.string.exit_local_incomplete
                    attempt.options.wipeData -> R.string.exit_wipe_partial
                    else -> R.string.exit_sign_out_partial
                }),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        ProfileExitStep.entries.forEach { step ->
            val result = attempt.results.getValue(step)
            val status = when (result) {
                ProfileExitStepResult.Pending -> if (step == attempt.currentStep) R.string.exit_in_progress else R.string.exit_pending
                ProfileExitStepResult.Done -> R.string.exit_done
                ProfileExitStepResult.Incomplete -> R.string.exit_incomplete
                ProfileExitStepResult.NotRequested -> R.string.exit_not_requested
            }
            Row(
                Modifier.fillMaxWidth().testTag("exit.step.${step.name}"),
                horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.FormField),
            ) {
                Text(stringResource(when (step) {
                    ProfileExitStep.LeaveGroups -> R.string.exit_groups
                    ProfileExitStep.RelayCleanup -> R.string.exit_relays
                    ProfileExitStep.LocalCleanup -> if (attempt.options.wipeData) R.string.exit_local_data else R.string.exit_local_session
                }), Modifier.weight(1f))
                Text(
                    stringResource(status), Modifier.weight(1f),
                    color = if (result == ProfileExitStepResult.Incomplete) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun ProfileExitReportDialog(report: ProfileExitAttempt, onDismiss: () -> Unit) {
    WhiteNoiseAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.exit_report_title, report.profileName)) },
        text = { ProfileExitStatus(report) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}
