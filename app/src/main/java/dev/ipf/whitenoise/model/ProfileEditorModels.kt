package dev.ipf.whitenoise.model

import java.util.Locale

object LightningAddress {
    private val localPattern = Regex("^[a-zA-Z0-9._-]+$")
    private val domainLabel = Regex("^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$")
    fun normalize(raw: String): String? {
        val value = raw.trim()
        if (value.isEmpty()) return ""
        val parts = value.split('@')
        if (parts.size != 2 || !localPattern.matches(parts[0]) || parts[0] in setOf(".", "..")) return null
        val domain = parts[1].lowercase(Locale.ROOT)
        val labels = domain.split('.')
        if (domain.length > 253 || labels.size < 2 || labels.any { !domainLabel.matches(it) }) return null
        return "${parts[0]}@$domain"
    }
}

data class ProfileEditDraft(
    val name: String,
    val about: String,
    val avatar: ProfileAvatar,
    val banner: ProfileAvatar?,
    val nostrAddress: String,
    val lightningAddress: String,
) {
    fun normalized(): ProfileEditDraft? {
        val lightning = LightningAddress.normalize(lightningAddress) ?: return null
        if (name.isBlank() || !ProfileSettingsPolicy.isValidNostrAddress(nostrAddress)) return null
        return copy(name = name.trim(), about = about.trim(), nostrAddress = nostrAddress.trim(), lightningAddress = lightning)
    }
    companion object {
        fun from(profile: Profile) = ProfileEditDraft(profile.name, profile.about, profile.avatar, profile.banner, profile.nostrAddress, profile.lightningAddress)
    }
}

enum class ProfileSavePhase { CheckingLightning, Publishing, Failed }
enum class ProfileSaveFailure { UnresolvedLightning, NoConnection, PublishFailed }
enum class ProfileSaveScenario(val developerLabel: String) {
    Success("Profile save succeeds"),
    UnresolvedLightning("Lightning address unresolved"),
    NoConnection("No connection during save"),
    PublishFailure("Profile publishing fails"),
}
data class ProfileSaveAttempt(
    val id: Long, val profileId: String, val draft: ProfileEditDraft,
    val phase: ProfileSavePhase, val scenario: ProfileSaveScenario,
    val failure: ProfileSaveFailure? = null,
) {
    val isBusy: Boolean get() = phase != ProfileSavePhase.Failed
}

object ProfileNameSuggestions {
    private val names = listOf("Quiet River", "Silver Finch", "Open Meadow", "Gentle Signal", "Night Orchard", "Cedar Trail")
    fun next(current: String, index: Int): String {
        val candidates = names.filterNot { it.equals(current.trim(), true) }
        return candidates[Math.floorMod(index, candidates.size)]
    }
}

/** Geometry shared by the image viewer's gestures and accessible zoom buttons. */
object ProfileImageZoom {
    fun maxPan(viewportWidth: Int, viewportHeight: Int, imageWidth: Int, imageHeight: Int, scale: Float): Pair<Float, Float> {
        if (viewportWidth <= 0 || viewportHeight <= 0 || imageWidth <= 0 || imageHeight <= 0) return 0f to 0f
        val fit = minOf(viewportWidth.toFloat() / imageWidth, viewportHeight.toFloat() / imageHeight)
        return ((imageWidth * fit * scale - viewportWidth) / 2).coerceAtLeast(0f) to
            ((imageHeight * fit * scale - viewportHeight) / 2).coerceAtLeast(0f)
    }
}

data class ProfileImageDraft(val profileId: String, val avatar: ProfileAvatar, val banner: ProfileAvatar?)
