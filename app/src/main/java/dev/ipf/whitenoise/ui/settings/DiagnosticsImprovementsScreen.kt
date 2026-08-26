package dev.ipf.whitenoise.ui.settings

import android.text.format.Formatter
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import dev.ipf.whitenoise.ui.components.WhiteNoiseModalBottomSheet as ModalBottomSheet
import dev.ipf.whitenoise.ui.components.WhiteNoiseSheetHeader
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.Profile
import dev.ipf.whitenoise.state.AppUiState
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import kotlinx.coroutines.launch

@Composable
internal fun DiagnosticsPromptHost(
    uiState: AppUiState,
    chatsResumed: Boolean,
    onAnalytics: (String, Boolean) -> Unit,
    onLogging: (String, Boolean) -> Unit,
    onDismiss: (String) -> Unit,
) {
    uiState.diagnosticsPromptProfile(chatsResumed)?.let { profile ->
        DiagnosticsPromptSheet(
            profile = profile,
            onAnalytics = { onAnalytics(profile.id, it) },
            onLogging = { onLogging(profile.id, it) },
            onDismiss = { onDismiss(profile.id) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiagnosticsPromptSheet(profile: Profile, onAnalytics: (Boolean) -> Unit, onLogging: (Boolean) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    // Only actual dismissals record a decision; composition disposal/recreation does not.
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().testTag("diagnostics.prompt")) {
            WhiteNoiseSheetHeader(
                stringResource(R.string.help_improve_white_noise),
                onClose = { scope.launch { sheetState.hide(); onDismiss() } },
            )
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(bottom = WhiteNoiseSpacing.Section)) {
                Text(
                    stringResource(R.string.diagnostics_intro),
                    Modifier.padding(horizontal = WhiteNoiseSpacing.Section).padding(bottom = WhiteNoiseSpacing.Section),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                DiagnosticsSwitches(profile, onAnalytics, onLogging, showDetails = false)
                Text(
                    stringResource(R.string.diagnostics_privacy),
                    Modifier.padding(horizontal = WhiteNoiseSpacing.SettingsSectionInset, vertical = WhiteNoiseSpacing.Related),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun DiagnosticsImprovementsScreen(
    profile: Profile,
    onBack: () -> Unit,
    onAnalytics: (Boolean) -> Unit,
    onLogging: (Boolean) -> Unit,
    onClear: () -> Unit,
) {
    var confirmClear by rememberSaveable(profile.id) { mutableStateOf(false) }
    val context = LocalContext.current
    SettingsScaffold(stringResource(R.string.diagnostics_improvements), onBack) {
        SettingsList {
            item { SettingsSection("Diagnostics") }
            item { DiagnosticsSwitches(profile, onAnalytics, onLogging, showDetails = true) }
            item { SettingsSection(stringResource(R.string.stored_diagnostic_logs)) }
            item {
                SettingsGroup {
                    SettingsValue("On This Device", if (profile.diagnostics.storedBytes == 0L) "None" else Formatter.formatShortFileSize(context, profile.diagnostics.storedBytes))
                    SettingsAction(
                        title = stringResource(R.string.clear_diagnostic_logs),
                        onClick = { confirmClear = true },
                        enabled = profile.diagnostics.storedBytes > 0,
                        destructive = true,
                    )
                }
                SettingsExplainer(stringResource(R.string.diagnostic_logs_retained))
            }
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.clear_diagnostic_logs_title)) },
            text = { Text(stringResource(R.string.clear_diagnostic_logs_detail)) },
            confirmButton = {
                TextButton(onClick = { onClear(); confirmClear = false }) {
                    Text(stringResource(R.string.clear_logs), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun DiagnosticsSwitches(profile: Profile, onAnalytics: (Boolean) -> Unit, onLogging: (Boolean) -> Unit, showDetails: Boolean) {
    SettingsGroup {
        SettingsSwitch(
            stringResource(R.string.share_anonymous_analytics),
            profile.diagnostics.analyticsEnabled,
            onAnalytics,
            subtitle = if (showDetails) stringResource(R.string.analytics_detail) else null,
        )
        SettingsSwitch(
            stringResource(R.string.share_diagnostic_logs),
            profile.diagnostics.loggingEnabled,
            onLogging,
            subtitle = if (showDetails) stringResource(R.string.diagnostic_logging_detail) else null,
        )
    }
}
