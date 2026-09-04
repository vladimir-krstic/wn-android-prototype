package dev.ipf.whitenoise.model

enum class IncomingExample(val label: String) {
    NotificationMessage("Message notification"), NotificationInvite("Invitation notification"), NotificationRemoved("Membership removal notification"), NotificationOtherProfile("Notification for another profile"),
    Text("Shared text"), Photo("Shared photo"), Video("Shared video"), Audio("Shared audio"), Document("Shared document"), Mixed("Text and mixed files"), Overflow("More files than fit in the draft"), Unavailable("Unavailable shared file"), TooLarge("Shared file too large"), Empty("Empty share"),
    Direct("Direct Share target"), MissingDirect("Missing Direct Share target"), WrongOwner("Direct Share belongs to another profile"), Conversation("Conversation shortcut"), MissingConversation("Deleted conversation shortcut"), MissingProfile("Signed-out shortcut owner"), ProfileLink("Canonical profile link"), InvalidProfile("Invalid profile link")
}
object IncomingExamples {
    fun entry(example: IncomingExample, profile: Profile, profiles: List<Profile>): IncomingEntry {
        val photo = MessageAttachment("photo",MessageAttachmentKind.Photo,"Trail photo",images = listOf(ProfileAvatar.Asset(AvatarAsset.GardenClub)),mimeType = "image/jpeg")
        val video = MessageAttachment("video",MessageAttachmentKind.Video,"Trail video",images = listOf(ProfileAvatar.Asset(AvatarAsset.GardenClub)),durationSeconds = 4,mimeType = "video/mp4")
        val audio = AttachmentReadingExamples.attachments().first { it.localSource == AttachmentLocalSource.AudioClip }
        val document = AttachmentReadingExamples.attachments().first { it.localSource == AttachmentLocalSource.PlainText }
        fun stream(a: MessageAttachment) = IncomingStream(a.id,a,a.mimeType)
        val target = IncomingTarget(profile.id,profile.chats.firstOrNull { IncomingSharing.canStage(profile,it) }?.id ?: "unavailable-chat")
        fun notification(p: Profile): IncomingEntry {
            val chat = p.chats.firstOrNull { it.timeline.filterIsInstance<ChatTimelineEntry.Message>().any { m -> !m.message.isDeleted } }
            return IncomingEntry.Notification(NotificationTarget(p.id,chat?.id ?: "unavailable-chat",
                chat?.timeline?.filterIsInstance<ChatTimelineEntry.Message>()?.firstOrNull { !it.message.isDeleted }?.id ?: "unavailable-message"))
        }
        return when(example) {
            IncomingExample.NotificationMessage -> notification(profile)
            IncomingExample.NotificationOtherProfile -> notification(profiles.firstOrNull { it.id != profile.id } ?: profile)
            IncomingExample.NotificationInvite -> IncomingEntry.Notification(NotificationTarget(profile.id,
                profile.chats.firstOrNull { it.membership == ChatMembership.Invited }?.id ?: "unavailable-invitation",kind = NotificationTargetKind.Invite))
            IncomingExample.NotificationRemoved -> IncomingEntry.Notification(NotificationTarget(profile.id,"removed-group",kind = NotificationTargetKind.ChatList))
            IncomingExample.Conversation -> IncomingEntry.Conversation(target)
            IncomingExample.MissingConversation -> IncomingEntry.Conversation(target.copy(chatId = "unavailable-chat"))
            IncomingExample.MissingProfile -> IncomingEntry.Conversation(target.copy(profileId = "signed-out-profile"))
            IncomingExample.ProfileLink -> IncomingEntry.ProfileLink(ProfileLinks.forKey(profile.people.firstOrNull()?.publicKey ?: profile.publicKey)!!.qrUri!!)
            IncomingExample.InvalidProfile -> IncomingEntry.ProfileLink("marmot://profile/not-a-public-key")
            else -> IncomingEntry.Share(when(example) {
                IncomingExample.Photo -> IncomingPayload(streams = listOf(stream(photo)))
                IncomingExample.Video -> IncomingPayload(streams = listOf(stream(video)))
                IncomingExample.Audio -> IncomingPayload(streams = listOf(stream(audio)))
                IncomingExample.Document -> IncomingPayload(streams = listOf(stream(document)))
                IncomingExample.Mixed -> IncomingPayload("Here are the plans for Saturday.",listOf(stream(photo),stream(video),stream(audio),stream(document)))
                IncomingExample.Overflow -> IncomingPayload(streams = List(12) { stream(photo.copy(id="photo-$it")) } + List(12) { stream(document.copy(id="document-$it")) })
                IncomingExample.Unavailable -> IncomingPayload(streams = listOf(stream(document).copy(readable=false)))
                IncomingExample.TooLarge -> IncomingPayload(streams = listOf(stream(document).copy(failure=IncomingContentFailure.TooLarge)))
                IncomingExample.Empty -> IncomingPayload()
                else -> IncomingPayload("See you at the trailhead on Saturday.")
            },shortcut = when(example) {
                IncomingExample.Direct -> target
                IncomingExample.MissingDirect -> target.copy(chatId="unavailable-chat")
                IncomingExample.WrongOwner -> target.copy(profileId=profiles.firstOrNull { it.id!=profile.id }?.id ?: "another-profile")
                else -> null
            })
        }
    }
}
