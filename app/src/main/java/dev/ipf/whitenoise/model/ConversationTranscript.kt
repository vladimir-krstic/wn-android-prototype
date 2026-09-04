package dev.ipf.whitenoise.model

/** A local authored-history document, deliberately not a fabricated Marmot wire-event archive. */
data class TranscriptSource(val profileId: String, val chatId: String, val title: String,
    val entries: List<ChatTimelineEntry>, val publicKeys: Map<String, String>) {
    companion object {
        fun capture(profile: Profile, chat: Chat) = TranscriptSource(profile.id, chat.id, chat.title, chat.timeline.toList(),
            profile.people.associate { it.id to it.publicKey } + (profile.id to profile.publicKey))
    }
}
enum class TranscriptPhase { Reading, Encoding, Ready, ChoosingDestination, Writing, Saved, Cancelled, Failed }
enum class TranscriptFailure { SourceUnavailable, Preparation, Destination, Write }
enum class TranscriptScenario(val developerLabel: String) {
    Success("Transcript available"), SourceUnavailable("Transcript source unavailable"), PreparationFailure("Transcript preparation fails"),
    WriteFailure("Transcript write fails")
}
data class TranscriptWork(val id: Long, val source: TranscriptSource, val scenario: TranscriptScenario,
    val phase: TranscriptPhase = TranscriptPhase.Reading, val readCount: Int = 0,
    val entries: List<ChatTimelineEntry> = emptyList(), val document: String? = null, val failure: TranscriptFailure? = null) {
    val busy get() = phase in setOf(TranscriptPhase.Reading, TranscriptPhase.Encoding, TranscriptPhase.ChoosingDestination, TranscriptPhase.Writing)
}

object ConversationTranscript {
    const val pageSize = 200
    fun timestamp(entry: ChatTimelineEntry): Long = (entry as? ChatTimelineEntry.Message)?.message?.let {
        it.createdAtMillis?.takeIf { value -> value > 0 } ?: GlobalSearchClock.timestamp(it)
    } ?: GlobalSearchClock.timestamp(ChatMessage(entry.id, "", entry.dayOrdinal, entry.dayLabel, entry.minuteOfDay, ""))
    fun ordered(source: TranscriptSource): List<ChatTimelineEntry> = source.entries.distinctBy { it.id }
        .sortedWith(compareBy<ChatTimelineEntry> { timestamp(it) }.thenBy { it.id })

    fun encode(source: TranscriptSource, entries: List<ChatTimelineEntry>): String = json(linkedMapOf(
        "schema" to "white-noise-local-transcript", "version" to 1, "exported_at_ms" to MessageForwarding.nowMillis, "chat_id" to source.chatId,
        "chat_name" to source.title, "event_count" to entries.size, "events" to entries.mapIndexed { index, entry ->
            val base = linkedMapOf<String, Any?>("index" to index, "id" to entry.id, "timeline_at_ms" to timestamp(entry))
            when (entry) {
                is ChatTimelineEntry.Message -> {
                    val m = entry.message
                    base + linkedMapOf("type" to "message", "author_id" to m.authorId,
                        "author_public_key" to source.publicKeys[m.authorId], "direction" to if (m.authorId == source.profileId) "outgoing" else "incoming",
                        "content" to if (m.isDeleted) null else m.text, "received_at_ms" to m.receivedAtMillis,
                        "reply_to_id" to m.replyToMessageId, "delivery_state" to m.deliveryState.name, "deletion_state" to m.deletionState.name,
                        "original_content" to if (m.isDeleted) null else m.editHistory?.original,
                        "original_timestamp_ms" to m.editHistory?.originalTimestampMillis,
                        "revisions" to if (m.isDeleted) emptyList<Any>() else m.editHistory?.revisions.orEmpty().map { r ->
                            linkedMapOf("id" to r.id, "content" to r.text, "timestamp_ms" to r.timestampMillis) },
                        "reactions" to m.reactions.map { r -> linkedMapOf("emoji" to r.emoji, "author_ids" to r.personIds) },
                        "attachments" to if (m.isDeleted) emptyList<Any>() else m.attachments.map { a ->
                            linkedMapOf("id" to a.id, "kind" to a.kind.name, "label" to a.label, "file_size_bytes" to a.fileSizeBytes,
                                "url" to a.externalUri, "duration_seconds" to a.durationSeconds,
                                "transcript" to a.transcript, "contact_person_id" to a.contactPersonId,
                                "link_title" to a.linkTitle, "link_domain" to a.linkDomain, "link_summary" to a.linkSummary,
                                "voice_format" to a.voiceFormat?.name, "photo_quality" to a.photoQuality?.name) })
                }
                is ChatTimelineEntry.Event -> base + mapOf("type" to "group_event", "content" to entry.text)
                is ChatTimelineEntry.Notice -> base + mapOf("type" to "notice", "content" to entry.text)
            }
        })) + "\n"

    /** JSON scalar escaping is shared for keys and values, including controls and lone surrogates. */
    internal fun json(value: Any?): String = when (value) {
        null -> "null"
        is String -> buildString {
            append('"'); value.forEach { c -> when (c) {
                '"' -> append("\\\""); '\\' -> append("\\\\"); '\n' -> append("\\n"); '\r' -> append("\\r"); '\t' -> append("\\t")
                else -> if (c.code < 0x20 || c.isSurrogate()) append("\\u" + c.code.toString(16).padStart(4, '0')) else append(c)
            } }; append('"')
        }
        is Number, is Boolean -> value.toString()
        is Map<*, *> -> value.entries.joinToString(",", "{", "}") { json(it.key.toString()) + ":" + json(it.value) }
        is Iterable<*> -> value.joinToString(",", "[", "]") { json(it) }
        else -> error("Unsupported transcript value")
    }
}
