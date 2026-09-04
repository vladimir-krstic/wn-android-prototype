package dev.ipf.whitenoise.ui.settings

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.core.net.toUri
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.AgentConnector
import dev.ipf.whitenoise.model.AgentSetupPolicy
import dev.ipf.whitenoise.model.Profile
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing

private const val AgentConnectorDocs =
    "https://github.com/marmot-protocol/mdk/blob/master/crates/agent-connector/README.md"

private data class AgentConnectorResources(
    val name: Int,
    val subtitle: Int,
    val prompt: Int,
)

@Composable
fun AiAgentsScreen(
    profile: Profile,
    onBack: () -> Unit,
    copyOverride: ((label: String, value: String) -> Unit)? = null,
    openDocsOverride: (() -> Boolean)? = null,
) {
    val context = LocalContext.current
    val publicKey = AgentSetupPolicy.publicKeyOrNull(profile)
    var feedback by rememberSaveable(profile.id) { mutableStateOf<Int?>(null) }
    val copyValue = copyOverride ?: remember(context) {
        { label: String, value: String -> copyToClipboard(context, label, value) }
    }
    val openDocs = openDocsOverride ?: remember(context) {
        { openAgentConnectorDocs(context) }
    }

    SettingsScaffold(
        title = stringResource(R.string.ai_agents_title),
        onBack = onBack,
    ) {
        SettingsList {
            item { SettingsSection(stringResource(R.string.ai_agents_about_section)) }
            item {
                SettingsGroup {
                    SettingsExplainer(
                        stringResource(R.string.ai_agents_about_body),
                    )
                }
            }
            item { SettingsSection(stringResource(R.string.ai_agents_connectors_section)) }
            item {
                SettingsGroup(modifier = Modifier.testTag("ai_agents.connectors")) {
                    SettingsExplainer(stringResource(R.string.ai_agents_connectors_body))
                    AgentSetupPolicy.connectors.forEachIndexed { index, connector ->
                        key(connector) {
                            if (index > 0) SettingsDivider()
                            AgentConnectorRow(
                                connector = connector,
                                publicKey = publicKey,
                                onCopy = { name, prompt ->
                                    copyValue("$name setup prompt", prompt)
                                    feedback = R.string.ai_agents_prompt_copied
                                },
                            )
                        }
                    }
                }
            }
            item { SettingsSection(stringResource(R.string.ai_agents_manual_section)) }
            item {
                SettingsGroup(modifier = Modifier.testTag("ai_agents.manual")) {
                    SettingsExplainer(stringResource(R.string.ai_agents_manual_body))
                    if (publicKey == null) {
                        Text(
                            text = stringResource(R.string.ai_agents_public_key_unavailable),
                            modifier = Modifier.padding(WhiteNoiseSpacing.FormField),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        Column(
                            modifier = Modifier.padding(WhiteNoiseSpacing.FormField),
                            verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
                        ) {
                            Text(
                                text = stringResource(R.string.ai_agents_public_key_label),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelLarge,
                            )
                            SelectionContainer {
                                Text(
                                    text = publicKey,
                                    modifier = Modifier.testTag("ai_agents.public_key"),
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            TextButton(
                                onClick = {
                                    copyValue("Public key", publicKey)
                                    feedback = R.string.ai_agents_public_key_copied
                                },
                                modifier = Modifier.testTag("ai_agents.copy_public_key"),
                            ) { Text(stringResource(R.string.ai_agents_copy_public_key)) }
                        }
                    }
                    SettingsDivider()
                    SettingsLink(
                        title = stringResource(R.string.ai_agents_docs_title),
                        subtitle = stringResource(R.string.ai_agents_docs_subtitle),
                        onClick = {
                            feedback = if (openDocs()) {
                                R.string.ai_agents_docs_opened
                            } else {
                                R.string.ai_agents_docs_failed
                            }
                        },
                    )
                }
            }
            feedback?.let { message ->
                item {
                    Text(
                        text = stringResource(message),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(WhiteNoiseSpacing.FormField)
                            .testTag("ai_agents.feedback")
                            .semantics { liveRegion = LiveRegionMode.Polite },
                        color = if (message == R.string.ai_agents_docs_failed) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun AgentConnectorRow(
    connector: AgentConnector,
    publicKey: String?,
    onCopy: (name: String, prompt: String) -> Unit,
) {
    val resources = connector.resources()
    val name = stringResource(resources.name)
    val prompt = publicKey?.let { stringResource(resources.prompt, it) }
    var expanded by rememberSaveable(connector.name, publicKey) { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(WhiteNoiseSpacing.FormField)
            .testTag("ai_agents.connector.${connector.name.lowercase()}"),
        verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
    ) {
        Text(name, style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(resources.subtitle),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
            TextButton(
                onClick = { expanded = !expanded },
                enabled = prompt != null,
                modifier = Modifier.testTag("ai_agents.toggle.${connector.name.lowercase()}"),
            ) {
                Text(
                    stringResource(
                        if (expanded) R.string.ai_agents_hide_prompt else R.string.ai_agents_show_prompt,
                    ),
                )
            }
            TextButton(
                onClick = { prompt?.let { onCopy(name, it) } },
                enabled = prompt != null,
                modifier = Modifier.testTag("ai_agents.copy.${connector.name.lowercase()}"),
            ) { Text(stringResource(R.string.ai_agents_copy_prompt)) }
        }
        if (expanded && prompt != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_agents.prompt.${connector.name.lowercase()}"),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                SelectionContainer {
                    Text(
                        text = prompt,
                        modifier = Modifier.padding(WhiteNoiseSpacing.FormField),
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

private fun AgentConnector.resources(): AgentConnectorResources = when (this) {
    AgentConnector.Hermes -> AgentConnectorResources(
        R.string.ai_agents_hermes_name,
        R.string.ai_agents_hermes_subtitle,
        R.string.ai_agents_hermes_prompt,
    )
    AgentConnector.OpenClaw -> AgentConnectorResources(
        R.string.ai_agents_openclaw_name,
        R.string.ai_agents_openclaw_subtitle,
        R.string.ai_agents_openclaw_prompt,
    )
    AgentConnector.OpenCode -> AgentConnectorResources(
        R.string.ai_agents_opencode_name,
        R.string.ai_agents_opencode_subtitle,
        R.string.ai_agents_opencode_prompt,
    )
    AgentConnector.Codex -> AgentConnectorResources(
        R.string.ai_agents_codex_name,
        R.string.ai_agents_codex_subtitle,
        R.string.ai_agents_codex_prompt,
    )
}

private fun openAgentConnectorDocs(context: Context): Boolean = runCatching {
    context.startActivity(Intent(Intent.ACTION_VIEW, AgentConnectorDocs.toUri()))
}.isSuccess
