package dev.ipf.whitenoise.ui.conversation

import android.content.ContentResolver
import android.net.Uri
import dev.ipf.whitenoise.ui.onboarding.AvatarImageProcessor

/** Message media needs enough detail for a full-screen preview, independent of avatar sizing. */
internal object ConversationImageProcessor {
    private const val MaximumDimension = 4096

    suspend fun prepare(contentResolver: ContentResolver, uri: Uri): ByteArray? =
        AvatarImageProcessor.prepare(
            contentResolver = contentResolver,
            uri = uri,
            maximumDimension = MaximumDimension,
            jpegQuality = 95,
            preservePng = true,
        )
}
