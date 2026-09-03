package dev.ipf.whitenoise.ui.conversation

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import dev.ipf.whitenoise.model.ProfileAvatar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Keep full-quality encoded media in state; only decode pixels needed by this presentation. */
@Composable
internal fun DeviceMediaImage(
    image: ProfileAvatar.DeviceImage,
    modifier: Modifier,
    contentScale: ContentScale,
) {
    var targetSize by remember { mutableStateOf(IntSize.Zero) }
    val bitmap by produceState<ImageBitmap?>(null, image, targetSize) {
        value = null
        if (targetSize == IntSize.Zero) return@produceState
        value = withContext(Dispatchers.Default) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(image.bytes, 0, image.bytes.size, bounds)
                val options = BitmapFactory.Options().apply {
                    inSampleSize = mediaImageSampleSize(
                        bounds.outWidth,
                        bounds.outHeight,
                        targetSize.width,
                        targetSize.height,
                    )
                }
                BitmapFactory.decodeByteArray(image.bytes, 0, image.bytes.size, options)?.asImageBitmap()
            }.getOrNull()
        }
    }
    Box(modifier.onSizeChanged { targetSize = it }) {
        bitmap?.let {
            Image(it, null, Modifier.fillMaxSize(), contentScale = contentScale)
        } ?: Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant))
    }
}

/** Never sample below either displayed dimension, including cropped and portrait thumbnails. */
internal fun mediaImageSampleSize(width: Int, height: Int, targetWidth: Int, targetHeight: Int): Int {
    if (width <= 0 || height <= 0 || targetWidth <= 0 || targetHeight <= 0) return 1
    var sample = 1
    while (width / (sample * 2L) >= targetWidth && height / (sample * 2L) >= targetHeight) {
        sample *= 2
    }
    return sample
}
