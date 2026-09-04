package dev.ipf.whitenoise.model

object AttachmentReadingExamples {
    fun attachments(): List<MessageAttachment> = AttachmentLocalSource.entries.map { source ->
        val name = when (source) {
            AttachmentLocalSource.Markdown -> "Notes from the trail.md"
            AttachmentLocalSource.PlainText -> "Meeting details.txt"
            AttachmentLocalSource.EmptyText -> "Blank notes.txt"
            AttachmentLocalSource.InvalidEncoding -> "Older notes.txt"
            AttachmentLocalSource.BinaryText -> "Unreadable notes.txt"
            AttachmentLocalSource.LargeText -> "Complete archive.txt"
            AttachmentLocalSource.LongMarkdown -> "Long walking journal.md"
            AttachmentLocalSource.AudioClip -> "Audio clip.wav"
            AttachmentLocalSource.AndroidPackage -> "White Noise.apk"
            AttachmentLocalSource.InvalidPackage -> "Incomplete app.apk"
        }
        MessageAttachment("document-${source.name}",MessageAttachmentKind.File,name, localSource = source,
            mimeType = when (source) {
                AttachmentLocalSource.Markdown, AttachmentLocalSource.LongMarkdown -> "text/markdown"
                AttachmentLocalSource.AudioClip -> "audio/wav"
                AttachmentLocalSource.AndroidPackage, AttachmentLocalSource.InvalidPackage -> "application/octet-stream"
                else -> "text/plain"
            }, durationSeconds = 4.takeIf { source == AttachmentLocalSource.AudioClip })
    }
}
