package dev.ipf.whitenoise

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.core.graphics.createBitmap
import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import dev.ipf.whitenoise.state.AppViewModel
import dev.ipf.whitenoise.ui.conversation.*
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(AndroidJUnit4::class)
class PhotoEditorTest {
    @get:Rule val rule = createAndroidComposeRule<EmptyTestActivity>()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun source(width: Int = 400, height: Int = 200, quadrants: Boolean = false): ProfileAvatar.DeviceImage {
        val bitmap = createBitmap(width,height).apply {
            eraseColor(Color.WHITE)
            if (quadrants) for (y in 0 until height) for (x in 0 until width) setPixel(x,y,when {
                x < width/2 && y < height/2 -> Color.RED; x >= width/2 && y < height/2 -> Color.GREEN
                x < width/2 -> Color.BLUE; else -> Color.YELLOW
            })
        }
        return try { ProfileAvatar.DeviceImage(ByteArrayOutputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it); it.toByteArray() }) }
        finally { bitmap.recycle() }
    }
    private fun decode(attachment: MessageAttachment): Bitmap {
        val bytes = (attachment.images.single() as ProfileAvatar.DeviceImage).bytes
        return BitmapFactory.decodeByteArray(bytes,0,bytes.size)!!
    }
    private fun model(scenario: PhotoEditorScenario = PhotoEditorScenario.Success): AppViewModel = AppViewModel().apply {
        completeSignIn(OnboardingOrigin.Initial)
        addDraftAttachments("fiatjaf",listOf(MessageAttachment("photo",MessageAttachmentKind.Photo,"Photo",images = listOf(source()))))
        openPhotoEditor(uiState.activeProfileId!!,"fiatjaf","photo",0)
        if (scenario != PhotoEditorScenario.Success) {
            // Ordinary public developer controls own failure injection.
            photoEditorAction(photoEditorSession!!.id,PhotoEditorEvent.Close)
            setDeveloperToolsEnabled(true)
            selectPhotoEditorScenario(scenario)
            openPhotoEditor(uiState.activeProfileId!!,"fiatjaf","photo",0)
        }
    }
    private fun show(vm: AppViewModel) {
        rule.setContent { WhiteNoiseTheme { vm.photoEditorSession?.let { state -> PhotoEditorDialog(state) { vm.photoEditorAction(state.id,it) } } } }
    }
    private fun ready(vm: AppViewModel) = rule.waitUntil(5_000) { vm.photoEditorSession?.phase == PhotoEditorPhase.Ready }
    @Test fun cropAndClockwiseRotationSaveTheSelectedPixelsAndActualDimensions() = runBlocking {
        val image = source(quadrants = true)
        val result = PhotoEditorRenderer.renderFrame(context,image,PhotoEditRecipe(PhotoCrop(.5f,0f,1f,1f),1),PhotoQuality.High).attachment!!
        assertEquals(200,result.pixelWidth); assertEquals(200,result.pixelHeight)
        val bitmap = decode(result)
        try { assertEquals(Color.YELLOW,bitmap.getPixel(30,100)); assertEquals(Color.GREEN,bitmap.getPixel(170,100)) } finally { bitmap.recycle() }
        assertEquals(listOf(image),result.sourceImages)
        assertEquals((result.images.single() as ProfileAvatar.DeviceImage).bytes.size,result.fileSizeBytes)
    }
    @Test fun eraseClearsInkWithoutErasingSourceAndNewInkCanCrossErasedRegion() = runBlocking {
        val image = source(400,400)
        val line = listOf(PhotoPoint(.1f,.5f),PhotoPoint(.9f,.5f))
        val recipe = PhotoEditRecipe(strokes = listOf(
            PhotoStroke(1,line,PhotoPenWidth.ExtraLarge,PhotoPenColor.Red),
            PhotoStroke(2,listOf(PhotoPoint(.5f,.2f),PhotoPoint(.5f,.8f)),PhotoPenWidth.ExtraLarge,PhotoPenColor.White,true),
            PhotoStroke(3,listOf(PhotoPoint(.5f,.5f)),PhotoPenWidth.Medium,PhotoPenColor.Blue)))
        val result = PhotoEditorRenderer.renderFrame(context,image,recipe,PhotoQuality.High).attachment!!
        val bitmap = decode(result)
        try { assertEquals(PhotoPenColor.Red.argb,bitmap.getPixel(100,200)); assertEquals(Color.WHITE,bitmap.getPixel(200,204)); assertEquals(PhotoPenColor.Blue.argb,bitmap.getPixel(200,200)); assertEquals(Color.WHITE,bitmap.getPixel(100,100)) } finally { bitmap.recycle() }
    }
    @Test fun qualityReprocessingReplaysRecipeFromOriginalAndClearsFrameOverrides() = runBlocking {
        val image = source(2400,1600)
        val recipe = PhotoEditRecipe(quarterTurns = 1,strokes = listOf(PhotoStroke(1,listOf(PhotoPoint(.5f,.5f)),PhotoPenWidth.ExtraLarge,PhotoPenColor.Red)))
        val rendered = PhotoEditorRenderer.renderFrame(context,image,recipe,PhotoQuality.Low).attachment!!.copy(photoFrameQualities = mapOf(0 to PhotoQuality.Low))
        assertEquals(1024,rendered.pixelHeight)
        val originalChoice = DraftPhotoProcessor.prepare(context,rendered,PhotoQuality.Original)!!
        assertEquals(2400,originalChoice.pixelHeight); assertEquals(1600,originalChoice.pixelWidth)
        assertEquals(recipe,originalChoice.photoEdits[0]); assertTrue(originalChoice.photoFrameQualities.isEmpty()); assertEquals(listOf(image),originalChoice.sourceImages)
        val bitmap = decode(originalChoice)
        try { assertEquals(PhotoPenColor.Red.argb,bitmap.getPixel(800,1200)) } finally { bitmap.recycle() }
    }
    @Test fun exifOrientationIsAppliedBeforeCropForEveryOrientation() = runBlocking {
        val bitmap = decode(MessageAttachment("image",MessageAttachmentKind.Photo,"Photo",images = listOf(source(quadrants = true))))
        val file = File.createTempFile("photo-editor-exif-",".jpg",context.cacheDir)
        try {
            for (orientation in 1..8) {
                file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG,100,it) }
                ExifInterface(file).apply { setAttribute(ExifInterface.TAG_ORIENTATION,orientation.toString()); saveAttributes() }
                val image = ProfileAvatar.DeviceImage(file.readBytes())
                val preview = PhotoEditorRenderer.preview(context,image)
                val result = PhotoEditorRenderer.renderFrame(context,image,PhotoEditRecipe(PhotoCrop(0f,0f,.5f,.5f)),PhotoQuality.High).attachment!!
                val output = decode(result); val shown = preview.bitmap!!
                try {
                    val expected = shown.getPixel(shown.width/4,shown.height/4); val actual = output.getPixel(output.width/2,output.height/2)
                    assertTrue("orientation $orientation", kotlin.math.abs(Color.red(expected)-Color.red(actual)) < 8 && kotlin.math.abs(Color.green(expected)-Color.green(actual)) < 8 && kotlin.math.abs(Color.blue(expected)-Color.blue(actual)) < 8)
                    assertEquals(if (orientation < 5) 200 else 100,result.pixelWidth); assertEquals(if (orientation < 5) 100 else 200,result.pixelHeight)
                } finally { output.recycle(); shown.recycle() }
            }
        } finally { bitmap.recycle(); file.delete() }
    }
    @Test fun corruptAndAnimatedSourcesHaveExplicitFailures() = runBlocking {
        assertEquals(PhotoEditorFailure.InvalidSource,PhotoEditorRenderer.preview(context,ProfileAvatar.DeviceImage(byteArrayOf(1,2,3))).failure)
        val gif = context.resources.openRawResource(R.raw.chat_animation).use { it.readBytes() }
        assertEquals(PhotoEditorFailure.InvalidSource,PhotoEditorRenderer.renderFrame(context,ProfileAvatar.DeviceImage(gif),PhotoEditRecipe(quarterTurns = 1),PhotoQuality.High).failure)
    }
    @Test fun closeAsksToDiscardAndKeepEditingRetainsUndoState() {
        val vm = model(); val before = vm.chat("fiatjaf")!!.draftAttachments; show(vm); ready(vm)
        rule.onNodeWithText("Rotate clockwise").performClick(); rule.onNodeWithContentDescription("Close").performClick()
        rule.onNodeWithText("Discard changes?").assertExists(); rule.onNodeWithText("Keep editing").performClick()
        rule.onNodeWithText("Undo").assertIsEnabled(); rule.onNodeWithContentDescription("Close").performClick(); rule.onNodeWithText("Discard").performClick()
        rule.runOnIdle { assertNull(vm.photoEditorSession); assertEquals(before,vm.chat("fiatjaf")!!.draftAttachments) }
    }
    @Test fun loadFailureRetryAndSaveFailureRetryPreserveEdits() {
        val vm = model(PhotoEditorScenario.LoadFailure); show(vm)
        rule.waitUntil(5_000) { vm.photoEditorSession?.phase == PhotoEditorPhase.Failed }
        rule.onNodeWithText("Retry").performClick(); ready(vm)
        rule.onNodeWithText("Rotate clockwise").performClick()
        rule.runOnIdle {
            val id = vm.photoEditorSession!!.id; vm.photoEditorAction(id,PhotoEditorEvent.Save)
            vm.photoEditorAction(id,PhotoEditorEvent.Saved(vm.photoEditorSession!!.revision,null,PhotoEditorFailure.SaveFailed))
        }
        rule.onNodeWithText("Couldn’t save this photo. Your changes are still here.").assertExists()
        rule.onNodeWithText("Retry").performClick(); rule.waitUntil(5_000) { vm.photoEditorSession == null }
        rule.runOnIdle { assertEquals(1,vm.chat("fiatjaf")!!.draftAttachments.single().photoEdits[0]!!.quarterTurns) }
    }
    @Test fun accessibleLineAndCropActionsSupportUndoRedoWithoutCanvasGestures() {
        val vm = model(); show(vm); ready(vm)
        rule.onNodeWithTag("photo.editor.tool.Draw").performClick(); rule.onNodeWithText("Draw a line").performScrollTo().performClick()
        rule.onNodeWithTag("photo.editor.coordinates.apply").performClick(); rule.runOnIdle { assertEquals(1,vm.photoEditorSession!!.history.current.strokes.size) }
        rule.onNodeWithText("Undo").performClick(); rule.runOnIdle { assertTrue(vm.photoEditorSession!!.history.current.strokes.isEmpty()) }
        rule.onNodeWithText("Redo").performClick(); rule.onNodeWithTag("photo.editor.tool.Crop").performClick()
        rule.onNodeWithText("Adjust crop").performScrollTo().performClick()
        rule.onNodeWithTag("photo.editor.coordinate.${R.string.photo_editor_left}").performSemanticsAction(SemanticsActions.SetProgress) { it(.2f) }
        rule.onNodeWithTag("photo.editor.coordinates.apply").performClick()
        rule.runOnIdle { assertEquals(.2f,vm.photoEditorSession!!.history.current.crop.left,.001f) }
    }
    @Test fun albumReviewTargetsTheExactFrameAndKeepsExclusionAfterAnEdit() {
        val first = source(); val second = source(quadrants = true)
        var attachments by mutableStateOf(listOf(MessageAttachment("album",MessageAttachmentKind.Photos,"Trip",images = listOf(first,second))))
        var editedIndex = -1; var excluded = emptySet<String>()
        rule.setContent { WhiteNoiseTheme { DraftMediaViewer(attachments,"album",{ id,index ->
            assertEquals("album",id); editedIndex = index
            attachments = attachments.map { it.copy(images = it.images.mapIndexed { n,image -> if (n == index) source(100,100) else image }) }
        }, {}, { excluded = it }) } }
        rule.onAllNodesWithTag("conversation.media.thumbnail.target")[1].performClick()
        rule.onAllNodesWithTag("conversation.media.inclusion.target").onLast().performClick()
        rule.onNodeWithTag("conversation.media.edit").performClick(); rule.onNodeWithText("Done").performClick()
        rule.runOnIdle { assertEquals(1,editedIndex); assertEquals(setOf("album"),excluded); assertEquals(first,attachments.single().images[0]) }
    }
}
