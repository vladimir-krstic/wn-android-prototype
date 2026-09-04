package dev.ipf.whitenoise.ui.settings

import androidx.compose.ui.res.stringResource

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
        title = stringResource(R.string.ui_chat_with_support),
        onBack = onBack,
    ) {
        SettingsList {
            item {
                SettingsGroup(modifier = Modifier.padding(top = WhiteNoiseSpacing.Section)) {
                    ListItem(
                        headlineContent = {
                            Text(
                                stringResource(R.string.ui_white_noise_support),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        },
                        supportingContent = {
                            Text(
                                stringResource(R.string.ui_questions_problems_and_suggestions),
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
                    stringResource(R.string.ui_ask_how_something_works_report_a_problem_or_share_a_su),
                )
            }
            if (!canStart) {
                item {
                    SettingsCallout(
                        title = stringResource(R.string.ui_profile_relays_need_attention),
                        text = stringResource(R.string.support_chat_relay_required),
                        modifier = Modifier.padding(top = WhiteNoiseSpacing.Section),
                    )
                }
                item {
                    SettingsGroup(modifier = Modifier.padding(top = WhiteNoiseSpacing.Related)) {
                        SettingsLink(
                            title = stringResource(R.string.ui_open_relays),
                            subtitle = stringResource(R.string.ui_review_connection_status_and_chat_messages_roles),
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
                ) { Text(stringResource(R.string.ui_start_chat)) }
            }
        }
    }
}
