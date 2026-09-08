package dev.ipf.whitenoise.ui.conversation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.state.FloatingMessageController
import dev.ipf.whitenoise.ui.components.*
import dev.ipf.whitenoise.ui.theme.amoledOutlineBorder
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import kotlin.math.roundToInt

internal val LocalFloatingMessages = staticCompositionLocalOf<FloatingMessageController?> { null }
internal val LocalFloatingObscured = staticCompositionLocalOf<(Boolean) -> Unit> { {} }
internal val LocalFloatingComposerHeight = staticCompositionLocalOf<(Float) -> Unit> { {} }

private class FloatingPlacement(x: Float = 1f, y: Float = 0.35f) {
    var x by mutableFloatStateOf(x)
    var y by mutableFloatStateOf(y)
    companion object {
        val Saver = listSaver<FloatingPlacement, Float>(
            save = { listOf(it.x, it.y) }, restore = { FloatingPlacement(it[0], it[1]) },
        )
    }
}

/** A navigation-level overlay, scoped to the active profile. It never creates a system overlay window. */
@Composable
internal fun FloatingMessagesHost(
    controller: FloatingMessageController, profileId: String?, visible: Boolean,
    modifier: Modifier = Modifier, onOpen: (FloatingMessageKey) -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    var obscured by remember { mutableStateOf(false) }
    val reportObscured: (Boolean) -> Unit = remember { { obscured = it } }
    var composerHeight by remember { mutableFloatStateOf(0f) }
    val reportHeight: (Float) -> Unit = remember { { composerHeight = it } }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var foreground by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ -> foreground = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    Box(modifier.fillMaxSize().testTag("floating.host")) {
        CompositionLocalProvider(LocalFloatingMessages provides controller, LocalFloatingComposerHeight provides reportHeight,
            LocalFloatingObscured provides reportObscured) {
            content(Modifier.fillMaxSize())
        }
        val entries = controller.entries
        key(profileId) {
            val placement = rememberSaveable(saver = FloatingPlacement.Saver) { FloatingPlacement() }
            if (visible && foreground && !obscured && entries.isNotEmpty()) {
                var expanded by rememberSaveable { mutableStateOf(false) }
                BackHandler(enabled = expanded) { expanded = false }
                FloatingMessageCard(controller, entries, composerHeight, placement, expanded,
                    onExpand = { expanded = true }, onCollapse = { expanded = false }, onOpen = {
                        expanded = false
                        onOpen(it)
                    })
            }
        }
    }
}

@Composable
private fun FloatingMessageCard(
    controller: FloatingMessageController, entries: List<FloatingMessageEntry>, composerHeight: Float, placement: FloatingPlacement,
    expanded: Boolean, onExpand: () -> Unit, onCollapse: () -> Unit, onOpen: (FloatingMessageKey) -> Unit,
) {
    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val bottom = with(density) { composerHeight.toDp() }.coerceAtLeast(80.dp)
    val margin = WhiteNoiseSpacing.CompactScreenMargin * 1.5f
    val index = entries.indexOfFirst { it.key == controller.selected }.coerceAtLeast(0)
    val position = stringResource(R.string.floating_position, index + 1, entries.size)
    val toggleExpansion = { if (expanded) onCollapse() else onExpand() }
    val toggleLabel = stringResource(if (expanded) R.string.collapse_message else R.string.floating_expand)
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().imePadding()
        .padding(start = margin, end = margin, top = 64.dp, bottom = bottom + 8.dp),
        contentAlignment = AbsoluteAlignment.TopLeft) {
        val availableHeight = constraints.maxHeight.toFloat()
        val expandedHeight = maxHeight.coerceAtMost(480.dp)
        val showPreview = maxHeight >= 144.dp
        val maxX = (constraints.maxWidth - cardSize.width).coerceAtLeast(0).toFloat()
        val maxY = (constraints.maxHeight - cardSize.height).coerceAtLeast(0).toFloat()
        // Anchor the top to the viewport, not the remaining space below this message.
        val top = (placement.y * availableHeight).coerceIn(0f, maxY)
        SideEffect {
            if (availableHeight > 0 && placement.y * availableHeight > maxY) placement.y = top / availableHeight
        }
        val moveTop = stringResource(R.string.floating_move_top)
        val moveBottom = stringResource(R.string.floating_move_bottom)
        val toBottom = { placement.y = if (availableHeight > 0) maxY / availableHeight else 0f }
        val drag = Modifier.pointerInput(maxX, availableHeight, maxY) {
            detectDragGestures(onDragEnd = { placement.x = if (placement.x < 0.5f) 0f else 1f }) { change, amount ->
                change.consume()
                if (maxX > 0) placement.x = (placement.x + amount.x / maxX).coerceIn(0f, 1f)
                if (availableHeight > 0) placement.y = (placement.y + amount.y / availableHeight).coerceIn(0f, maxY / availableHeight)
            }
        }
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer,
            border = amoledOutlineBorder(), shadowElevation = 6.dp,
            modifier = Modifier.absoluteOffset { IntOffset((placement.x * maxX).roundToInt(), top.roundToInt()) }
                .fillMaxWidth().then(if (expanded) Modifier.heightIn(max = expandedHeight) else Modifier)
                .onSizeChanged { cardSize = it }.testTag("floating.card")
                .semantics { customActions = listOf(
                    CustomAccessibilityAction(moveTop) { placement.y = 0f; true },
                    CustomAccessibilityAction(moveBottom) { toBottom(); true },
                ) }) {
            Box(Modifier.pointerInput(toggleExpansion) {
                detectTapGestures(onLongPress = { toggleExpansion() })
            }.semantics {
                onLongClick(toggleLabel) { toggleExpansion(); true }
                if (entries.size > 1) stateDescription = position
            }) {
                Column(Modifier.then(if (expanded) Modifier.fillMaxWidth().testTag("floating.expanded") else Modifier)) {
                    Row(Modifier.fillMaxWidth().then(drag).heightIn(min = 48.dp)
                        .padding(start = 16.dp).testTag("floating.header"),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.floating_title), Modifier.weight(1f), style = MaterialTheme.typography.labelLarge,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        FloatingMenu(controller, onOpen, onMoveTop = { placement.y = 0f }, onMoveBottom = toBottom)
                        if (expanded) IconButton(onCollapse, Modifier.testTag("floating.collapse")) {
                            Icon(painterResource(R.drawable.ic_close_fullscreen), stringResource(R.string.collapse_message))
                        }
                    }
                    if (expanded || showPreview) Row(Modifier.fillMaxWidth()
                        .then(if (expanded) Modifier.weight(1f, fill = false) else Modifier)
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        .floatingContentGestures(toggleExpansion), verticalAlignment = Alignment.Top) {
                        if (entries.size > 1) Box(Modifier.padding(end = 8.dp).testTag("floating.pagination")) {
                            PinPagination(index, entries.size)
                        }
                        FloatingPager(controller, entries, Modifier.weight(1f)) { entry, _ ->
                            Column(Modifier.fillMaxWidth()
                                .then(if (expanded) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                                .testTag("floating.preview"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (expanded) Text(floatingAuthor(entry), Modifier.testTag("floating.author"),
                                    style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (expanded) MaterialTheme(typography = MaterialTheme.typography.copy(bodyLarge = MaterialTheme.typography.bodyMedium)) {
                                    ReadOnlyMessageContent(entry.profile, entry.chat, entry.message)
                                }
                                else Text(MessageDocuments.plainText(MessageEditing.displayedText(entry.message)).takeIf { it.isNotBlank() }
                                    ?: entry.message.attachments.joinToString { it.label },
                                    modifier = Modifier.testTag("floating.text"),
                                    style = MaterialTheme.typography.bodyMedium, maxLines = 4, overflow = TextOverflow.Ellipsis)
                                if (expanded) Text(stringResource(R.string.floating_source, entry.chat.title, entry.message.timeLabel),
                                    modifier = Modifier.testTag("floating.source"),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall,
                                    overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The reference card owns content taps, including taps on images/links; drags still reach the pager and scroll container. */
private fun Modifier.floatingContentGestures(onLongPress: () -> Unit): Modifier =
    pointerInput(onLongPress) {
        awaitEachGesture {
            val pass = PointerEventPass.Initial
            val down = awaitFirstDown(requireUnconsumed = false, pass = pass)
            val tapped = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                while (true) {
                    val event = awaitPointerEvent(pass)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: return@withTimeoutOrNull false
                    if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop || event.changes.size > 1)
                        return@withTimeoutOrNull false
                    if (!change.pressed) {
                        change.consume()
                        return@withTimeoutOrNull true
                    }
                }
            }
            if (tapped == null) {
                onLongPress()
                do {
                    val event = awaitPointerEvent(pass)
                    event.changes.forEach { it.consume() }
                } while (event.changes.any { it.pressed })
            }
        }
    }

@Composable
private fun floatingAuthor(entry: FloatingMessageEntry): String = when (entry.message.authorId) {
    entry.profile.id -> stringResource(R.string.you)
    else -> entry.profile.people.firstOrNull { it.id == entry.message.authorId }?.displayName
        ?: if (!entry.chat.isGroup) entry.chat.title else stringResource(R.string.unknown_person)
}

/** Recreate only when membership changes. Selection otherwise stays synchronized across both presentations. */
@Composable
private fun FloatingPager(
    controller: FloatingMessageController, entries: List<FloatingMessageEntry>, modifier: Modifier,
    page: @Composable (FloatingMessageEntry, Int) -> Unit,
) = key(entries.map { it.key }) {
    val selectedIndex = entries.indexOfFirst { it.key == controller.selected }.coerceAtLeast(0)
    val pages = remember(entries.size) { FloatingMessagePages(entries.size) }
    val pager = rememberPagerState(initialPage = pages.pageFor(selectedIndex), pageCount = { pages.pageCount })
    LaunchedEffect(selectedIndex) {
        if (!pager.isScrollInProgress && pages.messageAt(pager.currentPage) != selectedIndex)
            pager.scrollToPage(pages.pageFor(selectedIndex))
    }
    LaunchedEffect(pager) {
        snapshotFlow { pager.isScrollInProgress to pager.settledPage }.collect { (scrolling, pageIndex) ->
            if (!scrolling) {
                val index = pages.messageAt(pageIndex)
                controller.select(entries[index].key)
                // Boundary copies make the swipe continuous. Recenter invisibly after settling.
                if (pages.isBoundary(pageIndex)) pager.scrollToPage(pages.pageFor(index))
            }
        }
    }
    HorizontalPager(pager, modifier, userScrollEnabled = entries.size > 1,
        key = { it }, verticalAlignment = Alignment.Top) { pageIndex ->
        val index = pages.messageAt(pageIndex)
        page(entries[index], index)
    }
}

@Composable
private fun FloatingMenu(
    controller: FloatingMessageController, onOpen: (FloatingMessageKey) -> Unit,
    onMoveSide: (() -> Unit)? = null, onMoveTop: (() -> Unit)? = null, onMoveBottom: (() -> Unit)? = null,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton({ open = true }, Modifier.testTag("floating.menu")) {
            Icon(painterResource(R.drawable.ic_more_vert), stringResource(R.string.floating_options))
        }
        WhiteNoiseDropdownMenu(open, { open = false }, items = buildList {
            add(WhiteNoiseMenuItem(stringResource(R.string.go_to_message), { controller.selected?.let(onOpen) }, R.drawable.ic_reply,
                modifier = Modifier.testTag("floating.open")))
            add(WhiteNoiseMenuItem(stringResource(R.string.floating_remove), { controller.selected?.let(controller::remove) }, R.drawable.ic_close,
                modifier = Modifier.testTag("floating.remove")))
            add(WhiteNoiseMenuItem(stringResource(R.string.floating_clear), controller::clear, R.drawable.ic_delete,
                modifier = Modifier.testTag("floating.clear")))
            if (onMoveSide != null) add(WhiteNoiseMenuItem(stringResource(R.string.floating_move_side), onMoveSide))
            if (onMoveTop != null) add(WhiteNoiseMenuItem(stringResource(R.string.floating_move_top), onMoveTop))
            if (onMoveBottom != null) add(WhiteNoiseMenuItem(stringResource(R.string.floating_move_bottom), onMoveBottom))
        })
    }
}
