@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.ipf.whitenoise.ui.chats

import dev.ipf.whitenoise.ui.components.WhiteNoiseListItemDefaults
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.ipf.whitenoise.ui.components.WhiteNoiseEntityPickerSheet
import dev.ipf.whitenoise.ui.components.WhiteNoisePickerItem
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.ui.components.WhiteNoiseAlertDialog as AlertDialog
import dev.ipf.whitenoise.ui.components.WhiteNoiseDropdownMenu
import dev.ipf.whitenoise.ui.components.WhiteNoiseMenuItem
import dev.ipf.whitenoise.ui.components.WhiteNoiseDialogChoiceRow
import dev.ipf.whitenoise.ui.components.WhiteNoiseTextField
import dev.ipf.whitenoise.ui.components.ProfileAvatar
import dev.ipf.whitenoise.ui.conversation.SearchHighlightedText
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.delay

internal val GlobalSearchFilterSaver = listSaver<GlobalSearchFilters, Any>(
    save = { listOf(ArrayList(it.chatIds), ArrayList(it.senderIds), it.date.name, it.fromDay ?: Long.MIN_VALUE,
        it.toDay ?: Long.MIN_VALUE, ArrayList(it.content.map { kind -> kind.name })) },
    restore = { saved ->
        @Suppress("UNCHECKED_CAST")
        GlobalSearchFilters((saved[0] as List<String>).toSet(), (saved[1] as List<String>).toSet(), GlobalSearchDate.valueOf(saved[2] as String),
            (saved[3] as Long).takeUnless { it == Long.MIN_VALUE }, (saved[4] as Long).takeUnless { it == Long.MIN_VALUE },
            (saved[5] as List<String>).map { GlobalSearchContent.valueOf(it) }.toSet())
    },
)

internal val GlobalSearchContent.labelResource get() = when (this) {
    GlobalSearchContent.Text -> R.string.global_content_text
    GlobalSearchContent.Links -> R.string.global_content_links
    GlobalSearchContent.ImagesVideo -> R.string.global_content_images
    GlobalSearchContent.VoiceAudio -> R.string.global_content_voice
    GlobalSearchContent.Files -> R.string.global_content_files
    GlobalSearchContent.AnyAttachment -> R.string.global_content_any
}
internal val GlobalSearchDate.labelResource get() = when (this) {
    GlobalSearchDate.AnyTime -> R.string.global_date_any
    GlobalSearchDate.Today -> R.string.global_date_today
    GlobalSearchDate.Last7Days -> R.string.global_date_week
    GlobalSearchDate.Last30Days -> R.string.global_date_month
    GlobalSearchDate.Custom -> R.string.global_date_custom
}

@Composable
private fun searchDateLabel(filters: GlobalSearchFilters): String = if (filters.date == GlobalSearchDate.Custom && filters.valid) {
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    "${LocalDate.ofEpochDay(filters.fromDay!!).format(formatter)} – ${LocalDate.ofEpochDay(filters.toDay!!).format(formatter)}"
} else stringResource(filters.date.labelResource)

@Composable
internal fun GlobalSearchFilterBar(profile: Profile, filters: GlobalSearchFilters, onChange: (GlobalSearchFilters) -> Unit) {
    val chips = buildList<Pair<String, () -> Unit>> {
        filters.chatIds.forEach { id -> profile.chats.firstOrNull { it.id == id }?.let { chat ->
            add(stringResource(R.string.global_chat_filter, chat.title) to { onChange(filters.copy(chatIds = filters.chatIds - id)) })
        } }
        filters.senderIds.forEach { id -> add(stringResource(R.string.global_sender_filter, GlobalSearch.senderName(profile, id)) to { onChange(filters.copy(senderIds = filters.senderIds - id)) }) }
        if (filters.date != GlobalSearchDate.AnyTime) add(searchDateLabel(filters) to { onChange(filters.copy(date = GlobalSearchDate.AnyTime, fromDay = null, toDay = null)) })
        filters.content.forEach { kind -> add(stringResource(kind.labelResource) to { onChange(filters.copy(content = filters.content - kind)) }) }
    }
    LazyRow(Modifier.fillMaxWidth().testTag("global.filters"), contentPadding = PaddingValues(horizontal = WhiteNoiseSpacing.CompactScreenMargin), horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
        if (filters.active) item { TextButton(onClick = { onChange(GlobalSearchFilters()) }) { Text(stringResource(R.string.global_clear_all)) } }
        items(chips) { (label, remove) ->
            val description = stringResource(R.string.global_remove_filter, label)
            InputChip(selected = true, onClick = remove, label = { Text(label) }, trailingIcon = { Icon(painterResource(R.drawable.ic_close), null) }, modifier = Modifier.semantics { contentDescription = description })
        }
    }
}

@Composable
internal fun GlobalSearchFilterMenu(
    expanded: Boolean,
    filters: GlobalSearchFilters,
    onDismiss: () -> Unit,
    onCategory: (String) -> Unit,
    onClear: () -> Unit,
) {
    WhiteNoiseDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("global.filterMenu"),
        items = listOf(
            Triple("chats", R.string.global_chats, filters.chatIds.isNotEmpty()),
            Triple("senders", R.string.global_senders, filters.senderIds.isNotEmpty()),
            Triple("dates", R.string.global_dates, filters.date != GlobalSearchDate.AnyTime),
            Triple("content", R.string.global_content, filters.content.isNotEmpty()),
        ).map { (category, label, active) ->
            WhiteNoiseMenuItem(
                label = stringResource(label), selected = active,
                onClick = { onDismiss(); onCategory(category) },
            )
        } + if (filters.active) listOf(
            WhiteNoiseMenuItem(label = stringResource(R.string.global_clear_all), onClick = { onDismiss(); onClear() }),
        ) else emptyList(),
    )
}

@Composable
internal fun GlobalSearchFilterPicker(profile: Profile, category: String, filters: GlobalSearchFilters, onChange: (GlobalSearchFilters) -> Unit, onDismiss: () -> Unit) {
    var custom by rememberSaveable(profile.id, category) { mutableStateOf(false) }
    if (category == "chats" || category == "senders") {
        val isChats = category == "chats"
        val selected = if (isChats) filters.chatIds else filters.senderIds
        WhiteNoiseEntityPickerSheet(
            title = stringResource(if (isChats) R.string.global_chats else R.string.global_senders),
            items = if (isChats) profile.chats.map { WhiteNoisePickerItem(it.id, it.title, it.avatar) }
                else GlobalSearch.senders(profile).map { (id, name) ->
                    WhiteNoisePickerItem(id, name, if (id == profile.id) profile.avatar else profile.people.firstOrNull { it.id == id }?.avatar)
                },
            selectedIds = selected, multiple = true,
            onSelect = { id ->
                val changed = if (id in selected) selected - id else selected + id
                onChange(if (isChats) filters.copy(chatIds = changed) else filters.copy(senderIds = changed))
            },
            onDismiss = onDismiss, onDone = onDismiss,
            searchTag = "global.filterSearch", rowTagPrefix = "global.choice",
        )
    } else AlertDialog(onDismissRequest = onDismiss,
        title = { Text(stringResource(when (category) { "chats" -> R.string.global_chats; "senders" -> R.string.global_senders; "dates" -> R.string.global_dates; "content" -> R.string.global_content; else -> R.string.global_filters })) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                when (category) {
                    "dates" -> GlobalSearchDate.entries.forEach { date ->
                        WhiteNoiseDialogChoiceRow(stringResource(date.labelResource), filters.date == date, onClick = {
                            if (date == GlobalSearchDate.Custom) custom = true
                            else onChange(filters.copy(date = date, fromDay = null, toDay = null))
                        })
                    }
                    "content" -> GlobalSearchContent.entries.forEach { kind -> SearchCheckRow(stringResource(kind.labelResource), kind in filters.content, "global.content.${kind.name}") {
                        onChange(filters.copy(content = if (kind in filters.content) filters.content - kind else filters.content + kind))
                    } }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) } },
    )
    if (custom) GlobalDateRangeDialog(filters, onDismiss = { custom = false }, onApply = { start, end ->
        onChange(filters.copy(date = GlobalSearchDate.Custom, fromDay = start, toDay = end)); custom = false
    })
}

@Composable
private fun SearchCheckRow(label: String, checked: Boolean, tag: String, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth().testTag(tag).toggleable(checked, role = Role.Checkbox, onValueChange = { onToggle() }), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked, null)
        Text(label, Modifier.weight(1f))
    }
}

@Composable
internal fun GlobalDateRangeDialog(filters: GlobalSearchFilters, onDismiss: () -> Unit, onApply: (Long, Long) -> Unit) {
    val initial = GlobalSearchClock.pickerMillis(GlobalSearchClock.today.toEpochDay())
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = filters.fromDay?.let(GlobalSearchClock::pickerMillis),
        initialSelectedEndDateMillis = filters.toDay?.let(GlobalSearchClock::pickerMillis),
        initialDisplayedMonthMillis = filters.fromDay?.let(GlobalSearchClock::pickerMillis) ?: initial,
    )
    val start = state.selectedStartDateMillis?.let(GlobalSearchClock::pickerDay)
    val end = state.selectedEndDateMillis?.let(GlobalSearchClock::pickerDay)
    DatePickerDialog(onDismissRequest = onDismiss,
        confirmButton = { TextButton(enabled = start != null && end != null && start <= end, onClick = { if (start != null && end != null) onApply(start, end) }) { Text(stringResource(R.string.global_apply)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    ) { DateRangePicker(state = state, modifier = Modifier.weight(1f, fill = false).testTag("global.dateRange")) }
}

@Composable
internal fun GlobalMessageRow(result: GlobalMessageResult, query: String, onOpen: () -> Unit) {
    ListItem(shapes = WhiteNoiseListItemDefaults.shapes(), onClick = onOpen, modifier = Modifier.testTag("global.message.${result.chatId}.${result.message.id}")) {
        Column(verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
            Text(stringResource(R.string.global_message_context, result.sender, result.chatTitle), style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            SearchHighlightedText(result.snippet, GlobalSearch.normalize(query), style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text("${result.message.dayLabel} · ${result.message.timeLabel}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun GlobalPersonRow(result: PeopleResult, unknown: Boolean, onOpen: (Person) -> Unit) {
    val name = if (unknown) stringResource(R.string.global_unknown_profile) else result.person.displayName
    ListItem(shapes = WhiteNoiseListItemDefaults.shapes(), onClick = { onOpen(if (unknown) result.person.copy(name = name) else result.person) }, modifier = Modifier.testTag("global.person.${result.person.id}"),
        leadingContent = { ProfileAvatar(name, result.person.avatar, Modifier.size(40.dp), contentDescription = null) },
    ) { Column { Text(name); if (result.person.nostrAddress.isNotBlank()) Text(result.person.nostrAddress, style = MaterialTheme.typography.bodySmall) } }
}

@Composable
internal fun GlobalSearchHeading(title: String) { Text(title, Modifier.fillMaxWidth().padding(WhiteNoiseSpacing.CompactScreenMargin), style = MaterialTheme.typography.titleSmall) }

@Composable
internal fun GlobalVoiceDialog(request: GlobalVoiceRequest, onComplete: (GlobalVoiceRequest) -> Unit, onRetry: () -> Unit, onDismiss: () -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(request.id, lifecycle) {
        if (request.scenario != GlobalVoiceScenario.Unavailable) lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { delay(600); onComplete(request) }
    }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.global_voice)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
            if (request.scenario == GlobalVoiceScenario.Unavailable) Text(stringResource(R.string.global_voice_unavailable))
            else { LinearProgressIndicator(Modifier.fillMaxWidth()); Text(stringResource(R.string.global_voice_processing)) }
        } },
        confirmButton = { if (request.scenario == GlobalVoiceScenario.Unavailable) TextButton(onClick = onRetry) { Text(stringResource(R.string.people_retry)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
internal fun GlobalPeopleFeedback(status: PeopleSearchStatus, identifier: Boolean, loading: Boolean, onRetry: () -> Unit) {
    val message = when {
        loading -> R.string.global_lookup
        status == PeopleSearchStatus.InvalidIdentifier && identifier -> R.string.people_invalid_identifier
        status == PeopleSearchStatus.AddressNotFound -> R.string.people_address_not_found
        status == PeopleSearchStatus.Unavailable -> R.string.people_unavailable
        status == PeopleSearchStatus.Partial -> R.string.people_partial
        else -> null
    }
    if (message != null) Column(Modifier.fillMaxWidth().padding(WhiteNoiseSpacing.CompactScreenMargin).semantics { liveRegion = LiveRegionMode.Polite }) {
        Text(stringResource(message))
        if (!loading && status in setOf(PeopleSearchStatus.AddressNotFound, PeopleSearchStatus.Unavailable, PeopleSearchStatus.Partial)) {
            TextButton(onClick = onRetry) { Text(stringResource(R.string.people_retry)) }
        }
    }
}
