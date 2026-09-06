package dev.ipf.whitenoise.ui.chats

import dev.ipf.whitenoise.model.Profile

internal data class ProfileSwitcherPresentation(
    val profile: Profile,
    val isActive: Boolean,
    val unreadCount: Int,
)

internal fun profileSwitcherPresentation(
    profiles: List<Profile>,
    activeProfileId: String?,
): List<ProfileSwitcherPresentation> {
    val active = profiles.firstOrNull { it.id == activeProfileId }
    val ordered = buildList {
        active?.let(::add)
        profiles.filterTo(this) { it.id != activeProfileId }
    }
    return ordered.map { profile ->
        ProfileSwitcherPresentation(
            profile = profile,
            isActive = profile.id == activeProfileId,
            unreadCount = profile.chats
                .asSequence()
                .filterNot { it.isArchived || it.hasEndedMembership }
                .sumOf { chat ->
                    chat.unreadCount.coerceAtLeast(if (chat.isMarkedUnread) 1 else 0)
                },
        )
    }
}
