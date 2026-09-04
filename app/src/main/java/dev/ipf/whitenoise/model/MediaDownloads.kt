package dev.ipf.whitenoise.model

import androidx.annotation.StringRes
import dev.ipf.whitenoise.R

enum class DownloadMediaType(@param:StringRes val labelRes: Int) {
    Photos(R.string.download_photos), Videos(R.string.download_videos),
    Audio(R.string.download_audio), Files(R.string.download_files),
}
enum class DownloadNetwork(@param:StringRes val labelRes: Int) {
    Wifi(R.string.download_wifi), Mobile(R.string.download_mobile),
    Roaming(R.string.download_roaming), Metered(R.string.download_metered),
}

/** Conditions overlap: metered Wi-Fi must satisfy both cells, for example. */
data class MediaDownloadMatrix(val enabled: Set<Pair<DownloadMediaType, DownloadNetwork>> = defaults) {
    fun allows(type: DownloadMediaType, network: DownloadNetwork) = type to network in enabled
    fun allows(type: DownloadMediaType, conditions: Set<DownloadNetwork>) =
        conditions.isNotEmpty() && conditions.all { allows(type, it) }
    fun change(type: DownloadMediaType, network: DownloadNetwork, enabled: Boolean) =
        copy(enabled = if (enabled) this.enabled + (type to network) else this.enabled - (type to network))
    companion object {
        val defaults = setOf(DownloadMediaType.Photos to DownloadNetwork.Wifi, DownloadMediaType.Audio to DownloadNetwork.Wifi)
    }
}

/** Developer-owned connectivity examples; never reads or claims device connectivity. */
enum class DownloadNetworkExample(val developerLabel: String, val conditions: Set<DownloadNetwork>) {
    Wifi("Unmetered Wi-Fi", setOf(DownloadNetwork.Wifi)),
    Mobile("Metered mobile data", setOf(DownloadNetwork.Mobile, DownloadNetwork.Metered)),
    MeteredWifi("Metered Wi-Fi", setOf(DownloadNetwork.Wifi, DownloadNetwork.Metered)),
    Roaming("Roaming mobile data", setOf(DownloadNetwork.Mobile, DownloadNetwork.Roaming, DownloadNetwork.Metered)),
    Unknown("Unknown network", emptySet()), Offline("Offline", emptySet()),
}

val MessageAttachment.downloadMediaType: DownloadMediaType? get() = when (kind) {
    MessageAttachmentKind.Photo, MessageAttachmentKind.Photos, MessageAttachmentKind.Gif -> DownloadMediaType.Photos
    MessageAttachmentKind.Video -> DownloadMediaType.Videos
    MessageAttachmentKind.Voice -> DownloadMediaType.Audio
    MessageAttachmentKind.File -> if (mimeType?.startsWith("audio/") == true) DownloadMediaType.Audio else DownloadMediaType.Files
    MessageAttachmentKind.Contact, MessageAttachmentKind.Link -> null
}

data class DownloadQueueCounts(val automatic: Int = 0, val manual: Int = 0, val active: Int = 0, val failed: Int = 0)
fun Profile.downloadQueueCounts(): DownloadQueueCounts {
    val transfers = chats.flatMap { it.timeline }.filterIsInstance<ChatTimelineEntry.Message>()
        .filterNot { it.message.isDeleted }.flatMap { it.message.attachments }.mapNotNull { it.transfer }
        .filter { it.direction == AttachmentTransferDirection.Download }
    return DownloadQueueCounts(
        automatic = transfers.count { it.phase == AttachmentTransferPhase.Queued && it.origin == AttachmentTransferOrigin.Automatic },
        manual = transfers.count { it.phase == AttachmentTransferPhase.Queued && it.origin == AttachmentTransferOrigin.Manual },
        active = transfers.count { it.phase == AttachmentTransferPhase.Active },
        failed = transfers.count { it.phase == AttachmentTransferPhase.Failed || it.phase == AttachmentTransferPhase.CacheMiss },
    )
}

/** Global policy affects only future imports; prepared attachments retain their byte policy. */
fun Chat.effectivePhotoQuality(settings: ProfileSettings): PhotoQuality =
    if (draftPhotoQualityExplicit) draftPhotoQuality else settings.sentMediaQuality.photoQuality
