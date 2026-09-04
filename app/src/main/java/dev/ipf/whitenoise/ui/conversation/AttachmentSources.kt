package dev.ipf.whitenoise.ui.conversation

import android.content.Context
import androidx.core.net.toUri
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.coroutines.coroutineContext

internal object AttachmentSources {
    fun mime(attachment: MessageAttachment): String = attachment.mimeType ?: when (attachment.localSource) {
        AttachmentLocalSource.Markdown, AttachmentLocalSource.LongMarkdown -> "text/markdown"
        AttachmentLocalSource.AudioClip -> "audio/wav"
        AttachmentLocalSource.AndroidPackage, AttachmentLocalSource.InvalidPackage -> PackageAttachments.Mime
        null -> if (attachment.label.endsWith(".pdf",true)) "application/pdf" else "application/octet-stream"
        else -> "text/plain"
    }
    fun open(context: Context, attachment: MessageAttachment): InputStream? {
        if (!attachment.bytesAvailable) return null
        attachment.externalUri?.let {
            val uri = it.toUri(); if (uri.scheme != "content") return null
            return context.contentResolver.openInputStream(uri)
        }
        return when (attachment.localSource) {
            AttachmentLocalSource.Markdown -> MessageReadingExamples.document.byteInputStream()
            AttachmentLocalSource.PlainText -> "Meet at the east gate at 09:00.\nBring water, a map and a warm layer.\n".byteInputStream()
            AttachmentLocalSource.EmptyText -> byteArrayOf().inputStream()
            AttachmentLocalSource.InvalidEncoding -> byteArrayOf(0xc3.toByte(),0x28).inputStream()
            AttachmentLocalSource.BinaryText -> byteArrayOf(0,1,2,3).inputStream()
            AttachmentLocalSource.LargeText -> ByteArray(TextAttachments.MaximumBytes+1) { 'A'.code.toByte() }.inputStream()
            AttachmentLocalSource.LongMarkdown -> ("# Trail journal\n\n" + "A quiet stretch by the river.\n\n".repeat(3000)).byteInputStream()
            AttachmentLocalSource.AudioClip -> context.resources.openRawResource(R.raw.shared_audio_clip)
            AttachmentLocalSource.AndroidPackage -> packageExample().inputStream()
            AttachmentLocalSource.InvalidPackage -> "This file is not an Android package.".byteInputStream()
            null -> when (attachment.label.lowercase(java.util.Locale.ROOT)) {
                "project brief.pdf" -> context.resources.openRawResource(R.raw.project_brief)
                "project notes.pdf" -> context.resources.openRawResource(R.raw.project_notes)
                "trail plan.pdf" -> context.resources.openRawResource(R.raw.trail_plan)
                "weekend notes.pdf" -> context.resources.openRawResource(R.raw.weekend_notes)
                else -> null
            }
        }
    }
    suspend fun readText(context: Context, attachment: MessageAttachment): TextAttachmentResult = withContext(Dispatchers.IO) {
        try {
            val bytes = open(context,attachment)?.use { input ->
                ByteArrayOutputStream().use { output ->
                    val buffer = ByteArray(8192)
                    while (output.size() <= TextAttachments.MaximumBytes) {
                        coroutineContext.ensureActive()
                        val count = input.read(buffer,0,minOf(buffer.size,TextAttachments.MaximumBytes+1-output.size()))
                        if (count < 0) break
                        if (count == 0) continue
                        output.write(buffer,0,count)
                    }
                    output.toByteArray()
                }
            } ?: return@withContext TextAttachmentResult.Failed(TextAttachmentFailure.Unavailable)
            coroutineContext.ensureActive(); TextAttachments.decode(bytes)
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { TextAttachmentResult.Failed(TextAttachmentFailure.Unavailable) }
    }
    /** Container-shape check only; this does not verify a signer or installability. */
    fun validPackage(file: File): Boolean = runCatching {
        ZipFile(file).use { zip ->
            val manifest = zip.getEntry("AndroidManifest.xml")?.takeUnless { it.isDirectory } ?: return@use false
            val header = ByteArray(8)
            java.io.DataInputStream(zip.getInputStream(manifest)).use { it.readFully(header) }
            val size = (4..7).sumOf { (header[it].toLong() and 255) shl ((it-4)*8) }
            header[0] == 3.toByte() && header[1] == 0.toByte() && header[2] == 8.toByte() && header[3] == 0.toByte() && size == manifest.size
        }
    }.getOrDefault(false)
    private fun packageExample(): ByteArray = ByteArrayOutputStream().use { output ->
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("AndroidManifest.xml")); zip.write(byteArrayOf(3,0,8,0,8,0,0,0)); zip.closeEntry()
        }
        output.toByteArray()
    }
}
