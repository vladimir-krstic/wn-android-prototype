package dev.ipf.whitenoise.ui.onboarding

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.math.roundToInt

object AvatarImageProcessor {
    private const val MaximumDimension = 512

    suspend fun prepare(
        contentResolver: ContentResolver,
        uri: Uri,
    ): ByteArray? = try {
        prepareImage(contentResolver, uri)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }

    private suspend fun prepareImage(
        contentResolver: ContentResolver,
        uri: Uri,
    ): ByteArray? = withContext(Dispatchers.IO) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsInput = contentResolver.openInputStream(uri) ?: return@withContext null
        boundsInput.use { input ->
            // Bounds-only decoding returns null even for a valid image; inspect the dimensions.
            BitmapFactory.decodeStream(input, null, bounds)
        }

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
        var sampleSize = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= MaximumDimension) {
            sampleSize *= 2
        }

        coroutineContext.ensureActive()
        val decoded = contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(
                input,
                null,
                BitmapFactory.Options().apply { inSampleSize = sampleSize },
            )
        } ?: return@withContext null

        coroutineContext.ensureActive()
        val oriented = orientBitmap(
            bitmap = decoded,
            orientation = contentResolver.openInputStream(uri)?.use { input ->
                ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            } ?: ExifInterface.ORIENTATION_NORMAL,
        )
        val targetSize = targetSize(oriented.width, oriented.height)
        val prepared = if (
            targetSize.width == oriented.width && targetSize.height == oriented.height
        ) {
            oriented
        } else {
            oriented.scale(
                targetSize.width,
                targetSize.height,
                true,
            )
        }

        ByteArrayOutputStream().use { output ->
            val succeeded = prepared.compress(Bitmap.CompressFormat.JPEG, 88, output)
            if (prepared !== oriented) prepared.recycle()
            if (oriented !== decoded) oriented.recycle()
            decoded.recycle()
            if (succeeded) output.toByteArray() else null
        }
    }

    internal fun targetSize(width: Int, height: Int): ImageSize {
        val largestDimension = max(width, height)
        if (largestDimension <= MaximumDimension) return ImageSize(width, height)
        val scale = MaximumDimension.toFloat() / largestDimension
        return ImageSize(
            width = (width * scale).roundToInt().coerceAtLeast(1),
            height = (height * scale).roundToInt().coerceAtLeast(1),
        )
    }

    private fun orientBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true,
        )
    }
}

data class ImageSize(val width: Int, val height: Int)
