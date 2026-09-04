package dev.ipf.whitenoise.ui.conversation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withSave
import androidx.core.graphics.withClip
import androidx.exifinterface.media.ExifInterface
import dev.ipf.whitenoise.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.coroutines.coroutineContext
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

internal data class PhotoEditorSource(val bytes: ByteArray, val width: Int, val height: Int, val orientation: Int, val mimeType: String) {
    val orientedWidth get() = if (orientation in setOf(5,6,7,8)) height else width
    val orientedHeight get() = if (orientation in setOf(5,6,7,8)) width else height
}
internal data class PhotoEditorPreview(val source: PhotoEditorSource?, val bitmap: Bitmap?, val failure: PhotoEditorFailure? = null)
internal data class PhotoEditorRender(val attachment: MessageAttachment? = null, val failure: PhotoEditorFailure? = null)

internal object PhotoEditorRenderer {
    private fun source(context: Context, image: ProfileAvatar): PhotoEditorSource? {
        val bytes = when (image) {
            is ProfileAvatar.DeviceImage -> image.bytes
            is ProfileAvatar.Asset -> DraftPhotoProcessor.bundledSource(context, image.asset)
            is ProfileAvatar.WebImage -> DraftPhotoProcessor.bundledSource(context, image.asset)
            ProfileAvatar.Monogram -> null
        } ?: return null
        if (bytes.isEmpty() || bytes.size > DraftPhotoProcessor.MaximumInputBytes) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (!PhotoEditing.validSource(bounds.outWidth, bounds.outHeight) || isAnimated(bytes, bounds.outMimeType)) return null
        val orientation = runCatching { ExifInterface(bytes.inputStream()).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1) }.getOrDefault(1)
        return PhotoEditorSource(bytes, bounds.outWidth, bounds.outHeight, orientation, bounds.outMimeType.orEmpty())
    }

    private fun isAnimated(bytes: ByteArray, mime: String?): Boolean {
        if (mime == "image/gif") return true
        if (mime == "image/png") {
            var offset = 8
            while (offset + 12 <= bytes.size) {
                var size = 0L; repeat(4) { size = (size shl 8) or (bytes[offset + it].toLong() and 255) }
                if (size > bytes.size - offset - 12) return true
                if (String(bytes, offset + 4, 4, Charsets.US_ASCII) == "acTL") return true
                offset += size.toInt() + 12
            }
        }
        if (mime == "image/webp" && bytes.size >= 21 && String(bytes, 12, 4, Charsets.US_ASCII) == "VP8X") return bytes[20].toInt() and 2 != 0
        return false
    }

    suspend fun preview(context: Context, image: ProfileAvatar): PhotoEditorPreview = withContext(Dispatchers.Default) {
        var raw: Bitmap? = null
        var oriented: Bitmap? = null
        var delivered = false
        try {
            val source = source(context, image) ?: return@withContext PhotoEditorPreview(null, null, PhotoEditorFailure.InvalidSource)
            var sample = 1
            while (max(source.width, source.height) / sample > PhotoEditing.PreviewEdge) sample *= 2
            raw = BitmapFactory.decodeByteArray(source.bytes, 0, source.bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
                ?: return@withContext PhotoEditorPreview(source, null, PhotoEditorFailure.Unavailable)
            oriented = orient(checkNotNull(raw), source.orientation)
            coroutineContext.ensureActive()
            delivered = true
            PhotoEditorPreview(source, oriented)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: OutOfMemoryError) { PhotoEditorPreview(null, null, PhotoEditorFailure.MemoryLimit) }
        catch (_: Exception) { PhotoEditorPreview(null, null, PhotoEditorFailure.Unavailable) }
        finally {
            if (raw !== oriented) raw?.recycle()
            if (!delivered) oriented?.recycle()
        }
    }

    /** Decode the selected source region before allocating paint/output bitmaps. */
    suspend fun renderFrame(context: Context, image: ProfileAvatar, recipe: PhotoEditRecipe, requested: PhotoQuality): PhotoEditorRender = withContext(Dispatchers.Default) {
        var decoded: Bitmap? = null
        var oriented: Bitmap? = null
        var output: Bitmap? = null
        try {
            val source = source(context, image) ?: return@withContext PhotoEditorRender(failure = PhotoEditorFailure.InvalidSource)
            if (recipe.strokes.size > PhotoEditing.MaximumStrokes || recipe.pointCount > PhotoEditing.MaximumTotalPoints || recipe.strokes.any { it.points.size > PhotoEditing.MaximumStrokePoints })
                return@withContext PhotoEditorRender(failure = PhotoEditorFailure.SaveFailed)
            if (recipe.isOriginal) {
                val clean = DraftPhotoProcessor.prepare(context, MessageAttachment("frame", MessageAttachmentKind.Photo, "Photo", images = listOf(image)), requested)
                return@withContext PhotoEditorRender(clean?.copy(sourceImages = listOf(image), photoEdits = mapOf(0 to recipe)),
                    failure = PhotoEditorFailure.SaveFailed.takeIf { clean == null })
            }
            val quality = PhotoEditing.effectiveQuality(requested, recipe)
            val crop = recipe.crop
            val corners = listOf(PhotoPoint(crop.left, crop.top), PhotoPoint(crop.right, crop.bottom)).map { inverseExif(it, source.orientation) }
            val region = Rect(
                floor(corners.minOf { it.x } * source.width).toInt().coerceIn(0, source.width - 1),
                floor(corners.minOf { it.y } * source.height).toInt().coerceIn(0, source.height - 1),
                ceil(corners.maxOf { it.x } * source.width).toInt().coerceIn(1, source.width),
                ceil(corners.maxOf { it.y } * source.height).toInt().coerceIn(1, source.height),
            )
            val ow = source.orientedWidth * crop.width
            val oh = source.orientedHeight * crop.height
            val scale = min(1f, min(quality.maxEdge.toFloat() / max(ow, oh), sqrt(PhotoEditing.MaximumOutputPixels / (ow.toDouble() * oh)).toFloat()))
            val width = (ow * scale).toInt().coerceAtLeast(1)
            val height = (oh * scale).toInt().coerceAtLeast(1)
            var sample = 1
            while (max(region.width(), region.height()) / (sample * 2) >= max(width, height) ||
                (region.width().toLong() / sample) * (region.height().toLong() / sample) > 24_000_000) sample *= 2
            val estimated = region.width().toLong() / sample * (region.height().toLong() / sample) * (if (source.orientation in 0..1) 4 else 8) + width.toLong() * height * 8 + source.bytes.size
            if (estimated > Runtime.getRuntime().maxMemory() * 3 / 4) return@withContext PhotoEditorRender(failure = PhotoEditorFailure.MemoryLimit)
            coroutineContext.ensureActive()
            @Suppress("DEPRECATION") // The byte-array overload supports the app's API 23 minimum.
            val decoder = BitmapRegionDecoder.newInstance(source.bytes, 0, source.bytes.size, false)
            try { decoded = decoder.decodeRegion(region, BitmapFactory.Options().apply { inSampleSize = sample }) }
            finally { decoder.recycle() }
            val raw = decoded ?: return@withContext PhotoEditorRender(failure = PhotoEditorFailure.Unavailable)
            oriented = orient(raw, source.orientation)
            val regionBitmap = checkNotNull(oriented)
            val turns = recipe.quarterTurns
            output = createBitmap(if (turns % 2 == 0) width else height, if (turns % 2 == 0) height else width)
            val canvas = Canvas(checkNotNull(output))
            canvas.withSave {
                rotate(canvas, turns, width.toFloat(), height.toFloat())
                drawBitmap(regionBitmap, null, RectF(0f, 0f, width.toFloat(), height.toFloat()), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                val fullWidth = width / crop.width; val fullHeight = height / crop.height
                translate(-crop.left * fullWidth, -crop.top * fullHeight)
                drawStrokes(canvas, recipe.strokes, fullWidth, fullHeight)
            }
            coroutineContext.ensureActive()
            val result = checkNotNull(output)
            val png = source.mimeType == "image/png" || regionBitmap.hasAlpha()
            val encoded = ByteArrayOutputStream().use { stream ->
                if (!result.compress(if (png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, quality.jpegQuality, stream)) return@withContext PhotoEditorRender(failure = PhotoEditorFailure.SaveFailed)
                stream.toByteArray()
            }
            if (encoded.size > DraftPhotoProcessor.MaximumInputBytes) return@withContext PhotoEditorRender(failure = PhotoEditorFailure.MemoryLimit)
            coroutineContext.ensureActive()
            PhotoEditorRender(MessageAttachment("frame", MessageAttachmentKind.Photo, "Photo", images = listOf(ProfileAvatar.DeviceImage(encoded)),
                sourceImages = listOf(image), photoEdits = mapOf(0 to recipe), photoQuality = requested,
                fileSizeBytes = encoded.size, pixelWidth = result.width, pixelHeight = result.height,
                mimeType = if (png) "image/png" else "image/jpeg", metadataPolicy = PhotoMetadataPolicy.Reencoded))
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: OutOfMemoryError) { PhotoEditorRender(failure = PhotoEditorFailure.MemoryLimit) }
        catch (_: Exception) { PhotoEditorRender(failure = PhotoEditorFailure.SaveFailed) }
        finally { output?.recycle(); if (oriented !== decoded) oriented?.recycle(); decoded?.recycle() }
    }

    /** Shared by the canvas preview and encoder: erase applies only to a separate annotation layer. */
    fun drawStrokes(canvas: Canvas, strokes: List<PhotoStroke>, width: Float, height: Float) {
        if (strokes.isEmpty()) return
        val layer = canvas.saveLayer(0f, 0f, width, height, null)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
        strokes.forEach { stroke ->
            paint.color = stroke.color.argb
            paint.strokeWidth = stroke.width.fraction * min(width, height)
            paint.xfermode = if (stroke.erase) PorterDuffXfermode(PorterDuff.Mode.CLEAR) else null
            val first = stroke.points.first()
            if (stroke.points.size == 1) canvas.drawPoint(first.x * width, first.y * height, paint)
            else {
                val path = Path().apply {
                    moveTo(first.x * width, first.y * height)
                    stroke.points.drop(1).forEach { lineTo(it.x * width, it.y * height) }
                }
                canvas.drawPath(path, paint)
            }
        }
        canvas.restoreToCount(layer)
    }

    fun drawPreview(canvas: Canvas, bitmap: Bitmap, recipe: PhotoEditRecipe, destination: RectF) {
        val crop = recipe.crop
        val width = bitmap.width * crop.width; val height = bitmap.height * crop.height
        val rotatedWidth = if (recipe.quarterTurns % 2 == 0) width else height
        val rotatedHeight = if (recipe.quarterTurns % 2 == 0) height else width
        canvas.withClip(destination) {
            translate(destination.left, destination.top)
            scale(destination.width() / rotatedWidth, destination.height() / rotatedHeight)
            rotate(canvas, recipe.quarterTurns, width, height)
            translate(-crop.left * bitmap.width, -crop.top * bitmap.height)
            drawBitmap(bitmap, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            drawStrokes(canvas, recipe.strokes, bitmap.width.toFloat(), bitmap.height.toFloat())
        }
    }

    private fun rotate(canvas: Canvas, turns: Int, width: Float, height: Float) {
        when (turns) { 1 -> { canvas.translate(height, 0f); canvas.rotate(90f) }; 2 -> { canvas.translate(width, height); canvas.rotate(180f) }; 3 -> { canvas.translate(0f, width); canvas.rotate(270f) } }
    }
    private fun inverseExif(p: PhotoPoint, orientation: Int): PhotoPoint = when (orientation) {
        2 -> PhotoPoint(1 - p.x, p.y); 3 -> PhotoPoint(1 - p.x, 1 - p.y); 4 -> PhotoPoint(p.x, 1 - p.y)
        5 -> PhotoPoint(p.y, p.x); 6 -> PhotoPoint(p.y, 1 - p.x); 7 -> PhotoPoint(1 - p.y, 1 - p.x); 8 -> PhotoPoint(1 - p.y, p.x)
        else -> p
    }
    private fun orient(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix().apply { when (orientation) {
            2 -> setScale(-1f, 1f); 3 -> setRotate(180f); 4 -> setScale(1f, -1f)
            5 -> { setRotate(90f); postScale(-1f, 1f) }; 6 -> setRotate(90f)
            7 -> { setRotate(-90f); postScale(-1f, 1f) }; 8 -> setRotate(-90f)
        } }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
