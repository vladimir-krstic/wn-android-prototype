package dev.ipf.whitenoise.navigation

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
enum class OnboardingOrigin {
    Initial,
    AddProfile,
}

sealed interface AppRoute {
    @Serializable
    data class Welcome(
        val origin: OnboardingOrigin = OnboardingOrigin.Initial,
    ) : AppRoute

    @Serializable
    data class SignIn(val origin: OnboardingOrigin) : AppRoute

    @Serializable
    data class SignUp(val origin: OnboardingOrigin) : AppRoute

    @Serializable
    data object SignedIn : AppRoute

    @Serializable
    data object NewChat : AppRoute

    @Serializable
    data class Folders(val profileId: String) : AppRoute

    @Serializable
    data class EditFolder(val profileId: String, val folderId: String? = null) : AppRoute

    @Serializable
    data class PersonProfile(val personId: String, val chatId: String? = null) : AppRoute

    @Serializable
    data class GroupsInCommon(val personId: String) : AppRoute

    @Serializable
    data class NewGroup(val initialPersonId: String? = null) : AppRoute

    @Serializable
    data class GroupSetup(val selectedPersonIds: List<String>) : AppRoute

    @Serializable
    data class Conversation(
        val chatId: String,
        val openSearch: Boolean = false,
        val targetMessageId: String? = null,
    ) : AppRoute

    @Serializable
    data class MessageDetails(val chatId: String, val messageId: String) : AppRoute

    @Serializable
    data class ChatInfo(val chatId: String) : AppRoute

    @Serializable
    data class SharedContent(val chatId: String, val category: String) : AppRoute

    @Serializable
    data class EditGroup(val chatId: String) : AppRoute

    @Serializable
    data class AddGroupMembers(val chatId: String) : AppRoute

    @Serializable
    data class ChatRelays(val chatId: String) : AppRoute

    @Serializable
    data class Settings(val showProfileSwitcher: Boolean = false) : AppRoute

    @Serializable
    data object ShareConnect : AppRoute

    @Serializable
    data object EditProfile : AppRoute

    @Serializable
    data object ProfileKeys : AppRoute

    @Serializable
    data object Notifications : AppRoute

    @Serializable
    data class ConversationNotifications(val chatId: String) : AppRoute

    @Serializable
    data object Appearance : AppRoute

    @Serializable
    data object ReadAloud : AppRoute

    @Serializable
    data object Dictation : AppRoute

    @Serializable
    data object Language : AppRoute

    @Serializable
    data object PrivacySecurity : AppRoute

    @Serializable
    data object DiagnosticsImprovements : AppRoute

    @Serializable
    data object DataUsage : AppRoute

    @Serializable
    data object ProfileRelays : AppRoute

    @Serializable
    data class ProfileRelayDetails(val relayId: String) : AppRoute

    @Serializable
    data object Support : AppRoute

    @Serializable
    data object Donate : AppRoute

    @Serializable
    data object ManageProfiles : AppRoute

    @Serializable
    data object DeveloperTools : AppRoute

    @Serializable
    data class Diagnostics(val chatId: String? = null) : AppRoute

    @Serializable
    data object KeyPackages : AppRoute

    @Serializable
    data class ConversationDebug(val chatId: String) : AppRoute

    companion object {
        val startDestination: AppRoute = Welcome()
    }
}
