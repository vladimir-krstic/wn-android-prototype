package dev.ipf.whitenoise.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Draws the pinned Signal Android emoji artwork without depending on Signal's renderer.
 *
 * The source emoji remains the product/state value. This composable only replaces the visual
 * presentation with an exact square crop from the bundled Signal sprite atlas.
 */
@Composable
internal fun SignalEmoji(
    emoji: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val loadState by produceState<SignalEmojiLoadState>(
        initialValue = SignalEmojiLoadState.Loading,
        key1 = emoji,
    ) {
        value = SignalEmojiRepository.resolve(context, emoji)
            ?.let(SignalEmojiLoadState::Ready)
            ?: SignalEmojiLoadState.Missing
    }
    val semanticsModifier = remember(contentDescription) {
        if (contentDescription == null) {
            Modifier
        } else {
            Modifier.semantics { this.contentDescription = contentDescription }
        }
    }

    Box(
        modifier = modifier.then(semanticsModifier),
        contentAlignment = Alignment.Center,
    ) {
        when (val state = loadState) {
            SignalEmojiLoadState.Loading -> Unit
            SignalEmojiLoadState.Missing -> {
                Text(
                    text = emoji,
                    style = LocalTextStyle.current.copy(
                        fontSize = 20.sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                    ),
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
            }
            is SignalEmojiLoadState.Ready -> {
                Canvas(Modifier.fillMaxSize()) {
                    val destinationSide = minOf(size.width, size.height).toInt()
                    val destinationOffset = IntOffset(
                        x = ((size.width - destinationSide) / 2f).toInt(),
                        y = ((size.height - destinationSide) / 2f).toInt(),
                    )
                    drawImage(
                        image = state.value.page.image,
                        srcOffset = state.value.sourceOffset,
                        srcSize = SignalEmojiRepository.SourceSize,
                        dstOffset = destinationOffset,
                        dstSize = IntSize(destinationSide, destinationSide),
                        filterQuality = FilterQuality.Low,
                    )
                }
            }
        }
    }
}

private sealed interface SignalEmojiLoadState {
    data object Loading : SignalEmojiLoadState
    data object Missing : SignalEmojiLoadState
    data class Ready(val value: SignalEmojiRenderData) : SignalEmojiLoadState
}

internal data class SignalEmojiTile(
    val sheet: String,
    val index: Int,
)

private data class SignalEmojiPage(
    val bitmap: Bitmap,
    val image: ImageBitmap,
)

private data class SignalEmojiRenderData(
    val page: SignalEmojiPage,
    val sourceOffset: IntOffset,
)

private object SignalEmojiRepository {
    private const val AssetRoot = "signal_emoji"
    private const val RawTileSize = 66
    private const val TileInset = 1
    private const val TilesPerRow = 16
    private const val BitmapCacheKilobytes = 24 * 1024

    val SourceSize = IntSize(RawTileSize - (TileInset * 2), RawTileSize - (TileInset * 2))

    private val atlasLock = Any()
    private val pageLock = Any()

    @Volatile
    private var atlas: Map<String, SignalEmojiTile>? = null

    private val pages = object : LruCache<String, SignalEmojiPage>(BitmapCacheKilobytes) {
        override fun sizeOf(key: String, value: SignalEmojiPage): Int =
            value.bitmap.allocationByteCount / 1024
    }

    suspend fun resolve(context: Context, emoji: String): SignalEmojiRenderData? =
        withContext(Dispatchers.IO) {
            val tile = loadAtlas(context)[emoji]
                ?: loadAtlas(context)[emoji.replace("\uFE0F", "")]
                ?: return@withContext null
            val page = loadPage(context, tile.sheet) ?: return@withContext null
            SignalEmojiRenderData(
                page = page,
                sourceOffset = IntOffset(
                    x = (tile.index % TilesPerRow) * RawTileSize + TileInset,
                    y = (tile.index / TilesPerRow) * RawTileSize + TileInset,
                ),
            )
        }

    internal fun hasEmoji(context: Context, emoji: String): Boolean {
        val loaded = loadAtlas(context)
        return loaded.containsKey(emoji) || loaded.containsKey(emoji.replace("\uFE0F", ""))
    }

    private fun loadAtlas(context: Context): Map<String, SignalEmojiTile> {
        atlas?.let { return it }
        return synchronized(atlasLock) {
            atlas ?: context.assets.open("$AssetRoot/emoji_data.json")
                .bufferedReader()
                .use { parseSignalEmojiAtlas(it.readText()) }
                .also { atlas = it }
        }
    }

    private fun loadPage(context: Context, sheet: String): SignalEmojiPage? = synchronized(pageLock) {
        pages[sheet] ?: context.assets.open("$AssetRoot/$sheet.webp").use { stream ->
            BitmapFactory.decodeStream(
                stream,
                null,
                BitmapFactory.Options().apply { inScaled = false },
            )?.let { bitmap ->
                SignalEmojiPage(bitmap, bitmap.asImageBitmap()).also { pages.put(sheet, it) }
            }
        }
    }
}

internal fun signalEmojiAtlasContains(context: Context, emoji: String): Boolean =
    SignalEmojiRepository.hasEmoji(context, emoji)

/** Parses only the stable `emoji` object in Signal's generated atlas manifest. */
internal fun parseSignalEmojiAtlas(source: String): Map<String, SignalEmojiTile> {
    val cursor = SignalEmojiManifestCursor(source)
    cursor.seekToEmojiObject()
    val result = LinkedHashMap<String, SignalEmojiTile>()

    while (!cursor.consumeIf('}')) {
        val sheet = cursor.readString()
        cursor.expect(':')
        cursor.expect('[')
        var tileIndex = 0
        while (!cursor.consumeIf(']')) {
            cursor.expect('[')
            while (!cursor.consumeIf(']')) {
                val emoji = decodeSignalEmojiHex(cursor.readString())
                val tile = SignalEmojiTile(sheet, tileIndex++)
                if (!result.containsKey(emoji)) result[emoji] = tile
                val selectorFreeEmoji = emoji.replace("\uFE0F", "")
                if (!result.containsKey(selectorFreeEmoji)) result[selectorFreeEmoji] = tile
                cursor.consumeIf(',')
            }
            cursor.consumeIf(',')
        }
        cursor.consumeIf(',')
    }
    return result
}

internal fun decodeSignalEmojiHex(value: String): String {
    require(value.length % 4 == 0) { "Emoji atlas value must contain UTF-16 code units" }
    return buildString(value.length / 4) {
        value.chunked(4).forEach { append(it.toInt(16).toChar()) }
    }
}

private class SignalEmojiManifestCursor(private val source: String) {
    private var index = 0

    fun seekToEmojiObject() {
        index = source.indexOf("\"emoji\"")
        require(index >= 0) { "Emoji atlas manifest has no emoji object" }
        index += "\"emoji\"".length
        expect(':')
        expect('{')
    }

    fun readString(): String {
        skipWhitespace()
        expect('"')
        val start = index
        while (index < source.length && source[index] != '"') index += 1
        require(index < source.length) { "Unterminated emoji atlas string" }
        return source.substring(start, index).also { index += 1 }
    }

    fun consumeIf(expected: Char): Boolean {
        skipWhitespace()
        if (index < source.length && source[index] == expected) {
            index += 1
            return true
        }
        return false
    }

    fun expect(expected: Char) {
        skipWhitespace()
        require(index < source.length && source[index] == expected) {
            "Expected '$expected' at emoji atlas offset $index"
        }
        index += 1
    }

    private fun skipWhitespace() {
        while (index < source.length && source[index].isWhitespace()) index += 1
    }
}
