package dev.ipf.whitenoise.ui.conversation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.ui.components.drawableResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToInt

internal object DraftPhotoProcessor {
    const val MaximumInputBytes = 32 * 1024 * 1024
    fun bundledSource(context: Context, asset: AvatarAsset): ByteArray? {
        val bitmap = BitmapFactory.decodeResource(context.resources, asset.drawableResource,
            BitmapFactory.Options().apply { inScaled = false }) ?: return null
        return try {
            ByteArrayOutputStream().use { output ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) null else output.toByteArray()
            }
        } finally { bitmap.recycle() }
    }
    suspend fun prepare(context: Context, attachment: MessageAttachment, quality: PhotoQuality): MessageAttachment? = try {
        prepareImage(context, attachment.copy(photoFrameQualities = emptyMap()), quality)
    } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
    catch (_: Exception) { null }

    private suspend fun prepareImage(context: Context, attachment: MessageAttachment, quality: PhotoQuality): MessageAttachment? = withContext(Dispatchers.IO) {
        val sources = attachment.sourceImages.ifEmpty { attachment.images }
        if (sources.size > 1) {
            val frames = sources.mapIndexed { index, image -> prepare(context, attachment.copy(images = listOf(image), sourceImages = emptyList(),
                photoEdits = attachment.photoEdits[index]?.let { mapOf(0 to it) }.orEmpty()), quality) }
            if (frames.any { it == null }) return@withContext null
            return@withContext attachment.copy(images = frames.flatMap { it!!.images }, sourceImages = sources,
                photoQuality = quality, fileSizeBytes = frames.sumOf { it!!.fileSizeBytes ?: 0 },
                pixelWidth = frames.first()!!.pixelWidth, pixelHeight = frames.first()!!.pixelHeight, mimeType = null,
                metadataPolicy = if (frames.any { it!!.metadataPolicy == PhotoMetadataPolicy.SafeFallback }) PhotoMetadataPolicy.SafeFallback else frames.first()!!.metadataPolicy)
        }
        val source = sources.firstOrNull() ?: return@withContext null
        val recipe = attachment.photoEdits[0]
        if (recipe != null && !recipe.isOriginal) {
            val rendered = PhotoEditorRenderer.renderFrame(context, source, recipe, quality).attachment ?: return@withContext null
            return@withContext attachment.copy(images = rendered.images, sourceImages = listOf(source), photoQuality = quality,
                fileSizeBytes = rendered.fileSizeBytes, pixelWidth = rendered.pixelWidth, pixelHeight = rendered.pixelHeight,
                mimeType = rendered.mimeType, metadataPolicy = rendered.metadataPolicy)
        }
        val bytes = when (source) {
            is ProfileAvatar.DeviceImage -> source.bytes
            is ProfileAvatar.Asset -> bundledSource(context, source.asset) ?: return@withContext null
            is ProfileAvatar.WebImage -> bundledSource(context, source.asset) ?: return@withContext null
            ProfileAvatar.Monogram -> return@withContext null
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bytes.size > MaximumInputBytes || bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
        val orientation = runCatching { ExifInterface(bytes.inputStream()).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1) }.getOrDefault(1)
        val original = if (quality == PhotoQuality.Original && orientation in 0..1) PhotoMetadata.strippedOriginal(bytes) else null
        coroutineContext.ensureActive()
        var width = bounds.outWidth
        var height = bounds.outHeight
        val output = original ?: run {
            var sample = 1
            while (maxOf(width, height) / (sample * 2) >= quality.maxEdge ||
                (width.toLong() / sample) * (height.toLong() / sample) > 24_000_000) sample *= 2
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return@withContext null
            val matrix = Matrix().apply {
                when (orientation) {
                    2 -> setScale(-1f, 1f); 3 -> setRotate(180f); 4 -> setScale(1f, -1f)
                    5 -> { setRotate(90f); postScale(-1f, 1f) }; 6 -> setRotate(90f)
                    7 -> { setRotate(-90f); postScale(-1f, 1f) }; 8 -> setRotate(-90f)
                }
            }
            val oriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            val ratio = (quality.maxEdge.toFloat() / maxOf(oriented.width, oriented.height)).coerceAtMost(1f)
            val scaled = if (ratio < 1f) oriented.scale((oriented.width * ratio).roundToInt().coerceAtLeast(1), (oriented.height * ratio).roundToInt().coerceAtLeast(1)) else oriented
            width = scaled.width; height = scaled.height
            try {
                ByteArrayOutputStream().use { stream ->
                    val format = if (bounds.outMimeType == "image/png" || scaled.hasAlpha()) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                    if (!scaled.compress(format, quality.jpegQuality, stream)) return@withContext null
                    stream.toByteArray()
                }
            } finally {
                if (scaled !== oriented) scaled.recycle()
                if (oriented !== bitmap) oriented.recycle()
                bitmap.recycle()
            }
        }
        coroutineContext.ensureActive()
        attachment.copy(images = listOf(ProfileAvatar.DeviceImage(output)), sourceImages = listOf(source),
            photoQuality = quality, fileSizeBytes = output.size, pixelWidth = width, pixelHeight = height,
            mimeType = if (output.firstOrNull() == 137.toByte()) "image/png" else "image/jpeg",
            metadataPolicy = when { original != null -> PhotoMetadataPolicy.StrippedOriginal
                quality == PhotoQuality.Original -> PhotoMetadataPolicy.SafeFallback
                else -> PhotoMetadataPolicy.Reencoded })
    }
}
