@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.ipf.whitenoise.ui.components

import android.text.format.DateFormat
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.MessageForwarding
import dev.ipf.whitenoise.model.MuteDuration
import dev.ipf.whitenoise.model.NotificationControls
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** Presets apply on selection. Custom date/time remains staged until the final future-time check. */
@Composable
fun MuteDurationDialog(
    onDismiss: () -> Unit,
    selectedDuration: MuteDuration? = null,
    onCustomSelect: ((Long) -> Unit)? = null,
    nowMillis: () -> Long = { MessageForwarding.nowMillis },
    zone: ZoneId = ZoneId.systemDefault(),
    onSelect: (MuteDuration) -> Unit,
) {
    var stage by rememberSaveable { mutableIntStateOf(0) }
    val initial = remember { Instant.ofEpochMilli(nowMillis()).atZone(zone).plusHours(1) }
    var selectedDay by rememberSaveable { mutableLongStateOf(initial.toLocalDate().toEpochDay()) }
    var selectedHour by rememberSaveable { mutableIntStateOf(initial.hour) }
    var selectedMinute by rememberSaveable { mutableIntStateOf(initial.minute) }
    var invalid by rememberSaveable { mutableStateOf(false) }
    if (stage == 1) {
        val minimum = Instant.ofEpochMilli(nowMillis()).atZone(zone).toLocalDate().toEpochDay()
        val date = rememberDatePickerState(initialSelectedDateMillis = java.time.LocalDate.ofEpochDay(selectedDay).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = remember(minimum) { object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay() >= minimum
            } })
        DatePickerDialog(onDismissRequest = { stage = 0 },
            confirmButton = { TextButton(onClick = {
                date.selectedDateMillis?.let { selectedDay = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay(); stage = 2; invalid = false }
            },enabled = date.selectedDateMillis != null,modifier = Modifier.testTag("mute.custom.date.next")) { Text(stringResource(R.string.notification_next)) } },
            dismissButton = { TextButton(onClick = { stage = 0 }) { Text(stringResource(R.string.cancel)) } }) {
            DatePicker(date,modifier = Modifier.testTag("mute.custom.date"))
        }
        return
    }
    if (stage == 2) {
        val time = rememberTimePickerState(selectedHour,selectedMinute,DateFormat.is24HourFormat(LocalContext.current))
        WhiteNoiseAlertDialog(onDismissRequest = { stage = 1 },title = { Text(stringResource(R.string.notification_custom_time)) },
            text = { Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
                Text(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM).format(java.time.LocalDate.ofEpochDay(selectedDay)))
                TimeInput(time,Modifier.testTag("mute.custom.time"))
                if (invalid) Text(stringResource(R.string.notification_future_time),color = MaterialTheme.colorScheme.error,modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            } },
            confirmButton = { TextButton(onClick = {
                selectedHour = time.hour; selectedMinute = time.minute
                val until = NotificationControls.customUntil(java.time.LocalDate.ofEpochDay(selectedDay),java.time.LocalTime.of(time.hour,time.minute),zone,nowMillis())
                if (until == null) invalid = true else onCustomSelect?.invoke(until)
            },modifier = Modifier.testTag("mute.custom.confirm")) { Text(stringResource(R.string.mute)) } },
            dismissButton = { TextButton(onClick = { stage = 0 }) { Text(stringResource(R.string.cancel)) } })
        return
    }
    WhiteNoiseAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.mute_for)) },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).selectableGroup()) {
                MuteDuration.entries.filter { it != MuteDuration.Custom }.forEach { duration ->
                    WhiteNoiseDialogChoiceRow(title = muteDurationLabel(duration),selected = duration == selectedDuration,
                        onClick = { onSelect(duration) },modifier = Modifier.testTag("mute.duration.${duration.name}"))
                }
                if (onCustomSelect != null) WhiteNoiseDialogChoiceRow(title = stringResource(R.string.notification_custom_time),
                    selected = selectedDuration == MuteDuration.Custom,onClick = { stage = 1 },modifier = Modifier.testTag("mute.duration.Custom"))
            }
        },confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        modifier = Modifier.testTag("mute.duration.dialog"),
    )
}

@Composable
private fun muteDurationLabel(duration: MuteDuration): String = stringResource(
    when (duration) {
        MuteDuration.OneHour -> R.string.mute_one_hour
        MuteDuration.EightHours -> R.string.mute_eight_hours
        MuteDuration.OneDay -> R.string.mute_one_day
        MuteDuration.OneWeek -> R.string.mute_one_week
        MuteDuration.Always -> R.string.mute_always
        MuteDuration.Custom -> R.string.notification_custom_time
    },
)
