@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package dev.ipf.whitenoise

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.ConversationMediaProjection
import dev.ipf.whitenoise.model.ConversationMediaSelection
import dev.ipf.whitenoise.model.MessageAttachmentKind
import dev.ipf.whitenoise.model.ProfileFixtures
import dev.ipf.whitenoise.ui.conversation.ReadOnlyMediaViewer
import dev.ipf.whitenoise.ui.conversation.rememberForegroundVideoPlayer
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MediaViewerVideoTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<EmptyTestActivity>()

    private val profile = ProfileFixtures.marmota
    private val items = ConversationMediaProjection.items(
        profile.chats.first { it.id == "catalog-media-viewer" },
        profile,
    )
    private val video = items.first { it.attachment.kind == MessageAttachmentKind.Video }

    @Test
    fun viewerPlaysPausesAndSeeksWithoutOpeningAnotherApp() {
        val visible = mutableStateOf(true)
        composeRule.setContent {
            WhiteNoiseTheme {
                if (visible.value) {
                    ReadOnlyMediaViewer(
                        ConversationMediaSelection(items, video.key),
                        onDismiss = { visible.value = false },
                        onForward = {},
                        onGoToMessage = {},
                    )
                }
            }
        }
        composeRule.onNodeWithText("Open Video").assertDoesNotExist()
        composeRule.waitUntil(10_000) {
            !composeRule.onNodeWithTag("conversation.media.viewer.video.seek")
                .fetchSemanticsNode().config.contains(SemanticsProperties.Disabled)
        }
        composeRule.onNodeWithContentDescription("Play").performClick()
        composeRule.onNodeWithContentDescription("Pause").assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription("Play").assertIsDisplayed()

        val seek = composeRule.onNodeWithTag("conversation.media.viewer.video.seek")
        seek.performSemanticsAction(SemanticsActions.SetProgress) { assertTrue(it(0.5f)) }
        composeRule.waitUntil(5_000) {
            seek.fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo]
                .current in 0.45f..0.55f
        }
        composeRule.onNodeWithContentDescription("Playback Speed").assertIsDisplayed()
        val controls = composeRule.onNodeWithTag("conversation.media.viewer.video.controls")
            .fetchSemanticsNode().boundsInRoot
        val share = composeRule.onNodeWithTag("conversation.media.viewer.share")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertTrue(controls.bottom <= share.top)
        composeRule.onNodeWithTag("conversation.media.viewer.forward").assertIsDisplayed()

        composeRule.runOnIdle { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.onNodeWithTag("conversation.media.viewer.pager").assertDoesNotExist()
    }

    @Test
    fun videoSurfaceSupportsTheSameDownwardDismissal() {
        val visible = mutableStateOf(true)
        composeRule.setContent {
            WhiteNoiseTheme {
                if (visible.value) {
                    ReadOnlyMediaViewer(
                        ConversationMediaSelection(items, video.key),
                        onDismiss = { visible.value = false },
                        onForward = {},
                        onGoToMessage = {},
                    )
                }
            }
        }
        composeRule.onNodeWithTag("conversation.media.viewer.pager").performTouchInput {
            swipe(Offset(centerX, height * 0.35f), Offset(centerX, height * 0.8f), 400)
        }
        composeRule.onNodeWithTag("conversation.media.viewer.pager").assertDoesNotExist()
    }

    @Test
    fun leavingPageOrForegroundReleasesPlayerAndRestoresPositionPaused() {
        val active = mutableStateOf(true)
        val present = mutableStateOf(true)
        val owner = object : LifecycleOwner {
            val registry = LifecycleRegistry(this)
            override val lifecycle: Lifecycle = registry
        }
        var player: ExoPlayer? = null
        composeRule.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
        composeRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                if (present.value) player = rememberForegroundVideoPlayer(video, active.value)
            }
        }
        fun awaitReady(): ExoPlayer {
            composeRule.waitUntil(10_000) {
                composeRule.runOnIdle { player?.playbackState == Player.STATE_READY }
            }
            return composeRule.runOnIdle { requireNotNull(player) }
        }
        val first = awaitReady()
        composeRule.runOnIdle {
            assertFalse(first.playWhenReady)
            first.seekTo(1_000)
            first.volume = 0f
            first.setPlaybackSpeed(1.5f)
            first.play()
            active.value = false
        }
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            assertNull(player)
            assertFalse(first.isPlaying)
            active.value = true
        }
        val second = awaitReady()
        composeRule.runOnIdle {
            assertNotSame(first, second)
            assertFalse(second.playWhenReady)
            assertTrue(second.currentPosition >= 1_000)
            assertEquals(0f, second.volume, 0.001f)
            assertEquals(1.5f, second.playbackParameters.speed, 0.001f)
            second.play()
            owner.registry.currentState = Lifecycle.State.STARTED
        }
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            assertNull(player)
            assertFalse(second.isPlaying)
            owner.registry.currentState = Lifecycle.State.RESUMED
        }
        val third = awaitReady()
        composeRule.runOnIdle {
            assertNotSame(second, third)
            assertFalse(third.playWhenReady)
            third.play()
            present.value = false
        }
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertFalse(third.isPlaying) }
    }
}
