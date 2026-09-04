package dev.ipf.whitenoise.ui.conversation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import dev.ipf.whitenoise.ui.components.WhiteNoiseAlertDialog as AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import dev.ipf.whitenoise.ui.components.WhiteNoiseModalBottomSheet as ModalBottomSheet
import dev.ipf.whitenoise.ui.components.WhiteNoiseSheetHeader
import dev.ipf.whitenoise.ui.components.WhiteNoiseScaffold as Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.Chat
import dev.ipf.whitenoise.model.ChatMessage
import dev.ipf.whitenoise.model.ComposerAvailability
import dev.ipf.whitenoise.model.EmojiCategory
import dev.ipf.whitenoise.model.EmojiSection
import dev.ipf.whitenoise.model.MessageAction
import dev.ipf.whitenoise.model.MessageActionPolicy
import dev.ipf.whitenoise.model.MessageDeletionScope
import dev.ipf.whitenoise.model.MessageForwarding
import androidx.compose.runtime.saveable.rememberSaveable
import dev.ipf.whitenoise.model.MessageDeliveryState
import dev.ipf.whitenoise.model.Profile
import dev.ipf.whitenoise.model.ReactionCatalog
import dev.ipf.whitenoise.model.composerAvailability
import dev.ipf.whitenoise.ui.components.AdaptiveContent
import dev.ipf.whitenoise.ui.components.ProfileAvatar
import dev.ipf.whitenoise.ui.components.SignalEmoji
import dev.ipf.whitenoise.ui.components.WhiteNoiseButton
import dev.ipf.whitenoise.ui.components.WhiteNoiseCompactSearchField
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import kotlinx.coroutines.launch

@Composable
internal fun actionLabel(action: MessageAction): String = stringResource(
    when (action) {
        MessageAction.RetrySend -> R.string.retry_send
        MessageAction.Edit -> R.string.message_edit
        MessageAction.EditHistory -> R.string.message_edit_history
        MessageAction.RetryEdit -> R.string.message_retry_edit
        MessageAction.DiscardEdit -> R.string.message_discard_edit
        MessageAction.OpenMessage -> R.string.message_reader
        MessageAction.SelectText -> R.string.message_select_text
        MessageAction.Reply -> R.string.reply
        MessageAction.Share -> R.string.attachment_share
        MessageAction.SaveAttachments -> R.string.save_attachments
        MessageAction.Forward -> R.string.forward
        MessageAction.Copy -> R.string.copy
        MessageAction.CopyMarkdown -> R.string.message_copy_markdown
        MessageAction.ReadAloud -> R.string.read_aloud
        MessageAction.StopReading -> R.string.stop_reading
        MessageAction.Transcribe -> R.string.transcribe
        MessageAction.ShowTranscript -> R.string.show_transcript
        MessageAction.HideTranscript -> R.string.hide_transcript
        MessageAction.CopyTranscript -> R.string.copy_transcript
        MessageAction.Select -> R.string.select
        MessageAction.Info -> R.string.info
        MessageAction.Delete -> R.string.delete
    },
)

internal fun actionIcon(action: MessageAction): Int = when (action) {
    MessageAction.RetrySend -> R.drawable.ic_refresh
    MessageAction.Edit -> R.drawable.ic_edit
    MessageAction.EditHistory -> R.drawable.ic_description
    MessageAction.RetryEdit -> R.drawable.ic_refresh
    MessageAction.DiscardEdit -> R.drawable.ic_close
    MessageAction.OpenMessage -> R.drawable.ic_description
    MessageAction.SelectText -> R.drawable.ic_content_copy
    MessageAction.Reply -> R.drawable.ic_reply
    MessageAction.Share -> R.drawable.ic_share
    MessageAction.SaveAttachments -> R.drawable.ic_download
    MessageAction.Forward -> R.drawable.ic_forward
    MessageAction.Copy -> R.drawable.ic_content_copy
    MessageAction.CopyMarkdown -> R.drawable.ic_content_copy
    MessageAction.ReadAloud -> R.drawable.ic_volume_up
    MessageAction.StopReading -> R.drawable.ic_stop
    MessageAction.Transcribe -> R.drawable.ic_description
    MessageAction.ShowTranscript -> R.drawable.ic_visibility
    MessageAction.HideTranscript -> R.drawable.ic_visibility_off
    MessageAction.CopyTranscript -> R.drawable.ic_content_copy
    MessageAction.Select -> R.drawable.ic_check
    MessageAction.Info -> R.drawable.ic_info
    MessageAction.Delete -> R.drawable.ic_delete
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EmojiPickerSheet(
    onDismiss: () -> Unit,
    onEmoji: (String) -> Unit,
    onConfigure: (() -> Unit)? = null,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val sections = remember(query) { ReactionCatalog.search(query) }
    val sectionRanges = remember(sections) { emojiSectionRanges(sections) }
    val gridState = rememberLazyGridState()
    val categoryState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(
            SheetValue.Hidden,
            SheetValue.Expanded,
        ),
    )
    val activeCategory by remember(gridState, sectionRanges) {
        derivedStateOf {
            sectionRanges.lastOrNull { it.firstItemIndex <= gridState.firstVisibleItemIndex }
                ?.category
                ?: sections.firstOrNull()?.category
        }
    }

    LaunchedEffect(query) {
        if (sections.isNotEmpty()) gridState.scrollToItem(0)
    }
    LaunchedEffect(activeCategory, query) {
        if (query.isBlank()) {
            val index = sections.indexOfFirst { it.category == activeCategory }
            if (index >= 0) categoryState.animateScrollToItem(index)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(EmojiPickerExpandedHeightFraction)
                    .widthIn(max = EmojiPickerMaximumWidth)
                    .testTag("emoji.picker"),
            ) {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = WhiteNoiseSpacing.CompactScreenMargin,
                            end = WhiteNoiseSpacing.CompactScreenMargin,
                            bottom = WhiteNoiseSpacing.Related,
                        )
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                coroutineScope.launch { sheetState.expand() }
                            }
                        }
                        .testTag("emoji.picker.search"),
                    placeholder = { Text(stringResource(R.string.search_emoji)) },
                    leadingIcon = {
                        Icon(painterResource(R.drawable.ic_search), contentDescription = null)
                    },
                    trailingIcon = if (query.isNotEmpty()) ({
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                painterResource(R.drawable.ic_close),
                                contentDescription = stringResource(R.string.clear_search),
                            )
                        }
                    }) else null,
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                )

                if (sections.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(R.string.no_emoji_found),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = EmojiPickerMinimumCellSize),
                        state = gridState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("emoji.picker.grid"),
                        contentPadding = PaddingValues(
                            start = WhiteNoiseSpacing.CompactScreenMargin,
                            end = WhiteNoiseSpacing.CompactScreenMargin,
                            bottom = WhiteNoiseSpacing.Related,
                        ),
                    ) {
                        sections.forEach { section ->
                            item(
                                key = "${section.category.id}:header",
                                span = { GridItemSpan(maxLineSpan) },
                            ) {
                                Text(
                                    text = emojiCategoryLabel(section.category),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = EmojiPickerSectionHeaderMinimumHeight)
                                        .padding(top = WhiteNoiseSpacing.Related)
                                        .semantics { heading() }
                                        .testTag("emoji.picker.header.${section.category.id}"),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            gridItemsIndexed(
                                items = section.emoji,
                                key = { index, emoji -> "${section.category.id}:$index:$emoji" },
                            ) { index, emoji ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(EmojiPickerMinimumCellSize),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Surface(
                                        onClick = { onEmoji(emoji) },
                                        modifier = Modifier
                                            .size(EmojiPickerMinimumCellSize)
                                            .testTag("emoji.picker.item.${section.category.id}.$index"),
                                        shape = CircleShape,
                                        color = Color.Transparent,
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            SignalEmoji(
                                                emoji = emoji,
                                                modifier = Modifier.size(EmojiPickerEmojiSize),
                                                contentDescription = emoji,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (query.isBlank()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    EmojiCategoryBar(
                        sections = sections,
                        selected = activeCategory,
                        state = categoryState,
                        onConfigure = onConfigure,
                        onCategory = { category ->
                            val itemIndex = sectionRanges
                                .firstOrNull { it.category == category }
                                ?.firstItemIndex
                                ?: return@EmojiCategoryBar
                            coroutineScope.launch {
                                sheetState.expand()
                                gridState.animateScrollToItem(itemIndex)
                            }
                        },
                    )
                }
            }
        }
    }
}

private const val EmojiPickerExpandedHeightFraction = 0.88f
private val EmojiPickerMaximumWidth = 600.dp
private val EmojiPickerMinimumCellSize = 48.dp
private val EmojiPickerEmojiSize = 32.dp
private val EmojiPickerSectionHeaderMinimumHeight = 36.dp

private data class EmojiSectionRange(
    val category: EmojiCategory,
    val firstItemIndex: Int,
)

private fun emojiSectionRanges(sections: List<EmojiSection>): List<EmojiSectionRange> {
    var itemIndex = 0
    return sections.map { section ->
        EmojiSectionRange(section.category, itemIndex).also {
            itemIndex += section.emoji.size + 1
        }
    }
}

@Composable
private fun EmojiCategoryBar(
    sections: List<EmojiSection>,
    selected: EmojiCategory?,
    state: androidx.compose.foundation.lazy.LazyListState,
    onConfigure: (() -> Unit)?,
    onCategory: (EmojiCategory) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag("emoji.picker.categories"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            onConfigure?.let { configure ->
                IconButton(
                    onClick = configure,
                    modifier = Modifier
                        .padding(start = WhiteNoiseSpacing.Related)
                        .testTag("emoji.picker.configure"),
                ) {
                    Icon(
                        painterResource(R.drawable.ic_emoji_settings),
                        contentDescription = stringResource(R.string.configure_reactions),
                    )
                }
            }
            LazyRow(
                state = state,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    start = if (onConfigure == null) WhiteNoiseSpacing.Related else 0.dp,
                    end = WhiteNoiseSpacing.Related,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items(sections, key = { it.category.id }) { section ->
                    val category = section.category
                    val isSelected = selected == category
                    IconToggleButton(
                        checked = isSelected,
                        onCheckedChange = { onCategory(category) },
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { this.selected = isSelected }
                            .testTag("emoji.picker.category.${category.id}"),
                    ) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            } else {
                                Color.Transparent
                            },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(emojiCategoryIcon(category)),
                                    contentDescription = emojiCategoryLabel(category),
                                    modifier = Modifier.size(22.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun emojiCategoryLabel(category: EmojiCategory): String = stringResource(
    when (category) {
        EmojiCategory.Recent -> R.string.emoji_category_recent
        EmojiCategory.SmileysAndPeople -> R.string.emoji_category_smileys_people
        EmojiCategory.AnimalsAndNature -> R.string.emoji_category_animals_nature
        EmojiCategory.FoodAndDrink -> R.string.emoji_category_food_drink
        EmojiCategory.Activities -> R.string.emoji_category_activities
        EmojiCategory.TravelAndPlaces -> R.string.emoji_category_travel_places
        EmojiCategory.Objects -> R.string.emoji_category_objects
        EmojiCategory.Symbols -> R.string.emoji_category_symbols
        EmojiCategory.Flags -> R.string.emoji_category_flags
    },
)

private fun emojiCategoryIcon(category: EmojiCategory): Int = when (category) {
    EmojiCategory.Recent -> R.drawable.ic_emoji_recent
    EmojiCategory.SmileysAndPeople -> R.drawable.ic_emoji_smileys
    EmojiCategory.AnimalsAndNature -> R.drawable.ic_emoji_animals
    EmojiCategory.FoodAndDrink -> R.drawable.ic_emoji_food
    EmojiCategory.Activities -> R.drawable.ic_emoji_activities
    EmojiCategory.TravelAndPlaces -> R.drawable.ic_emoji_travel
    EmojiCategory.Objects -> R.drawable.ic_emoji_objects
    EmojiCategory.Symbols -> R.drawable.ic_emoji_symbols
    EmojiCategory.Flags -> R.drawable.ic_emoji_flags
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConfigureReactionsSheet(
    current: List<String>,
    onDismiss: () -> Unit,
    onApply: (List<String>) -> Unit,
    onPickSlot: (Int, List<String>) -> Unit,
) {
    var draft by remember(current) { mutableStateOf(current) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        WhiteNoiseSheetHeader(stringResource(R.string.configure_reactions))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = WhiteNoiseSpacing.Section).padding(bottom = WhiteNoiseSpacing.Section),
                verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.FormField),
            ) {
                Text(
                    stringResource(R.string.configure_reactions_guidance),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
                ) {
                    itemsIndexed(draft, key = { index, _ -> index }) { index, emoji ->
                        val description = stringResource(R.string.reaction_slot_description, index + 1, emoji)
                        Surface(
                            onClick = { onPickSlot(index, draft) },
                            modifier = Modifier.size(56.dp).semantics {
                                contentDescription = description
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                SignalEmoji(
                                    emoji = emoji,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
                ) {
                    TextButton(
                        onClick = { draft = ReactionCatalog.defaults },
                        modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                    ) {
                        Text(stringResource(R.string.reset))
                    }
                    WhiteNoiseButton(
                        onClick = { onApply(draft) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.done))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ForwardMessagesSheet(
    profile: Profile,
    sourceChatId: String,
    onDismiss: () -> Unit,
    allowsAccompanyingMessage: Boolean = false,
    onForward: (List<String>, String) -> Unit,
    destinationProfiles: List<Profile> = listOf(profile),
    onForwardToProfile: ((String, List<String>, String) -> Boolean)? = null,
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    var query by rememberSaveable(profile.id) { mutableStateOf("") }
    var destinationId by rememberSaveable(profile.id) { mutableStateOf(profile.id) }
    val destination = destinationProfiles.firstOrNull { it.id == destinationId } ?: profile
    var selected by rememberSaveable(profile.id, destination.id, stateSaver = androidx.compose.runtime.saveable.listSaver<Set<String>, String>(save = { it.toList() }, restore = { it.toSet() })) { mutableStateOf(emptySet<String>()) }
    var accompanyingMessage by rememberSaveable(profile.id) { mutableStateOf("") }
    var profileChoice by remember { mutableStateOf(false) }
    var startFailed by remember { mutableStateOf(false) }
    fun submit() {
        startFailed = if (onForwardToProfile != null) !onForwardToProfile(destination.id, selected.toList(), accompanyingMessage)
            else { onForward(selected.toList(), accompanyingMessage); false }
    }
    LaunchedEffect(destinationProfiles.map { it.id }) {
        if (destinationId !in destinationProfiles.map { it.id }) { destinationId = profile.id; selected = emptySet() }
    }
    LaunchedEffect(destination.chats.map { it.id }) { selected = selected.filter { id -> destination.chats.any { it.id == id } }.toSet() }

    var bottomOverlayHeightPx by remember { mutableIntStateOf(0) }
    val destinationListState = rememberLazyListState()
    val density = LocalDensity.current
    val bottomSafePadding = WindowInsets.safeDrawing
        .only(WindowInsetsSides.Bottom)
        .asPaddingValues()
        .calculateBottomPadding()
    val bottomContentPadding = with(density) { bottomOverlayHeightPx.toDp() } +
        WhiteNoiseSpacing.CompactScreenMargin +
        WhiteNoiseSpacing.Related +
        maxOf(bottomSafePadding, WhiteNoiseSpacing.Section)
    val topIsScrolled by remember {
        derivedStateOf { destinationListState.canScrollBackward }
    }
    val topContainerColor by animateColorAsState(
        targetValue = if (topIsScrolled) {
            MaterialTheme.colorScheme.surfaceContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        label = "ForwardSheetTopContainer",
    )
    val chats = destination.chats.filter {
        (destination.id != profile.id || it.id != sourceChatId) && (query.isBlank() || it.title.contains(query, ignoreCase = true))
    }
    val folders = destination.chatFolders.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
    if (profileChoice) AlertDialog(onDismissRequest = { profileChoice = false }, title = { Text(stringResource(R.string.batch_profile_choice)) },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            destinationProfiles.forEach { candidate -> TextButton(onClick = { destinationId = candidate.id; selected = emptySet(); profileChoice = false; startFailed = false }) {
                Text(candidate.name)
            } }
        } }, confirmButton = { TextButton(onClick = { profileChoice = false }) { Text(stringResource(R.string.cancel)) } })
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = topContainerColor,
        contentWindowInsets = {
            WindowInsets.safeDrawing.only(
                WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
            )
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .testTag("conversation.forward.content"),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().fillMaxHeight().widthIn(max = 600.dp),
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth().testTag("conversation.forward.top"),
                    color = topContainerColor,
                ) {
                    Column {
                        WhiteNoiseSheetHeader(stringResource(R.string.forward))
                        if (destinationProfiles.size > 1) TextButton(onClick = { profileChoice = true }, modifier = Modifier.testTag("conversation.forward.profile")) {
                            Text(stringResource(R.string.batch_forward_from, destination.name))
                        }
                        if (startFailed) Text(stringResource(R.string.batch_forward_start_failed), color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin))
                        Text(
                            stringResource(R.string.forward_selection_limit),
                            modifier = Modifier.padding(horizontal = WhiteNoiseSpacing.Section),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        WhiteNoiseCompactSearchField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = stringResource(R.string.search_chats),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = WhiteNoiseSpacing.CompactScreenMargin,
                                    vertical = WhiteNoiseSpacing.Related,
                                )
                                .testTag("conversation.forward.search"),
                        )
                    }
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .heightIn(min = 180.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Box(Modifier.fillMaxSize()) {
                        if (chats.isEmpty() && folders.isEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(bottom = bottomContentPadding),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(stringResource(R.string.no_results), style = MaterialTheme.typography.titleMedium)
                                Text(
                                    stringResource(R.string.no_results_detail),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        } else {
                            LazyColumn(
                                state = destinationListState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("conversation.forward.destinations"),
                                contentPadding = PaddingValues(
                                    start = WhiteNoiseSpacing.CompactScreenMargin,
                                    top = WhiteNoiseSpacing.Related,
                                    end = WhiteNoiseSpacing.CompactScreenMargin,
                                    bottom = bottomContentPadding,
                                ),
                            ) {
                                if (folders.isNotEmpty()) item { Text(stringResource(R.string.batch_folders), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(vertical = WhiteNoiseSpacing.Related)) }
                                itemsIndexed(folders, key = { _, folder -> "folder:${folder.id}" }) { _, folder ->
                                    val members = MessageForwarding.folderMembers(destination, profile.id, sourceChatId, folder)
                                    ForwardFolderChoice(folder, members, selected) { selected = MessageForwarding.toggleFolder(selected, members) }
                                    Spacer(Modifier.height(WhiteNoiseSpacing.Related))
                                }
                                if (chats.isNotEmpty()) item { Text(stringResource(R.string.batch_chats), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(vertical = WhiteNoiseSpacing.Related)) }
                                itemsIndexed(chats, key = { _, chat -> chat.id }) { index, chat ->
                                    val checked = chat.id in selected
                                    val failure = MessageForwarding.targetFailure(destination, chat)
                                    val enabled = checked || failure == null
                                    val shapes = ListItemDefaults.segmentedShapes(
                                        index = index,
                                        count = chats.size,
                                        defaultShapes = ListItemDefaults.shapes(
                                            shape = RoundedCornerShape(0.dp),
                                        ),
                                    ).let { positionalShapes ->
                                        positionalShapes.copy(selectedShape = positionalShapes.shape)
                                    }
                                    val destinationColors = ListItemDefaults.colors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                                        selectedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                                    )
                                    ListItem(
                                        checked = checked,
                                        onCheckedChange = { nextChecked ->
                                            selected = if (nextChecked) selected + chat.id else selected - chat.id
                                        },
                                        enabled = enabled,
                                        shapes = shapes,
                                        colors = destinationColors,
                                        supportingContent = {
                                            Text(
                                                failure?.let { forwardFailureText(it) } ?: chat.displayPreview,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        },
                                        leadingContent = {
                                            ProfileAvatar(
                                                chat.title,
                                                chat.visibleAvatar,
                                                Modifier.size(48.dp),
                                                contentDescription = null,
                                            )
                                        },
                                        trailingContent = if (checked) {
                                            {
                                                Icon(
                                                    painter = painterResource(R.drawable.ic_check),
                                                    contentDescription = null,
                                                    modifier = Modifier.testTag(
                                                        "conversation.forward.destination.${chat.id}.check",
                                                    ),
                                                    tint = MaterialTheme.colorScheme.primary,
                                                )
                                            }
                                        } else {
                                            null
                                        },
                                        modifier = Modifier.testTag(
                                            "conversation.forward.destination.${chat.id}",
                                        ),
                                        content = {
                                            Text(
                                                chat.title,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        },
                                    )
                                    if (index != chats.lastIndex) {
                                        HorizontalDivider(
                                            thickness = 2.dp,
                                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        )
                                    }
                                }
                            }
                        }
                        val forwardDescription = if (selected.isEmpty()) {
                            stringResource(R.string.forward)
                        } else {
                            pluralStringResource(
                                R.plurals.forward_to_chats,
                                selected.size,
                                selected.size,
                            )
                        }
                        if (allowsAccompanyingMessage) {
                            ForwardMessageComposer(
                                value = accompanyingMessage,
                                onValueChange = { accompanyingMessage = it },
                                onForward = { submit() },
                                enabled = selected.isNotEmpty(),
                                forwardDescription = forwardDescription,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(
                                        start = WhiteNoiseSpacing.CompactScreenMargin,
                                        end = WhiteNoiseSpacing.CompactScreenMargin,
                                        bottom = WhiteNoiseSpacing.CompactScreenMargin,
                                    )
                                    .navigationBarsPadding()
                                    .onSizeChanged { bottomOverlayHeightPx = it.height },
                            )
                        } else {
                            WhiteNoiseButton(
                                onClick = { submit() },
                                enabled = selected.isNotEmpty(),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(
                                        start = WhiteNoiseSpacing.CompactScreenMargin,
                                        end = WhiteNoiseSpacing.CompactScreenMargin,
                                        bottom = WhiteNoiseSpacing.CompactScreenMargin,
                                    )
                                    .navigationBarsPadding()
                                    .onSizeChanged { bottomOverlayHeightPx = it.height }
                                    .testTag("conversation.forward.submit"),
                            ) {
                                Text(forwardDescription)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ForwardMessageComposer(
    value: String,
    onValueChange: (String) -> Unit,
    onForward: () -> Unit,
    enabled: Boolean,
    forwardDescription: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .testTag("conversation.forward.composer"),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("conversation.forward.message"),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                minLines = 1,
                maxLines = 4,
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, top = 12.dp, bottom = 12.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                stringResource(R.string.add_a_message),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                IconButton(
                    onClick = onForward,
                    enabled = enabled,
                    modifier = Modifier.fillMaxSize().testTag("conversation.forward.submit"),
                ) {
                    Surface(
                        modifier = Modifier.size(32.dp),
                        shape = CircleShape,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                        contentColor = if (enabled) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_upward),
                                contentDescription = forwardDescription,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun DeleteMessagesDialog(
    messages: List<ChatMessage>,
    profileId: String,
    onDismiss: () -> Unit,
    onDelete: (MessageDeletionScope) -> Unit,
    remoteIds: Set<String> = messages.filter { !it.isDeleted && it.authorId == profileId }.map { it.id }.toSet(),
    busy: Boolean = false,
    startFailed: Boolean = false,
) {
    val remoteCount = messages.count { it.id in remoteIds }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (messages.size == 1) {
                    stringResource(R.string.delete_message_question)
                } else {
                    pluralStringResource(
                        R.plurals.delete_selected_question,
                        messages.size,
                        messages.size,
                    )
                },
            )
        },
        text = { Column {
            Text(when {
                remoteCount in 1 until messages.size -> stringResource(R.string.batch_delete_mixed, remoteCount, messages.size - remoteCount)
                remoteCount > 0 -> stringResource(R.string.batch_delete_everyone_detail)
                else -> stringResource(R.string.delete_for_me_explanation)
            })
            if (startFailed) Text(stringResource(R.string.batch_delete_start_failed), color = MaterialTheme.colorScheme.error)
        } },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (remoteCount > 0) {
                    TextButton(onClick = { onDelete(MessageDeletionScope.ForEveryone) }, enabled = !busy) {
                        Text(
                            stringResource(R.string.delete_for_everyone),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                TextButton(onClick = { onDelete(MessageDeletionScope.ForMe) }, enabled = !busy) {
                    Text(stringResource(R.string.delete_for_me), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
internal fun SelectionBottomBar(
    selectedCount: Int,
    canForward: Boolean,
    onDelete: () -> Unit,
    onForward: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(WhiteNoiseSpacing.Related),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
            ) {
                IconButton(onClick = onDelete, enabled = selectedCount > 0) {
                    Icon(
                        painterResource(R.drawable.ic_delete),
                        contentDescription = stringResource(R.string.delete_selected_messages),
                        tint = if (selectedCount > 0) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        },
                    )
                }
                Text(
                    pluralStringResource(R.plurals.selected_count, selectedCount, selectedCount),
                    modifier = Modifier.weight(1f).semantics {
                        liveRegion = LiveRegionMode.Polite
                    },
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                FilledTonalIconButton(onClick = onForward, enabled = canForward) {
                    Icon(
                        painterResource(R.drawable.ic_forward),
                        contentDescription = stringResource(R.string.forward_selected_messages),
                    )
                }
            }
            if (selectedCount > 0 && !canForward) {
                Text(
                    stringResource(R.string.forward_selection_unavailable),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = WhiteNoiseSpacing.Related),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
internal fun SearchResultsBottomBar(
    count: Int,
    current: Int,
    onOlder: () -> Unit,
    onNewer: () -> Unit,
) {
    val countLabel = if (count == 0) {
        stringResource(R.string.matches_zero)
    } else {
        pluralStringResource(R.plurals.match_position, count, current + 1, count)
    }
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .testTag("conversation.searchControls")
                .padding(
                    horizontal = WhiteNoiseSpacing.CompactScreenMargin,
                    vertical = WhiteNoiseSpacing.Related,
                ),
            horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalIconButton(
                onClick = onOlder,
                enabled = count > 0 && current < count - 1,
            ) {
                Icon(
                    painterResource(R.drawable.ic_arrow_up),
                    contentDescription = stringResource(R.string.previous_match),
                )
            }
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .testTag("conversation.searchCount")
                    .semantics { liveRegion = LiveRegionMode.Polite },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                Text(
                    countLabel,
                    modifier = Modifier.padding(
                        horizontal = WhiteNoiseSpacing.CompactScreenMargin,
                        vertical = 10.dp,
                    ),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            FilledTonalIconButton(
                onClick = onNewer,
                enabled = count > 0 && current > 0,
            ) {
                Icon(
                    painterResource(R.drawable.ic_arrow_down),
                    contentDescription = stringResource(R.string.next_match),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageDetailsScreen(
    profile: Profile,
    chat: Chat,
    message: ChatMessage,
    onBack: () -> Unit,
) {
    val outgoing = message.authorId == profile.id
    val sender = profile.people.firstOrNull { it.id == message.authorId }
    val senderName = sender?.displayName ?: chat.title
    val unknownPerson = stringResource(R.string.unknown_person)
    fun reactionPersonName(personId: String): String = when (personId) {
        profile.id -> profile.name
        chat.id -> chat.title
        else -> profile.people.firstOrNull { it.id == personId }?.displayName ?: unknownPerson
    }
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                scrollBehavior = dev.ipf.whitenoise.ui.components.LocalWhiteNoiseHeaderScroll.current,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                title = { Text(stringResource(R.string.message_details)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { padding ->
        AdaptiveContent(Modifier.fillMaxSize().padding(padding)) {
            dev.ipf.whitenoise.ui.components.WhiteNoiseLazyColumn(
                modifier = Modifier.testTag("message.details.list"),
                contentPadding = PaddingValues(WhiteNoiseSpacing.CompactScreenMargin),
                verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.FormField),
            ) {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(WhiteNoiseSpacing.CompactScreenMargin),
                            verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
                        ) {
                            if (message.text.isNotBlank()) {
                                Text(message.text, style = MaterialTheme.typography.bodyLarge)
                            }
                            TimelineAttachmentContent(
                                attachments = message.attachments,
                                outgoing = outgoing,
                                messageId = message.id,
                                onOpenMedia = {},
                            )
                        }
                    }
                }
                item { MessageFactsSection(profile, message) }
                if (message.reactions.isNotEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.reactions),
                                    modifier = Modifier.padding(
                                        start = WhiteNoiseSpacing.CompactScreenMargin,
                                        top = WhiteNoiseSpacing.CompactScreenMargin,
                                        end = WhiteNoiseSpacing.CompactScreenMargin,
                                        bottom = WhiteNoiseSpacing.Related,
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                message.reactions.forEachIndexed { index, reaction ->
                                    if (index > 0) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(
                                                horizontal = WhiteNoiseSpacing.CompactScreenMargin,
                                            ),
                                            color = MaterialTheme.colorScheme.outlineVariant,
                                        )
                                    }
                                    val people = reaction.personIds.distinct()
                                    ListItem(
                                        modifier = Modifier.testTag("message.details.reaction.$index"),
                                        headlineContent = {
                                            Text(people.joinToString(", ", transform = ::reactionPersonName))
                                        },
                                        supportingContent = {
                                            Text(
                                                pluralStringResource(
                                                    R.plurals.people_reacted,
                                                    people.size,
                                                    people.size,
                                                ),
                                            )
                                        },
                                        leadingContent = {
                                            SignalEmoji(
                                                emoji = reaction.emoji,
                                                modifier = Modifier.size(32.dp),
                                                contentDescription = reaction.emoji,
                                            )
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Column {
                            if (!outgoing) {
                                ListItem(
                                    headlineContent = { Text(stringResource(R.string.sent_from)) },
                                    supportingContent = { Text(senderName) },
                                    leadingContent = {
                                        if (sender != null) {
                                            ProfileAvatar(
                                                sender.displayName,
                                                sender.avatar,
                                                Modifier.size(40.dp),
                                                contentDescription = null,
                                            )
                                        } else {
                                            Icon(
                                                painterResource(R.drawable.ic_person),
                                                contentDescription = null,
                                            )
                                        }
                                    },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                )
                            } else {
                                val recipients = if (chat.isGroup) {
                                    chat.members.filterNot { it.personId == profile.id }.mapNotNull { member ->
                                        profile.people.firstOrNull { it.id == member.personId }
                                    }
                                } else {
                                    val peer = (chat.kind as? dev.ipf.whitenoise.model.ChatKind.Direct)?.personId
                                    profile.people.firstOrNull { it.id == peer }?.let(::listOf).orEmpty()
                                }
                                recipients.forEach { person ->
                                    ListItem(
                                        headlineContent = { Text(person.displayName) },
                                        supportingContent = {
                                            Text(
                                                stringResource(
                                                    when (message.deliveryState) {
                                                        MessageDeliveryState.Streaming -> R.string.message_streaming
                                                        MessageDeliveryState.Sending -> R.string.sending
                                                        MessageDeliveryState.Failed -> R.string.not_delivered
                                                        MessageDeliveryState.Sent -> R.string.sent
                                                    },
                                                ),
                                            )
                                        },
                                        leadingContent = {
                                            ProfileAvatar(
                                                person.displayName,
                                                person.avatar,
                                                Modifier.size(40.dp),
                                                contentDescription = null,
                                            )
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    )
                                }
                                if (recipients.isEmpty()) {
                                    ListItem(
                                        headlineContent = { Text(chat.title) },
                                        supportingContent = {
                                            Text(
                                                stringResource(
                                                    when (message.deliveryState) {
                                                        MessageDeliveryState.Streaming -> R.string.message_streaming
                                                        MessageDeliveryState.Sending -> R.string.sending
                                                        MessageDeliveryState.Failed -> R.string.not_delivered
                                                        MessageDeliveryState.Sent -> R.string.sent
                                                    },
                                                ),
                                            )
                                        },
                                        leadingContent = {
                                            Icon(
                                                painterResource(R.drawable.ic_person),
                                                contentDescription = null,
                                            )
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
