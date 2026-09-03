package dev.ipf.whitenoise.ui.onboarding

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.AccessAttempt
import dev.ipf.whitenoise.model.AccessFailure
import dev.ipf.whitenoise.model.AccessPhase
import dev.ipf.whitenoise.model.Profile
import dev.ipf.whitenoise.model.StartupPhase
import dev.ipf.whitenoise.ui.components.AdaptiveContent
import dev.ipf.whitenoise.ui.components.ProfileAvatar
import dev.ipf.whitenoise.ui.components.WhiteNoiseAlertDialog
import dev.ipf.whitenoise.ui.components.WhiteNoiseButton
import dev.ipf.whitenoise.ui.components.WhiteNoiseModalBottomSheet
import dev.ipf.whitenoise.ui.components.WhiteNoiseScaffold
import dev.ipf.whitenoise.ui.components.WhiteNoiseSheetHeader
import dev.ipf.whitenoise.ui.components.whiteNoiseVerticalScroll
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing

@StringRes
internal fun accessProgressLabel(phase: AccessPhase?): Int = when (phase) {
    AccessPhase.CreatingProfile -> R.string.creating_profile
    AccessPhase.AmberIdentity -> R.string.access_amber_identity
    AccessPhase.AmberProof -> R.string.access_amber_proof
    AccessPhase.Recovering -> R.string.access_recovering
    else -> R.string.signing_in
}

@StringRes
private fun AccessFailure.message(): Int = when (this) {
    AccessFailure.Offline -> R.string.access_offline
    AccessFailure.SignIn -> R.string.access_sign_in_failed
    AccessFailure.CreateProfile -> R.string.access_creation_failed
    AccessFailure.SetupRetry -> R.string.access_setup_retry
    AccessFailure.PublicationRetry -> R.string.access_publication_retry
    AccessFailure.UnexpectedSetup -> R.string.access_setup_unexpected
    AccessFailure.RecoveryPartial -> R.string.access_recovery_partial
    AccessFailure.RecoveryUnexpected -> R.string.access_recovery_unexpected
    AccessFailure.AmberUnavailable -> R.string.access_amber_unavailable
    AccessFailure.AmberCancelled -> R.string.access_amber_cancelled
    AccessFailure.AmberRejected -> R.string.access_amber_rejected
    AccessFailure.AmberMismatch -> R.string.access_amber_mismatch
}

@Composable
fun AccessFeedback(
    attempt: AccessAttempt?,
    onRetry: (Long) -> Unit,
    onRecover: (Long) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (attempt == null) return
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
    ) {
        attempt.failure?.let { failure ->
            Text(
                stringResource(failure.message()),
                color = if (failure == AccessFailure.AmberCancelled) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("access.failure").semantics { liveRegion = LiveRegionMode.Polite },
            )
            TextButton(onClick = { onRetry(attempt.id) }) { Text(stringResource(R.string.try_again)) }
        }
        if (attempt.phase != AccessPhase.RecoveryConsent) {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
        }
    }
    if (attempt.phase == AccessPhase.RecoveryConsent) {
        WhiteNoiseAlertDialog(
            onDismissRequest = onCancel,
            title = { Text(stringResource(R.string.access_recovery_title)) },
            text = {
                Text(
                    stringResource(R.string.access_recovery_details),
                    modifier = Modifier.whiteNoiseVerticalScroll(rememberScrollState()),
                )
            },
            confirmButton = {
                TextButton(onClick = { onRecover(attempt.id) }) { Text(stringResource(R.string.access_recover)) }
            },
            dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetainedProfilesSheet(profiles: List<Profile>, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    WhiteNoiseModalBottomSheet(onDismissRequest = onDismiss) {
        WhiteNoiseSheetHeader(stringResource(R.string.access_choose_profile), onClose = onDismiss)
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
            items(profiles, key = Profile::id) { profile ->
                ListItem(
                    supportingContent = { Text(profile.shortPublicKey) },
                    leadingContent = {
                        ProfileAvatar(
                            name = profile.name, avatar = profile.avatar,
                            modifier = Modifier.size(48.dp), contentDescription = null,
                        )
                    },
                    modifier = Modifier.clickable { onSelect(profile.id) },
                ) { Text(profile.name) }
            }
        }
    }
}

@Composable
fun StartupScreen(phase: StartupPhase, hasProfiles: Boolean, onRetry: () -> Unit, onChooseProfile: () -> Unit) {
    WhiteNoiseScaffold(contentWindowInsets = WindowInsets.safeDrawing) { padding ->
        AdaptiveContent(Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier.align(Alignment.Center).widthIn(max = 520.dp).fillMaxWidth()
                    .whiteNoiseVerticalScroll(rememberScrollState()).padding(WhiteNoiseSpacing.Section),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.FormField),
            ) {
                if (phase == StartupPhase.Loading) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.access_starting), Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                } else {
                    Text(stringResource(R.string.access_startup_failed), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(if (hasProfiles) R.string.access_startup_retained else R.string.access_startup_retry))
                    WhiteNoiseButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.try_again))
                    }
                    if (hasProfiles) TextButton(onClick = onChooseProfile) {
                        Text(stringResource(R.string.access_choose_profile))
                    }
                }
            }
        }
    }
}
