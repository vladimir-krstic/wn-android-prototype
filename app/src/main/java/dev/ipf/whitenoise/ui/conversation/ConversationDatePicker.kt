package dev.ipf.whitenoise.ui.conversation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.ConversationDates
import dev.ipf.whitenoise.model.GlobalSearchClock
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConversationDatePicker(dates: ConversationDates, anchorId: String?, onDismiss: () -> Unit, onJump: (LocalDate) -> Unit) {
    val initial = dates.anchor(anchorId)
    val state = rememberDatePickerState(
        initialSelectedDateMillis = GlobalSearchClock.pickerMillis(initial.toEpochDay()),
        initialDisplayedMonthMillis = GlobalSearchClock.pickerMillis(initial.toEpochDay()),
        yearRange = (dates.earliest?.year ?: dates.today.year)..dates.today.year,
        selectableDates = remember(dates) { object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = dates.selectable(LocalDate.ofEpochDay(GlobalSearchClock.pickerDay(utcTimeMillis)))
            override fun isSelectableYear(year: Int) = dates.earliest?.let { year in it.year..dates.today.year } == true
        } },
    )
    val selected = state.selectedDateMillis?.let { LocalDate.ofEpochDay(GlobalSearchClock.pickerDay(it)) }
    DatePickerDialog(onDismissRequest = onDismiss,
        modifier = Modifier.testTag("conversation.datePicker"),
        confirmButton = {
            TextButton(onClick = { selected?.takeIf(dates::selectable)?.let(onJump) },
                enabled = selected != null && dates.selectable(selected), modifier = Modifier.testTag("conversation.datePicker.jump")) {
                Text(stringResource(R.string.jump_date_confirm))
            }
        },
        dismissButton = { TextButton(onDismiss, Modifier.testTag("conversation.datePicker.cancel")) { Text(stringResource(R.string.cancel)) } },
    ) {
        if (dates.earliest == null) Text(stringResource(R.string.jump_date_empty), Modifier.padding(WhiteNoiseSpacing.Section))
        else Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
            DatePicker(state, title = { Text(stringResource(R.string.jump_to_date), Modifier.padding(WhiteNoiseSpacing.Section)) },
                modifier = Modifier.testTag("conversation.datePicker.calendar"))
        }
    }
}
