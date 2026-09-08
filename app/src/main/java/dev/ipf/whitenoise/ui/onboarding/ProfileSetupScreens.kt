package dev.ipf.whitenoise.ui.onboarding

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.ui.components.WhiteNoiseButton
import dev.ipf.whitenoise.ui.components.WhiteNoiseScaffold
import dev.ipf.whitenoise.ui.components.WhiteNoiseTextField
import dev.ipf.whitenoise.ui.components.whiteNoiseVerticalScroll
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import dev.ipf.whitenoise.ui.theme.whiteNoiseStatusColors

@StringRes
internal fun ProfileSetupStep.title(): Int = when (this) {
    ProfileSetupStep.Profile -> R.string.setup_profile
    ProfileSetupStep.Follows -> R.string.setup_follows
    ProfileSetupStep.Relays -> R.string.setup_relays
    ProfileSetupStep.Inbox -> R.string.setup_inbox
    ProfileSetupStep.Device -> R.string.setup_device
    ProfileSetupStep.Messaging -> R.string.setup_messaging
}

@StringRes
private fun ProfileSetupStatus.label(): Int = when (this) {
    ProfileSetupStatus.Waiting -> R.string.setup_waiting
    ProfileSetupStatus.Checking -> R.string.setup_checking
    ProfileSetupStatus.Done -> R.string.setup_done
    ProfileSetupStatus.Skipped -> R.string.setup_skipped
    ProfileSetupStatus.Attention -> R.string.setup_attention
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SetupPage(
    title: String,
    onBack: () -> Unit,
    actions: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    WhiteNoiseScaffold(
        topBar = {
            TopAppBar(title = { Text(title) }, navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back))
                }
            }, scrollBehavior = dev.ipf.whitenoise.ui.components.LocalWhiteNoiseHeaderScroll.current)
        },
        bottomBar = {
            Box(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(WhiteNoiseSpacing.PinnedActionInset), contentAlignment = Alignment.Center) {
                Column(Modifier.widthIn(max = 520.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related), content = actions)
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 520.dp).fillMaxSize()
                .whiteNoiseVerticalScroll(rememberScrollState()).padding(WhiteNoiseSpacing.CompactScreenMargin),
                verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.FormField), content = content)
        }
    }
}

@Composable
fun ProfileSetupScreen(session: ProfileSetupSession, onBack: () -> Unit, onStep: (ProfileSetupStep) -> Unit, onOpenChats: () -> Unit) {
    SetupPage(stringResource(R.string.setup_title), onBack, actions = {
        WhiteNoiseButton(onClick = onOpenChats, enabled = session.ready && session.work == null,
            modifier = Modifier.fillMaxWidth().testTag("setup.open_chats")) { Text(stringResource(R.string.setup_open_chats)) }
    }) {
        if (!session.ready) Text(stringResource(R.string.setup_subtitle), style = MaterialTheme.typography.bodyLarge)
        Column {
            session.checks.forEach { check ->
                val status = stringResource(check.status.label())
                val actionable = check.status == ProfileSetupStatus.Attention
                val colors = whiteNoiseStatusColors()
                val leading: @Composable () -> Unit = {
                    Box(Modifier.size(24.dp).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
                        when (check.status) {
                            ProfileSetupStatus.Checking -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                            ProfileSetupStatus.Done -> Icon(painterResource(R.drawable.ic_check), null, tint = colors.success)
                            ProfileSetupStatus.Attention -> Icon(painterResource(R.drawable.ic_warning), null, tint = colors.warning)
                            ProfileSetupStatus.Skipped -> Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            ProfileSetupStatus.Waiting -> Text("○", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                val supporting: @Composable () -> Unit = { Text(status, Modifier.clearAndSetSemantics {}) }
                val headline: @Composable () -> Unit = { Text(stringResource(check.step.title())) }
                val rowModifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
                    stateDescription = status
                    liveRegion = LiveRegionMode.Polite
                }.testTag("setup.step.${check.step.name}")
                val rowColors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
                if (actionable) {
                    ListItem(onClick = { onStep(check.step) }, modifier = rowModifier.semantics { role = Role.Button },
                        leadingContent = leading, supportingContent = supporting,
                        trailingContent = { Icon(painterResource(R.drawable.ic_chevron_right), null) },
                        colors = rowColors, content = headline)
                } else {
                    ListItem(modifier = rowModifier, leadingContent = leading,
                        supportingContent = supporting, colors = rowColors, content = headline)
                }
            }
        }
    }
}

@Composable
fun ProfileSetupDetailScreen(
    session: ProfileSetupSession,
    step: ProfileSetupStep,
    onBack: () -> Unit,
    onAction: (ProfileSetupAction) -> Unit,
    onEditProfile: () -> Unit,
    onDiscoveryUrl: (String) -> Unit,
) {
    val check = session.check(step)
    val actions = session.actions(step)
    val busy = session.work?.step == step
    val issue = check.issue
    val relay = remember(session.id) { TextFieldState(session.discoveryUrl) }
    LaunchedEffect(relay) { snapshotFlow { relay.text.toString() }.collect(onDiscoveryUrl) }
    val primary = when (issue) {
        ProfileSetupIssue.OptionalProfile, ProfileSetupIssue.ProfileSave -> ProfileSetupAction.SaveProfile
        ProfileSetupIssue.MissingRelays -> ProfileSetupAction.UseDefaults
        ProfileSetupIssue.Device -> ProfileSetupAction.Acknowledge
        else -> ProfileSetupAction.Retry
    }
    val primaryLabel = when (primary) {
        ProfileSetupAction.SaveProfile -> R.string.setup_edit_profile
        ProfileSetupAction.UseDefaults -> R.string.setup_use_defaults
        ProfileSetupAction.Acknowledge -> if (session.deviceDiscovery == SetupDeviceDiscovery.NoneFound) R.string.setup_continue else R.string.setup_continue_anyway
        else -> R.string.try_again
    }
    SetupPage(stringResource(step.title()), onBack, actions = {
        WhiteNoiseButton(onClick = { if (primary == ProfileSetupAction.SaveProfile) onEditProfile() else onAction(primary) },
            enabled = primary in actions || (busy && session.work?.action == primary), loading = busy && session.work?.action == primary, loadingLabel = stringResource(R.string.setup_working), modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(primaryLabel))
        }
        if (step == ProfileSetupStep.Profile) TextButton(onClick = { onAction(ProfileSetupAction.Skip) }, enabled = ProfileSetupAction.Skip in actions, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.setup_not_now))
        }
        if (issue in setOf(ProfileSetupIssue.MissingRelays, ProfileSetupIssue.InconclusiveRelays)) OutlinedButton(
            onClick = { onAction(ProfileSetupAction.FindSettings) }, enabled = ProfileSetupAction.FindSettings in actions, modifier = Modifier.fillMaxWidth()) {
            if (busy && session.work?.action == ProfileSetupAction.FindSettings) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(WhiteNoiseSpacing.Related))
                Text(stringResource(R.string.setup_working))
            } else Text(stringResource(R.string.setup_find_settings))
        }
    }) {
        Text(stringResource(when (issue) {
            ProfileSetupIssue.ProfileLookup -> R.string.setup_profile_lookup_failed
            ProfileSetupIssue.OptionalProfile, ProfileSetupIssue.ProfileSave -> R.string.setup_profile_optional
            ProfileSetupIssue.MissingRelays -> R.string.setup_relay_explanation
            ProfileSetupIssue.InconclusiveRelays -> R.string.setup_relay_inconclusive
            ProfileSetupIssue.Device -> when (session.deviceDiscovery) {
                SetupDeviceDiscovery.NoneFound -> R.string.setup_device_none
                SetupDeviceDiscovery.Possible -> R.string.setup_device_possible
                SetupDeviceDiscovery.Unknown -> R.string.setup_device_unknown
            }
            else -> R.string.setup_messaging_failed
        }))
        if (issue == ProfileSetupIssue.MissingRelays) {
            ProfileSetupPolicy.defaultRelays.forEach { relay -> Text(relay.url, style = MaterialTheme.typography.bodyMedium) }
        }
        if (issue in setOf(ProfileSetupIssue.MissingRelays, ProfileSetupIssue.InconclusiveRelays)) {
            Text(stringResource(R.string.setup_discovery_help), style = MaterialTheme.typography.bodyMedium)
            WhiteNoiseTextField(state = relay, enabled = !busy, label = { Text(stringResource(R.string.setup_relay_url)) },
                lineLimits = TextFieldLineLimits.SingleLine, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                isError = session.invalidDiscoveryUrl, modifier = Modifier.fillMaxWidth(),
                supportingText = if (session.invalidDiscoveryUrl) { { Text(stringResource(R.string.setup_invalid_relay)) } } else null)
        }
    }
}
