package dev.ipf.whitenoise.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
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
    ) {
        SettingsList {
            item {
                SettingsGroup(modifier = Modifier.padding(top = WhiteNoiseSpacing.Section)) {
                    ListItem(
                        headlineContent = {
                            Text(
                                "White Noise Support",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        },
                        supportingContent = {
                            Text(
                                "Questions, problems, and suggestions",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        leadingContent = {
                            Surface(
                                modifier = Modifier.size(48.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                shape = CircleShape,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_settings_chat_bubble_outline),
                                    contentDescription = null,
                                    modifier = Modifier.padding(12.dp),
                                )
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
                SettingsExplainer(
                    "Ask how something works, report a problem, or share a suggestion.",
                )
            }
            if (!canStart) {
                item {
                    SettingsCallout(
                        title = "Profile relays need attention",
                        text = "Choose a connected Chat Messages relay before starting a support chat.",
                        modifier = Modifier.padding(top = WhiteNoiseSpacing.Section),
                    )
                }
                item {
                    SettingsGroup(modifier = Modifier.padding(top = WhiteNoiseSpacing.Related)) {
                        SettingsLink(
                            title = "Open Relays",
                            subtitle = "Review connection status and Chat Messages roles.",
                            onClick = onRelays,
                        )
                    }
                }
            }
            item {
                WhiteNoiseButton(
                    onClick = onStart,
                    enabled = canStart,
                    modifier = Modifier
                        .padding(
                            horizontal = WhiteNoiseSpacing.CompactScreenMargin,
                            vertical = WhiteNoiseSpacing.Section,
                        )
                        .fillMaxWidth(),
                ) { Text("Start Chat") }
            }
        }
    }
}
