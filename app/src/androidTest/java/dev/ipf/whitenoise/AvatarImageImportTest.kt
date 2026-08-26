package dev.ipf.whitenoise

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
class AvatarImageImportTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @get:Rule
    val temporaryFiles = TemporaryFolder(
        File(context.cacheDir, "shared").apply { check(isDirectory || mkdirs()) },
    )

    @Test
    fun pngContentUriContinuesPastTheBoundsOnlyDecode() = runBlocking {
        val source = imageFile(Bitmap.CompressFormat.PNG, width = 1024, height = 512)

        assertPreparedSize(source, width = 512, height = 256)
    }

    @Test
    fun jpegContentUriPreservesSmallImageDimensions() = runBlocking {
        val source = imageFile(Bitmap.CompressFormat.JPEG, width = 120, height = 80)

        assertPreparedSize(source, width = 120, height = 80)
    }

    @Test
    fun jpegOrientationIsAppliedBeforeReturningTheAvatar() = runBlocking {
        val source = imageFile(Bitmap.CompressFormat.JPEG, width = 320, height = 160)
        ExifInterface(source).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            saveAttributes()
        }

        assertPreparedSize(source, width = 160, height = 320)
    }

    @Test
    fun invalidImageStillReturnsFailure() = runBlocking {
        val source = temporaryFiles.newFile("invalid.png").apply { writeText("not an image") }

        assertNull(AvatarImageProcessor.prepare(context.contentResolver, contentUri(source)))
    }

    private fun imageFile(format: Bitmap.CompressFormat, width: Int, height: Int): File {
        val source = temporaryFiles.newFile(if (format == Bitmap.CompressFormat.PNG) "image.png" else "image.jpg")
        val bitmap = createBitmap(width, height)
        try {
            bitmap.eraseColor(Color.GRAY)
            source.outputStream().use { output ->
                assertTrue(bitmap.compress(format, 100, output))
            }
        } finally {
            bitmap.recycle()
        }
        return source
    }

    private suspend fun assertPreparedSize(source: File, width: Int, height: Int) {
        val bytes = checkNotNull(AvatarImageProcessor.prepare(context.contentResolver, contentUri(source)))
        val bitmap = checkNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
        try {
            assertEquals(width, bitmap.width)
            assertEquals(height, bitmap.height)
        } finally {
            bitmap.recycle()
        }
    }

    private fun contentUri(file: File) =
        FileProvider.getUriForFile(context, "${context.packageName}.files", file)
}
