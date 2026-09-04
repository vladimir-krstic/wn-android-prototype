package dev.ipf.whitenoise.ui.settings

import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.HelpAboutPolicy
import dev.ipf.whitenoise.model.HelpExternalDestination
import dev.ipf.whitenoise.ui.components.WhiteNoiseAlertDialog
import dev.ipf.whitenoise.ui.components.WhiteNoiseButton
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing

@Composable
fun HelpScreen(
    onBack: () -> Unit,
    onReportBug: () -> Unit,
    onAbout: () -> Unit,
) {
    SettingsScaffold(title = stringResource(R.string.help_title), onBack = onBack) {
        SettingsList {
            item {
                SettingsGroup(modifier = Modifier.padding(top = WhiteNoiseSpacing.Section)) {
                    SettingsLink(
                        title = stringResource(R.string.report_bug_title),
                        subtitle = stringResource(R.string.report_bug_summary),
                        onClick = onReportBug,
                        leading = { HelpLeadingIcon(R.drawable.ic_bug_report) },
                    )
                    SettingsDivider()
                    SettingsLink(
                        title = stringResource(R.string.about_licenses_title),
                        subtitle = stringResource(R.string.about_licenses_summary),
                        onClick = onAbout,
                        leading = { HelpLeadingIcon(R.drawable.ic_info) },
                    )
                }
            }
        }
    }
}

@Composable
fun BugReportScreen(
    onBack: () -> Unit,
    onOpenExternal: ((HelpExternalDestination) -> Boolean)? = null,
) {
    val context = LocalContext.current
    val open = onOpenExternal ?: remember(context) { { destination -> openHelpExternal(context, destination) } }
    var openFailed by rememberSaveable { mutableStateOf(false) }
    fun openReport() {
        openFailed = !open(HelpExternalDestination.BugReport)
    }
    SettingsScaffold(
        title = stringResource(R.string.report_bug_title),
        onBack = onBack,
        bottomBar = {
            SettingsBottomAction {
                WhiteNoiseButton(
                    onClick = ::openReport,
                    modifier = Modifier.fillMaxWidth().testTag("help.bug.open"),
                ) { Text(stringResource(R.string.report_bug_open_github)) }
            }
        },
    ) {
        SettingsList {
            item { SettingsSection(stringResource(R.string.report_bug_destination_section)) }
            item {
                SettingsGroup {
                    SettingsValue(
                        title = stringResource(R.string.report_bug_destination),
                        value = stringResource(R.string.report_bug_destination_detail),
                    )
                }
            }
            item { SettingsSection(stringResource(R.string.report_bug_privacy_section)) }
            item {
                SettingsCallout(
                    title = stringResource(R.string.report_bug_no_attachments_title),
                    text = stringResource(R.string.report_bug_no_attachments_detail),
                    modifier = Modifier.testTag("help.bug.privacy"),
                    leading = { HelpLeadingIcon(R.drawable.ic_settings_front_hand) },
                )
            }
            item { SettingsExplainer(stringResource(R.string.report_bug_public_reminder)) }
        }
    }
    if (openFailed) {
        HelpOpenFailureDialog(
            title = stringResource(R.string.report_bug_open_failed_title),
            body = stringResource(R.string.report_bug_open_failed_detail),
            onRetry = ::openReport,
            onDismiss = { openFailed = false },
        )
    }
}

@Composable
fun AboutLicensesScreen(
    versionName: String,
    buildNumber: String,
    onBack: () -> Unit,
    onOpenExternal: ((HelpExternalDestination) -> Boolean)? = null,
    onOpenLicenses: (() -> Boolean)? = null,
) {
    val context = LocalContext.current
    val openExternal = onOpenExternal ?: remember(context) { { destination -> openHelpExternal(context, destination) } }
    val openLicenses = onOpenLicenses ?: remember(context) { { openSourceLicenses(context) } }
    var failedDestination by rememberSaveable { mutableStateOf<AboutOpenFailure?>(null) }
    fun openPrivacy() {
        failedDestination = if (openExternal(HelpExternalDestination.PrivacyPolicy)) null else AboutOpenFailure.Privacy
    }
    fun showLicenses() {
        failedDestination = if (openLicenses()) null else AboutOpenFailure.Licenses
    }
    SettingsScaffold(title = stringResource(R.string.about_licenses_title), onBack = onBack) {
        SettingsList {
            item { SettingsSection(stringResource(R.string.about_app_section)) }
            item {
                SettingsGroup {
                    SettingsMetadata(stringResource(R.string.about_version), versionName)
                    SettingsDivider()
                    SettingsMetadata(stringResource(R.string.about_build), buildNumber)
                }
            }
            item { SettingsSection(stringResource(R.string.about_legal_section)) }
            item {
                SettingsGroup {
                    SettingsLink(
                        title = stringResource(R.string.open_source_licenses),
                        subtitle = stringResource(R.string.open_source_licenses_summary),
                        onClick = ::showLicenses,
                        leading = { HelpLeadingIcon(R.drawable.ic_description) },
                    )
                    SettingsDivider()
                    SettingsLink(
                        title = stringResource(R.string.privacy_policy),
                        subtitle = stringResource(R.string.privacy_policy_summary),
                        onClick = ::openPrivacy,
                        leading = { HelpLeadingIcon(R.drawable.ic_settings_front_hand) },
                    )
                }
            }
        }
    }
    failedDestination?.let { failure ->
        HelpOpenFailureDialog(
            title = stringResource(
                if (failure == AboutOpenFailure.Licenses) {
                    R.string.licenses_open_failed_title
                } else {
                    R.string.privacy_policy_open_failed_title
                },
            ),
            body = stringResource(R.string.external_open_failed_detail),
            onRetry = if (failure == AboutOpenFailure.Licenses) ::showLicenses else ::openPrivacy,
            onDismiss = { failedDestination = null },
        )
    }
}

private enum class AboutOpenFailure { Licenses, Privacy }

@Composable
private fun HelpOpenFailureDialog(
    title: String,
    body: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    WhiteNoiseAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = onRetry) { Text(stringResource(R.string.help_retry)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun HelpLeadingIcon(@DrawableRes icon: Int) {
    Icon(
        painter = painterResource(icon),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(24.dp),
    )
}

internal fun helpExternalIntent(destination: HelpExternalDestination): Intent? {
    val target = HelpAboutPolicy.target(destination)
    if (!HelpAboutPolicy.isAllowed(destination, target.url)) return null
    return Intent(Intent.ACTION_VIEW, target.url.toUri())
}

private fun openHelpExternal(context: Context, destination: HelpExternalDestination): Boolean {
    val intent = helpExternalIntent(destination) ?: return false
    if (intent.resolveActivity(context.packageManager) == null) return false
    return runCatching { context.startActivity(intent) }.isSuccess
}

private fun openSourceLicenses(context: Context): Boolean {
    val intent = Intent(context, OssLicensesMenuActivity::class.java)
    if (intent.resolveActivity(context.packageManager) == null) return false
    OssLicensesMenuActivity.setActivityTitle(context.getString(R.string.open_source_licenses))
    return runCatching { context.startActivity(intent) }.isSuccess
}
