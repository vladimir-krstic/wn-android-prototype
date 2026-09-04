package dev.ipf.whitenoise.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.ipf.whitenoise.model.AppSelfUpdateScenario
import dev.ipf.whitenoise.model.AppUpdateCheckScenario
import dev.ipf.whitenoise.model.AppUpdateDistribution
import dev.ipf.whitenoise.model.AppUpdateState
import dev.ipf.whitenoise.model.AppUpdates

class AppUpdateController(installedVersion: String) {
    var state by mutableStateOf(AppUpdates.initial(installedVersion))
        private set

    fun selectDistribution(distribution: AppUpdateDistribution) {
        state = AppUpdates.selectDistribution(state, distribution)
    }

    fun previewCheck(scenario: AppUpdateCheckScenario) {
        state = AppUpdates.previewCheck(state, scenario)
    }

    fun selectSelfUpdateScenario(scenario: AppSelfUpdateScenario) {
        state = AppUpdates.selectSelfUpdateScenario(state, scenario)
    }

    fun beginCheck() { state = AppUpdates.beginCheck(state) }
    fun completeCheck(generation: Long) { state = AppUpdates.completeCheck(state, generation) }
    fun dismissBanner() { state = AppUpdates.dismissBanner(state) }
    fun beginSelfUpdate() { state = AppUpdates.beginSelfUpdate(state) }
    fun confirmDownload() { state = AppUpdates.confirmDownload(state) }
    fun advanceSelfUpdate(generation: Long) { state = AppUpdates.advanceSelfUpdate(state, generation) }
    fun reviewInstallPermission() { state = AppUpdates.reviewInstallPermission(state) }
    fun requestInstall() { state = AppUpdates.requestInstall(state) }
    fun retry() { state = AppUpdates.retry(state) }
    fun cancel() { state = AppUpdates.cancel(state) }
}
