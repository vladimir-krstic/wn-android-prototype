package dev.ipf.whitenoise.model

/** High retains the previously accepted 4096px/95 message-photo default. */
enum class PhotoQuality(val maxEdge: Int, val jpegQuality: Int) {
    Low(1024, 70), Standard(2048, 85), High(4096, 95), Original(4096, 100),
}

enum class PhotoMetadataPolicy { Reencoded, StrippedOriginal, SafeFallback }
enum class RecentMediaAccess { None, SelectedOnly, Full, Unavailable }

data class SharedDeviceContact(val name: String? = null, val phone: String? = null, val email: String? = null) {
    val fields: List<String> get() = listOfNotNull(name, phone, email).filter(String::isNotBlank)
    val displayName: String get() = fields.firstOrNull().orEmpty()
    val text: String get() = fields.joinToString("\n")
    fun selected(name: Boolean, phone: Boolean, email: Boolean) = SharedDeviceContact(
        this.name?.takeIf { name }, this.phone?.takeIf { phone }, this.email?.takeIf { email },
    )
    fun vCard(): String {
        fun escape(s: String) = s.replace("\\", "\\\\").replace("\r\n", "\n").replace('\r', '\n')
            .replace("\n", "\\n").replace(";", "\\;").replace(",", "\\,")
        return buildString {
            append("BEGIN:VCARD\r\nVERSION:3.0\r\n")
            append("N:${escape(displayName)};;;;\r\nFN:${escape(displayName)}\r\n")
            phone?.takeIf(String::isNotBlank)?.let { append("TEL;TYPE=CELL:${escape(it)}\r\n") }
            email?.takeIf(String::isNotBlank)?.let { append("EMAIL:${escape(it)}\r\n") }
            append("END:VCARD\r\n")
        }
    }
    fun attachment(id: String): MessageAttachment? = takeIf { fields.isNotEmpty() }?.let {
        MessageAttachment(id, MessageAttachmentKind.Contact, displayName, deviceContact = this,
            fileSizeBytes = vCard().toByteArray(Charsets.UTF_8).size, mimeType = "text/vcard")
    }
}

enum class AttachmentTransferPhase { Idle, Queued, Active, Available, Cancelled, Failed, CacheMiss, Expired, Invalid, Unavailable }
enum class AttachmentTransferOrigin { Automatic, Manual }
enum class AttachmentTransferDirection { Upload, Download }
enum class AttachmentTransferScenario(val developerLabel: String) {
    Success("Success"), Failure("Recoverable failure"), CacheMiss("Cache miss"), Expired("Expired"), Invalid("Invalid attachment"),
}

data class AttachmentTransfer(
    val phase: AttachmentTransferPhase = AttachmentTransferPhase.Queued,
    val direction: AttachmentTransferDirection = AttachmentTransferDirection.Download,
    val origin: AttachmentTransferOrigin = AttachmentTransferOrigin.Manual,
    val automaticSuppressed: Boolean = false,
    val progress: Int = 0,
    val revision: Long = 0,
    val attempt: Int = 1,
    val scenario: AttachmentTransferScenario = AttachmentTransferScenario.Success,
) {
    val running: Boolean get() = phase == AttachmentTransferPhase.Queued || phase == AttachmentTransferPhase.Active
    val retryable: Boolean get() = phase in setOf(AttachmentTransferPhase.Cancelled, AttachmentTransferPhase.Failed, AttachmentTransferPhase.CacheMiss)
    fun cancel() = if (running) copy(phase = AttachmentTransferPhase.Cancelled, automaticSuppressed = true, revision = revision + 1) else this
    fun retry() = if (retryable || phase == AttachmentTransferPhase.Idle) copy(phase = AttachmentTransferPhase.Queued,
        origin = AttachmentTransferOrigin.Manual, automaticSuppressed = false, progress = 0, attempt = attempt + 1, revision = revision + 1) else this
    fun requestManual() = if (phase == AttachmentTransferPhase.Queued && origin == AttachmentTransferOrigin.Automatic)
        copy(origin = AttachmentTransferOrigin.Manual, revision = revision + 1) else retry()
    fun admitAutomatically() = if (phase == AttachmentTransferPhase.Idle && !automaticSuppressed)
        copy(phase = AttachmentTransferPhase.Queued, origin = AttachmentTransferOrigin.Automatic, revision = revision + 1) else this
    fun stopAutomatic() = if (phase == AttachmentTransferPhase.Queued && origin == AttachmentTransferOrigin.Automatic)
        copy(phase = AttachmentTransferPhase.Idle, progress = 0, revision = revision + 1) else this
    fun restartAutomatic() = if (automaticSuppressed && phase == AttachmentTransferPhase.Cancelled)
        copy(phase = AttachmentTransferPhase.Idle, automaticSuppressed = false, revision = revision + 1) else this
    fun advance(expectedRevision: Long): AttachmentTransfer {
        if (revision != expectedRevision || !running) return this
        if (phase == AttachmentTransferPhase.Queued) return copy(phase = AttachmentTransferPhase.Active, revision = revision + 1)
        val progress = (progress + 25).coerceAtMost(100)
        val phase = when {
            progress < 75 -> AttachmentTransferPhase.Active
            scenario == AttachmentTransferScenario.Expired -> AttachmentTransferPhase.Expired
            scenario == AttachmentTransferScenario.Invalid -> AttachmentTransferPhase.Invalid
            attempt == 1 && scenario == AttachmentTransferScenario.Failure -> AttachmentTransferPhase.Failed
            attempt == 1 && scenario == AttachmentTransferScenario.CacheMiss -> AttachmentTransferPhase.CacheMiss
            progress == 100 -> AttachmentTransferPhase.Available
            else -> AttachmentTransferPhase.Active
        }
        return copy(phase = phase, progress = progress, revision = revision + 1)
    }
}

val MessageAttachment.bytesAvailable: Boolean
    get() = isAvailable && (transfer == null || transfer.phase == AttachmentTransferPhase.Available)

/** Flatten albums by attachment identity + image index, never a mutable list position. */
data class AttachmentExportKey(val attachmentId: String, val imageIndex: Int = 0)
enum class AttachmentExportOutcome { Ready, Saved, HandedOff, Unavailable, Cancelled, Failed, NoHandler }
object AttachmentExports {
    fun keys(message: ChatMessage): List<AttachmentExportKey> = if (message.isDeleted) emptyList() else message.attachments
        .filter { it.kind != MessageAttachmentKind.Link && (it.kind != MessageAttachmentKind.Contact || it.deviceContact != null) }
        .flatMap { attachment -> List(attachment.images.size.coerceAtLeast(1)) { AttachmentExportKey(attachment.id, it) } }
    fun mimeType(types: List<String>): String {
        val unique = types.distinct()
        return unique.singleOrNull() ?: unique.map { it.substringBefore('/') }.distinct().singleOrNull()?.let { "$it/*" } ?: "*/*"
    }
}
