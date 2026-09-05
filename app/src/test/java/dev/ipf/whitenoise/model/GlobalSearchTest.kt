package dev.ipf.whitenoise.model

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class GlobalSearchTest {
    private fun message(id: String, text: String = "trailhead", author: String = "a", day: String = "Today", minute: Int = 600,
        attachments: List<MessageAttachment> = emptyList()) = ChatMessage(id, author, 7, day, minute, "10:00", text, attachments)
    private fun chat(id: String, vararg messages: ChatMessage) = Chat(id, 0, ChatKind.Group, "Chat $id", timeline = messages.map(ChatTimelineEntry::Message))
    private fun profile(vararg chats: Chat) = Profile("me", "My Profile", "key", people = listOf(Person("a", "Alice", nickname = "Ally", privateNotes = "secret note"), Person("b", "Bob")), chats = chats.toList())
    private fun ids(profile: Profile, query: String = "", filters: GlobalSearchFilters = GlobalSearchFilters()) = GlobalSearch.results(profile, query, filters).messages.map { it.message.id }.toSet()

    @Test fun distinguishesOrdinaryChatMatchesFromExactMessagesAcrossArchivedAndEndedHistory() {
        val title = chat("title", message("unrelated", "unrelated")).copy(title = "Trailhead plans")
        val archived = chat("archived", message("archived-hit")).copy(isArchived = true)
        val ended = chat("ended", message("ended-hit")).copy(membership = ChatMembership.Left)
        val results = GlobalSearch.results(profile(title, archived, ended), "TRAILHEAD", GlobalSearchFilters())
        assertEquals(listOf("title"), results.chats.map { it.id })
        assertEquals(setOf("archived-hit", "ended-hit"), results.messages.map { it.message.id }.toSet())
        assertEquals("Ally", results.messages.first().sender)
        assertEquals(listOf("title"), GlobalSearch.results(profile(title.copy(title = "Café plans")), "CAFE", GlobalSearchFilters()).chats.map { it.id })
    }
    @Test fun excludesDeletedSystemPrivateNotesAuthorNamesAndUnsentDrafts() {
        val chat = chat("a", message("deleted").copy(deletionState = MessageDeletionState.DeletedByOther), message("safe", "quiet"))
            .copy(draftText = "trailhead", timeline = listOf(ChatTimelineEntry.Event("event", "trailhead"), ChatTimelineEntry.Notice("notice", "trailhead")) +
                listOf(ChatTimelineEntry.Message(message("deleted").copy(deletionState = MessageDeletionState.DeletedByOther)), ChatTimelineEntry.Message(message("safe", "quiet"))))
        val profile = profile(chat)
        listOf("trailhead", "secret note", "Ally").forEach { assertTrue(ids(profile, it).isEmpty()) }
        assertEquals(setOf("safe"), ids(profile, "quiet"))
    }
    @Test fun normalizesWhitespaceAndMarkupButKeepsSearchableNamedLinkDestinations() {
        val file = MessageAttachment("file", MessageAttachmentKind.File, "route.pdf", externalUri = "content://private-device-location")
        val msg = message("body", "Meet\n  at **the trailhead** and [map](https://maps.example/route).", attachments = listOf(file))
        val profile = profile(chat("a", msg))
        assertEquals(setOf("body"), ids(profile, "AT  THE\nTRAILHEAD"))
        assertEquals(setOf("body"), ids(profile, "maps.example/route"))
        assertEquals(setOf("body"), ids(profile, "route.pdf"))
        assertTrue(ids(profile, "private-device-location").isEmpty())
        assertFalse(GlobalSearch.body(msg).contains("**"))
    }
    @Test fun chatAndSenderFiltersUseOrWithinAndAcrossCategories() {
        val profile = profile(chat("x", message("xa"), message("xb", author = "b")), chat("y", message("ya"), message("ym", author = "me")), chat("z", message("za")))
        val filters = GlobalSearchFilters(chatIds = setOf("x", "y"), senderIds = setOf("a", "me"))
        assertEquals(setOf("xa", "ya", "ym"), ids(profile, filters = filters))
        assertTrue(GlobalSearch.results(profile, "", filters).chats.isEmpty())
        assertEquals(setOf("x", "y"), GlobalSearch.results(profile, "", filters.copy(senderIds = emptySet())).chats.map { it.id }.toSet())
        assertEquals(setOf("ym"), ids(profile, filters = filters.copy(senderIds = setOf("me"))))
    }
    @Test fun contentClassifiesAllAttachmentKindsAndAudioFilesWithoutTreatingContactAsText() {
        fun kinds(kind: MessageAttachmentKind, label: String = "Attachment") = GlobalSearch.content(message("m", "", attachments = listOf(MessageAttachment("a", kind, label))))
        listOf(MessageAttachmentKind.Photo, MessageAttachmentKind.Photos, MessageAttachmentKind.Video, MessageAttachmentKind.Gif).forEach {
            assertEquals(setOf(GlobalSearchContent.AnyAttachment, GlobalSearchContent.ImagesVideo), kinds(it))
        }
        assertEquals(setOf(GlobalSearchContent.AnyAttachment, GlobalSearchContent.VoiceAudio), kinds(MessageAttachmentKind.Voice))
        assertEquals(setOf(GlobalSearchContent.AnyAttachment, GlobalSearchContent.VoiceAudio), kinds(MessageAttachmentKind.File, "Recording.M4A"))
        assertEquals(setOf(GlobalSearchContent.AnyAttachment, GlobalSearchContent.Files), kinds(MessageAttachmentKind.File, "Plan.pdf"))
        assertEquals(setOf(GlobalSearchContent.AnyAttachment, GlobalSearchContent.Links), kinds(MessageAttachmentKind.Link))
        assertEquals(setOf(GlobalSearchContent.AnyAttachment), kinds(MessageAttachmentKind.Contact))
        assertEquals(setOf(GlobalSearchContent.Text, GlobalSearchContent.Links), GlobalSearch.content(message("link", "[Map](https://example.com)")))
        assertEquals(setOf(GlobalSearchContent.Text), GlobalSearch.content(message("text")))
    }
    @Test fun contentSelectionsUnionThenIntersectDateAndQuery() {
        val photo = MessageAttachment("p", MessageAttachmentKind.Photo, "Trailhead photo")
        val profile = profile(chat("x", message("image", "", attachments = listOf(photo)), message("link", "trailhead https://example.com"),
            message("old", "trailhead https://example.com", day = "Yesterday"), message("text")))
        val filters = GlobalSearchFilters(date = GlobalSearchDate.Today, content = setOf(GlobalSearchContent.ImagesVideo, GlobalSearchContent.Links))
        assertEquals(setOf("image", "link"), ids(profile, "trailhead", filters))
        assertEquals(setOf("image"), ids(profile, filters = filters.copy(content = setOf(GlobalSearchContent.AnyAttachment))))
    }
    @Test fun presetsIncludeCalendarStartAndExcludeTomorrow() {
        val today = GlobalSearchClock.today
        listOf(GlobalSearchDate.Today to 0L, GlobalSearchDate.Last7Days to 6L, GlobalSearchDate.Last30Days to 29L).forEach { (preset, days) ->
            val bounds = GlobalSearchFilters(date = preset).bounds()!!
            val start = GlobalSearchClock.pickerMillis(today.minusDays(days).toEpochDay())
            val tomorrow = GlobalSearchClock.pickerMillis(today.plusDays(1).toEpochDay())
            assertEquals(start, bounds.startInclusive); assertEquals(tomorrow, bounds.endExclusive)
            assertTrue(start in bounds); assertTrue(tomorrow - 1 in bounds); assertFalse(start - 1 in bounds); assertFalse(tomorrow in bounds)
        }
    }
    @Test fun customRangeIsInclusiveAndInvalidRangeReturnsNoResults() {
        val day = GlobalSearchClock.today.toEpochDay()
        val filters = GlobalSearchFilters(date = GlobalSearchDate.Custom, fromDay = day, toDay = day)
        val profile = profile(chat("x", message("start", minute = 0), message("end", minute = 1439), message("old", day = "Yesterday")))
        assertEquals(setOf("start", "end"), ids(profile, filters = filters))
        listOf(filters.copy(fromDay = null), filters.copy(toDay = null), filters.copy(fromDay = day + 1)).forEach {
            assertFalse(it.valid); assertNull(it.bounds()); assertTrue(ids(profile, "trailhead", it).isEmpty())
        }
    }
    @Test fun pickerDatesAreUtcCivilDaysAndBoundsHonorDst() {
        val zone = ZoneId.of("America/New_York")
        listOf(LocalDate.of(2026, 3, 8) to 23L, LocalDate.of(2026, 11, 1) to 25L).forEach { (date, hours) ->
            val day = date.toEpochDay()
            assertEquals(day, GlobalSearchClock.pickerDay(GlobalSearchClock.pickerMillis(day)))
            val bounds = GlobalSearchFilters(date = GlobalSearchDate.Custom, fromDay = day, toDay = day).bounds(zone = zone)!!
            assertEquals(hours * 3_600_000, bounds.endExclusive - bounds.startInclusive)
            assertEquals(date.atStartOfDay(zone).toInstant().toEpochMilli(), bounds.startInclusive)
        }
    }
    @Test fun visibleDateLabelsOverridePerChatOrdinals() {
        val expected = mapOf("Today" to "2026-08-03", "Yesterday" to "2026-08-02", "Friday" to "2026-07-31", "Wed" to "2026-07-29", "Thu" to "2026-07-30", "Sat" to "2026-08-01",
            "Jul 28, 2025" to "2025-07-28", "Dec 8, 2025" to "2025-12-08", "Mon, Jul 14" to "2025-07-14", "Jul 30" to "2026-07-30")
        expected.forEach { (label, date) -> assertEquals(LocalDate.parse(date), GlobalSearchClock.date(message("x", day = label).copy(dayOrdinal = 41))) }
    }
    @Test fun snippetsCenterMatchAndNeverCutSurrogatePairs() {
        val body = "🙂".repeat(100) + "TRAILHEAD" + "🚲".repeat(100)
        val snippet = GlobalSearch.snippet(body, "trailhead", 41)
        assertTrue(snippet.startsWith("…")); assertTrue(snippet.endsWith("…")); assertTrue(snippet.contains("TRAILHEAD"))
        snippet.forEachIndexed { i, c ->
            if (c.isHighSurrogate()) assertTrue(snippet.getOrNull(i + 1)?.isLowSurrogate() == true)
            if (c.isLowSurrogate()) assertTrue(snippet.getOrNull(i - 1)?.isHighSurrogate() == true)
        }
        assertTrue(GlobalSearch.snippet("before " + "x".repeat(180) + " after", "x".repeat(180)).contains("x".repeat(180)))
    }
    @Test fun reconciliationPrunesRemovedIdsAndPreservesOtherFilters() {
        val profile = profile(chat("x", message("a")))
        val filters = GlobalSearchFilters(setOf("x", "gone"), setOf("a", "gone"), date = GlobalSearchDate.Today, content = setOf(GlobalSearchContent.Text))
        val reconciled = filters.reconcile(profile)
        assertEquals(setOf("x"), reconciled.chatIds); assertEquals(setOf("a"), reconciled.senderIds)
        assertEquals(filters.date, reconciled.date); assertEquals(filters.content, reconciled.content)
        assertTrue(GlobalSearch.results(profile, "", GlobalSearchFilters()).messages.isEmpty())
    }
    @Test fun identifierIntentRecognizesSupportedLinksButNotOrdinaryMessageUrls() {
        val key = PeopleDiscovery.directory.first().publicKey
        listOf(key, "nostr:$key", "https://whitenoise.chat/profile/$key", "river@whitenoise.example", "npub-invalid", LoginPrototypeData.privateKey).forEach {
            assertTrue(it, GlobalSearch.identifierIntent(it))
        }
        assertFalse(GlobalSearch.identifierIntent("https://maps.example/route")); assertFalse(GlobalSearch.identifierIntent("Meet @ the trailhead"))
    }
    @Test fun identifierLookupRejectsMalformedLinksAndSupportsRetriedAddressAndUnknownKey() {
        val profile = profile()
        listOf("nostr:invalid", "whitenoise://profile/invalid", "https://whitenoise.chat/profile/invalid", LoginPrototypeData.privateKey).forEach {
            assertEquals(PeopleSearchStatus.InvalidIdentifier, GlobalSearch.people(profile, it, PeopleSearchScenario.Success).status)
        }
        val address = "river@whitenoise.example"
        assertEquals(PeopleSearchStatus.Unavailable, GlobalSearch.people(profile, address, PeopleSearchScenario.Unavailable).status)
        assertEquals("river-song", GlobalSearch.people(profile, address, PeopleSearchScenario.Success).people.single().person.id)
        val key = PublicReferenceEncoding.fixtureKey("unknown-global-person")
        assertEquals(PeopleSearchStatus.NoProfile, GlobalSearch.people(profile, "nostr:$key", PeopleSearchScenario.Success).status)
    }
    @Test fun voiceCompletionsRequireOwnerUnchangedQueryAndActiveSearch() {
        val request = GlobalVoiceRequest(1, "me", "existing", GlobalVoiceScenario.Device)
        assertEquals("meeting tomorrow", GlobalSearch.voiceResult(request, "me", "existing", true, "  meeting tomorrow  "))
        assertNull(GlobalSearch.voiceResult(request, "other", "existing", true, "meeting tomorrow")); assertNull(GlobalSearch.voiceResult(request, "me", "new", true, "meeting tomorrow"))
        assertNull(GlobalSearch.voiceResult(request, "me", "existing", false, "meeting tomorrow"))
        assertNull(GlobalSearch.voiceResult(request, "me", "existing", true, "  "))
        assertNull(GlobalSearch.voiceResult(request, "me", "existing", true, null))
        listOf(GlobalVoiceScenario.Cancelled, GlobalVoiceScenario.Unavailable).forEach { assertNull(GlobalSearch.voiceResult(request.copy(scenario = it), "me", "existing", true, "meeting tomorrow")) }
    }
}
