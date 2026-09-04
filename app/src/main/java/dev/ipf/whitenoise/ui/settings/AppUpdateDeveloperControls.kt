package dev.ipf.whitenoise.ui.settings

import dev.ipf.whitenoise.R

import androidx.compose.ui.res.stringResource

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.ipf.whitenoise.model.AppSelfUpdateScenario
import dev.ipf.whitenoise.model.AppUpdateCheckScenario
import dev.ipf.whitenoise.model.AppUpdateDistribution
import dev.ipf.whitenoise.state.AppUpdateController

@Composable
fun AppUpdateDeveloperControls(controller: AppUpdateController) {
    val state = controller.state
    var choice by remember { mutableStateOf<String?>(null) }
    if (choice == "check") {
        ScenarioChoiceDialog(
            title = stringResource(R.string.developer_update_check_state),
            choices = AppUpdateCheckScenario.entries,
            selected = state.checkScenario,
            label = { it.developerLabel },
            onSelect = controller::previewCheck,
            onDismiss = { choice = null },
        )
    }
    if (choice == "flow") {
        ScenarioChoiceDialog(
            title = stringResource(R.string.developer_self_update_outcome),
            choices = AppSelfUpdateScenario.entries,
            selected = state.selfUpdateScenario,
            label = { it.developerLabel },
            onSelect = controller::selectSelfUpdateScenario,
            onDismiss = { choice = null },
        )
    }
    SettingsDivider()
    SettingsSwitch(
        title = stringResource(R.string.developer_store_managed_update_distribution),
        subtitle = stringResource(R.string.developer_hides_every_in_app_update_entry_and_banner),
        checked = state.distribution == AppUpdateDistribution.StoreManaged,
        onCheckedChange = {
            controller.selectDistribution(
                if (it) AppUpdateDistribution.StoreManaged else AppUpdateDistribution.SelfManaged,
            )
        },
    )
    SettingsDivider()
    SettingsLink(
        title = stringResource(R.string.developer_update_check_state),
        subtitle = state.checkScenario.developerLabel,
        onClick = { choice = "check" },
        enabled = state.distribution == AppUpdateDistribution.SelfManaged,
    )
    SettingsDivider()
    SettingsLink(
        title = stringResource(R.string.developer_self_update_outcome),
        subtitle = state.selfUpdateScenario.developerLabel,
        onClick = { choice = "flow" },
        enabled = state.distribution == AppUpdateDistribution.SelfManaged,
    )
}
