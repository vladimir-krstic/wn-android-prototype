package dev.ipf.whitenoise.model

import java.text.BreakIterator
import java.util.Locale

/** Speech retains authored UTF-16 offsets; separators have no authored character. */
object SpeechDocuments {
    fun project(source: String): SourceText {
        fun runs(values: List<DocumentRun>) = values.fold(SourceText.from("")) { a, b -> a + b.source }
        val separator = SourceText("\n", listOf(-1), listOf(-1))
        fun join(values: List<SourceText>) = values.filter { it.text.isNotBlank() }.reduceOrNull { a, b -> a + separator + b } ?: SourceText.from("")
        fun blocks(values: List<DocumentBlock>): SourceText = join(values.map { block -> when (block) {
            is DocumentBlock.Paragraph -> runs(block.runs)
            is DocumentBlock.Heading -> runs(block.runs)
            is DocumentBlock.Code -> block.source
            is DocumentBlock.Quote -> blocks(block.blocks)
            is DocumentBlock.ListBlock -> join(block.items.map { blocks(it.blocks) })
            is DocumentBlock.Table -> join((listOf(block.header) + block.rows).flatMap { row -> row.map(::runs) })
            is DocumentBlock.Details -> join(listOf(runs(block.summary), blocks(block.blocks)))
            is DocumentBlock.Blank, DocumentBlock.Divider -> SourceText.from("")
        } })
        return blocks(MessageDocuments.parse(source).blocks).takeIf { it.text.isNotBlank() } ?: SourceText.from(source)
    }

    fun sentences(text: SourceText, locale: Locale): List<SourceText> {
        val result = mutableListOf<SourceText>()
        val iterator = BreakIterator.getSentenceInstance(locale).apply { setText(text.text) }
        var start = iterator.first()
        var end = iterator.next()
        var pendingStart: Int? = null
        val titles = setOf("dr.", "mr.", "mrs.", "ms.", "prof.", "sr.", "jr.")
        while (end != BreakIterator.DONE) {
            // Keep authored block boundaries even when punctuation does not end a sentence.
            val candidate = text.text.substring(start, end)
            if ('\n' !in candidate && candidate.trimEnd().takeLastWhile { !it.isWhitespace() }.lowercase(locale) in titles) {
                if (pendingStart == null) pendingStart = start
                start = end; end = iterator.next(); continue
            }
            var from = pendingStart ?: start
            pendingStart = null
            for (i in from until end) if (text.text[i] == '\n') {
                trimmed(text.slice(from, i))?.let(result::add); from = i + 1
            }
            trimmed(text.slice(from, end))?.let(result::add)
            start = end; end = iterator.next()
        }
        pendingStart?.let { trimmed(text.slice(it))?.let(result::add) }
        return result
    }
    private fun trimmed(value: SourceText): SourceText? {
        val start = value.text.indexOfFirst { !it.isWhitespace() }
        val end = value.text.indexOfLast { !it.isWhitespace() } + 1
        return if (start < 0) null else value.slice(start, end)
    }
    fun range(text: SourceText): IntRange? {
        val start = text.offsets.filter { it >= 0 }.minOrNull() ?: return null
        val end = text.ends.filter { it > start }.maxOrNull() ?: return null
        return start until end
    }
    fun sentenceAt(sentences: List<SourceText>, sourceOffset: Int): Int =
        sentences.indexOfFirst { range(it)?.let { range -> sourceOffset <= range.last } == true }
            .takeIf { it >= 0 } ?: sentences.lastIndex.coerceAtLeast(0)
    fun overlaps(text: SourceText, range: IntRange): Boolean = text.offsets.indices.any {
        text.offsets[it] >= 0 && text.offsets[it] <= range.last && text.ends[it] > range.first
    }
}

data class SpeechItem(val id: String, val authored: String, val plain: Boolean = false)
data class SpeechPrepared(val item: SpeechItem, val sentences: List<SourceText>)
data class SpeechOwner(val profileId: String, val chatId: String?, val attachmentRequestId: String? = null)
enum class SpeechPhase { Speaking, Paused, Loading, EdgeError, EngineError, Unavailable, Completed }
enum class SpeechMove { PreviousSentence, NextSentence, PreviousMessage, NextMessage }
enum class SpeechEdgeScenario { Success, EarlierFailure, LaterFailure }
data class SpeechEdge(val targetIndex: Int, val lastSentence: Boolean, val paused: Boolean)
data class SpeechToken(val sessionId: Long, val revision: Long, val message: Int, val sentence: Int, val chunk: Int) {
    val value: String get() = "$sessionId:$revision:$message:$sentence:$chunk"
}
data class SpeechReturnTarget(val sessionId: Long, val owner: SpeechOwner, val message: SpeechItem)

/** Immutable queue. Only windowSize messages are parsed; local history settles by revision. */
data class SpeechSession(
    val id: Long,
    val owner: SpeechOwner,
    val catalog: List<SpeechItem>,
    val windowStart: Int,
    val window: List<SpeechPrepared>,
    val messageIndex: Int,
    val sentenceIndex: Int = 0,
    val chunkIndex: Int = 0,
    val phase: SpeechPhase = SpeechPhase.Speaking,
    val revision: Long = 1,
    val rangeEnd: Int = 0,
    val rangeStart: Int = 0,
    val edge: SpeechEdge? = null,
    val following: Boolean = true,
    val windowSize: Int = 8,
    val chunkLimit: Int = 3_900,
    val locale: Locale = Locale.ROOT,
) {
    val current: SpeechPrepared get() = window[messageIndex - windowStart]
    val sentence: SourceText get() = current.sentences[sentenceIndex]
    val chunks: List<String> get() = SpeechTextChunks.split(sentence.text, chunkLimit)
    val chunk: String get() = chunks[chunkIndex]
    val token: SpeechToken? get() = if (phase == SpeechPhase.Speaking) SpeechToken(id, revision, messageIndex, sentenceIndex, chunkIndex) else null
    val passage: MessagePassage? get() {
        if (phase in setOf(SpeechPhase.Unavailable, SpeechPhase.Completed)) return null
        val offset = chunks.take(chunkIndex).sumOf { it.length }
        val from = offset + if (rangeEnd > rangeStart) rangeStart else 0
        val end = offset + if (rangeEnd > rangeStart) rangeEnd else chunk.length
        val current = sentence.slice(from, end)
        return SpeechDocuments.range(current)?.let { MessagePassage(current.text, it.first, it.last + 1) }
    }
    val progress: Float get() {
        val total = current.sentences.sumOf { it.text.length }.coerceAtLeast(1)
        val before = current.sentences.take(sentenceIndex).sumOf { it.text.length } + chunks.take(chunkIndex).sumOf { it.length }
        return if (phase == SpeechPhase.Completed) 1f else ((before + rangeEnd).toFloat() / total).coerceIn(0f, 1f)
    }
    val returnTarget: SpeechReturnTarget? get() = if (phase == SpeechPhase.Unavailable || owner.chatId == null || owner.attachmentRequestId != null) null
        else SpeechReturnTarget(id, owner, current.item)
    val navigable get() = phase !in setOf(SpeechPhase.Loading, SpeechPhase.EdgeError, SpeechPhase.Unavailable)

    /** Append at the logical tail without disturbing the current callback or parsing history. */
    fun append(items: List<SpeechItem>): SpeechSession {
        val known = catalog.mapTo(mutableSetOf()) { it.id }
        val added = items.filter { it.authored.isNotBlank() && known.add(it.id) }
        if (added.isEmpty()) return this
        val next = catalog + added
        return copy(catalog = next, window = if (windowStart + window.size == catalog.size && window.size < windowSize)
            prepare(next, windowStart, windowSize, locale) else window)
    }

    fun pause(): SpeechSession = if (phase == SpeechPhase.Speaking) copy(phase = SpeechPhase.Paused, revision = revision + 1) else this
    fun resume(): SpeechSession = if (phase in setOf(SpeechPhase.Paused, SpeechPhase.EngineError)) copy(phase = SpeechPhase.Speaking, revision = revision + 1, chunkIndex = 0, rangeEnd = 0, rangeStart = 0) else this
    fun fail(expected: SpeechToken): SpeechSession = if (token == expected) copy(phase = SpeechPhase.EngineError, revision = revision + 1, rangeEnd = 0, rangeStart = 0) else this
    fun unavailable(): SpeechSession = if (phase == SpeechPhase.Unavailable) this else copy(phase = SpeechPhase.Unavailable, revision = revision + 1, edge = null)
    fun range(expected: SpeechToken, end: Int, start: Int = 0): SpeechSession =
        if (token == expected && start >= 0 && end > start && end <= chunk.length && end > rangeEnd)
            copy(rangeEnd = end, rangeStart = start) else this
    fun done(expected: SpeechToken): SpeechSession {
        if (token != expected) return this
        return if (chunkIndex < chunks.lastIndex) copy(chunkIndex = chunkIndex + 1, revision = revision + 1, rangeEnd = 0, rangeStart = 0)
        else move(SpeechMove.NextSentence)
    }
    fun seek(index: Int, startPlaying: Boolean = false): SpeechSession {
        if (!navigable || index !in current.sentences.indices) return this
        return copy(sentenceIndex = index, chunkIndex = 0, rangeEnd = 0, revision = revision + 1,
            phase = if (!startPlaying && phase == SpeechPhase.Paused) SpeechPhase.Paused else SpeechPhase.Speaking, edge = null)
    }
    fun move(action: SpeechMove): SpeechSession {
        if (!navigable) return this
        val sentenceDelta = when (action) { SpeechMove.PreviousSentence -> -1; SpeechMove.NextSentence -> 1; else -> 0 }
        if (sentenceDelta != 0 && sentenceIndex + sentenceDelta in current.sentences.indices) return seek(sentenceIndex + sentenceDelta)
        val backwards = action in setOf(SpeechMove.PreviousSentence, SpeechMove.PreviousMessage)
        val target = messageIndex + if (backwards) -1 else 1
        if (target < 0) return seek(0)
        if (target >= catalog.size) return copy(phase = SpeechPhase.Completed, revision = revision + 1, rangeEnd = 0, edge = null, following = false)
        val last = action == SpeechMove.PreviousSentence
        if (target !in windowStart until windowStart + window.size) return copy(phase = SpeechPhase.Loading,
            revision = revision + 1, edge = SpeechEdge(target, last, phase == SpeechPhase.Paused), rangeEnd = 0, rangeStart = 0)
        val prepared = window[target - windowStart]
        return copy(messageIndex = target, sentenceIndex = if (last) prepared.sentences.lastIndex else 0, chunkIndex = 0,
            rangeEnd = 0, revision = revision + 1, edge = null,
            phase = if (phase == SpeechPhase.Paused) SpeechPhase.Paused else SpeechPhase.Speaking)
    }
    fun retryEdge(): SpeechSession = if (phase == SpeechPhase.EdgeError && edge != null) copy(phase = SpeechPhase.Loading, revision = revision + 1) else this
    fun settleEdge(expectedRevision: Long, failed: Boolean): SpeechSession {
        val request = edge ?: return this
        if (revision != expectedRevision || phase != SpeechPhase.Loading) return this
        if (failed) return copy(phase = SpeechPhase.EdgeError, revision = revision + 1)
        val start = if (request.targetIndex < windowStart) maxOf(0, request.targetIndex - windowSize + 1) else request.targetIndex
        val prepared = prepare(catalog, start, windowSize, locale)
        return copy(windowStart = start, window = prepared, messageIndex = request.targetIndex,
            sentenceIndex = if (request.lastSentence) prepared[request.targetIndex - start].sentences.lastIndex else 0,
            chunkIndex = 0, rangeEnd = 0, edge = null, revision = revision + 1,
            phase = if (request.paused) SpeechPhase.Paused else SpeechPhase.Speaking)
    }
    companion object {
        fun create(id: Long, owner: SpeechOwner, items: List<SpeechItem>, startId: String,
            sourceOffset: Int = 0, locale: Locale = Locale.ROOT, chunkLimit: Int = 3_900, windowSize: Int = 8,
            selection: MessagePassage? = null): SpeechSession? {
            require(chunkLimit >= 2 && windowSize > 0)
            val catalog = items.filter { it.authored.isNotBlank() }.distinctBy { it.id }
            val index = catalog.indexOfFirst { it.id == startId }.takeIf { it >= 0 } ?: return null
            if (selection != null) {
                val item = catalog[index]
                val full = SpeechDocuments.project(item.authored)
                val selected = full.offsets.indices.filter { full.offsets[it] >= selection.sourceStart && full.ends[it] <= selection.sourceEnd }
                if (selected.isEmpty()) return null
                val projection = full.slice(selected.first(), selected.last() + 1)
                val sentences = SpeechDocuments.sentences(projection, locale)
                if (sentences.isEmpty()) return null
                return SpeechSession(id, owner, listOf(item), 0, listOf(SpeechPrepared(item, sentences)), 0,
                    locale = locale, chunkLimit = chunkLimit, windowSize = windowSize)
            }
            val start = maxOf(0, index - windowSize / 2)
            val window = prepare(catalog, start, windowSize, locale)
            val sentence = SpeechDocuments.sentenceAt(window[index - start].sentences, sourceOffset)
            return SpeechSession(id, owner, catalog, start, window, index, sentenceIndex = sentence,
                locale = locale, chunkLimit = chunkLimit, windowSize = windowSize)
        }
        private fun prepare(items: List<SpeechItem>, start: Int, size: Int, locale: Locale): List<SpeechPrepared> =
            items.drop(start).take(size).map { SpeechPrepared(it, SpeechDocuments.sentences(if (it.plain) SourceText.from(it.authored) else SpeechDocuments.project(it.authored), locale)) }
    }
}

object SpeechOwnership {
    fun eligible(message: ChatMessage): Boolean = !message.isDeleted && message.text.isNotBlank() &&
        message.attachments.none { it.kind == MessageAttachmentKind.Voice } &&
        message.expiresAtMillis?.let { it <= MessageForwarding.nowMillis } != true
    fun items(chat: Chat): List<SpeechItem> = chat.timeline.filterIsInstance<ChatTimelineEntry.Message>()
        .map { it.message }.filter(::eligible).map { SpeechItem(it.id, it.text) }
    fun owns(profile: Profile?, target: SpeechReturnTarget): Boolean = profile?.id == target.owner.profileId &&
        profile.chats.firstOrNull { it.id == target.owner.chatId }?.let(::items)?.any { it == target.message } == true
}
