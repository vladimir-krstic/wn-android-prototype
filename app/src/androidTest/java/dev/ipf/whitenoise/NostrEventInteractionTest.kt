package dev.ipf.whitenoise

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import dev.ipf.whitenoise.model.ChatMessage
import dev.ipf.whitenoise.model.MessageDocuments
import dev.ipf.whitenoise.model.NostrEventCard
import dev.ipf.whitenoise.model.NostrEventKind
import dev.ipf.whitenoise.model.NostrEventReference
import dev.ipf.whitenoise.model.NostrEventState
import dev.ipf.whitenoise.model.NostrProfileOccurrence
import dev.ipf.whitenoise.model.ProfileFixtures
import dev.ipf.whitenoise.model.PublicReferenceEncoding
import dev.ipf.whitenoise.ui.conversation.MessageDocumentContent
import dev.ipf.whitenoise.ui.conversation.NostrEventCards
import dev.ipf.whitenoise.ui.conversation.UnavailableNostrProfileDialog
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NostrEventInteractionTest {
    @get:Rule
    val compose = createComposeRule()

    private val profile = ProfileFixtures.marmota
    private fun reference(
        id: String,
        state: NostrEventState,
        retry: NostrEventState? = null,
    ) = NostrEventReference(
        id,
        "nostr:${PublicReferenceEncoding.encode("note", List(32) { it + id.length })}",
        state,
        retry,
    )

    private fun message(vararg references: NostrEventReference) = ChatMessage(
        "event-message",
        "maya-chen",
        0,
        "Today",
        0,
        "Now",
        nostrEvents = references.toList(),
    )

    @Test
    fun loadedKindsExposeTextLabelsAndOpenActions() {
        compose.setContent {
            WhiteNoiseTheme {
                NostrEventCards(
                    message(
                        reference("note", NostrEventState.Loaded(NostrEventCard(NostrEventKind.Note, profile.people.first().publicKey, null, "A note"))),
                        reference("image", NostrEventState.Loaded(NostrEventCard(NostrEventKind.Image, profile.people.first().publicKey, "Ridge", "An image"))),
                        reference("event", NostrEventState.Loaded(NostrEventCard(NostrEventKind.Event, profile.people.first().publicKey, "Walk", "At 8:00"))),
                    ),
                    profile,
                    onRetry = { _, _ -> },
                    onOpenPerson = {},
                )
            }
        }
        compose.onNodeWithText("Note").assertIsDisplayed()
        compose.onNodeWithText("Image").assertIsDisplayed()
        compose.onNodeWithText("Event").assertIsDisplayed()
        compose.onAllNodesWithText("Open event").fetchSemanticsNodes().also { assertEquals(3, it.size) }
    }

    @Test
    fun articleUsesTheExistingRichReaderAndBackClosesIt() {
        val card = NostrEventCard(
            NostrEventKind.Article,
            profile.people.first().publicKey,
            "A quiet route",
            "A short route guide",
            readerBody = "# A quiet route\n\n- Bring water\n- Share the plan",
        )
        compose.setContent {
            WhiteNoiseTheme {
                NostrEventCards(message(reference("article", NostrEventState.Loaded(card))), profile, { _, _ -> }, {})
            }
        }
        compose.onNodeWithText("Read").performClick()
        compose.onNodeWithTag("nostr_event.reader").assertIsDisplayed()
        compose.onNodeWithText("Bring water").assertIsDisplayed()
        compose.onNodeWithText("A quiet route").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithTag("nostr_event.reader").assertDoesNotExist()
    }

    @Test
    fun referencedVideoOpensTheBundledLocalMediaViewer() {
        val card = NostrEventCard(NostrEventKind.Video, profile.people.first().publicKey, "Trail clip", "Eight seconds")
        compose.setContent {
            WhiteNoiseTheme {
                NostrEventCards(message(reference("video", NostrEventState.Loaded(card))), profile, { _, _ -> }, {})
            }
        }
        compose.onNodeWithText("Play").performClick()
        compose.onNodeWithTag("nostr_event.video_viewer").assertIsDisplayed()
        compose.onNodeWithContentDescription("Close").performClick()
        compose.onNodeWithTag("nostr_event.video_viewer").assertDoesNotExist()
    }

    @Test
    fun recoveryUsesTheExactReferenceRevisionAndCopyKeepsAuthoredForm() {
        val loaded = NostrEventState.Loaded(
            NostrEventCard(NostrEventKind.Note, profile.people.first().publicKey, null, "Available again"),
        )
        val event = reference("failed", NostrEventState.Failed, loaded)
        val retries = mutableListOf<Pair<String, Int>>()
        compose.setContent {
            WhiteNoiseTheme {
                NostrEventCards(message(event), profile, { id, revision -> retries += id to revision }, {})
            }
        }
        compose.onNodeWithText("Couldn’t load this event.").assertIsDisplayed()
        compose.onNodeWithTag("conversation.nostr_event.retry.${event.id}").performClick()
        compose.runOnIdle { assertEquals(listOf(event.id to event.revision), retries) }
        compose.onNodeWithTag("conversation.nostr_event.copy.${event.id}").performClick()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.runOnIdle {
            val clip = (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip
            assertEquals(event.authoredReference, clip?.getItemAt(0)?.text?.toString())
        }
    }

    @Test
    fun encodedMemberAndNonMemberReferencesUseDistinctInAppText() {
        val maya = profile.people.first { it.id == "maya-chen" }
        val opened = mutableListOf<NostrProfileOccurrence>()
        compose.setContent {
            WhiteNoiseTheme {
                Column {
                    MessageDocumentContent(
                        MessageDocuments.parse("@${maya.publicKey}"),
                        profile.people,
                        {},
                        memberIds = setOf(maya.id),
                        onOpenProfileReference = { opened += it },
                    )
                    MessageDocumentContent(
                        MessageDocuments.parse("@${maya.publicKey}"),
                        profile.people,
                        {},
                        memberIds = emptySet(),
                        onOpenProfileReference = { opened += it },
                    )
                }
            }
        }
        compose.onNodeWithText("@${maya.displayName}").assertIsDisplayed().performClick()
        compose.onNodeWithText(maya.displayName).assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(2, opened.size)
            assertTrue(opened.all { it.publicKey == maya.publicKey })
        }
    }

    @Test
    fun unavailableProfileStaysInAppAndRetryReportsTheSecondMiss() {
        val key = PublicReferenceEncoding.fixtureKey("missing-profile")
        val occurrence = NostrProfileOccurrence("nostr:$key", key, key, 0 until key.length + 6, false)
        compose.setContent {
            WhiteNoiseTheme { UnavailableNostrProfileDialog(occurrence, onDismiss = {}) }
        }
        compose.onNodeWithText("Profile unavailable").assertIsDisplayed()
        compose.onNodeWithTag("nostr_profile.retry").performClick()
        compose.onNodeWithText("This profile still isn’t available. The reference was kept.").assertIsDisplayed()
        compose.onNodeWithText("Checked again").assertIsDisplayed()
    }

    @Test
    fun largeTypeKeepsRecoveryAndCopyReachable() {
        val event = reference("unavailable", NostrEventState.Unavailable, NostrEventState.Loading)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides androidx.compose.ui.unit.Density(density.density, 2f)) {
                WhiteNoiseTheme { NostrEventCards(message(event), profile, { _, _ -> }, {}) }
            }
        }
        compose.onNodeWithText("This event is unavailable right now.").assertIsDisplayed()
        compose.onNodeWithText("Copy event reference").assertIsDisplayed()
        compose.onNodeWithText("Retry").assertIsDisplayed()
    }
}
