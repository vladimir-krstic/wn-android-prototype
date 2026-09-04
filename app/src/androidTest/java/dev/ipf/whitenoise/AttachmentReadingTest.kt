package dev.ipf.whitenoise

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.core.content.FileProvider
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
import java.io.File
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class AttachmentReadingTest {
    @get:Rule val rule=createAndroidComposeRule<EmptyTestActivity>()
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private val profile=ProfileFixtures.marmota
    private fun attachment(source: AttachmentLocalSource) = AttachmentReadingExamples.attachments().first { it.localSource == source }
    private fun message(file: MessageAttachment)=ChatMessage("file-message",profile.id,1,"Today",1,"Now",attachments=listOf(file))
    private fun show(file: MessageAttachment,scenario: AttachmentAccessScenario=AttachmentAccessScenario.Success) {
        rule.setContent { WhiteNoiseTheme { AttachmentReaderDialog(profile,message(file),file,file,scenario,{}) } }
    }
    @Test fun plainTextLoadsAndCopiesTheCompleteFileAndFullFilename() {
        val file=attachment(AttachmentLocalSource.PlainText).copy(label="A very long meeting filename that remains available for copying with all of its original details.txt")
        show(file)
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("attachment.reader.body").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("attachment.reader.copy").performClick()
        rule.runOnIdle { assertTrue((context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip!!.getItemAt(0).text.toString().contains("Bring water")) }
        rule.onNodeWithText("View full filename").performClick();rule.onNodeWithTag("attachment.reader.full.filename").assertTextEquals(file.label)
        rule.onNodeWithText("Copy").performClick()
        rule.runOnIdle { assertEquals(file.label,(context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip!!.getItemAt(0).text.toString()) }
    }
    @Test fun loadFailureRetryAndEmptyCopyStateAreRealTransitions() {
        show(attachment(AttachmentLocalSource.EmptyText),AttachmentAccessScenario.LoadFailure)
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("attachment.reader.retry").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("attachment.reader.retry").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("This file is empty.").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("attachment.reader.copy").assertIsNotEnabled();rule.onNodeWithTag("attachment.reader.more").performClick();rule.onNodeWithTag("attachment.reader.external").assertIsEnabled()
    }
    @Test fun sourceRemovalRevokesCopyAndProfileChangeClosesTheReader() {
        val file=attachment(AttachmentLocalSource.PlainText)
        var currentProfile by mutableStateOf(profile)
        var chat by mutableStateOf(profile.chats.first().copy(timeline=listOf(ChatTimelineEntry.Message(message(file)))))
        rule.setContent { WhiteNoiseTheme { AttachmentReaderScope(currentProfile,chat) {
            val actions=LocalAttachmentAccess.current
            Button({ actions.open?.invoke("file-message",file) },Modifier.testTag("open.file")) { Text("Open file") }
        } } }
        rule.onNodeWithTag("open.file").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("attachment.reader.body").fetchSemanticsNodes().isNotEmpty() }
        rule.runOnIdle { chat=chat.copy(timeline=emptyList()) }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("This file is no longer available. Return to the chat to open it again.").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("attachment.reader.copy").assertIsNotEnabled()
        rule.runOnIdle { currentProfile=profile.copy(id="other-profile") }
        rule.onNodeWithTag("attachment.reader").assertDoesNotExist()
    }
    @Test fun formattedPreviewShowsTruncationWhileCopyKeepsAllSource() {
        val file=attachment(AttachmentLocalSource.LongMarkdown);show(file)
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Showing part of this file. Copy keeps the full text; open the file in another app to view everything.").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("attachment.reader.copy").performClick()
        val expected=runBlocking { AttachmentSources.readText(context,file) } as TextAttachmentResult.Ready
        rule.runOnIdle { assertEquals(expected.text,(context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip!!.getItemAt(0).text.toString()) }
    }
    @Test fun nativeFileReadsStayWithinThePreviewBudgetAndRecognizeInvalidEncoding() = runBlocking {
        val file=File(File(context.cacheDir,"shared").apply { mkdirs() },"bounded-reader.txt")
        try {
            file.writeBytes(ByteArray(TextAttachments.MaximumBytes+8192) { 65 })
            val uri=FileProvider.getUriForFile(context,"${context.packageName}.files",file)
            val selected=MessageAttachment("bounded",MessageAttachmentKind.File,"bounded-reader.txt",externalUri=uri.toString(),fileSizeBytes=1)
            assertEquals(TextAttachmentResult.Failed(TextAttachmentFailure.TooLarge),AttachmentSources.readText(context,selected))
            assertEquals(TextAttachmentResult.Failed(TextAttachmentFailure.InvalidEncoding),AttachmentSources.readText(context,attachment(AttachmentLocalSource.InvalidEncoding)))
            assertEquals(TextAttachmentResult.Failed(TextAttachmentFailure.Binary),AttachmentSources.readText(context,attachment(AttachmentLocalSource.BinaryText)))
        } finally { file.delete() }
    }
    @Test fun packageArchiveValidationAndDispatchGuardNeverLaunchAnInstaller() = runBlocking {
        val valid=attachment(AttachmentLocalSource.AndroidPackage);val invalid=attachment(AttachmentLocalSource.InvalidPackage)
        val good=exportAttachment(context,valid,AttachmentExportKey(valid.id))!!
        val bad=exportAttachment(context,invalid,AttachmentExportKey(invalid.id))!!
        try { assertTrue(AttachmentSources.validPackage(good.file));assertFalse(AttachmentSources.validPackage(bad.file)) }
        finally { good.file.delete();bad.file.delete() }
        assertEquals(ExternalFileResult.PackageBlocked,openExternalAttachment(context,valid) { true })
        assertEquals(ExternalFileResult.Unavailable,openExternalAttachment(context,attachment(AttachmentLocalSource.PlainText)) { false })
    }
    @Test fun packagePermissionStateHasSaveAndCloseWithoutAnInstallAction() {
        show(attachment(AttachmentLocalSource.AndroidPackage),AttachmentAccessScenario.PackagePermission)
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Installation permission is required to open this Android app.").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Save file").assertIsEnabled();rule.onNodeWithText("Install").assertDoesNotExist()
    }
    @Test fun mediaFiltersRetainAlbumFramesAndUnavailableVideoWithoutChangingSources() {
        val photo=MessageAttachment("photos",MessageAttachmentKind.Photos,"Trip",images=listOf(ProfileAvatar.Asset(AvatarAsset.Fox),ProfileAvatar.Asset(AvatarAsset.Marmot)))
        val video=MessageAttachment("video",MessageAttachmentKind.Video,"Video",isAvailable=false)
        val source=message(photo).copy(attachments=listOf(photo,video),createdAtMillis=Instant.parse("2026-07-10T12:00:00Z").toEpochMilli())
        val chat=profile.chats.first().copy(timeline=listOf(ChatTimelineEntry.Message(source)));var jumped=""
        rule.setContent { WhiteNoiseTheme { SharedContentScreen(profile,chat,SharedContentCategory.Media,{},onGoToMessage={ jumped=it }) } }
        rule.onNodeWithTag("shared.filter.Videos").performClick()
        rule.onNodeWithTag("conversation.shared.media.${ConversationMediaKey(source.id,photo.id,0).stableId}").assertDoesNotExist()
        rule.onNodeWithText("Media unavailable").performClick();rule.runOnIdle { assertEquals(source.id,jumped) }
        rule.onNodeWithTag("shared.filter.Images").performClick()
        rule.onNodeWithTag("conversation.shared.media.${ConversationMediaKey(source.id,photo.id,1).stableId}").assertExists()
    }
    @Test fun missingAudioExplainsFailureAndKeepsSourceNavigation() {
        val file=MessageAttachment("missing-audio",MessageAttachmentKind.Voice,"Voice message")
        val chat=profile.chats.first().copy(timeline=listOf(ChatTimelineEntry.Message(message(file))))
        val item=SharedContentProjection.items(chat,profile,SharedContentCategory.Voice).single()
        rule.setContent { WhiteNoiseTheme { SharedContentScreen(profile,chat,SharedContentCategory.Voice,{}) } }
        rule.onNodeWithTag("shared.voice.play.${item.id}").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Couldn’t play this audio. Try again.").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("shared.message.${item.id}").assertIsEnabled();rule.onNodeWithText("Retry").assertIsEnabled()
    }
    @Test fun nativeAudioRejectsStaleLoadThenSupportsPauseAndSeek() {
        val file=attachment(AttachmentLocalSource.AudioClip)
        val exported=runBlocking { exportAttachment(context,file,AttachmentExportKey(file.id))!! }
        lateinit var audio: LibraryAudioController
        rule.setContent { WhiteNoiseTheme { Text("Audio") } }
        rule.runOnIdle {
            audio=LibraryAudioController(context)
            val first=audio.begin("first");val second=audio.begin("second")
            assertFalse(audio.ready(first,exported.file.toURI().toString()))
            assertTrue(audio.ready(second,exported.file.toURI().toString()))
        }
        try {
            rule.waitUntil(10_000) { audio.state.durationMillis > 0 }
            rule.runOnIdle { audio.pause();audio.seek(2000);assertTrue(audio.state.positionMillis in 1900..2100);assertNotEquals(LibraryAudioPhase.Playing,audio.state.phase) }
        } finally { rule.runOnIdle { audio.close() };exported.file.delete() }
    }
    @Test fun gifViewerExportPreservesAnimationBytesAndStaleShareDoesNotDispatch() = runBlocking {
        val file=MessageAttachment("gif",MessageAttachmentKind.Gif,"Animation")
        val chat=profile.chats.first().copy(timeline=listOf(ChatTimelineEntry.Message(message(file))))
        val item=ConversationMediaProjection.items(chat,profile).single()
        val output=java.io.ByteArrayOutputStream()
        copyMedia(context,item,output)
        assertArrayEquals(context.resources.openRawResource(R.raw.chat_animation).use { it.readBytes() },output.toByteArray())
        assertEquals("image/gif",item.mimeType)
        assertFalse(shareMedia(context,item) { false })
    }
    @Test fun changedAudioSourceReleasesTheOldPlaybackControls() {
        val file=attachment(AttachmentLocalSource.AudioClip)
        var chat by mutableStateOf(profile.chats.first().copy(timeline=listOf(ChatTimelineEntry.Message(message(file)))))
        val key=SharedContentProjection.items(chat,profile,SharedContentCategory.Voice).single().id
        rule.setContent { WhiteNoiseTheme { SharedContentScreen(profile,chat,SharedContentCategory.Voice,{}) } }
        rule.onNodeWithTag("shared.voice.play.$key").performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithTag("shared.voice.seek.$key").fetchSemanticsNodes().isNotEmpty() }
        rule.runOnIdle { chat=chat.copy(timeline=listOf(ChatTimelineEntry.Message(message(file.copy(localSource=null))))) }
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("shared.voice.seek.$key").fetchSemanticsNodes().isEmpty() }
        rule.onNodeWithTag("shared.voice.play.$key").assertIsEnabled()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun narrowReaderWithLargeRtlTextKeepsDocumentAndActionsReachable() {
        val file=attachment(AttachmentLocalSource.PlainText).copy(label="A long filename for the meeting details and walking directions.txt")
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(androidx.compose.ui.unit.DpSize(320.dp,600.dp))) {
                val density=androidx.compose.ui.platform.LocalDensity.current
                CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density,2f),
                    androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
                    WhiteNoiseTheme { AttachmentReaderDialog(profile,message(file),file,file,AttachmentAccessScenario.Success,{}) }
                }
            }
        }
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("attachment.reader.body").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("attachment.reader.body").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("attachment.reader.copy").assertIsDisplayed().assertIsEnabled()
        rule.onNodeWithTag("attachment.reader.more").performClick()
        rule.onNodeWithTag("attachment.reader.external").assertIsDisplayed().assertIsEnabled()
    }

}
