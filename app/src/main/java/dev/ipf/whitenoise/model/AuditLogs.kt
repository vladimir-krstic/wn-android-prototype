package dev.ipf.whitenoise.model

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class AuditLogFile(val id: Long, val profileId: String, val name: String, val content: String) {
    val bytes get() = content.toByteArray(Charsets.UTF_8).size
}
data class AuditLogState(val enabled: Boolean = false, val files: List<AuditLogFile> = emptyList())
enum class AuditLogAction { Enable, Disable, Export, Delete }
enum class AuditLogPhase { Consent, Applying, ChoosingDestination, Writing, Complete, Failed }
enum class AuditLogFailure { Update, Empty, Preparation, Destination, Write, Delete, PartialDelete, SourceChanged }
enum class AuditLogScenario(val label: String) { Success("Audit operation succeeds"), UpdateFails("Recording update fails once"), PreparationFails("Audit export preparation fails once"), WriteFails("Audit export write fails once"), DeleteFails("Audit deletion fails once"), PartialDelete("Some audit files cannot be deleted") }
data class AuditLogWork(val id: Long, val profileId: String, val action: AuditLogAction, val phase: AuditLogPhase,
    val scenario: AuditLogScenario, val files: List<AuditLogFile>, val attempt: Int = 0,
    val removed: Set<Long> = emptySet(), val failure: AuditLogFailure? = null, val archive: ByteArray? = null) {
    val busy get() = phase in setOf(AuditLogPhase.Applying,AuditLogPhase.ChoosingDestination,AuditLogPhase.Writing)
}

object AuditLogs {
    /** Deterministic local session sample, separate from sanitized Diagnostic Logs. No engine records are collected. */
    fun sample(id: Long, profiles: List<Profile>): AuditLogFile {
        val lines = profiles.map { p -> ConversationTranscript.json(linkedMapOf(
            "event" to "session.opened", "profile_public_key" to p.publicKey,
            "profile_name" to p.name, "platform" to "Android",
            "message" to p.chats.asSequence().flatMap { it.timeline.asSequence() }.filterIsInstance<ChatTimelineEntry.Message>()
                .lastOrNull { !it.message.isDeleted }?.message?.text)) }
        return AuditLogFile(id,profiles.single().id,"audit-session-$id.jsonl",lines.joinToString("\n",postfix = "\n"))
    }
    fun archive(files: List<AuditLogFile>): ByteArray {
        require(files.isNotEmpty())
        require(files.map { it.name }.distinct().size == files.size)
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip -> files.forEach { file ->
            require(file.name.matches(Regex("audit-session-[0-9]+\\.jsonl")))
            zip.putNextEntry(ZipEntry(file.name).apply { time = 0L })
            zip.write(file.content.toByteArray(Charsets.UTF_8)); zip.closeEntry()
        } }
        return output.toByteArray()
    }
}
