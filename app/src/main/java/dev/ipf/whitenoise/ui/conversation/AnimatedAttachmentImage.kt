package dev.ipf.whitenoise.ui.conversation

import android.animation.ValueAnimator
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import androidx.core.graphics.drawable.toDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.ImageView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.MessageAttachment
import dev.ipf.whitenoise.model.ProfileAvatar
import dev.ipf.whitenoise.model.bytesAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

@Composable
internal fun AnimatedAttachmentImage(attachment: MessageAttachment, modifier: Modifier = Modifier, active: Boolean = true) {
    val resources = LocalResources.current
    val owner = LocalLifecycleOwner.current
    var finished by remember(attachment.id, attachment.images, attachment.bytesAvailable) { mutableStateOf(false) }
    val drawable by produceState<Drawable?>(null, attachment.id, attachment.images, attachment.bytesAvailable, resources) {
        value = if (!attachment.bytesAvailable) null else withContext(Dispatchers.IO) {
            val bytes = (attachment.images.firstOrNull() as? ProfileAvatar.DeviceImage)?.bytes
                ?: resources.openRawResource(R.raw.chat_animation).use { it.readBytes() }
            if (bytes.size > DraftPhotoProcessor.MaximumInputBytes) null else runCatching {
                if (Build.VERSION.SDK_INT >= 28) ImageDecoder.decodeDrawable(ImageDecoder.createSource(ByteBuffer.wrap(bytes))) { decoder, info, _ ->
                    val scale = (512f / maxOf(info.size.width, info.size.height)).coerceAtMost(1f)
                    decoder.setTargetSize((info.size.width * scale).toInt().coerceAtLeast(1), (info.size.height * scale).toInt().coerceAtLeast(1))
                } else {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                    var sample = 1
                    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 512) sample *= 2
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })?.toDrawable(resources)
                }
            }.getOrNull()
        }
        finished = true
    }
    val description = stringResource(if (drawable == null) R.string.attachment_gif_unavailable else if (Build.VERSION.SDK_INT >= 28 && drawable is AnimatedImageDrawable) R.string.attachment_gif_description else R.string.attachment_gif_static)
    DisposableEffect(drawable, owner, active) {
        val animated = if (Build.VERSION.SDK_INT >= 28) drawable as? AnimatedImageDrawable else null
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) { if (active && Build.VERSION.SDK_INT >= 28 && ValueAnimator.areAnimatorsEnabled()) animated?.start() }
            override fun onStop(owner: LifecycleOwner) { if (Build.VERSION.SDK_INT >= 28) animated?.stop() }
        }
        owner.lifecycle.addObserver(observer)
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) observer.onStart(owner)
        onDispose { owner.lifecycle.removeObserver(observer); if (Build.VERSION.SDK_INT >= 28) animated?.stop() }
    }
    Box(modifier) {
        if (!finished) androidx.compose.material3.CircularProgressIndicator()
        else if (drawable == null) Text(description) else AndroidView(factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.FIT_CENTER } },
            update = { it.setImageDrawable(drawable); it.contentDescription = description }, modifier = Modifier.fillMaxSize())
    }
}
