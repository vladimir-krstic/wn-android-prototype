package dev.ipf.whitenoise.ui.share

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.state.*
import dev.ipf.whitenoise.ui.components.*
import dev.ipf.whitenoise.ui.components.WhiteNoiseAlertDialog as AlertDialog
import dev.ipf.whitenoise.ui.components.WhiteNoiseLazyColumn as LazyColumn
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import kotlinx.coroutines.delay

internal val LocalIncoming = staticCompositionLocalOf<IncomingController?> { null }

@Composable
internal fun IncomingHost(controller: IncomingController, route: String?, onboarding: Boolean, onOpen: (IncomingOpen) -> Boolean, content: @Composable () -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val work = controller.work
    val snackbar = remember { SnackbarHostState() }
    val currentOpen = rememberUpdatedState(onOpen)
    SideEffect { controller.observeRoute(route,onboarding); controller.reconcile() }
    LaunchedEffect(work?.id, work?.phase, work?.attempt, controller.locked, lifecycle) {
        if (work?.running == true && !controller.locked) lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            delay(450)
            if (work.phase == IncomingPhase.Opening) {
                controller.opening(work.id)?.let { controller.opened(work.id,currentOpen.value(it)) }
            } else controller.advance(work.id,work.phase,work.attempt)
        }
    }
    val completion = work?.takeIf { it.phase == IncomingPhase.Complete && it.committed != null }
    val otherCount = (completion?.committed?.chatIds?.size ?: 1) - 1
    val otherText = pluralStringResource(R.plurals.incoming_other_chats,otherCount,otherCount)
    val dropped = completion?.committed?.dropped ?: 0
    val droppedText = pluralStringResource(R.plurals.incoming_dropped,dropped,dropped)
    LaunchedEffect(completion?.id) {
        if (completion != null && (otherCount > 0 || dropped > 0)) snackbar.showSnackbar(listOfNotNull(otherText.takeIf { otherCount > 0 },droppedText.takeIf { dropped > 0 }).joinToString(" "))
    }
    CompositionLocalProvider(LocalIncoming provides controller) {
        Box(Modifier.fillMaxSize()) {
            content()
            SnackbarHost(snackbar,Modifier.align(Alignment.BottomCenter).navigationBarsPadding().imePadding())
        }
        if (work != null && work.phase !in setOf(IncomingPhase.Queued,IncomingPhase.Complete,IncomingPhase.Cancelled) && !controller.locked) {
            Dialog(onDismissRequest = { controller.cancel(work.id) }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
                IncomingShareScreen(controller)
            }
        }
    }
}

@Composable
internal fun IncomingShareScreen(controller: IncomingController) {
    val work = controller.work ?: return
    val profiles = controller.signedProfiles()
    val selected = profiles.firstOrNull { it.id == work.selectedProfileId }
    val share = work.entry is IncomingEntry.Share
    val query = rememberSaveable(work.id,selected?.id,saver = TextFieldState.Saver) { TextFieldState() }
    var choosingProfile by rememberSaveable(work.id) { mutableStateOf(false) }
    val title = stringResource(if (share) R.string.incoming_share_to else R.string.incoming_open_title)
    BackHandler { if (choosingProfile) choosingProfile = false else if (query.text.isNotEmpty()) query.edit { replace(0,length,"") } else controller.cancel(work.id) }
    Scaffold(modifier = Modifier.fillMaxSize().testTag("incoming.screen").semantics { paneTitle = title },
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { WhiteNoiseTopBar(title,{ controller.cancel(work.id) }) },
        bottomBar = { if (work.phase == IncomingPhase.Choosing) Surface {
            AdaptiveContent(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(WhiteNoiseSpacing.CompactScreenMargin)) {
                WhiteNoiseButton(onClick = { controller.submit(work.id) }, enabled = work.selectedChatIds.isNotEmpty(), modifier = Modifier.fillMaxWidth().testTag("incoming.stage")) {
                    Text(pluralStringResource(R.plurals.incoming_share_count,work.selectedChatIds.size,work.selectedChatIds.size))
                }
            }
        } }) { padding ->
        AdaptiveContent(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            LazyColumn(Modifier.fillMaxSize().imePadding().testTag("incoming.list"), contentPadding = PaddingValues(WhiteNoiseSpacing.CompactScreenMargin), verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
                if (share && work.committed == null) item {
                    TextButton(onClick = { choosingProfile = true }, enabled = !work.running && profiles.isNotEmpty(), modifier = Modifier.testTag("incoming.profile")) {
                        Text(selected?.name ?: stringResource(R.string.incoming_choose_profile))
                    }
                }
                work.failure?.let { failure -> item {
                    Text(stringResource(incomingFailureResource(failure, work.committed != null)), color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }.testTag("incoming.failure"))
                } }
                if (work.running) item {
                    Text(stringResource(when(work.phase) { IncomingPhase.Preparing -> R.string.incoming_preparing; IncomingPhase.Applying -> R.string.incoming_staging; else -> R.string.incoming_opening }), Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    TextButton(onClick = { controller.cancel(work.id) }) { Text(stringResource(R.string.cancel)) }
                }
                if (work.phase == IncomingPhase.Failed) item {
                    FlowRow {
                        if (selected != null && work.failure !in setOf(IncomingFailure.ContentEmpty,IncomingFailure.ContentInvalid,IncomingFailure.ContentUnavailable,IncomingFailure.ContentTooLarge))
                            TextButton(onClick = { controller.retry(work.id) }) { Text(stringResource(R.string.lifecycle_retry)) }
                        if (!share) TextButton(onClick = { controller.goToChats(work.id) }) { Text(stringResource(R.string.incoming_go_chats)) }
                        if (share && work.committed == null) TextButton(onClick = { choosingProfile = true }) { Text(stringResource(R.string.incoming_choose_profile)) }
                        TextButton(onClick = { controller.cancel(work.id) }) { Text(stringResource(R.string.cancel)) }
                    }
                }
                if (work.phase == IncomingPhase.Choosing) {
                    item {
                        Text(stringResource(R.string.incoming_review))
                        if (work.fallback) Text(stringResource(R.string.incoming_shortcut_fallback))
                        work.prepared?.let { p ->
                            if (p.text.isNotEmpty()) Text(p.text, maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            if (p.media.isNotEmpty() || p.documents.isNotEmpty()) Text(pluralStringResource(R.plurals.incoming_files,p.media.size+p.documents.size,p.media.size+p.documents.size))
                        }
                    }
                    item { WhiteNoiseTextField(state = query, lineLimits = TextFieldLineLimits.SingleLine, label = { Text(stringResource(R.string.incoming_search)) }, modifier = Modifier.fillMaxWidth().testTag("incoming.search")) }
                    val targets = selected?.let { controller.targets(it.id) }.orEmpty().filter { it.title.contains(query.text,ignoreCase = true) }
                    if (targets.isEmpty()) item { Text(stringResource(R.string.incoming_no_chats)) }
                    else {
                        item { Text(stringResource(R.string.incoming_recent),style = MaterialTheme.typography.titleSmall) }
                        items(targets,key = { it.id }) { chat ->
                            val checked = chat.id in work.selectedChatIds
                            ListItem(supportingContent = if (chat.isArchived) ({ Text(stringResource(R.string.incoming_archived)) }) else null,
                                leadingContent = { Checkbox(checked,onCheckedChange = null, modifier = Modifier.clearAndSetSemantics { }) },
                                modifier = Modifier.fillMaxWidth().toggleable(value = checked,role = Role.Checkbox,onValueChange = { controller.toggle(work.id,chat.id) }).testTag("incoming.chat.${chat.id}")) { Text(chat.title) }
                        }
                    }
                }
            }
        }
    }
    if (choosingProfile) AlertDialog(onDismissRequest = { choosingProfile = false }, title = { Text(stringResource(R.string.incoming_choose_profile)) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) { profiles.forEach { p -> TextButton(onClick = { if (controller.chooseProfile(work.id,p.id)) choosingProfile = false }) { Text(p.name) } } }
    }, confirmButton = { TextButton(onClick = { choosingProfile = false }) { Text(stringResource(R.string.cancel)) } })
}

private fun incomingFailureResource(failure: IncomingFailure, staged: Boolean): Int = if (staged) R.string.incoming_open_failed else when(failure) {
    IncomingFailure.ContentEmpty -> R.string.incoming_empty
    IncomingFailure.ContentInvalid -> R.string.incoming_invalid
    IncomingFailure.ContentUnavailable -> R.string.incoming_unavailable
    IncomingFailure.ContentTooLarge -> R.string.incoming_too_large
    IncomingFailure.ProfileUnavailable -> R.string.incoming_profile_unavailable
    IncomingFailure.TargetUnavailable -> R.string.incoming_target_unavailable
    IncomingFailure.Preparation -> R.string.incoming_prepare_failed
    IncomingFailure.Apply -> R.string.incoming_stage_failed
    IncomingFailure.Open -> R.string.incoming_target_unavailable
    IncomingFailure.SourceChanged -> R.string.incoming_changed
}
