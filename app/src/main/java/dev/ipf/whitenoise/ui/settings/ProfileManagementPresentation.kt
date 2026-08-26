package dev.ipf.whitenoise.ui.settings

import dev.ipf.whitenoise.model.Profile

internal sealed interface ProfileManagementPresentation {
    data object Add : ProfileManagementPresentation

    data class SingleAlternate(
        val profile: Profile,
    ) : ProfileManagementPresentation

    data class MultipleAlternates(
        val previewProfiles: List<Profile>,
        val remainingCount: Int,
    ) : ProfileManagementPresentation
}

internal data class ProfileSwitcherPresentation(
    val profile: Profile,
    val isActive: Boolean,
    val unreadCount: Int,
)

internal fun profileManagementPresentation(
    profiles: List<Profile>,
    activeProfileId: String?,
): ProfileManagementPresentation {
    val alternates = profiles.filterNot { it.id == activeProfileId }
    return when (alternates.size) {
        0 -> ProfileManagementPresentation.Add
        1 -> ProfileManagementPresentation.SingleAlternate(alternates.single())
        else -> ProfileManagementPresentation.MultipleAlternates(
            previewProfiles = alternates.take(PROFILE_PREVIEW_LIMIT),
            remainingCount = (alternates.size - PROFILE_PREVIEW_LIMIT).coerceAtLeast(0),
        )
    }
}

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

private const val PROFILE_PREVIEW_LIMIT = 3
