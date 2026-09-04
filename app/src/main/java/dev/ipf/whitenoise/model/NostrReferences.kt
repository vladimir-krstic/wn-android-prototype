package dev.ipf.whitenoise.model

import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

data class NostrProfileOccurrence(
    val authoredReference: String,
    val encodedReference: String,
    val publicKey: String,
    val range: IntRange,
    val mention: Boolean,
)

data class NostrProfileDisplay(
    val occurrence: NostrProfileOccurrence,
    val person: Person?,
    val mentionIsMember: Boolean,
) {
    val memberMention: Boolean get() = person != null && mentionIsMember
    val visibleText: String
        get() = when {
            person != null && memberMention -> "@${person.displayName}"
            person != null -> person.displayName
            else -> (if (mentionIsMember) "@" else "") + shortenedReference(occurrence.encodedReference)
        }
}

object NostrProfileReferences {
    private const val body = "ac-hj-np-z02-9"
    private val candidate = Regex(
        "(?i)(?<![$body])(?:@|nostr:)?(?:npub1[$body]{58}|nprofile1[$body]{1,4900})(?![$body])",
    )

    fun occurrences(text: String): List<NostrProfileOccurrence> = candidate.findAll(text).mapNotNull { match ->
        val authored = match.value
        val mention = authored.startsWith('@')
        val encoded = authored.removePrefix("@").removePrefixIgnoreCase("nostr:")
        val publicKey = ProfileLinks.parse(encoded)?.value ?: return@mapNotNull null
        NostrProfileOccurrence(
            authoredReference = authored,
            encodedReference = encoded,
            publicKey = publicKey,
            range = match.range,
            mention = mention,
        )
    }.toList()

    fun displays(
        text: String,
        people: List<Person>,
        memberIds: Set<String>? = null,
    ): List<NostrProfileDisplay> = occurrences(text).map { occurrence ->
        val person = people.firstOrNull { it.publicKey == occurrence.publicKey }
        NostrProfileDisplay(
            occurrence = occurrence,
            person = person,
            mentionIsMember = occurrence.mention && (memberIds == null || person?.id in memberIds),
        )
    }
}

data class NostrProfileTextFragment(
    val source: SourceText,
    val display: NostrProfileDisplay? = null,
)

object NostrProfileTextProjection {
    fun project(
        source: SourceText,
        people: List<Person>,
        memberIds: Set<String>? = null,
    ): List<NostrProfileTextFragment> {
        val displays = NostrProfileReferences.displays(source.text, people, memberIds)
        if (displays.isEmpty()) return listOf(NostrProfileTextFragment(source))
        val result = mutableListOf<NostrProfileTextFragment>()
        var cursor = 0
        displays.forEach { display ->
            val range = display.occurrence.range
            if (range.first > cursor) result += NostrProfileTextFragment(source.slice(cursor, range.first))
            val original = source.slice(range.first, range.last + 1)
            result += NostrProfileTextFragment(
                source = SourceText(
                    text = display.visibleText,
                    offsets = List(display.visibleText.length) { original.offsets.first() },
                    ends = List(display.visibleText.length) { original.ends.last() },
                ),
                display = display,
            )
            cursor = range.last + 1
        }
        if (cursor < source.text.length) result += NostrProfileTextFragment(source.slice(cursor))
        return result
    }
}

private fun String.removePrefixIgnoreCase(prefix: String): String =
    if (startsWith(prefix, ignoreCase = true)) substring(prefix.length) else this

fun shortenedReference(value: String): String {
    val trimmed = value.trim()
    return if (trimmed.length <= 19) trimmed else "${trimmed.take(12)}…${trimmed.takeLast(6)}"
}

enum class NostrEventKind {
    Note,
    Article,
    Image,
    Video,
    Document,
    Event,
}

sealed interface NostrPublicEventPointer {
    data class Event(val id: List<Int>) : NostrPublicEventPointer
    data class Address(val identifier: String, val author: List<Int>, val kind: Long) : NostrPublicEventPointer
}

object NostrPublicEventReferences {
    fun parse(raw: String): NostrPublicEventPointer? {
        val candidate = raw.trim().removePrefixIgnoreCase("nostr:")
        val (prefix, payload) = PublicReferenceEncoding.decode(candidate) ?: return null
        return when (prefix) {
            "note" -> payload.takeIf { it.size == 32 }?.let(NostrPublicEventPointer::Event)
            "nevent" -> parseTlv(payload)?.let { fields ->
                val eventId = fields[0]?.singleOrNull()?.takeIf { it.size == 32 }
                if (
                    eventId != null &&
                    fields.hasOptionalSingleField(2, 32) &&
                    fields.hasOptionalSingleField(3, 4)
                ) {
                    NostrPublicEventPointer.Event(eventId)
                } else {
                    null
                }
            }
            "naddr" -> parseTlv(payload)?.let { fields ->
                val identifier = fields[0]?.singleOrNull()?.decodeIdentifier()
                val author = fields[2]?.singleOrNull()?.takeIf { it.size == 32 }
                val kind = fields[3]?.singleOrNull()?.takeIf { it.size == 4 }?.fold(0L) { value, byte ->
                    (value shl 8) or byte.toLong()
                }
                if (identifier != null && author != null && kind != null) {
                    NostrPublicEventPointer.Address(identifier, author, kind)
                } else {
                    null
                }
            }
            else -> null
        }
    }

    private fun parseTlv(payload: List<Int>): Map<Int, List<List<Int>>>? {
        val fields = linkedMapOf<Int, MutableList<List<Int>>>()
        var offset = 0
        while (offset < payload.size) {
            if (offset + 2 > payload.size) return null
            val type = payload[offset]
            val length = payload[offset + 1]
            offset += 2
            if (offset + length > payload.size) return null
            fields.getOrPut(type) { mutableListOf() } += payload.subList(offset, offset + length)
            offset += length
        }
        return fields
    }

    private fun Map<Int, List<List<Int>>>.hasOptionalSingleField(type: Int, size: Int): Boolean =
        this[type]?.let { values -> values.size == 1 && values.single().size == size } ?: true

    private fun List<Int>.decodeIdentifier(): String? = runCatching {
        Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(map(Int::toByte).toByteArray()))
            .toString()
    }.getOrNull()?.takeIf { value -> value.isNotBlank() && value.none { it.isISOControl() } }
}

data class NostrEventCard(
    val kind: NostrEventKind,
    val authorPublicKey: String,
    val title: String? = null,
    val summary: String,
    val metadata: List<String> = emptyList(),
    val readerBody: String? = null,
    val image: ProfileAvatar? = null,
)

sealed interface NostrEventState {
    data object Loading : NostrEventState
    data class Loaded(val card: NostrEventCard) : NostrEventState
    data object NotFound : NostrEventState
    data object Invalid : NostrEventState
    data object Unavailable : NostrEventState
    data object Failed : NostrEventState
}

data class NostrEventReference(
    val id: String,
    val authoredReference: String,
    val state: NostrEventState,
    val retryState: NostrEventState? = null,
    val revision: Int = 0,
) {
    init {
        require(state !is NostrEventState.Loaded || NostrPublicEventReferences.parse(authoredReference) != null)
    }
    val canRetry: Boolean
        get() = retryState != null && state in setOf(
            NostrEventState.NotFound,
            NostrEventState.Unavailable,
            NostrEventState.Failed,
        )

    fun retry(expectedRevision: Int): NostrEventReference? =
        retryState?.takeIf { canRetry && expectedRevision == revision }?.let {
            copy(state = it, retryState = null, revision = revision + 1)
        }
}

object NostrEventExamples {
    const val IdPrefix = "nostr-event-example-"

    fun add(chat: Chat, profile: Profile): Chat? {
        if (chat.membership != ChatMembership.Active) return null
        val authorId = (chat.kind as? ChatKind.Direct)?.personId
            ?: chat.members.firstOrNull { it.personId != profile.id }?.personId
            ?: return null
        val author = profile.people.firstOrNull { it.id == authorId } ?: return null
        val latest = chat.timeline.maxWithOrNull(
            compareBy<ChatTimelineEntry> { it.dayOrdinal }.thenBy { it.minuteOfDay },
        )
        val absoluteMinute = (latest?.dayOrdinal ?: 0) * 1_440 + (latest?.minuteOfDay ?: -6) + 6
        val messages = examples(author, absoluteMinute)
        return chat.copy(
            timeline = chat.timeline.filterNot { it.id.startsWith(IdPrefix) } + messages.map(ChatTimelineEntry::Message),
            preview = messages.last().text,
            previewAuthor = author.displayName,
            timestamp = "Now",
        )
    }

    fun retry(
        chat: Chat,
        messageId: String,
        referenceId: String,
        expectedRevision: Int,
    ): Chat? {
        if (chat.membership != ChatMembership.Active) return null
        var changed = false
        val timeline = chat.timeline.map { entry ->
            if (entry !is ChatTimelineEntry.Message || entry.id != messageId || entry.message.isDeleted) return@map entry
            val updated = entry.message.nostrEvents.map { reference ->
                if (reference.id != referenceId) reference else reference.retry(expectedRevision)?.also { changed = true } ?: reference
            }
            entry.copy(message = entry.message.copy(nostrEvents = updated))
        }
        return chat.copy(timeline = timeline).takeIf { changed }
    }

    private fun examples(author: Person, firstAbsoluteMinute: Int): List<ChatMessage> {
        fun noteReference(seed: String): String = PublicReferenceEncoding.encode(
            "note",
            List(32) { index -> (seed[index % seed.length].code + index * 29) and 255 },
        )
        fun reference(
            suffix: String,
            state: NostrEventState,
            retryState: NostrEventState? = null,
        ) = NostrEventReference(
            id = "$IdPrefix$suffix-reference",
            authoredReference = "nostr:${noteReference(suffix)}",
            state = state,
            retryState = retryState,
        )
        fun card(
            kind: NostrEventKind,
            title: String?,
            summary: String,
            metadata: List<String> = emptyList(),
            body: String? = null,
            image: ProfileAvatar? = null,
        ) = NostrEventState.Loaded(
            NostrEventCard(kind, author.publicKey, title, summary, metadata, body, image),
        )
        val loaded = listOf(
            reference("note", card(NostrEventKind.Note, null, "Meet at the trail entrance just after sunrise.")),
            reference(
                "article",
                card(
                    NostrEventKind.Article,
                    "Planning a quiet weekend outside",
                    "A short guide to choosing the route, sharing the plan, and leaving time to rest.",
                    listOf("8 min read", "Published today"),
                    "# Planning a quiet weekend outside\n\nA good plan leaves room to change course. Share the route before you leave, pack for the weather, and choose a clear turnaround time.\n\n## Before the walk\n\n- Check the forecast\n- Carry water\n- Tell someone when you expect to return",
                ),
            ),
            reference("image", card(NostrEventKind.Image, "Morning on the ridge", "A view from the overlook above the trail.", listOf("1600 × 1067"), image = ProfileAvatar.Asset(AvatarAsset.GardenClub))),
            reference("video", card(NostrEventKind.Video, "Trail after the rain", "A short clip from the lower path.", listOf("0:08", "1920 × 1080"))),
            reference("document", card(NostrEventKind.Document, "Packing checklist", "Water, rain layer, map, first-aid kit, and a charged phone.", listOf("Document"), body = "# Packing checklist\n\n- Water\n- Rain layer\n- Map\n- First-aid kit\n- Charged phone")),
            reference("event", card(NostrEventKind.Event, "Saturday trail walk", "Meet at 8:00 AM by the north entrance.", listOf("Saturday", "8:00 AM"))),
        )
        val retryLoaded = card(NostrEventKind.Note, null, "The event is available after trying again.")
        val states = loaded + listOf(
            reference("loading", NostrEventState.Loading),
            reference("not-found", NostrEventState.NotFound, retryLoaded),
            reference("invalid", NostrEventState.Invalid),
            reference("unavailable", NostrEventState.Unavailable, retryLoaded),
            reference("failed", NostrEventState.Failed, retryLoaded),
        )
        return states.mapIndexed { index, event ->
            val absoluteMinute = firstAbsoluteMinute + index * 6
            ChatMessage(
                id = "$IdPrefix${index + 1}",
                authorId = author.id,
                dayOrdinal = absoluteMinute / 1_440,
                dayLabel = "Today",
                minuteOfDay = absoluteMinute % 1_440,
                timeLabel = "Now",
                text = event.authoredReference,
                nostrEvents = listOf(event),
            )
        }
    }
}
