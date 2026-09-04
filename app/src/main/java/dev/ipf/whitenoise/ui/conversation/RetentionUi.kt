package dev.ipf.whitenoise.ui.conversation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.state.*
import dev.ipf.whitenoise.ui.components.WhiteNoiseAlertDialog as AlertDialog
import dev.ipf.whitenoise.ui.components.WhiteNoiseTextField
import dev.ipf.whitenoise.ui.settings.SpeechSettingOption
import dev.ipf.whitenoise.ui.settings.SpeechSettingsChoices
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import kotlinx.coroutines.delay

internal val LocalRetention = staticCompositionLocalOf<RetentionController?> { null }

@Composable
internal fun RetentionHost(controller: RetentionController) {
    SideEffect { controller.reconcile() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val timed = controller.hasDeadlines()
    LaunchedEffect(controller, lifecycle, timed) {
        if (timed) lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) { val expected = controller.nowMillis; delay(1_000); controller.tick(expected) }
        }
    }
    controller.work.values.filter { it.running }.forEach { w -> key(w.owner) {
        LaunchedEffect(w.id, w.phase, lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            delay(450); controller.advance(w.owner, w.id, w.phase)
        } }
    } }
}

@Composable
internal fun retentionLabel(duration: DisappearingDuration): String {
    if (duration == DisappearingDuration.Off) return stringResource(R.string.retention_off)
    if (duration == DisappearingDuration.NinetyDays) return pluralStringResource(R.plurals.retention_days, 90, 90)
    val unit = RetentionUnit.entries.reversed().first { duration.seconds % it.seconds == 0L }
    val count = (duration.seconds / unit.seconds).toInt()
    return pluralStringResource(unit.pluralResource, count, count)
}
private val RetentionUnit.pluralResource get() = when (this) {
    RetentionUnit.Seconds -> R.plurals.retention_seconds
    RetentionUnit.Minutes -> R.plurals.retention_minutes
    RetentionUnit.Hours -> R.plurals.retention_hours
    RetentionUnit.Days -> R.plurals.retention_days
    RetentionUnit.Weeks -> R.plurals.retention_weeks
    RetentionUnit.Months -> R.plurals.retention_months
    RetentionUnit.Years -> R.plurals.retention_years
}
private val RetentionUnit.labelResource get() = when (this) {
    RetentionUnit.Seconds -> R.string.retention_unit_seconds
    RetentionUnit.Minutes -> R.string.retention_unit_minutes
    RetentionUnit.Hours -> R.string.retention_unit_hours
    RetentionUnit.Days -> R.string.retention_unit_days
    RetentionUnit.Weeks -> R.string.retention_unit_weeks
    RetentionUnit.Months -> R.string.retention_unit_months
    RetentionUnit.Years -> R.string.retention_unit_years
}

@Composable
internal fun RetentionPicker(current: DisappearingDuration, editable: Boolean = true, onDismiss: () -> Unit, onPick: (DisappearingDuration) -> Unit, error: String? = null) {
    var selected by rememberSaveable(current) { mutableStateOf(current) }
    var custom by rememberSaveable { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.disappearing_messages_title)) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
            Text(stringResource(R.string.retention_explainer))
            Text(stringResource(R.string.retention_existing_deadlines))
            if (!editable) Text(stringResource(R.string.retention_readonly))
            error?.let { Text(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = MaterialTheme.colorScheme.error) }
            DisappearingDuration.entries.forEach { duration ->
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected == duration, enabled = editable, role = Role.RadioButton, onClick = { selected = duration }),
                    verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected == duration, onClick = null, enabled = editable, modifier = Modifier.clearAndSetSemantics { })
                    Text(retentionLabel(duration), Modifier.padding(start = WhiteNoiseSpacing.Related))
                }
            }
            TextButton(onClick = { custom = true }, enabled = editable) {
                Text(if (selected in DisappearingDuration.entries) stringResource(R.string.retention_custom) else stringResource(R.string.retention_custom_value, retentionLabel(selected)))
            }
        }
    }, confirmButton = {
        TextButton(onClick = { if (editable) onPick(selected) else onDismiss() }) { Text(stringResource(if (editable) R.string.save else R.string.done)) }
    }, dismissButton = { if (editable) TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } })
    if (custom) CustomRetentionDialog(selected, { custom = false }) { selected = it; custom = false }
}

@Composable
private fun CustomRetentionDialog(initial: DisappearingDuration, onDismiss: () -> Unit, onPick: (DisappearingDuration) -> Unit) {
    val seed = remember(initial) { CustomRetentionInput.from(initial) }
    val value = rememberSaveable(saver = TextFieldState.Saver) { TextFieldState(seed.value) }
    var unitName by rememberSaveable { mutableStateOf(seed.unit.name) }
    val unit = RetentionUnit.valueOf(unitName)
    val input = CustomRetentionInput(value.text.toString(), unit)
    var unitPicker by rememberSaveable { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.retention_custom)) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.FormField)) {
            WhiteNoiseTextField(state = value, label = { Text(stringResource(R.string.retention_value)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), lineLimits = TextFieldLineLimits.SingleLine,
                isError = input.duration == null, errorMessage = stringResource(R.string.retention_range, unit.maximum),
                supportingText = { Text(stringResource(R.string.retention_range, unit.maximum)) }, modifier = Modifier.testTag("retention.custom.value"))
            TextButton(onClick = { unitPicker = true }) { Text(stringResource(R.string.retention_unit_value, stringResource(unit.labelResource))) }
            if (unit in setOf(RetentionUnit.Months, RetentionUnit.Years)) Text(stringResource(R.string.retention_calendar_units))
        }
    }, confirmButton = { TextButton(onClick = { input.duration?.let(onPick) }, enabled = input.duration != null) { Text(stringResource(R.string.retention_set)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } })
    if (unitPicker) SpeechSettingsChoices(stringResource(R.string.retention_unit), RetentionUnit.entries.map { candidate ->
        SpeechSettingOption(stringResource(candidate.labelResource), unit == candidate) { unitName = candidate.name; unitPicker = false }
    }, { unitPicker = false })
}

@Composable
internal fun RetentionConfirmation(profile: Profile, chat: Chat) {
    val controller = LocalRetention.current ?: return
    val owner = GroupOwner(profile.id, chat.id)
    val work = controller.work[owner]?.takeIf { it.phase == RetentionPhase.Confirm } ?: return
    AlertDialog(onDismissRequest = { controller.dismiss(owner, work.id) }, title = { Text(stringResource(R.string.retention_confirm_title)) },
        text = { Text(stringResource(R.string.retention_confirm_detail, retentionLabel(work.after))) },
        confirmButton = { TextButton(onClick = { controller.confirm(owner, work.id) }) { Text(stringResource(R.string.retention_set_timer), color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { controller.dismiss(owner, work.id) }) { Text(stringResource(R.string.cancel)) } })
}

@Composable
internal fun RetentionWorkPanel(profile: Profile, chat: Chat) {
    val controller = LocalRetention.current ?: return
    val owner = GroupOwner(profile.id, chat.id)
    val work = controller.work[owner]?.takeUnless { it.phase == RetentionPhase.Confirm } ?: return
    Column(Modifier.fillMaxWidth().padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin), verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
        Text(stringResource(when (work.phase) {
            RetentionPhase.Applying -> R.string.retention_applying
            RetentionPhase.Refreshing -> R.string.retention_refreshing
            RetentionPhase.RefreshFailed -> R.string.retention_refresh_failed
            RetentionPhase.Complete -> R.string.retention_updated
            else -> if (work.failure == RetentionFailure.Unavailable) R.string.retention_failed else R.string.retention_changed
        }), Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        if (work.running) LinearProgressIndicator(Modifier.fillMaxWidth()) else FlowRow {
            if (work.phase in setOf(RetentionPhase.Failed, RetentionPhase.RefreshFailed)) TextButton(onClick = { controller.retry(owner, work.id) }) { Text(stringResource(R.string.lifecycle_retry)) }
            TextButton(onClick = { controller.dismiss(owner, work.id) }) { Text(stringResource(R.string.lifecycle_dismiss)) }
        }
    }
}

@Composable
internal fun MessageExpiryIndicator(message: ChatMessage, semanticsEnabled: Boolean = true) {
    val controller = LocalRetention.current
    val presentation = MessageRetentionPolicy.presentation(message, controller?.nowMillis ?: MessageForwarding.nowMillis) ?: return
    val label = stringResource(R.string.retention_message)
    val remaining = presentation.remainingMillis
    val state = if (remaining == null) stringResource(R.string.retention_waiting) else {
        val seconds = (remaining / 1_000 + if (remaining % 1_000 == 0L) 0 else 1).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val unit = RetentionUnit.entries.reversed().firstOrNull { seconds >= it.seconds } ?: RetentionUnit.Seconds
        val count = (seconds / unit.seconds + if (seconds % unit.seconds == 0L) 0 else 1).toInt()
        stringResource(R.string.retention_remaining, pluralStringResource(unit.pluralResource, count, count))
    }
    val modifier = Modifier.size(14.dp).then(if (semanticsEnabled)
        Modifier.semantics { contentDescription = label; stateDescription = state }.testTag("retention.message.${message.id}")
        else Modifier.clearAndSetSemantics { })
    if (presentation.fraction == null) Icon(painterResource(R.drawable.ic_timer), null, modifier, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    else CircularProgressIndicator(progress = { presentation.fraction }, modifier = modifier, strokeWidth = 1.5.dp,
        color = MaterialTheme.colorScheme.onSurfaceVariant, trackColor = MaterialTheme.colorScheme.outlineVariant)
}
