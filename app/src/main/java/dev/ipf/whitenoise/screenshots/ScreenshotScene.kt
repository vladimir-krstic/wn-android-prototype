package dev.ipf.whitenoise.screenshots

enum class ScreenshotScene(
    val route: String,
    val title: String,
    val description: String,
) {
    Relays(
        route = "relays",
        title = "Relays",
        description = "Profile relay health and advanced configuration",
    ),
    ProfileSwitcher(
        route = "profile-switcher",
        title = "Profile switcher",
        description = "Settings with the multi-profile sheet open",
    ),
    Chats(
        route = "chats",
        title = "Chats",
        description = "Populated marketing conversation list",
    ),
    Conversation(
        route = "conversation",
        title = "Fiatjaf conversation",
        description = "Replies, reactions, and a media collage",
    ),
    ShareConnect(
        route = "share-connect",
        title = "Share & connect",
        description = "Profile sharing and QR connection",
    ),
    ;

    companion object {
        fun fromRoute(route: String?): ScreenshotScene? = entries.firstOrNull { it.route == route }
    }
}
