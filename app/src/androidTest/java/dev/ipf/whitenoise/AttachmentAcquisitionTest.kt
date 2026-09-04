package dev.ipf.whitenoise

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.core.graphics.createBitmap
import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.ui.conversation.*
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer

@RunWith(AndroidJUnit4::class)
class AttachmentAcquisitionTest {
    @get:Rule val rule = createAndroidComposeRule<EmptyTestActivity>()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun contactPreviewOmitsUncheckedPhoneAndNeverAddsOnCancel() {
        var result: SharedDeviceContact? = null; var dismissed = false
        rule.setContent { WhiteNoiseTheme { DeviceContactPreview(SharedDeviceContact("Ada", "123", "ada@example.com"), { dismissed = true }, { result = it }) } }
        rule.onNodeWithText("123").performClick(); rule.onNodeWithTag("contact.preview.add").performClick()
        rule.runOnIdle { assertNull(result!!.phone); assertEquals("ada@example.com", result!!.email); result = null }
        rule.onNodeWithText("Cancel").performClick(); rule.runOnIdle { assertTrue(dismissed); assertNull(result) }
    }
    @Test fun contactCannotQueueAnEmptyCard() {
        rule.setContent { WhiteNoiseTheme { DeviceContactPreview(SharedDeviceContact("Ada"), {}, {}) } }
        rule.onNodeWithText("Ada").performClick(); rule.onNodeWithTag("contact.preview.add").assertIsNotEnabled()
    }
    @Test fun recentAccessChangesVisibleItemsAndKeepsGalleryAvailable() {
        var access by mutableStateOf(RecentMediaAccess.None); var gallery = 0; var selected: ProfileAvatar? = null
        rule.setContent { WhiteNoiseTheme { RecentMediaSheet(access, {}, { gallery++ }, { selected = it }) } }
        rule.onNodeWithText("No recent media selected.").assertExists(); rule.onNodeWithTag("recent.media.gallery").performClick()
        rule.runOnIdle { access = RecentMediaAccess.SelectedOnly; assertEquals(1, gallery) }
        rule.onNodeWithTag("recent.media.Fox").assertDoesNotExist(); rule.onNodeWithTag("recent.media.Marmot").performClick()
        rule.runOnIdle { assertEquals(ProfileAvatar.Asset(AvatarAsset.Marmot), selected); access = RecentMediaAccess.Unavailable }
        rule.onNodeWithTag("recent.media.Marmot").assertDoesNotExist(); rule.onNodeWithTag("recent.media.gallery").assertIsEnabled()
    }
    @Test fun qualityPickerReportsChoiceAndCancellationDoesNotChangeIt() {
        var chosen = PhotoQuality.High
        rule.setContent { WhiteNoiseTheme { PhotoQualityDialog(PhotoQuality.High, {}, { chosen = it }) } }
        rule.onNodeWithTag("photo.quality.Original").performClick(); rule.runOnIdle { assertEquals(PhotoQuality.Original, chosen) }
        rule.onNodeWithText("Cancel").performClick(); rule.runOnIdle { assertEquals(PhotoQuality.Original, chosen) }
    }
    @Test fun transferControlsDispatchStableIdentityAndRevision() {
        var state by mutableStateOf(AttachmentTransfer(revision = 12)); var event = ""
        rule.setContent { WhiteNoiseTheme { CompositionLocalProvider(LocalAttachmentEnvironment provides AttachmentEnvironment(transfer = { message, attachment, action, revision ->
            assertEquals("message", message); assertEquals("attachment", attachment); assertEquals(state.revision, revision)
            event = action; state = if (action == "cancel") state.cancel() else state.retry()
        })) { AttachmentTransferControls("message", MessageAttachment("attachment", MessageAttachmentKind.File, "File", transfer = state)) } } }
        rule.onNodeWithText("Cancel").performClick(); rule.onNodeWithText("Transfer cancelled").assertExists()
        rule.onNodeWithText("Retry").performClick(); rule.runOnIdle { assertEquals("retry", event); assertEquals(14L, state.revision) }
    }
    @Test fun exportReportsUnknownFileInsteadOfSubstitutingSampleContent() = runBlocking {
        val missing = MessageAttachment("missing", MessageAttachmentKind.File, "Unknown.docx")
        assertNull(exportAttachment(context, missing, AttachmentExportKey(missing.id)))
        val contact = SharedDeviceContact("Ada", "123").attachment("contact")!!
        val exported = exportAttachment(context, contact, AttachmentExportKey(contact.id))!!
        try { assertEquals("text/vcard", exported.mimeType); assertEquals(contact.deviceContact!!.vCard(), exported.file.readText()) }
        finally { exported.file.delete() }
    }
    @Test fun mixedExportKeepsAvailableRowsAndExplainsMissingBytes() {
        val message = ChatMessage("mixed", "p", 1, "Today", 1, "Now", attachments = listOf(
            SharedDeviceContact("Ada", "123").attachment("contact")!!,
            MessageAttachment("missing", MessageAttachmentKind.File, "Unknown.docx")))
        rule.setContent { WhiteNoiseTheme { MessageAttachmentExportSheet(message, true, onDismiss = {}) } }
        rule.waitUntil(3_000) { rule.onAllNodesWithText("Attachment unavailable").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("attachment.export.share").performClick()
        rule.onNodeWithText("Some attachments are unavailable. Share the available items?").assertExists()
        rule.onNodeWithText("Cancel").performClick(); rule.onNodeWithTag("attachment.export.missing.0").assertExists()
    }
    @Test fun photoQualityUsesOriginalSourceAndReportsRealEncodedBytes() = runBlocking {
        val bitmap = createBitmap(2400, 1600).apply { eraseColor(Color.WHITE); setPixel(100, 100, Color.BLACK) }
        val bytes = ByteArrayOutputStream().use { stream -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream); stream.toByteArray() }
        bitmap.recycle()
        val source = MessageAttachment("photo", MessageAttachmentKind.Photo, "Photo", images = listOf(ProfileAvatar.DeviceImage(bytes)))
        val low = DraftPhotoProcessor.prepare(context, source, PhotoQuality.Low)!!
        assertEquals(1024, low.pixelWidth); assertEquals(low.images.single().let { (it as ProfileAvatar.DeviceImage).bytes.size }, low.fileSizeBytes)
        val original = DraftPhotoProcessor.prepare(context, low, PhotoQuality.Original)!!
        assertEquals(2400, original.pixelWidth); assertEquals(1600, original.pixelHeight)
        assertEquals(PhotoMetadataPolicy.StrippedOriginal, original.metadataPolicy)
        val pixels = (original.images.single() as ProfileAvatar.DeviceImage).bytes
        val decoded = BitmapFactory.decodeByteArray(pixels, 0, pixels.size)!!
        try { assertEquals(Color.BLACK, decoded.getPixel(100,100)) } finally { decoded.recycle() }
    }
    @Test fun originalJpegStripsGpsAndCameraAndRotatedSourceUsesSafeFallback() = runBlocking {
        val file = File.createTempFile("photo-metadata-", ".jpg", context.cacheDir)
        val bitmap = createBitmap(60, 40).apply { eraseColor(Color.WHITE) }
        try {
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
            ExifInterface(file).apply { setLatLong(44.8,20.4); setAttribute(ExifInterface.TAG_MAKE,"Private camera"); saveAttributes() }
            fun attachment() = MessageAttachment("photo", MessageAttachmentKind.Photo, "Photo", images = listOf(ProfileAvatar.DeviceImage(file.readBytes())))
            val original = DraftPhotoProcessor.prepare(context, attachment(), PhotoQuality.Original)!!
            val exif = ExifInterface((original.images.single() as ProfileAvatar.DeviceImage).bytes.inputStream())
            assertNull(exif.latLong); assertNull(exif.getAttribute(ExifInterface.TAG_MAKE))
            ExifInterface(file).apply { setAttribute(ExifInterface.TAG_ORIENTATION,"6"); saveAttributes() }
            val rotated = DraftPhotoProcessor.prepare(context, attachment(), PhotoQuality.Original)!!
            assertEquals(PhotoMetadataPolicy.SafeFallback,rotated.metadataPolicy); assertEquals(40,rotated.pixelWidth);assertEquals(60,rotated.pixelHeight)
        } finally { bitmap.recycle();file.delete() }
    }
    @Test fun bundledGifDecodesAsAnimatedAndInvalidContentIsRejected() {
        val source = context.resources.openRawResource(R.raw.chat_animation).use { it.readBytes() }
        val stripped = PhotoMetadata.strippedGif(source)!!
        if (Build.VERSION.SDK_INT >= 28) {
            val decoded = ImageDecoder.decodeDrawable(ImageDecoder.createSource(ByteBuffer.wrap(stripped)))
            assertTrue(decoded is AnimatedImageDrawable)
        } else assertNotNull(BitmapFactory.decodeByteArray(stripped,0,stripped.size))
        assertNull(PhotoMetadata.strippedGif("invalid".toByteArray()))
    }
    @Test fun uploadContinuesAfterNavigatingToSettings() {
        val vm=dev.ipf.whitenoise.state.AppViewModel().apply { completeSignIn(dev.ipf.whitenoise.navigation.OnboardingOrigin.Initial);dismissDiagnosticsPrompt(uiState.activeProfileId!!) }
        val chatId="fiatjaf"
        vm.addDraftAttachments(chatId,listOf(MessageAttachment("upload",MessageAttachmentKind.Photo,"Photo",images=listOf(ProfileAvatar.Asset(AvatarAsset.Fox)))))
        vm.sendDraft(chatId)
        val message=vm.chat(chatId)!!.timeline.filterIsInstance<ChatTimelineEntry.Message>().last().message
        lateinit var nav: androidx.navigation.NavHostController
        rule.setContent { nav=androidx.navigation.compose.rememberNavController();WhiteNoiseTheme { dev.ipf.whitenoise.navigation.WhiteNoiseNavHost(nav,vm) } }
        rule.runOnIdle { nav.navigate(dev.ipf.whitenoise.navigation.AppRoute.Settings) }
        rule.waitUntil(4_000) { vm.message(chatId,message.id)!!.attachments.single().transfer!!.phase==AttachmentTransferPhase.Available }
        rule.runOnIdle { assertEquals("upload",vm.message(chatId,message.id)!!.attachments.single().id) }
    }

}
