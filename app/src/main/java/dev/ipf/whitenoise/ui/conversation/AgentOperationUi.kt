package dev.ipf.whitenoise.ui.conversation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.AgentOperation
import dev.ipf.whitenoise.model.AgentOperationPhase
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AgentOperationCard(
    messageId: String,
    operation: AgentOperation,
    onLongPress: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable(messageId) { mutableStateOf(false) }
    val status = operation.statusText()
    val toggle = if (expanded) {
        stringResource(R.string.agent_operation_hide_details)
    } else {
        stringResource(R.string.agent_operation_show_details)
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 440.dp)
            .testTag("conversation.agent_operation.$messageId")
            .combinedClickable(
                enabled = operation.canExpand || onLongPress != null,
                role = Role.Button,
                onClickLabel = toggle.takeIf { operation.canExpand },
                onLongClickLabel = stringResource(R.string.show_message_actions).takeIf { onLongPress != null },
                onClick = { if (operation.canExpand) expanded = !expanded },
                onLongClick = onLongPress,
            )
            .semantics { stateDescription = status },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
        ) {
            Text(
                stringResource(R.string.agent_operation_label),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    operation.name,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    status,
                    color = operation.statusColor(),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.testTag("conversation.agent_operation.status.$messageId"),
                )
            }
            Text(
                operation.summary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (operation.isInProgress) {
                val progress = operation.progress
                if (progress == null) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("conversation.agent_operation.progress.$messageId"),
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("conversation.agent_operation.progress.$messageId"),
                    )
                }
            }
            operation.totalSteps?.takeIf { it > 0 }?.let { total ->
                Text(
                    pluralStringResource(
                        R.plurals.agent_operation_progress,
                        total,
                        operation.boundedCompletedSteps,
                        total,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            if (operation.canExpand) {
                Text(
                    toggle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            if (expanded) {
                operation.arguments?.takeIf(String::isNotBlank)?.let {
                    AgentOperationDetail(stringResource(R.string.agent_operation_arguments), it)
                }
                operation.result?.takeIf(String::isNotBlank)?.let {
                    AgentOperationDetail(stringResource(R.string.agent_operation_result), it)
                }
                operation.statusDetail?.takeIf(String::isNotBlank)?.let {
                    AgentOperationDetail(stringResource(R.string.agent_operation_status_detail), it)
                }
                operation.durationMillis?.let {
                    AgentOperationDetail(
                        stringResource(R.string.agent_operation_duration),
                        stringResource(R.string.agent_operation_duration_ms, it),
                    )
                }
            }
        }
    }
}

@Composable
private fun AgentOperationDetail(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelSmall,
        )
        SelectionContainer {
            Text(value, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun AgentOperation.statusText(): String = stringResource(
    when (phase) {
        AgentOperationPhase.Queued -> R.string.agent_operation_status_queued
        AgentOperationPhase.Running -> R.string.agent_operation_status_running
        AgentOperationPhase.Succeeded -> R.string.agent_operation_status_succeeded
        AgentOperationPhase.Failed -> R.string.agent_operation_status_failed
        AgentOperationPhase.Cancelled -> R.string.agent_operation_status_cancelled
        AgentOperationPhase.Unavailable -> R.string.agent_operation_status_unavailable
    },
)

@Composable
private fun AgentOperation.statusColor(): Color = when (phase) {
    AgentOperationPhase.Failed -> MaterialTheme.colorScheme.error
    AgentOperationPhase.Succeeded -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
