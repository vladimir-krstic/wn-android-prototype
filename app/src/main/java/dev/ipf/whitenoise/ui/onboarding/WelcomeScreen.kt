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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.navigation.OnboardingOrigin
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
) {
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
                    .widthIn(max = WelcomeContentMaxWidth)
                    .fillMaxWidth()
                    .padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin)
                    .padding(bottom = WhiteNoiseSpacing.PinnedActionInset),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
            ) {
                WhiteNoiseOutlinedButton(
                    onClick = onSignIn,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.sign_in))
                }
                WhiteNoiseButton(
                    onClick = onSignUp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.sign_up))
                }
            }
        }
    }
}
