package dev.ipf.whitenoise.screenshots

import androidx.annotation.DrawableRes

internal enum class RelayStatus {
    Connected,
    Reconnecting,
    Disconnected,
}

internal data class RelayFixture(
    val name: String,
    val url: String,
    val status: RelayStatus,
)

internal val relayFixtures =
    listOf(
        RelayFixture("Primal", "wss://relay.primal.net", RelayStatus.Connected),
        RelayFixture("Damus", "wss://relay.damus.io", RelayStatus.Connected),
        RelayFixture("nos.lol", "wss://nos.lol", RelayStatus.Connected),
        RelayFixture("Nostr.Band", "wss://relay.nostr.band", RelayStatus.Connected),
        RelayFixture("Vertex", "wss://relay.vertexlab.io (Read Only)", RelayStatus.Connected),
        RelayFixture("White Noise Profile", "wss://relay.whitenoise.chat", RelayStatus.Reconnecting),
        RelayFixture("White Noise Inbox", "wss://inbox.whitenoise.chat", RelayStatus.Disconnected),
    )

internal data class ProfileFixture(
    val name: String,
    val npub: String,
    @param:DrawableRes val avatar: Int,
    val selected: Boolean = false,
)

internal val profileFixtures =
    listOf(
        ProfileFixture(
            name = "Marmota",
            npub = "npub1m8z7q4k…8x4k",
            avatar = R.drawable.profile_avatar_marmota,
            selected = true,
        ),
        ProfileFixture(
            name = "Pebble",
            npub = "npub1p8c4y6m…c5n7",
            avatar = R.drawable.profile_avatar_pebble,
        ),
        ProfileFixture(
            name = "Open Quill",
            npub = "npub1q2v9n6t…m2d9",
            avatar = R.drawable.profile_avatar_open_quill,
        ),
        ProfileFixture(
            name = "Cipher Wheel",
            npub = "npub1k9w3h7s…f8r2",
            avatar = R.drawable.profile_avatar_cipher_wheel,
        ),
        ProfileFixture(
            name = "Free Signal",
            npub = "npub1n7d2p5x…w6r4",
            avatar = R.drawable.profile_avatar_free_signal,
        ),
    )

internal data class ChatFixture(
    val name: String,
    val preview: String,
    val timestamp: String,
    @param:DrawableRes val avatar: Int,
    val pinned: Boolean = false,
    val unread: String? = null,
    val muted: Boolean = false,
    val previewPrefix: String? = null,
)

internal val chatFixtures =
    listOf(
        ChatFixture(
            name = "Nostr Devs",
            preview = "Marmot draft merged. Time to test the new flow.",
            previewPrefix = "Tim:",
            timestamp = "Yesterday",
            avatar = R.drawable.legacy_avatar_nostr_devs,
            pinned = true,
        ),
        ChatFixture(
            name = "Radia Perlman",
            preview = "Let the network heal itself; loops (and censors) break.",
            timestamp = "Sunday",
            avatar = R.drawable.legacy_avatar_radia_perlman,
            pinned = true,
        ),
        ChatFixture(
            name = "Hal Finney",
            preview = "Running bitcoin… still amazes me how far we’ve come.",
            timestamp = "Now",
            avatar = R.drawable.legacy_avatar_hal_finney,
        ),
        ChatFixture(
            name = "Judith “St. Jude” Milhon",
            preview = "Hacking means finding clever ways around dumb rules.",
            timestamp = "2m",
            avatar = R.drawable.legacy_avatar_judith_milhon,
            unread = "2",
        ),
        ChatFixture(
            name = "Marmots",
            preview = "Big plans—or no plans at all!",
            previewPrefix = "Jude:",
            timestamp = "9m",
            avatar = R.drawable.legacy_avatar_marmots,
            unread = "99+",
        ),
        ChatFixture(
            name = "Whitfield Diffie",
            preview = "Key exchange since ’76—still my favorite handshake.",
            previewPrefix = "You:",
            timestamp = "1h",
            avatar = R.drawable.legacy_avatar_whitfield_diffie,
        ),
        ChatFixture(
            name = "Richard Stallman",
            preview = "Free as in freedom, not as in beer. Keep your keys libre.",
            timestamp = "8h",
            avatar = R.drawable.legacy_avatar_richard_stallman,
            muted = true,
        ),
        ChatFixture(
            name = "Eric Hughes",
            preview = "Cypherpunks still write code. Ship the patch?",
            timestamp = "Yesterday",
            avatar = R.drawable.legacy_avatar_eric_hughes,
            unread = "12",
        ),
        ChatFixture(
            name = "David Chaum",
            preview = "Privacy is necessary for an open society in the electronic age.",
            timestamp = "Saturday",
            avatar = R.drawable.legacy_avatar_david_chaum,
        ),
        ChatFixture(
            name = "Satoshi Nakamoto",
            preview = "Chancellor on Brink of Second Bailout for Banks.",
            timestamp = "Friday",
            avatar = R.drawable.legacy_avatar_satoshi_nakamoto,
        ),
        ChatFixture(
            name = "Fiatjaf",
            preview = "Portable identity for the win.",
            timestamp = "Thursday",
            avatar = R.drawable.avatar_fiatjaf,
        ),
        ChatFixture(
            name = "Mina Park",
            preview = "Let’s pick this up after lunch",
            previewPrefix = "Draft:",
            timestamp = "Thursday",
            avatar = R.drawable.avatar_mina_park,
        ),
        ChatFixture(
            name = "Theo Grant",
            preview = "Voice message",
            timestamp = "Wednesday",
            avatar = R.drawable.avatar_theo_grant,
            unread = "1",
        ),
        ChatFixture(
            name = "Maya Chen",
            preview = "Can you send the latest version when you have a moment?",
            timestamp = "Monday",
            avatar = R.drawable.avatar_maya_chen,
            unread = "1",
        ),
    )
