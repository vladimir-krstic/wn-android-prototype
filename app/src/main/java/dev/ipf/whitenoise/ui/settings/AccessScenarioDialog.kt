package dev.ipf.whitenoise.ui.settings

import dev.ipf.whitenoise.R

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import dev.ipf.whitenoise.model.AccessScenario
import dev.ipf.whitenoise.model.ProfileExitScenario
import dev.ipf.whitenoise.ui.components.WhiteNoiseAlertDialog
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing

internal val AccessScenario.developerLabel: String
    get() = when (this) {
        AccessScenario.Success -> "Success"
        AccessScenario.Offline -> "No connection"
        AccessScenario.SignInFailure -> "Sign-in / creation failure"
        AccessScenario.SetupRetry -> "Setup can resume"
        AccessScenario.PublicationRetry -> "Connection publication can resume"
        AccessScenario.RecoveryConsent -> "Recovery consent, then success"
        AccessScenario.RecoveryPartial -> "Recovery consent, then partial result"
        AccessScenario.UnexpectedSetup -> "Unexpected setup state"
        AccessScenario.RecoveryUnexpected -> "Unexpected state during recovery"
        AccessScenario.AmberUnavailable -> "Amber unavailable"
        AccessScenario.AmberIdentityCancelled -> "Amber identity request cancelled"
        AccessScenario.AmberIdentityRejected -> "Amber identity request rejected"
        AccessScenario.AmberProofCancelled -> "Amber proof cancelled"
        AccessScenario.AmberProofRejected -> "Amber proof rejected"
        AccessScenario.AmberMismatch -> "Amber identity mismatch"
    }

@Composable
internal fun AccessScenarioDialog(current: AccessScenario, onSelect: (AccessScenario) -> Unit, onDismiss: () -> Unit) {
    var selected by remember(current) { mutableStateOf(current) }
    WhiteNoiseAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.developer_access_scenarios)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
                Text(stringResource(R.string.developer_the_next_access_attempt_uses_this_one_shot_result_retr))
                LazyColumn(Modifier.weight(1f, fill = false)) {
                    items(AccessScenario.entries, key = { it.name }) { scenario ->
                        Row(
                            modifier = Modifier.fillMaxWidth().selectable(
                                selected = selected == scenario, role = Role.RadioButton,
                                onClick = { selected = scenario },
                            ).padding(vertical = WhiteNoiseSpacing.Related),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = selected == scenario, onClick = null)
                            Text(scenario.developerLabel, Modifier.padding(start = WhiteNoiseSpacing.Related))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSelect(selected); onDismiss() }) { Text(stringResource(R.string.developer_use_scenario)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

internal val ProfileExitScenario.developerLabel: String
    get() = when (this) {
        ProfileExitScenario.Success -> "All steps complete"
        ProfileExitScenario.GroupLeaveFailure -> "Group departure incomplete"
        ProfileExitScenario.RelayCleanupFailure -> "Relay cleanup incomplete"
        ProfileExitScenario.LocalCleanupFailure -> "Local cleanup incomplete"
        ProfileExitScenario.AllCleanupFailure -> "All cleanup incomplete"
    }

@Composable
internal fun ProfileExitScenarioDialog(current: ProfileExitScenario, onSelect: (ProfileExitScenario) -> Unit, onDismiss: () -> Unit) {
    var selected by remember(current) { mutableStateOf(current) }
    WhiteNoiseAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.developer_sign_out_scenarios)) },
        text = {
            LazyColumn {
                items(ProfileExitScenario.entries, key = { it.name }) { scenario ->
                    Row(
                        Modifier.fillMaxWidth().selectable(selected == scenario, role = Role.RadioButton,
                            onClick = { selected = scenario }).padding(vertical = WhiteNoiseSpacing.Related),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected == scenario, onClick = null)
                        Text(scenario.developerLabel, Modifier.padding(start = WhiteNoiseSpacing.Related))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSelect(selected); onDismiss() }) { Text(stringResource(R.string.developer_use_scenario)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
internal fun <T> ScenarioChoiceDialog(title: String, choices: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit, onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss, title = { androidx.compose.material3.Text(title) },
        text = { androidx.compose.foundation.lazy.LazyColumn {
            items(choices.size) { index ->
                val choice = choices[index]
                androidx.compose.material3.TextButton(onClick = { onSelect(choice); onDismiss() }) {
                    androidx.compose.material3.Text((if (choice == selected) "✓ " else "") + label(choice))
                }
            }
        } },
        confirmButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { androidx.compose.material3.Text(stringResource(R.string.close)) } },
    )
}
