package dev.ipf.whitenoise

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.ipf.whitenoise.ui.conversation.ConversationImageProcessor
import dev.ipf.whitenoise.ui.onboarding.AvatarImageProcessor
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConversationImageImportTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @get:Rule
    val temporaryFiles = TemporaryFolder(
        File(context.cacheDir, "shared").apply { check(isDirectory || mkdirs()) },
    )

    @Test
    fun screenshotRetainsFullResolutionAndExactPixelsWhileAvatarsStaySmall() = runBlocking {
        val source = imageFile(1080, 2400, Bitmap.CompressFormat.PNG)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", source)
        val media = checkNotNull(ConversationImageProcessor.prepare(context.contentResolver, uri))
        val bitmap = checkNotNull(BitmapFactory.decodeByteArray(media, 0, media.size))
        try {
            assertEquals(1080, bitmap.width)
            assertEquals(2400, bitmap.height)
            assertEquals(Color.BLACK, bitmap.getPixel(100, 100))
            assertEquals(Color.WHITE, bitmap.getPixel(101, 100))
        } finally {
            bitmap.recycle()
        }
        val avatar = checkNotNull(AvatarImageProcessor.prepare(context.contentResolver, uri))
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(avatar, 0, avatar.size, bounds)
        assertEquals(512, bounds.outHeight)
    }

    @Test
    fun largePhotoIsBoundedAndExifOrientationIsApplied() = runBlocking {
        val source = imageFile(8192, 1024, Bitmap.CompressFormat.JPEG)
        ExifInterface(source).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            saveAttributes()
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", source)
        val media = checkNotNull(ConversationImageProcessor.prepare(context.contentResolver, uri))
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(media, 0, media.size, bounds)
        assertEquals(512, bounds.outWidth)
        assertEquals(4096, bounds.outHeight)
    }

    @Test
    fun unreadableSelectionReturnsTheExistingImportFailure() = runBlocking {
        val source = temporaryFiles.newFile("broken.png").apply { writeText("not an image") }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", source)
        assertNull(ConversationImageProcessor.prepare(context.contentResolver, uri))
    }

    private fun imageFile(width: Int, height: Int, format: Bitmap.CompressFormat): File {
        val source = temporaryFiles.newFile("selected-image")
        val bitmap = createBitmap(width, height)
        try {
            bitmap.eraseColor(Color.WHITE)
            bitmap.setPixel(100, 100, Color.BLACK)
            source.outputStream().use { assertTrue(bitmap.compress(format, 100, it)) }
        } finally {
            bitmap.recycle()
        }
        return source
    }
}
