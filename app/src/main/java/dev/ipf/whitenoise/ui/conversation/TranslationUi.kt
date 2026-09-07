@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package dev.ipf.whitenoise.ui.conversation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.ui.components.*
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import kotlinx.coroutines.delay
import java.util.Locale

@Composable internal fun translationLanguageName(language: TranslationLanguage): String =
    Locale.forLanguageTag(language.tag).getDisplayLanguage(androidx.core.os.ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.ENGLISH)

@Composable internal fun TranslationWork(controller: TranslationController) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(controller, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) controller.pause()
            if (event == Lifecycle.Event.ON_START) controller.resume()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); controller.pause() }
    }
    val pending = controller.entries.values.firstOrNull { it.phase in setOf(TranslationPhase.Loading, TranslationPhase.Downloading) }
    LaunchedEffect(pending?.generation, pending?.phase) {
        pending?.let { delay(if (it.phase == TranslationPhase.Downloading) 900 else 500); controller.complete(it.message.id, it.generation) }
    }
}

@Composable private fun TranslationSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    WhiteNoiseModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden, enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded))) {
        Column(Modifier.fillMaxWidth().heightIn(max = LocalWindowInfo.current.containerDpSize.height * .88f)
            .padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin).testTag("translation.sheet")) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) {
                    Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.close))
                }
                Text(stringResource(R.string.translation_target), style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f).semantics { heading() })
            }
            Spacer(Modifier.height(16.dp))
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), content = content)
        }
    }
}

@Composable private fun TranslationActionRow(label: String, tag: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = { Icon(painterResource(R.drawable.ic_chevron_right), null, Modifier.size(24.dp)) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth().testTag(tag).clickable(role = Role.Button, onClick = onClick),
    )
}

@Composable internal fun TranslationLanguagePicker(onSelect: (TranslationLanguage) -> Unit, onDismiss: () -> Unit) {
    TranslationSheet(onDismiss) {
        TranslationLanguage.entries.forEach { language ->
            TranslationActionRow(translationLanguageName(language), "translation.language.${language.name}") { onSelect(language) }
        }
    }
}

@Composable internal fun MessageTranslationSheet(
    message: ChatMessage, state: MessageTranslation?, onSelect: (TranslationLanguage) -> Unit,
    onToggle: () -> Unit, onRetry: () -> Unit, onDownload: () -> Unit, onCancel: () -> Unit, onDismiss: () -> Unit,
) {
    val detected = remember(message.text) { TranslationExamples.detectedLanguage(message.text) }
    TranslationSheet(onDismiss) {
        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = dev.ipf.whitenoise.ui.theme.amoledOutlineBorder()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(message.text, style = MaterialTheme.typography.bodyLarge, maxLines = 5, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("translation.input"))
                Text(stringResource(R.string.translation_detected, detected?.let { translationLanguageName(it) } ?: stringResource(R.string.translation_not_detected)),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("translation.detected"))
            }
        }
        if (state != null && state.phase !in setOf(TranslationPhase.Ready, TranslationPhase.Cancelled)) {
            Spacer(Modifier.height(8.dp))
            WhiteNoiseCallout(text = stringResource(translationStatus(state.phase)),
                modifier = Modifier.testTag("translation.sheet_status").semantics { liveRegion = LiveRegionMode.Polite }, content = {
                    if (state.phase in setOf(TranslationPhase.Loading, TranslationPhase.Downloading)) LinearProgressIndicator(Modifier.fillMaxWidth())
                    FlowRow {
                        if (state.phase == TranslationPhase.MissingPack) TextButton(onClick = onDownload) { Text(stringResource(R.string.writing_download)) }
                        else if (state.phase !in setOf(TranslationPhase.Loading, TranslationPhase.Downloading)) TextButton(onClick = onRetry) { Text(stringResource(R.string.history_retry)) }
                        TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
                    }
                })
        }
        Spacer(Modifier.height(8.dp))
        if (state?.phase == TranslationPhase.Ready) {
            TranslationActionRow(stringResource(if (state.showOriginal) R.string.translation_show_translation else R.string.translation_show_original),
                "translation.toggle.${message.id}", onToggle)
        }
        TranslationLanguage.entries.filter { it != detected }.forEach { language ->
            TranslationActionRow(translationLanguageName(language), "translation.language.${language.name}") { onSelect(language) }
        }
    }
}

internal fun translationStatus(phase: TranslationPhase): Int = when (phase) {
    TranslationPhase.Loading -> R.string.translation_loading
    TranslationPhase.Downloading -> R.string.writing_downloading
    TranslationPhase.MissingPack -> R.string.translation_missing_pack
    TranslationPhase.Unavailable -> R.string.translation_unavailable
    TranslationPhase.Unsupported -> R.string.translation_unsupported
    TranslationPhase.Uncertain -> R.string.translation_uncertain
    TranslationPhase.Timeout -> R.string.translation_timeout
    else -> R.string.translation_error
}

@Composable internal fun TranslationBubbleStatus(message: ChatMessage, enabled: Boolean, containerColor: Color = MaterialTheme.colorScheme.surface) {
    if (!enabled) return
    val result = LocalMessageTranslation.current?.state(message) ?: return
    if (result.phase != TranslationPhase.Ready || result.showOriginal || result.source == result.target) return
    val source = translationLanguageName(result.source ?: return)
    val target = translationLanguageName(result.target)
    val description = stringResource(R.string.translation_languages, source, target)
    val foreground = dev.ipf.whitenoise.ui.theme.forwardedLabelColor(containerColor, LocalContentColor.current)
    FlowRow(Modifier.padding(top = 4.dp).testTag("translation.status.${message.id}").clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(source, style = MaterialTheme.typography.bodySmall, color = foreground, modifier = Modifier.align(Alignment.CenterVertically))
        Icon(painterResource(R.drawable.ic_translation_arrow), null, Modifier.size(16.dp).align(Alignment.CenterVertically), tint = foreground)
        Text(target, style = MaterialTheme.typography.bodySmall, color = foreground, modifier = Modifier.align(Alignment.CenterVertically))
    }
}
