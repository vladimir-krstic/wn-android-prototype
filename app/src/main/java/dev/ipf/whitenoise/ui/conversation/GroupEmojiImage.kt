package dev.ipf.whitenoise.ui.conversation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.graphics.createBitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.ui.components.ProfileAvatar
import dev.ipf.whitenoise.ui.components.WhiteNoiseAlertDialog as AlertDialog
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal sealed interface GroupEmojiRender {
    data object Loading : GroupEmojiRender
    data object Unsupported : GroupEmojiRender
    data object Failed : GroupEmojiRender
    data class Ready(val image: ProfileAvatar.DeviceImage, val emojis: List<String>) : GroupEmojiRender
}
internal object GroupEmojiRenderer {
    fun render(emojis: List<String>, supports: (Paint, String) -> Boolean = { paint, text -> paint.hasGlyph(text) }): GroupEmojiRender {
        if (emojis.size !in 1..2 || emojis.any { it.isBlank() }) return GroupEmojiRender.Failed
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; color = Color.WHITE; textSize = if (emojis.size == 1) 300f else 210f }
        if (emojis.any { !supports(paint, it) }) return GroupEmojiRender.Unsupported
        val bitmap = createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        return try {
            val canvas = Canvas(bitmap); canvas.drawColor(0xff3c4043.toInt())
            val gap = if (emojis.size == 1) 0f else 32f
            val width = emojis.sumOf { paint.measureText(it).toDouble() }.toFloat()
            if (width + gap > 456f) paint.textSize *= (456f - gap) / width
            val widths = emojis.map { paint.measureText(it) }
            var left = (512f - widths.sum() - gap) / 2f
            val baseline = 256f - (paint.fontMetrics.ascent + paint.fontMetrics.descent) / 2f
            emojis.forEachIndexed { i, emoji -> canvas.drawText(emoji, left + widths[i] / 2, baseline, paint); left += widths[i] + gap }
            ByteArrayOutputStream().use { output ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) GroupEmojiRender.Failed
                else GroupEmojiRender.Ready(ProfileAvatar.DeviceImage(output.toByteArray()), emojis.toList())
            }
        } finally { bitmap.recycle() }
    }
}

@Composable
internal fun GroupEmojiImageDialog(onDismiss: () -> Unit, onUse: (ProfileAvatar) -> Unit) {
    var selected by rememberSaveable(stateSaver = listSaver<GroupEmojiSelection, Any>(
        save = { it.emojis + it.limitReached },
        restore = { GroupEmojiSelection(it.dropLast(1).map { value -> value as String }, it.last() as Boolean) },
    )) { mutableStateOf(GroupEmojiSelection()) }
    var picker by rememberSaveable { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    val scenario = LocalGroupWork.current?.imageScenario ?: GroupImageScenario.Success
    val rendered by produceState<GroupEmojiRender>(GroupEmojiRender.Loading, selected.emojis, scenario, retry) {
        value = GroupEmojiRender.Loading
        value = withContext(Dispatchers.Default) {
            try {
                when {
                    retry == 0 && scenario == GroupImageScenario.UnsupportedEmoji -> GroupEmojiRender.Unsupported
                    retry == 0 && scenario == GroupImageScenario.RenderFailure -> GroupEmojiRender.Failed
                    else -> GroupEmojiRenderer.render(selected.emojis)
                }
            } catch (e: CancellationException) { throw e } catch (_: Exception) { GroupEmojiRender.Failed }
        }
    }
    val ready = (rendered as? GroupEmojiRender.Ready)?.takeIf { it.emojis == selected.emojis }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.group_emoji_create)) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
            Text(stringResource(R.string.group_emoji_hint))
            ready?.let { ProfileAvatar("", it.image, Modifier.size(120.dp), contentDescription = selected.emojis.joinToString(" ")) }
            selected.emojis.forEachIndexed { index, emoji ->
                TextButton(onClick = { selected = selected.remove(index) }) { Text("$emoji · ${stringResource(R.string.group_emoji_remove, index + 1)}") }
            }
            TextButton(onClick = { picker = true }) { Text(stringResource(R.string.group_emoji_create)) }
            if (selected.limitReached) Text(stringResource(R.string.group_emoji_limit), color = MaterialTheme.colorScheme.error)
            if (selected.emojis.isNotEmpty()) when (rendered) {
                GroupEmojiRender.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
                GroupEmojiRender.Unsupported -> Text(stringResource(R.string.group_emoji_unsupported), color = MaterialTheme.colorScheme.error)
                GroupEmojiRender.Failed -> Text(stringResource(R.string.group_emoji_failed), color = MaterialTheme.colorScheme.error)
                else -> Unit
            }
            if (selected.emojis.isNotEmpty() && rendered in setOf(GroupEmojiRender.Failed, GroupEmojiRender.Unsupported))
                TextButton(onClick = { retry++ }) { Text(stringResource(R.string.dictation_retry)) }
        }
    }, confirmButton = {
        TextButton(onClick = { ready?.let { onUse(it.image) } }, enabled = selected.emojis.isNotEmpty() && ready != null) { Text(stringResource(R.string.group_emoji_use)) }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } })
    if (picker) EmojiPickerSheet(onDismiss = { picker = false }, onEmoji = { selected = selected.add(it); picker = false })
}
