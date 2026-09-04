package dev.ipf.whitenoise.model

import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.util.Locale

/** Authored, offline examples. Real picked files continue to use their granted content URI. */
enum class AttachmentLocalSource { Markdown, PlainText, EmptyText, InvalidEncoding, BinaryText, LargeText, LongMarkdown, AudioClip, AndroidPackage, InvalidPackage }
enum class AttachmentAccessScenario(val developerLabel: String) {
    Success("Available"), LoadFailure("Load failure"), NoHandler("No external viewer"),
    PackagePermission("Installation permission required"), PackageNoInstaller("No installer"), PackageReady("Package ready for review"),
}
enum class TextAttachmentFormat { PlainText, Markdown }
enum class TextAttachmentEncoding { Utf8, Utf16Le, Utf16Be }
data class TextAttachmentCandidate(val name: String, val mime: String, val format: TextAttachmentFormat)
enum class TextAttachmentFailure { Unavailable, TooLarge, InvalidEncoding, Binary, SourceChanged }
sealed interface TextAttachmentResult {
    data class Ready(val text: String, val encoding: TextAttachmentEncoding, val byteCount: Int) : TextAttachmentResult
    data class Failed(val reason: TextAttachmentFailure) : TextAttachmentResult
}
data class TextAttachmentPresentation(val source: String, val document: MessageDocument?, val truncated: Boolean, val preview: String = source) {
    val speech: String get() = document?.let { MessageDocuments.plainText(it.blocks) } ?: source
}
object TextAttachments {
    const val MaximumBytes = 512 * 1024
    const val MaximumFormattedCharacters = 64 * 1024
    const val MaximumFormattedLines = 2048
    private val markdownMimes = setOf("text/markdown", "text/x-markdown")
    private val textExtensions = setOf("txt", "text", "log", "csv", "json", "xml", "yaml", "yml")
    private val textApplications = setOf("application/json", "application/xml", "application/yaml", "application/x-yaml")
    fun safeName(name: String) = name.replace('\\','/').substringAfterLast('/').filterNot {
        Character.isISOControl(it) || Character.getType(it) == Character.FORMAT.toInt()
    }.trim().takeUnless { it.isBlank() || it == "." || it == ".." } ?: "attachment"
    fun normalizedMime(mime: String?) = mime.orEmpty().substringBefore(';').trim().lowercase(Locale.ROOT)
        .takeIf { it.matches(Regex("[a-z0-9][a-z0-9!#\$&^_.+-]*/[a-z0-9][a-z0-9!#\$&^_.+-]*")) }.orEmpty()
    fun candidate(attachment: MessageAttachment): TextAttachmentCandidate? {
        if (attachment.kind != MessageAttachmentKind.File) return null
        val name = safeName(attachment.label); val mime = normalizedMime(attachment.mimeType)
        val extension = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val format = when {
            mime in markdownMimes || extension in setOf("md", "markdown") -> TextAttachmentFormat.Markdown
            mime.startsWith("text/") || mime in textApplications ||
                (mime.startsWith("application/") && (mime.endsWith("+json") || mime.endsWith("+xml"))) || extension in textExtensions -> TextAttachmentFormat.PlainText
            else -> return null
        }
        return TextAttachmentCandidate(name, mime.ifEmpty { if (format == TextAttachmentFormat.Markdown) "text/markdown" else "text/plain" }, format)
    }
    fun decode(bytes: ByteArray): TextAttachmentResult {
        if (bytes.size > MaximumBytes) return TextAttachmentResult.Failed(TextAttachmentFailure.TooLarge)
        fun starts(vararg values: Int) = bytes.size >= values.size && values.indices.all { bytes[it].toInt() and 255 == values[it] }
        val (encoding, offset) = when {
            starts(0xef,0xbb,0xbf) -> TextAttachmentEncoding.Utf8 to 3
            starts(0xff,0xfe) -> TextAttachmentEncoding.Utf16Le to 2
            starts(0xfe,0xff) -> TextAttachmentEncoding.Utf16Be to 2
            else -> TextAttachmentEncoding.Utf8 to 0
        }
        if (encoding != TextAttachmentEncoding.Utf8 && (bytes.size - offset) % 2 != 0) return TextAttachmentResult.Failed(TextAttachmentFailure.InvalidEncoding)
        val charset = when (encoding) { TextAttachmentEncoding.Utf8 -> Charsets.UTF_8; TextAttachmentEncoding.Utf16Le -> Charsets.UTF_16LE; TextAttachmentEncoding.Utf16Be -> Charsets.UTF_16BE }
        val text = try { charset.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes,offset,bytes.size-offset)).toString() }
        catch (_: java.nio.charset.CharacterCodingException) { return TextAttachmentResult.Failed(TextAttachmentFailure.InvalidEncoding) }
        if (text.any { Character.isISOControl(it) && it !in "\n\r\t" }) return TextAttachmentResult.Failed(TextAttachmentFailure.Binary)
        return TextAttachmentResult.Ready(text,encoding,bytes.size)
    }
    fun presentation(text: String, format: TextAttachmentFormat): TextAttachmentPresentation {
        var end = minOf(text.length, MaximumFormattedCharacters)
        var lines = 0
        for (index in 0 until end) if (text[index] == '\n' && ++lines >= MaximumFormattedLines) { end = index; break }
        if (end > 0 && end < text.length && text[end - 1].isHighSurrogate()) end--
        val preview = text.substring(0,end)
        return TextAttachmentPresentation(text,if (format == TextAttachmentFormat.Markdown) MessageDocuments.parse(preview) else null,end < text.length,preview)
    }
}

/** Lossless UTF-16 chunking for Android speech input limits; never split a surrogate pair. */
object SpeechTextChunks {
    fun split(text: String, maximum: Int): List<String> {
        require(maximum >= 2)
        val chunks = mutableListOf<String>(); var start = 0
        while (start < text.length) {
            var end = minOf(text.length,start+maximum)
            if (end < text.length) {
                if (text[end - 1].isHighSurrogate()) end--
                val boundary = (end-1 downTo start+maximum/2).firstOrNull { text[it].isWhitespace() }
                if (boundary != null) end = boundary+1
            }
            chunks += text.substring(start,end); start = end
        }
        return chunks
    }
}

enum class PackageOpenOutcome { OrdinaryFile, InvalidPackage, RestrictedDistribution, PermissionRequired, NoInstaller, Ready }
object PackageAttachments {
    const val Mime = "application/vnd.android.package-archive"
    fun candidate(attachment: MessageAttachment): Boolean {
        if (attachment.kind != MessageAttachmentKind.File) return false
        val mime = TextAttachments.normalizedMime(attachment.mimeType)
        return mime == Mime || (mime in setOf("", "application/octet-stream") && TextAttachments.safeName(attachment.label).endsWith(".apk",true))
    }
    fun outcome(attachment: MessageAttachment, validArchive: Boolean, installationEnabled: Boolean, permission: Boolean, installer: Boolean): PackageOpenOutcome = when {
        !candidate(attachment) -> PackageOpenOutcome.OrdinaryFile
        !validArchive -> PackageOpenOutcome.InvalidPackage
        !installationEnabled -> PackageOpenOutcome.RestrictedDistribution
        !permission -> PackageOpenOutcome.PermissionRequired
        !installer -> PackageOpenOutcome.NoInstaller
        else -> PackageOpenOutcome.Ready
    }
}

enum class LibraryAudioPhase { Idle, Loading, Paused, Playing, Ended, Failed }
data class LibraryAudioState(val key: String? = null, val revision: Long = 0, val phase: LibraryAudioPhase = LibraryAudioPhase.Idle, val positionMillis: Long = 0, val durationMillis: Long = 0) {
    fun start(key: String) = LibraryAudioState(key,revision+1,LibraryAudioPhase.Loading)
    fun failed(revision: Long) = if (revision != this.revision) this else copy(phase = LibraryAudioPhase.Failed)
    fun observed(revision: Long, position: Long, duration: Long, phase: LibraryAudioPhase) = if (revision != this.revision) this else copy(
        positionMillis = position.coerceIn(0,duration.coerceAtLeast(0)),durationMillis = duration.coerceAtLeast(0),phase = phase)
    fun clear() = LibraryAudioState(revision = revision+1)
}
