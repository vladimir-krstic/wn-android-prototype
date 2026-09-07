package dev.ipf.whitenoise.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

enum class GlobalSearchContent { Text, Links, ImagesVideo, VoiceAudio, Files, AnyAttachment }
enum class GlobalSearchDate { AnyTime, Today, Last7Days, Last30Days, Custom }
enum class GlobalSearchChatType { Direct, Groups }
data class SearchDateBounds(val startInclusive: Long, val endExclusive: Long) {
    operator fun contains(timestamp: Long) = timestamp >= startInclusive && timestamp < endExclusive
}
data class GlobalSearchFilters(
    val chatIds: Set<String> = emptySet(), val senderIds: Set<String> = emptySet(),
    val date: GlobalSearchDate = GlobalSearchDate.AnyTime, val fromDay: Long? = null, val toDay: Long? = null,
    val content: Set<GlobalSearchContent> = emptySet(),
    val folderIds: Set<String> = emptySet(),
    val chatTypes: Set<GlobalSearchChatType> = emptySet(),
) {
    val active get() = chatIds.isNotEmpty() || folderIds.isNotEmpty() || chatTypes.isNotEmpty() || messageOnly
    val messageOnly get() = senderIds.isNotEmpty() || date != GlobalSearchDate.AnyTime || content.isNotEmpty()
    val valid get() = date != GlobalSearchDate.Custom || (fromDay != null && toDay != null && fromDay <= toDay)
    /** Folder/type scope also supplies the chat picker, so it cannot retain hidden incompatible IDs. */
    fun scopedChats(profile: Profile): List<Chat> {
        val folderChats = if (folderIds.isEmpty()) null else profile.chatFolders
            .filter { it.id in folderIds }
            .flatMap { ChatFolders.rows(profile.chats, it) }.map { it.id }.toSet()
        return profile.chats.filter { chat ->
            (folderChats == null || chat.id in folderChats) &&
                (chatTypes.isEmpty() || (if (chat.isGroup) GlobalSearchChatType.Groups else GlobalSearchChatType.Direct) in chatTypes)
        }
    }
    fun reconcile(profile: Profile): GlobalSearchFilters {
        val existing = copy(folderIds = folderIds.intersect(profile.chatFolders.map { it.id }.toSet()))
        return existing.copy(
            chatIds = chatIds.intersect(existing.scopedChats(profile).map { it.id }.toSet()),
            senderIds = senderIds.intersect(GlobalSearch.senders(profile).map { it.first }.toSet()),
        )
    }
    fun bounds(today: LocalDate = GlobalSearchClock.today, zone: ZoneId = GlobalSearchClock.zone): SearchDateBounds? {
        if (!valid || date == GlobalSearchDate.AnyTime) return null
        val start = when (date) {
            GlobalSearchDate.Today -> today
            GlobalSearchDate.Last7Days -> today.minusDays(6)
            GlobalSearchDate.Last30Days -> today.minusDays(29)
            GlobalSearchDate.Custom -> LocalDate.ofEpochDay(fromDay!!)
            GlobalSearchDate.AnyTime -> return null
        }
        val end = if (date == GlobalSearchDate.Custom) LocalDate.ofEpochDay(toDay!!) else today
        return SearchDateBounds(start.atStartOfDay(zone).toInstant().toEpochMilli(), end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli())
    }
}

/** Calendar mapping for the accepted display-only fixtures; production supplies real timestamps. */
object GlobalSearchClock {
    val today: LocalDate = LocalDate.of(2026, 8, 3)
    val zone: ZoneId = ZoneOffset.UTC
    private val dateFormat = DateTimeFormatter.ofPattern("MMM d, uuuu", Locale.ENGLISH)
    private val weekdayFormat = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)
    fun pickerDay(utcMillis: Long) = Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
    fun pickerMillis(day: Long) = LocalDate.ofEpochDay(day).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    fun date(message: ChatMessage): LocalDate {
        val label = message.dayLabel.trim()
        if (label.equals("Today", true)) return today
        if (label.equals("Yesterday", true)) return today.minusDays(1)
        DayOfWeek.entries.firstOrNull { it.name.equals(label, true) || it.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).equals(label, true) }
            ?.let { return today.with(TemporalAdjusters.previous(it)) }
        runCatching { LocalDate.parse(label, dateFormat) }.getOrNull()?.let { return it }
        val monthDay = label.substringAfter(", ", label)
        val weekday = label.substringBefore(", ").takeIf { ", " in label }
        for (year in today.year downTo today.year - 7) {
            val candidate = runCatching { LocalDate.parse("$monthDay, $year", dateFormat) }.getOrNull() ?: continue
            if (candidate > today) continue
            if (weekday != null && candidate.format(weekdayFormat) != weekday) continue
            return candidate
        }
        return today.minusDays((3L - message.dayOrdinal).coerceAtLeast(0))
    }
    fun timestamp(message: ChatMessage) = date(message).atStartOfDay(zone).plusMinutes(message.minuteOfDay.toLong()).toInstant().toEpochMilli()
}

data class GlobalMessageResult(val chatId: String, val chatTitle: String, val message: ChatMessage, val sender: String, val snippet: String)
data class GlobalSearchResults(val chats: List<Chat>, val messages: List<GlobalMessageResult>)
enum class GlobalVoiceScenario(val developerLabel: String) { Device("Device speech recognition"), Success("Recognized phrase"), Cancelled("Cancelled"), Unavailable("Voice search unavailable") }
data class GlobalVoiceRequest(val id: Long, val profileId: String, val originalQuery: String, val scenario: GlobalVoiceScenario)

object GlobalSearch {
    const val voicePhrase = "trailhead"
    private val whitespace = Regex("\\s+")
    private val link = Regex("(?i)(https?://|www\\.)\\S+")
    fun identifierIntent(query: String): Boolean = ProfileLinks.identifierIntent(query)
    fun people(profile: Profile, query: String, scenario: PeopleSearchScenario): PeopleSearchResult {
        val normalized = ProfileLinks.normalizeRecipient(query)
        if (identifierIntent(query) && '@' !in normalized && PrivateKeyValidator.state(normalized) != PrivateKeyState.PublicKey) {
            return PeopleSearchResult(emptyList(), PeopleSearchStatus.InvalidIdentifier)
        }
        return PeopleDiscovery.resolve(profile, query, scenario)
    }
    fun normalize(value: String) = whitespace.replace(value, " ").trim()
    fun senderName(profile: Profile, id: String) = if (id == profile.id) profile.name else profile.people.firstOrNull { it.id == id }?.displayName ?: "Member"
    fun senders(profile: Profile): List<Pair<String, String>> = profile.chats.flatMap { chat -> chat.timeline.filterIsInstance<ChatTimelineEntry.Message>().map { it.message.authorId } }
        .distinct().map { it to senderName(profile, it) }.sortedBy { it.second.lowercase(Locale.ROOT) }
    fun content(message: ChatMessage): Set<GlobalSearchContent> = buildSet {
        if (InlineMessageMarkup.plainText(message.text).isNotBlank()) add(GlobalSearchContent.Text)
        if (link.containsMatchIn(message.text) || message.attachments.any { it.kind == MessageAttachmentKind.Link }) add(GlobalSearchContent.Links)
        if (message.attachments.isNotEmpty()) add(GlobalSearchContent.AnyAttachment)
        message.attachments.forEach { attachment -> when (attachment.kind) {
            MessageAttachmentKind.Photo, MessageAttachmentKind.Photos, MessageAttachmentKind.Video, MessageAttachmentKind.Gif -> add(GlobalSearchContent.ImagesVideo)
            MessageAttachmentKind.Voice -> add(GlobalSearchContent.VoiceAudio)
            MessageAttachmentKind.File -> if (SharedContentProjection.isAudio(attachment)) add(GlobalSearchContent.VoiceAudio) else add(GlobalSearchContent.Files)
            else -> Unit
        } }
    }
    fun body(message: ChatMessage) = normalize(buildString {
        InlineMessageMarkup.segments(message.text).forEach { segment ->
            append(segment.text)
            if (segment.destination != null && segment.destination != segment.text) append(" (${segment.destination})")
        }
        message.attachments.forEach { attachment ->
            append(' '); append(attachment.label); append(' '); append(attachment.linkTitle.orEmpty()); append(' '); append(attachment.linkDomain.orEmpty())
            // Search shareable link destinations, never device-owned content/file URIs.
            if (attachment.kind == MessageAttachmentKind.Link && attachment.externalUri?.let(link::containsMatchIn) == true) {
                append(' '); append(attachment.externalUri)
            }
        }
    })
    fun snippet(body: String, query: String, maxLength: Int = 140): String {
        val text = normalize(body); val needle = normalize(query)
        if (text.length <= maxLength) return text
        val found = text.indexOf(needle, ignoreCase = true).coerceAtLeast(0)
        val window = maxOf(maxLength, needle.length)
        var start = (found - (window - needle.length) / 2).coerceIn(0, (text.length - window).coerceAtLeast(0))
        var end = (start + window).coerceAtMost(text.length)
        if (start > 0 && text[start].isLowSurrogate()) start++
        if (end > start && text[end - 1].isHighSurrogate()) end--
        return (if (start > 0) "…" else "") + text.substring(start, end) + if (end < text.length) "…" else ""
    }
    fun results(profile: Profile, query: String, filters: GlobalSearchFilters): GlobalSearchResults {
        val needle = normalize(query)
        if (!filters.valid) return GlobalSearchResults(emptyList(), emptyList())
        if (needle.isEmpty() && !filters.active) return GlobalSearchResults(emptyList(), emptyList())
        val normalizedNeedle = needle.normalizedSearchText()
        val scoped = filters.scopedChats(profile).filter { filters.chatIds.isEmpty() || it.id in filters.chatIds }
        val chats = if (filters.messageOnly) emptyList() else scoped.filter { needle.isEmpty() ||
            listOf(it.title, it.displayPreview, if (it.isGroup) it.description else "").any { text -> normalize(text).normalizedSearchText().contains(normalizedNeedle) }
        }.sortedWith(ChatOrganization.order)
        val bounds = filters.bounds()
        // Parse each candidate date once. Sorting must not reparse display labels on every comparison.
        val messages = scoped.flatMap { chat ->
            chat.timeline.asSequence()
                .filterIsInstance<ChatTimelineEntry.Message>()
                .map { it.message }
                .filter { !it.isDeleted && (filters.senderIds.isEmpty() || it.authorId in filters.senderIds) }
                .mapNotNull { message ->
                    val timestamp = if (bounds != null) GlobalSearchClock.timestamp(message) else null
                    if (bounds != null && timestamp != null && timestamp !in bounds) return@mapNotNull null
                    if (filters.content.isNotEmpty() && content(message).none { it in filters.content }) return@mapNotNull null
                    val text = body(message)
                    if ((text.isBlank() && message.attachments.isEmpty()) || (needle.isNotEmpty() && !text.contains(needle, true))) return@mapNotNull null
                    val result = GlobalMessageResult(
                        chat.id, chat.title, message, senderName(profile, message.authorId), snippet(text, needle),
                    )
                    (timestamp ?: GlobalSearchClock.timestamp(message)) to result
                }
                .toList()
        }.sortedWith(compareByDescending<Pair<Long, GlobalMessageResult>> { it.first }
            .thenBy { it.second.chatId }.thenBy { it.second.message.id })
            .map { it.second }
        return GlobalSearchResults(chats, messages)
    }
    fun voiceResult(request: GlobalVoiceRequest, owner: String?, currentQuery: String, isSearching: Boolean, recognizedText: String?): String? =
        recognizedText?.trim()?.takeIf {
            it.isNotBlank() && request.profileId == owner && request.originalQuery == currentQuery && isSearching &&
                request.scenario in setOf(GlobalVoiceScenario.Device, GlobalVoiceScenario.Success)
        }
}
