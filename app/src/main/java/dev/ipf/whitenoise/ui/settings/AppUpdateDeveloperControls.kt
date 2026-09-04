package dev.ipf.whitenoise.ui.settings

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
            title = "Update check state",
            choices = AppUpdateCheckScenario.entries,
            selected = state.checkScenario,
            label = { it.developerLabel },
            onSelect = controller::previewCheck,
            onDismiss = { choice = null },
        )
    }
    if (choice == "flow") {
        ScenarioChoiceDialog(
            title = "Self-update outcome",
            choices = AppSelfUpdateScenario.entries,
            selected = state.selfUpdateScenario,
            label = { it.developerLabel },
            onSelect = controller::selectSelfUpdateScenario,
            onDismiss = { choice = null },
        )
    }
    SettingsDivider()
    SettingsSwitch(
        title = "Store-managed update distribution",
        subtitle = "Hides every in-app update entry and banner",
        checked = state.distribution == AppUpdateDistribution.StoreManaged,
        onCheckedChange = {
            controller.selectDistribution(
                if (it) AppUpdateDistribution.StoreManaged else AppUpdateDistribution.SelfManaged,
            )
        },
    )
    SettingsDivider()
    SettingsLink(
        title = "Update check state",
        subtitle = state.checkScenario.developerLabel,
        onClick = { choice = "check" },
        enabled = state.distribution == AppUpdateDistribution.SelfManaged,
    )
    SettingsDivider()
    SettingsLink(
        title = "Self-update outcome",
        subtitle = state.selfUpdateScenario.developerLabel,
        onClick = { choice = "flow" },
        enabled = state.distribution == AppUpdateDistribution.SelfManaged,
    )
}
