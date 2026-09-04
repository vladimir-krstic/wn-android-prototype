package dev.ipf.whitenoise.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NostrReferenceTest {
    private val profile = ProfileFixtures.marmota
    private val maya = profile.people.first { it.id == "maya-chen" }
    private val mayaProfile = PublicReferenceEncoding.encode(
        "nprofile",
        listOf(0, 32) + PublicReferenceEncoding.decode(maya.publicKey)!!.second,
    )

    @Test
    fun profileOccurrencesValidateNpubAndNprofileWithoutChangingAuthoredRanges() {
        val source = "Ask @${maya.publicKey}, then open nostr:$mayaProfile."
        val occurrences = NostrProfileReferences.occurrences(source)
        assertEquals(2, occurrences.size)
        assertEquals("@${maya.publicKey}", source.substring(occurrences[0].range))
        assertEquals("nostr:$mayaProfile", source.substring(occurrences[1].range))
        assertTrue(occurrences[0].mention)
        assertFalse(occurrences[1].mention)
        assertEquals(maya.publicKey, occurrences[1].publicKey)
    }

    @Test
    fun invalidChecksumAndPrivateReferencesStayOrdinaryAuthoredText() {
        val invalid = maya.publicKey.dropLast(1) + if (maya.publicKey.last() == 'q') "p" else "q"
        assertTrue(NostrProfileReferences.occurrences("@$invalid nsec1${"q".repeat(58)}").isEmpty())
    }

    @Test
    fun memberMentionAndKnownNonMemberHaveDifferentVisibleTreatment() {
        val source = "@${maya.publicKey}"
        val member = NostrProfileReferences.displays(source, profile.people, setOf(maya.id)).single()
        val nonMember = NostrProfileReferences.displays(source, profile.people, emptySet()).single()
        assertEquals("@${maya.displayName}", member.visibleText)
        assertTrue(member.memberMention)
        assertEquals(maya.displayName, nonMember.visibleText)
        assertFalse(nonMember.memberMention)
    }

    @Test
    fun projectedDisplayMapsEveryReplacementUnitToTheCompleteAuthoredReference() {
        val source = SourceText.from("See @${maya.publicKey} now")
        val fragment = NostrProfileTextProjection.project(source, profile.people, setOf(maya.id))
            .single { it.display != null }
        val authored = fragment.display!!.occurrence.range
        assertEquals("@${maya.displayName}", fragment.source.text)
        assertEquals(List(fragment.source.text.length) { authored.first }, fragment.source.offsets)
        assertEquals(List(fragment.source.text.length) { authored.last + 1 }, fragment.source.ends)
    }

    @Test
    fun publicEventEncodingDoesNotBecomeAProfileLink() {
        val note = PublicReferenceEncoding.encode("note", List(32) { it })
        assertEquals("note", PublicReferenceEncoding.decode(note)?.first)
        assertNull(ProfileLinks.parse(note))
        assertTrue(NostrPublicEventReferences.parse("nostr:$note") is NostrPublicEventPointer.Event)

        val event = PublicReferenceEncoding.encode("nevent", listOf(0, 32) + List(32) { 255 - it })
        assertTrue(NostrPublicEventReferences.parse(event) is NostrPublicEventPointer.Event)

        val identifier = "weekend".map(Char::code)
        val address = PublicReferenceEncoding.encode(
            "naddr",
            listOf(0, identifier.size) + identifier +
                listOf(2, 32) + List(32) { it } +
                listOf(3, 4, 0, 0, 117, 71),
        )
        assertNull(
            NostrPublicEventReferences.parse(
                PublicReferenceEncoding.encode(
                    "naddr",
                    listOf(0, 2, 0xc3, 0x28) +
                        listOf(2, 32) + List(32) { it } +
                        listOf(3, 4, 0, 0, 117, 71),
                ),
            ),
        )
        assertEquals(30_023L, (NostrPublicEventReferences.parse(address) as NostrPublicEventPointer.Address).kind)
        assertNull(NostrPublicEventReferences.parse(PublicReferenceEncoding.encode("nevent", listOf(0, 31) + List(31) { it })))
        assertNull(
            NostrPublicEventReferences.parse(
                PublicReferenceEncoding.encode(
                    "nevent",
                    listOf(0, 32) + List(32) { it } + listOf(2, 31) + List(31) { it },
                ),
            ),
        )
    }

    @Test
    fun examplesCoverEveryCardKindAndEveryRequiredRecoveryState() {
        val chat = profile.chats.first { it.id == "fiatjaf" }
        val updated = NostrEventExamples.add(chat, profile)!!
        val references = updated.timeline.filter { it.id.startsWith(NostrEventExamples.IdPrefix) }
            .filterIsInstance<ChatTimelineEntry.Message>()
            .flatMap { it.message.nostrEvents }
        val loadedKinds = references.mapNotNull { (it.state as? NostrEventState.Loaded)?.card?.kind }.toSet()
        assertEquals(NostrEventKind.entries.toSet(), loadedKinds)
        assertTrue(references.any { it.state == NostrEventState.Loading })
        assertTrue(references.any { it.state == NostrEventState.NotFound })
        assertTrue(references.any { it.state == NostrEventState.Invalid })
        assertTrue(references.any { it.state == NostrEventState.Unavailable })
        assertTrue(references.any { it.state == NostrEventState.Failed })
        assertTrue(references.all { it.authoredReference.startsWith("nostr:note1") })
    }

    @Test
    fun reinsertingExamplesReplacesOwnedRowsAndEndedChatIsRejected() {
        val chat = profile.chats.first { it.id == "fiatjaf" }
        val once = NostrEventExamples.add(chat, profile)!!
        val twice = NostrEventExamples.add(once, profile)!!
        assertEquals(
            once.timeline.count { it.id.startsWith(NostrEventExamples.IdPrefix) },
            twice.timeline.count { it.id.startsWith(NostrEventExamples.IdPrefix) },
        )
        assertNull(NostrEventExamples.add(chat.copy(membership = ChatMembership.Left), profile))
    }

    @Test
    fun retryRequiresExactLiveReferenceAndRevision() {
        val chat = NostrEventExamples.add(profile.chats.first { it.id == "fiatjaf" }, profile)!!
        val entry = chat.timeline.filterIsInstance<ChatTimelineEntry.Message>().first {
            it.message.nostrEvents.singleOrNull()?.state == NostrEventState.NotFound
        }
        val reference = entry.message.nostrEvents.single()
        assertNull(NostrEventExamples.retry(chat, entry.id, reference.id, reference.revision + 1))
        val updated = NostrEventExamples.retry(chat, entry.id, reference.id, reference.revision)!!
        val after = updated.timeline.filterIsInstance<ChatTimelineEntry.Message>()
            .first { it.id == entry.id }.message.nostrEvents.single()
        assertTrue(after.state is NostrEventState.Loaded)
        assertEquals(reference.revision + 1, after.revision)
        assertNull(NostrEventExamples.retry(updated, entry.id, reference.id, reference.revision))
        assertNull(NostrEventExamples.retry(chat.copy(membership = ChatMembership.Removed), entry.id, reference.id, reference.revision))
        val deleted = chat.copy(
            timeline = chat.timeline.map { candidate ->
                if (candidate !is ChatTimelineEntry.Message || candidate.id != entry.id) candidate else {
                    candidate.copy(message = candidate.message.copy(deletionState = MessageDeletionState.DeletedByOther))
                }
            },
        )
        assertNull(NostrEventExamples.retry(deleted, entry.id, reference.id, reference.revision))
    }

    @Test
    fun viewModelGatesExamplesAndRetryToTheExactActiveProfileAndChat() {
        val viewModel = dev.ipf.whitenoise.state.AppViewModel().apply {
            completeSignIn(dev.ipf.whitenoise.navigation.OnboardingOrigin.Initial)
        }
        val profileId = viewModel.uiState.activeProfileId!!
        val chatId = viewModel.uiState.activeProfile!!.chats.first { it.id == "fiatjaf" }.id
        assertFalse(viewModel.addNostrEventExamples(profileId, chatId))
        viewModel.setDeveloperToolsEnabled(true)
        assertTrue(viewModel.addNostrEventExamples(profileId, chatId))
        val entry = viewModel.chat(chatId)!!.timeline.filterIsInstance<ChatTimelineEntry.Message>().first {
            it.message.nostrEvents.singleOrNull()?.state == NostrEventState.Failed
        }
        val reference = entry.message.nostrEvents.single()
        assertFalse(viewModel.retryNostrEvent("wrong-profile", chatId, entry.id, reference.id, reference.revision))
        assertTrue(viewModel.retryNostrEvent(profileId, chatId, entry.id, reference.id, reference.revision))
        assertNotNull(viewModel.chat(chatId))
    }
}
