package dev.ipf.whitenoise.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.state.*
import dev.ipf.whitenoise.ui.components.*
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal val LocalNotificationControls = staticCompositionLocalOf<NotificationController?> { null }

@Composable
internal fun NotificationControlsHost(controller: NotificationController, route: String?, content: @Composable () -> Unit) {
    val permission = rememberNotificationPermissionAccess()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val work = controller.work
    val preview = controller.preview
    SideEffect {
        controller.observeRoute(route)
        controller.observePermission(permission.status == NotificationPermissionStatus.Allowed)
        controller.reconcile()
    }
    LaunchedEffect(work?.id,work?.attempt,work?.failure,lifecycle) {
        if (work?.running == true) lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            delay(450); controller.advance(work.id,work.attempt)
        }
    }
    LaunchedEffect(preview?.id,preview?.phase,lifecycle) {
        if (preview?.phase in setOf(VibrationPreviewPhase.Preparing,VibrationPreviewPhase.Playing)) lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            delay(if (preview?.phase == VibrationPreviewPhase.Playing) 600 else 300)
            preview?.let { controller.advancePreview(it.id,it.phase) }
        }
    }
    CompositionLocalProvider(LocalNotificationControls provides controller) { content() }
    work?.let { NotificationWorkDialog(controller,it) }
}

@Composable
internal fun NotificationWorkDialog(controller: NotificationController, work: NotificationWork) {
    WhiteNoiseAlertDialog(onDismissRequest = { controller.cancel(work.id) }, title = { Text(stringResource(R.string.notification_controls_title)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
            Text(stringResource(work.failure?.let(::notificationFailureResource) ?: R.string.notification_saving),
                color = if (work.running) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }.testTag("notification.work.status"))
            if (work.running) LinearProgressIndicator(Modifier.fillMaxWidth())
        } },
        confirmButton = { if (!work.running && work.failure != NotificationFailure.ExpiredTime)
            TextButton(onClick = { controller.retry(work.id) }) { Text(stringResource(R.string.lifecycle_retry)) } },
        dismissButton = { TextButton(onClick = { controller.cancel(work.id) }) { Text(stringResource(R.string.cancel)) } },
        modifier = Modifier.testTag("notification.work"))
}

private fun notificationFailureResource(failure: NotificationFailure) = when (failure) {
    NotificationFailure.Save -> R.string.notification_save_failed
    NotificationFailure.Permission -> R.string.notification_permission_needed
    NotificationFailure.PushUnavailable -> R.string.notification_push_unavailable
    NotificationFailure.ServiceRejected -> R.string.notification_connection_rejected
    NotificationFailure.ServiceStopped -> R.string.notification_connection_stopped
    NotificationFailure.Changed -> R.string.notification_settings_changed
    NotificationFailure.ExpiredTime -> R.string.notification_future_time
}

internal fun notificationCategoryResource(category: NotificationCategory) = when (category) {
    NotificationCategory.DirectMessages -> R.string.notification_category_direct
    NotificationCategory.GroupMessages -> R.string.notification_category_groups
    NotificationCategory.Mentions -> R.string.notification_category_mentions
    NotificationCategory.Reactions -> R.string.notification_category_reactions
    NotificationCategory.Invitations -> R.string.notification_category_invites
    NotificationCategory.GroupMembership -> R.string.notification_category_membership
    NotificationCategory.AgentActivity -> R.string.notification_category_agents
    NotificationCategory.AppUpdates -> R.string.notification_category_updates
}
internal fun vibrationResource(choice: VibrationChoice) = when (choice) {
    VibrationChoice.SystemDefault -> R.string.notification_vibration_system
    VibrationChoice.Short -> R.string.notification_vibration_short
    VibrationChoice.Double -> R.string.notification_vibration_double
    VibrationChoice.Long -> R.string.notification_vibration_long
}

@Composable
internal fun NotificationSettingsFeedback(result: NotificationSettingsOpen?, onDismiss: () -> Unit) {
    if (result == NotificationSettingsOpen.AppFallback || result == NotificationSettingsOpen.Unavailable) {
        WhiteNoiseAlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.notification_categories)) },
            text = { Text(stringResource(if (result == NotificationSettingsOpen.AppFallback) R.string.notification_category_fallback else R.string.notification_category_unavailable)) },
            confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) } })
    }
}

@Composable
internal fun ConversationNotificationScreen(profile: Profile, chat: Chat, controller: NotificationController, onBack: () -> Unit,
    onOpenCategory: ((NotificationCategory, Boolean) -> NotificationSettingsOpen)? = null) {
    val context = LocalContext.current
    var muteOpen by rememberSaveable(profile.id,chat.id) { mutableStateOf(false) }
    var modeOpen by rememberSaveable(profile.id,chat.id) { mutableStateOf(false) }
    var vibrationOpen by rememberSaveable(profile.id,chat.id) { mutableStateOf(false) }
    var settingsResult by rememberSaveable(profile.id,chat.id) { mutableStateOf<NotificationSettingsOpen?>(null) }
    val effective = NotificationControls.effectiveVibration(chat.vibration,controller.environment.vibrationOverride)
    val vibrationLabel = when {
        !effective.enabled -> stringResource(R.string.notification_vibration_off)
        effective.pattern == null -> stringResource(R.string.notification_vibration_custom)
        effective.overridden -> stringResource(R.string.notification_vibration_override,stringResource(vibrationResource(effective.pattern)))
        else -> stringResource(vibrationResource(chat.vibration))
    }
    SettingsScaffold(stringResource(R.string.notification_sounds_title),onBack) {
        SettingsList {
            item { SettingsSection(stringResource(R.string.notification_controls_title)) }
            item { SettingsGroup {
                SettingsSwitch(stringResource(R.string.mute),chat.muteDuration != null, {
                    if (it) muteOpen = true else controller.request(NotificationChange.Mute(null),chat.id,profile.id)
                }, subtitle = chat.mutedUntilMillis?.let { stringResource(R.string.notification_muted_until,formatMuteTime(it)) }
                    ?: stringResource(if (chat.muteDuration != null) R.string.notification_notify_nothing else R.string.notification_mute_detail))
                SettingsDivider()
                SettingsLink(stringResource(R.string.notification_notify_for),onClick = { modeOpen = true },
                    value = stringResource(if (chat.notifyFor == NotifyFor.AllMessages) R.string.notification_notify_all else R.string.notification_notify_mentions))
                SettingsDivider()
                SettingsLink(stringResource(R.string.notification_vibration),onClick = { vibrationOpen = true },value = vibrationLabel)
            } }
            item { SettingsExplainer(stringResource(R.string.notification_notify_restore)) }
            item { SettingsSection(stringResource(R.string.notification_categories)) }
            item { SettingsExplainer(stringResource(R.string.notification_chat_categories_detail)) }
            NotificationCategory.forChat(chat).forEach { category -> item(key = category.name) {
                val custom = NotificationControls.usesCustom(chat,category)
                SettingsGroup(modifier = Modifier.testTag("notification.category.${category.name}")) {
                    SettingsLink(stringResource(notificationCategoryResource(category)),
                        subtitle = stringResource(if (custom) R.string.notification_scope_custom else R.string.notification_scope_global),
                        onClick = { settingsResult = onOpenCategory?.invoke(category,custom) ?: openNotificationCategory(context,category,custom) })
                    if (category.overridable) {
                        SettingsDivider()
                        SettingsSwitch(stringResource(R.string.notification_scope_custom),custom,{
                            controller.request(NotificationChange.Scope(category,it),chat.id,profile.id)
                        },subtitle = stringResource(R.string.notification_scope_switch_detail))
                    }
                }
            } }
        }
    }
    if (muteOpen) MuteDurationDialog(onDismiss = { muteOpen = false },selectedDuration = chat.muteDuration,
        onSelect = { controller.request(NotificationChange.Mute(it),chat.id,profile.id); muteOpen = false },
        onCustomSelect = { controller.request(NotificationChange.Mute(MuteDuration.Custom,it),chat.id,profile.id); muteOpen = false },
        nowMillis = { controller.nowMillis })
    if (modeOpen) WhiteNoiseAlertDialog(onDismissRequest = { modeOpen = false },title = { Text(stringResource(R.string.notification_notify_for)) },
        text = { Column(Modifier.selectableGroup()) { NotifyFor.entries.forEach { mode ->
            WhiteNoiseDialogChoiceRow(stringResource(if (mode == NotifyFor.AllMessages) R.string.notification_notify_all else R.string.notification_notify_mentions),
                mode == chat.notifyFor,{ controller.request(NotificationChange.Mode(mode),chat.id,profile.id); modeOpen = false },Modifier.testTag("notification.mode.${mode.name}"))
        } } },confirmButton = {},dismissButton = { TextButton(onClick = { modeOpen = false }) { Text(stringResource(R.string.cancel)) } })
    if (vibrationOpen) VibrationChoiceDialog(profile,chat,controller) { vibrationOpen = false }
    NotificationSettingsFeedback(settingsResult) { settingsResult = null }
}

internal fun formatMuteTime(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM,FormatStyle.SHORT).format(Instant.ofEpochMilli(millis).atZone(zone))

@Composable
internal fun VibrationChoiceDialog(profile: Profile, chat: Chat, controller: NotificationController, onDismiss: () -> Unit) {
    var selected by rememberSaveable(profile.id,chat.id) { mutableStateOf(chat.vibration) }
    DisposableEffect(profile.id,chat.id) { onDispose { controller.cancelPreview(GroupOwner(profile.id,chat.id)) } }
    WhiteNoiseAlertDialog(onDismissRequest = onDismiss,title = { Text(stringResource(R.string.notification_vibration)) },
        text = { Column(Modifier.verticalScroll(rememberScrollState()).selectableGroup()) {
            VibrationChoice.entries.forEach { choice ->
                WhiteNoiseDialogChoiceRow(stringResource(vibrationResource(choice)),selected == choice,{ selected = choice; controller.cancelPreview(GroupOwner(profile.id,chat.id)) },Modifier.testTag("notification.vibration.${choice.name}"))
            }
            TextButton(onClick = { controller.preview(chat.id,selected,profile.id) },modifier = Modifier.testTag("notification.vibration.preview")) { Text(stringResource(R.string.notification_preview_vibration)) }
            controller.preview?.takeIf { it.owner == GroupOwner(profile.id,chat.id) }?.let { preview ->
                Text(stringResource(when (preview.phase) {
                    VibrationPreviewPhase.Preparing -> R.string.notification_preview_preparing
                    VibrationPreviewPhase.Playing -> R.string.notification_preview_playing
                    VibrationPreviewPhase.Complete -> R.string.notification_preview_complete
                    VibrationPreviewPhase.Unavailable -> R.string.notification_preview_unavailable
                }),modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }.testTag("notification.preview.status"))
            }
        } },
        confirmButton = { TextButton(onClick = { controller.request(NotificationChange.Vibration(selected),chat.id,profile.id); onDismiss() }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } })
}
