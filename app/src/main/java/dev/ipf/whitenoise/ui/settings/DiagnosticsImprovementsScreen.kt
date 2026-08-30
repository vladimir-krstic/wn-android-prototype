package dev.ipf.whitenoise.ui.settings

import android.text.format.Formatter
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import dev.ipf.whitenoise.ui.components.WhiteNoiseAlertDialog as AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import dev.ipf.whitenoise.ui.components.WhiteNoiseModalBottomSheet as ModalBottomSheet
import dev.ipf.whitenoise.ui.components.WhiteNoiseSheetHeader
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
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
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    val scope = rememberCoroutineScope()
    // Only actual dismissals record a decision; composition disposal/recreation does not.
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().testTag("diagnostics.prompt")) {
            WhiteNoiseSheetHeader(
                stringResource(R.string.help_improve_white_noise),
                onClose = { scope.launch { sheetState.hide(); onDismiss() } },
            )
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = WhiteNoiseSpacing.Section)
                    .testTag("diagnostics.prompt.content"),
            ) {
                Text(
                    stringResource(R.string.diagnostics_intro),
                    Modifier
                        .padding(
                            start = WhiteNoiseSpacing.Section,
                            end = WhiteNoiseSpacing.Section,
                            bottom = WhiteNoiseSpacing.Section,
                        )
                        .testTag("diagnostics.prompt.intro"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                DiagnosticsPromptSwitch(
                    title = stringResource(R.string.share_anonymous_analytics),
                    checked = profile.diagnostics.analyticsEnabled,
                    onCheckedChange = onAnalytics,
                    tag = "analytics",
                )
                DiagnosticsPromptSwitch(
                    title = stringResource(R.string.share_diagnostic_logs),
                    checked = profile.diagnostics.loggingEnabled,
                    onCheckedChange = onLogging,
                    tag = "logging",
                )
                Text(
                    stringResource(R.string.diagnostics_privacy),
                    Modifier
                        .padding(
                            start = WhiteNoiseSpacing.Section,
                            top = WhiteNoiseSpacing.Related,
                            end = WhiteNoiseSpacing.Section,
                        )
                        .testTag("diagnostics.prompt.privacy"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DiagnosticsPromptSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = WhiteNoiseSpacing.Related)
            .clip(MaterialTheme.shapes.large)
            .heightIn(min = 56.dp)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .semantics(mergeDescendants = true) { }
            .testTag("diagnostics.prompt.$tag.row")
            .padding(horizontal = WhiteNoiseSpacing.FormField),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .testTag("diagnostics.prompt.$tag.label"),
            style = MaterialTheme.typography.bodyLarge,
        )
        Switch(
            checked = checked,
            onCheckedChange = null,
            modifier = Modifier
                .testTag("diagnostics.prompt.$tag.switch")
                .clearAndSetSemantics { },
        )
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
            item {
                DiagnosticsSwitches(
                    profile = profile,
                    onAnalytics = onAnalytics,
                    onLogging = onLogging,
                )
            }
            if (profile.diagnostics.records.isNotEmpty()) {
                item { SettingsSection(stringResource(R.string.stored_diagnostic_logs)) }
                item {
                    SettingsGroup(
                        modifier = Modifier.testTag("diagnostics.stored.group"),
                    ) {
                        SettingsValue(
                            "On This Device",
                            if (profile.diagnostics.storedBytes == 0L) {
                                "None"
                            } else {
                                Formatter.formatShortFileSize(context, profile.diagnostics.storedBytes)
                            },
                        )
                        SettingsDivider(Modifier.testTag("diagnostics.stored.divider"))
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
private fun DiagnosticsSwitches(
    profile: Profile,
    onAnalytics: (Boolean) -> Unit,
    onLogging: (Boolean) -> Unit,
) {
    SettingsGroup(
        modifier = Modifier
            .padding(top = WhiteNoiseSpacing.Section)
            .testTag("diagnostics.choices.group"),
    ) {
        SettingsSwitch(
            stringResource(R.string.share_anonymous_analytics),
            profile.diagnostics.analyticsEnabled,
            onAnalytics,
            subtitle = stringResource(R.string.analytics_detail),
        )
        SettingsDivider(Modifier.testTag("diagnostics.choices.divider"))
        SettingsSwitch(
            stringResource(R.string.share_diagnostic_logs),
            profile.diagnostics.loggingEnabled,
            onLogging,
            subtitle = stringResource(R.string.diagnostic_logging_detail),
        )
    }
}
