package dev.ipf.whitenoise.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import dev.ipf.whitenoise.ui.components.whiteNoiseVerticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.Profile
import dev.ipf.whitenoise.ui.components.WhiteNoiseButton
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing

@Composable
fun SupportScreen(
    profile: Profile,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onRelays: () -> Unit,
) {
    val hasExistingChat = profile.chats.any { it.id == "white-noise-support" }
    val canStart = hasExistingChat || profile.chatRelayUrls.isNotEmpty()

    SettingsScaffold(
        title = "Chat with support",
        onBack = onBack,
        bottomBar = {
            SettingsBottomAction {
                WhiteNoiseButton(
                    onClick = onStart,
                    enabled = canStart,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Start Chat") }
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .whiteNoiseVerticalScroll(rememberScrollState())
                .padding(vertical = WhiteNoiseSpacing.Section),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.FormField),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_info),
                    contentDescription = null,
                    modifier = Modifier.padding(WhiteNoiseSpacing.FormField),
                )
            }
            Text(
                "White Noise Support",
                modifier = Modifier.padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                "Questions, problems, and suggestions",
                modifier = Modifier.padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "Ask how something works, report a problem, or share a suggestion.",
                modifier = Modifier.padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (!canStart) {
                SettingsCallout(
                    title = "Profile relays need attention",
                    text = "Choose a connected Chat Messages relay before starting a support chat.",
                    modifier = Modifier.padding(top = WhiteNoiseSpacing.Related),
                )
                SettingsGroup {
                    SettingsLink(
                        title = "Open Relays",
                        subtitle = "Review connection status and Chat Messages roles.",
                        onClick = onRelays,
                    )
                }
            }
        }
    }
}
