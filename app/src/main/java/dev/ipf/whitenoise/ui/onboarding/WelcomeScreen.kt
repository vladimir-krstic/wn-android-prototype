@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.ipf.whitenoise.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import dev.ipf.whitenoise.model.AccessAttempt
import dev.ipf.whitenoise.model.Profile
import dev.ipf.whitenoise.ui.components.whiteNoiseVerticalScroll
import dev.ipf.whitenoise.ui.components.WhiteNoiseButton
import dev.ipf.whitenoise.ui.components.WhiteNoiseOutlinedButton
import dev.ipf.whitenoise.ui.components.WhiteNoiseTopBar
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing

private const val WelcomeMarkWidthFraction = 0.5f
private const val WelcomeMarkAspectRatio = 598f / 460f
private val WelcomeContentMaxWidth = 520.dp

@Composable
fun WelcomeScreen(
    origin: OnboardingOrigin,
    onSignIn: () -> Unit,
    onSignUp: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    retainedProfiles: List<Profile> = emptyList(),
    attempt: AccessAttempt? = null,
    onContinueProfile: (String) -> Unit = {},
    onRetry: (Long) -> Unit = {},
    onRecover: (Long) -> Unit = {},
    onCancel: () -> Unit = {},
) {
    var chooseProfile by rememberSaveable { mutableStateOf(false) }
    val continuingProfile = retainedProfiles.firstOrNull { it.id == attempt?.candidate?.id } ?: retainedProfiles.firstOrNull()
    val busy = attempt?.phase?.isBusy == true
    Scaffold(
        modifier = modifier.fillMaxSize(),
        // Welcome has no editor: the outgoing screen's IME must not lift its logo or actions.
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout),
        topBar = {
            if (origin == OnboardingOrigin.AddProfile) {
                WhiteNoiseTopBar(
                    title = stringResource(R.string.add_profile),
                    onBack = onBack,
                )
            }
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .consumeWindowInsets(contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                val availableMarkHeight =
                    (maxHeight - WhiteNoiseSpacing.CompactScreenMargin * 2).coerceAtLeast(0.dp)
                val markWidth = minOf(
                    maxWidth.coerceAtMost(WelcomeContentMaxWidth) * WelcomeMarkWidthFraction,
                    availableMarkHeight * WelcomeMarkAspectRatio,
                )
                Icon(
                    painter = painterResource(R.drawable.ic_white_noise_mark),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.size(
                        width = markWidth,
                        height = markWidth / WelcomeMarkAspectRatio,
                    ),
                )
            }
            Column(
                modifier = Modifier
                    .then(if (retainedProfiles.isNotEmpty()) Modifier.weight(1f, fill = false)
                        .whiteNoiseVerticalScroll(rememberScrollState()) else Modifier)
                    .widthIn(max = WelcomeContentMaxWidth)
                    .fillMaxWidth()
                    .padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin)
                    .padding(bottom = WhiteNoiseSpacing.PinnedActionInset),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
            ) {
                continuingProfile?.let { profile ->
                    WhiteNoiseButton(
                        onClick = { onContinueProfile(profile.id) },
                        enabled = attempt == null || busy,
                        loading = busy,
                        loadingLabel = stringResource(accessProgressLabel(attempt?.phase)),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.access_continue_as, profile.name)) }
                    if (retainedProfiles.size > 1) TextButton(onClick = { chooseProfile = true }, enabled = attempt == null) {
                        Text(stringResource(R.string.access_choose_profile))
                    }
                    AccessFeedback(attempt, onRetry, onRecover, onCancel)
                }
                WhiteNoiseOutlinedButton(
                    onClick = onSignIn,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.sign_in))
                }
                WhiteNoiseButton(
                    onClick = onSignUp,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.sign_up))
                }
            }
        }
    }
    if (chooseProfile) RetainedProfilesSheet(
        retainedProfiles,
        onSelect = { chooseProfile = false; onContinueProfile(it) },
        onDismiss = { chooseProfile = false },
    )
}
