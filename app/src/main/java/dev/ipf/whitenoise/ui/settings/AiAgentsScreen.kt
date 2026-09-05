package dev.ipf.whitenoise.ui.settings

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.AgentConnector
import dev.ipf.whitenoise.model.AgentSetupPolicy
import dev.ipf.whitenoise.model.Profile
import dev.ipf.whitenoise.ui.components.WhiteNoiseButton
import dev.ipf.whitenoise.ui.components.WhiteNoiseCallout
import dev.ipf.whitenoise.ui.components.WhiteNoiseLazyColumn
import dev.ipf.whitenoise.ui.components.WhiteNoiseListItemDefaults
import dev.ipf.whitenoise.ui.components.WhiteNoiseModalBottomSheet
import dev.ipf.whitenoise.ui.components.WhiteNoiseSheetHeader
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
    var selectedConnector by rememberSaveable(profile.id, publicKey) { mutableStateOf<AgentConnector?>(null) }
    val copyValue = copyOverride ?: remember(context) {
        { label: String, value: String -> copyToClipboard(context, label, value) }
    }
    val openDocs = openDocsOverride ?: remember(context) {
        { openAgentConnectorDocs(context) }
    }

    SettingsScaffold(title = stringResource(R.string.ai_agents_title), onBack = onBack) {
        SettingsList {
            item {
                SettingsCallout(
                    title = stringResource(R.string.ai_agents_about_section),
                    text = stringResource(R.string.ai_agents_about_body),
                )
            }
            if (publicKey == null) item {
                SettingsCallout(stringResource(R.string.ai_agents_public_key_unavailable), isError = true)
            }
            item { SettingsSection(stringResource(R.string.ai_agents_connectors_section)) }
            item {
                SettingsGroup(modifier = Modifier.testTag("ai_agents.connectors")) {
                    AgentSetupPolicy.connectors.forEach { connector ->
                        row {
                            SettingsLink(
                                title = stringResource(connector.resources().name),
                                subtitle = stringResource(connector.resources().subtitle),
                                enabled = publicKey != null,
                                onClick = { selectedConnector = connector },
                            )
                        }
                    }
                }
            }
            item { SettingsExplainer(stringResource(R.string.ai_agents_connectors_body)) }
            item { SettingsSection(stringResource(R.string.ai_agents_manual_section)) }
            item {
                SettingsGroup(modifier = Modifier.testTag("ai_agents.manual")) {
                    item {
                        ListItem(
                            onClick = {
                                publicKey?.let {
                                    copyValue("Public key", it)
                                    feedback = R.string.ai_agents_public_key_copied
                                }
                            },
                            enabled = publicKey != null,
                            modifier = Modifier.fillMaxWidth().testTag("ai_agents.copy_public_key"),
                            shapes = WhiteNoiseListItemDefaults.shapes(),
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                            ),
                            supportingContent = publicKey?.let {
                                { Text(profile.shortPublicKey, Modifier.testTag("ai_agents.public_key")) }
                            },
                            trailingContent = {
                                Icon(painterResource(R.drawable.ic_content_copy), contentDescription = null)
                            },
                        ) { Text(stringResource(R.string.ai_agents_copy_public_key)) }
                    }
                    row {
                        SettingsLink(
                            title = stringResource(R.string.ai_agents_docs_title),
                            subtitle = stringResource(R.string.ai_agents_docs_subtitle),
                            onClick = {
                                feedback = if (openDocs()) R.string.ai_agents_docs_opened else R.string.ai_agents_docs_failed
                            },
                        )
                    }
                }
            }
            item { SettingsExplainer(stringResource(R.string.ai_agents_manual_body)) }
            feedback?.let { message ->
                item {
                    SettingsCallout(
                        text = stringResource(message),
                        modifier = Modifier.testTag("ai_agents.feedback")
                            .semantics { liveRegion = LiveRegionMode.Polite },
                        isError = message == R.string.ai_agents_docs_failed,
                    )
                }
            }
        }
    }
    selectedConnector?.let { connector ->
        if (publicKey != null) AgentSetupSheet(
            connector = connector,
            publicKey = publicKey,
            onDismiss = { selectedConnector = null },
            onCopy = copyValue,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgentSetupSheet(
    connector: AgentConnector,
    publicKey: String,
    onDismiss: () -> Unit,
    onCopy: (label: String, value: String) -> Unit,
) {
    val resources = connector.resources()
    val name = stringResource(resources.name)
    val prompt = stringResource(resources.prompt, publicKey)
    val id = connector.name.lowercase()
    var copied by rememberSaveable(connector, publicKey) { mutableStateOf(false) }
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    WhiteNoiseModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().heightIn(max = LocalWindowInfo.current.containerDpSize.height * 0.88f)) {
            WhiteNoiseSheetHeader(stringResource(R.string.ai_agents_setup_title, name), onClose = onDismiss)
            WhiteNoiseLazyColumn(
                modifier = Modifier.weight(1f, fill = false).fillMaxWidth().testTag("ai_agents.setup_content"),
                contentPadding = PaddingValues(
                    horizontal = WhiteNoiseSpacing.CompactScreenMargin,
                    vertical = WhiteNoiseSpacing.Related,
                ),
                verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.FormField),
            ) {
                item {
                    WhiteNoiseCallout(text = stringResource(R.string.ai_agents_setup_instruction, name))
                }
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth().testTag("ai_agents.prompt.$id"),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    ) {
                        SelectionContainer {
                            Text(
                                text = prompt,
                                modifier = Modifier.padding(WhiteNoiseSpacing.FormField),
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                item {
                    Text(
                        stringResource(R.string.ai_agents_manual_body),
                        Modifier.padding(horizontal = WhiteNoiseSpacing.FormField),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(
                Modifier.fillMaxWidth().padding(WhiteNoiseSpacing.CompactScreenMargin),
                verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
            ) {
                if (copied) Text(
                    stringResource(R.string.ai_agents_prompt_copied),
                    Modifier.testTag("ai_agents.copy_feedback").semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                WhiteNoiseButton(
                    onClick = { onCopy("$name setup prompt", prompt); copied = true },
                    modifier = Modifier.fillMaxWidth().testTag("ai_agents.copy.$id"),
                ) {
                    Icon(painterResource(R.drawable.ic_content_copy), contentDescription = null, modifier = Modifier.size(24.dp))
                    Text(stringResource(R.string.ai_agents_copy_prompt), Modifier.padding(start = WhiteNoiseSpacing.Related))
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
