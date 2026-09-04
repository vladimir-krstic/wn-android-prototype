package dev.ipf.whitenoise.ui.conversation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.ui.components.WhiteNoiseAlertDialog as AlertDialog
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.state.GroupLifecycleController
import dev.ipf.whitenoise.ui.settings.SettingsGroup
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import kotlinx.coroutines.delay

internal val LocalGroupLifecycle = staticCompositionLocalOf<GroupLifecycleController?> { null }

@Composable
internal fun GroupLifecycleHost(controller: GroupLifecycleController) {
    SideEffect { controller.reconcile() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    controller.work.values.filter { it.running }.forEach { w -> key(w.owner) {
        LaunchedEffect(w.id, w.stage, lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            delay(650); controller.advance(w.owner, w.id, w.stage)
        } }
    } }
}

@Composable
internal fun GroupLifecyclePanel(profile: Profile, chat: Chat, onLeft: () -> Unit) {
    val controller = LocalGroupLifecycle.current ?: return
    if (!chat.isGroup) return
    val owner = GroupOwner(profile.id, chat.id)
    LaunchedEffect(owner, controller.stateScenario) { controller.open(owner) }
    val work = controller.work[owner]
    val busy = LocalGroupWork.current?.locked(owner) == true
    val admin = chat.hasAuthoritativeGroupAdmin(profile.id)
    val capability = chat.disbandCapability
    var confirm by rememberSaveable(profile.id, chat.id) { mutableStateOf<String?>(null) }
    var target by rememberSaveable(profile.id, chat.id) { mutableStateOf<String?>(null) }
    var pick by rememberSaveable(profile.id, chat.id) { mutableStateOf(false) }
    var thenLeave by rememberSaveable(profile.id, chat.id) { mutableStateOf(false) }
    var rejected by rememberSaveable(profile.id, chat.id) { mutableStateOf(false) }
    LaunchedEffect(work?.id, work?.stage) {
        if (work?.stage == GroupLifecycleStage.Complete && work.thenLeave && chat.membership == ChatMembership.Left) {
            controller.dismiss(owner, work.id); onLeft()
        }
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin),
        verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
        if (chat.groupLifecycle != GroupLifecycle.Active) {
            Text(stringResource(lifecycleNotice(chat.groupLifecycle)), Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        if (work != null && (work.stage != GroupLifecycleStage.Complete || work.failure != null)) {
            Column(Modifier.testTag("group.lifecycle.status").semantics { liveRegion = LiveRegionMode.Polite }) {
                Text(stringResource(when {
                    work.failure == GroupLifecycleFailure.SourceChanged -> R.string.group_action_changed
                    work.failure != null && work.steppedDown -> R.string.group_leave_partial
                    work.failure != null && work.granted -> R.string.group_transfer_partial
                    work.failure != null -> R.string.group_action_failed
                    work.stage == GroupLifecycleStage.Grant -> R.string.group_transferring
                    work.stage == GroupLifecycleStage.StepDown -> R.string.group_stepping_down
                    work.stage == GroupLifecycleStage.Leave -> R.string.group_leaving
                    work.stage == GroupLifecycleStage.Converge || work.stage == GroupLifecycleStage.AcceptDisband -> R.string.group_disband_pending
                    work.stage == GroupLifecycleStage.Recover -> R.string.group_repairing
                    else -> R.string.group_updating
                }))
                if (work.running) LinearProgressIndicator(Modifier.fillMaxWidth())
                else {
                    if (work.failure == GroupLifecycleFailure.SourceChanged && work.granted) Text(stringResource(R.string.group_transfer_partial))
                    if (work.failure == GroupLifecycleFailure.SourceChanged && work.steppedDown) Text(stringResource(R.string.group_leave_partial))
                    FlowRow {
                        TextButton(onClick = { rejected = !controller.retry(owner, work.id) }, enabled = controller.canRetry(owner, work.id)) { Text(stringResource(R.string.lifecycle_retry)) }
                        TextButton(onClick = { controller.dismiss(owner, work.id) }) { Text(stringResource(R.string.lifecycle_dismiss)) }
                    }
                }
            }
        } else if (work?.stage == GroupLifecycleStage.Complete && !capability.requestFailed) {
            Text(stringResource(when (work.action) {
                GroupLifecycleAction.Transfer -> R.string.group_transfer_complete
                GroupLifecycleAction.StepDown -> R.string.group_step_down_complete
                GroupLifecycleAction.EnableDisband -> R.string.group_disband_enabled
                else -> R.string.group_action_complete
            }), Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        if (rejected) Text(stringResource(R.string.group_action_changed), color = MaterialTheme.colorScheme.error)
        if (capability.requestFailed) {
            Text(stringResource(R.string.group_disband_failed), color = MaterialTheme.colorScheme.error)
            TextButton(onClick = { rejected = !controller.begin(owner, GroupLifecycleAction.Acknowledge) }, enabled = !busy && chat.hasVerifiedSelf(profile.id)) {
                Text(stringResource(R.string.lifecycle_dismiss))
            }
        }
        if (admin) capability.blockers.forEach { blocker -> Text(stringResource(when (blocker) {
            DisbandBlocker.UnsupportedMembers -> R.string.group_disband_unsupported
            DisbandBlocker.PendingInvitations -> R.string.group_disband_invitations
            DisbandBlocker.UpdateInProgress -> R.string.group_disband_update
        })) }
        SettingsGroup {
            if (admin) {
                LifecycleRow(stringResource(R.string.group_transfer_admin), !busy && chat.members.any { it.personId != profile.id && it.role == GroupRole.Member }) {
                    thenLeave = false; pick = true
                }
                LifecycleRow(stringResource(R.string.group_step_down), !busy && chat.members.any { it.personId != profile.id }) {
                    if (chat.isSoleAdmin(profile.id)) { thenLeave = false; pick = true } else confirm = GroupLifecycleAction.StepDown.name
                }
                if (capability.canEnable && !capability.enabled) LifecycleRow(stringResource(R.string.group_enable_disband), !busy && capability.blockers.isEmpty() && !capability.requestFailed) {
                    confirm = GroupLifecycleAction.EnableDisband.name
                }
                if (capability.enabled && capability.canDisband) LifecycleRow(stringResource(R.string.group_disband_action), !busy && capability.blockers.isEmpty() && !capability.requestFailed, true) {
                    confirm = GroupLifecycleAction.Disband.name
                }
            }
            if (chat.membership == ChatMembership.Active && chat.groupLifecycle == GroupLifecycle.Active) LifecycleRow(
                stringResource(if (chat.isSoleMember(profile.id)) R.string.group_delete_solo else R.string.leave_group),
                !busy && chat.hasVerifiedSelf(profile.id), true) {
                if (chat.isSoleAdmin(profile.id) && !chat.isSoleMember(profile.id)) { thenLeave = true; pick = true }
                else confirm = (if (chat.isSoleMember(profile.id)) GroupLifecycleAction.Delete else GroupLifecycleAction.Leave).name
            }
            if (chat.hasEndedMembership || chat.groupLifecycle == GroupLifecycle.Disbanded) LifecycleRow(stringResource(R.string.group_delete_local), !busy, true) {
                confirm = GroupLifecycleAction.Delete.name
            }
            if (chat.groupLifecycle == GroupLifecycle.Unrecoverable) LifecycleRow(stringResource(R.string.group_repair), !busy && chat.hasVerifiedSelf(profile.id)) {
                rejected = !controller.begin(owner, GroupLifecycleAction.Recover)
            }
        }
    }
    if (pick) AlertDialog(onDismissRequest = { pick = false }, title = { Text(stringResource(if (thenLeave) R.string.group_transfer_leave else R.string.group_transfer_admin)) },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.group_select_admin))
            chat.members.filter { it.personId != profile.id && it.personId != "white-noise-support" && it.role == GroupRole.Member }.forEach { member ->
                val name = profile.people.firstOrNull { it.id == member.personId }?.displayName ?: stringResource(R.string.group_member_fallback)
                TextButton(onClick = { target = member.personId; pick = false; confirm = GroupLifecycleAction.Transfer.name }, enabled = admin && !busy) { Text(name) }
            }
        } }, confirmButton = {}, dismissButton = { TextButton(onClick = { pick = false }) { Text(stringResource(R.string.cancel)) } })
    confirm?.let { raw ->
        val action = GroupLifecycleAction.valueOf(raw)
        val name = profile.people.firstOrNull { it.id == target }?.displayName ?: stringResource(R.string.group_member_fallback)
        val title = stringResource(when (action) {
            GroupLifecycleAction.Transfer -> if (thenLeave) R.string.group_transfer_leave else R.string.group_transfer_admin
            GroupLifecycleAction.StepDown -> R.string.group_step_down
            GroupLifecycleAction.Leave -> R.string.leave_group
            GroupLifecycleAction.Delete -> if (chat.isSoleMember(profile.id)) R.string.group_delete_solo else R.string.group_delete_local
            GroupLifecycleAction.EnableDisband -> R.string.group_enable_disband
            else -> R.string.group_disband_action
        })
        AlertDialog(onDismissRequest = { confirm = null }, title = { Text(title) }, text = { Text(stringResource(when (action) {
            GroupLifecycleAction.Transfer -> if (thenLeave) R.string.group_transfer_leave_detail else R.string.group_transfer_detail
            GroupLifecycleAction.StepDown -> R.string.group_step_down_detail
            GroupLifecycleAction.Leave -> R.string.leave_history_detail
            GroupLifecycleAction.Delete -> if (chat.isSoleMember(profile.id)) R.string.group_delete_solo_detail else R.string.group_delete_local_detail
            GroupLifecycleAction.EnableDisband -> R.string.group_enable_disband_detail
            else -> R.string.group_disband_detail
        }, name)) }, confirmButton = { TextButton(onClick = {
            rejected = !controller.begin(owner, action, target, thenLeave && action == GroupLifecycleAction.Transfer); confirm = null
        }, enabled = !busy) { Text(title, color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text(stringResource(R.string.cancel)) } })
    }
}

@Composable
private fun LifecycleRow(label: String, enabled: Boolean, destructive: Boolean = false, onClick: () -> Unit) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    ) { Text(label, color = if (!enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        else if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface) }
}

internal fun lifecycleNotice(state: GroupLifecycle): Int = when (state) {
    GroupLifecycle.Unrecoverable -> R.string.group_frozen_notice
    GroupLifecycle.Disbanding -> R.string.group_disband_pending
    GroupLifecycle.Disbanded -> R.string.group_ended_notice
    GroupLifecycle.Active -> R.string.group_action_complete
}
