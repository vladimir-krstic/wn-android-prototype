package dev.ipf.whitenoise.model

import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import dev.ipf.whitenoise.ui.settings.qrMatrix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileSettingsTest {
    @Test
    fun profileAndChatColorSettingsStayIndependentlyOwned() {
        val profile = ProfileFixtures.marmota
        val first = profile.chats[0]
        val second = profile.chats[1]
        val colors = profile.settings.colors.updateTheme(AppearanceColorTheme.Light) {
            it.copy(actionArgb = 0xFF1D4ED8L, mineBubbleArgb = 0xFF15803DL)
        }
        val changed = profile.copy(
            settings = profile.settings.copy(colors = colors),
            chats = profile.chats.map { if (it.id == first.id) it.copy(bubbleColors = ChatBubbleColorOverrides(mineArgb = 0xFFB91C1CL)) else it },
        )
        assertEquals(0xFF1D4ED8L, changed.settings.colors.light.actionArgb)
        assertEquals(0xFFB91C1CL, changed.chats[0].bubbleColors.mineArgb)
        assertEquals(ChatBubbleColorOverrides(), changed.chats.first { it.id == second.id }.bubbleColors)
        assertEquals(AppearanceColorPreferences(), profile.settings.colors)
    }
    @Test
    fun appearanceAndLanguageDefaultsMatchTheAcceptedProfilePreferences() {
        val settings = ProfileSettings()
        assertEquals(AppearancePreference.System, settings.appearance)
        assertEquals(LanguagePreference.System, settings.language)
        assertEquals(
            listOf(
                "System default",
                "English",
                "German",
                "Spanish",
                "French",
                "Italian",
                "Portuguese",
                "Serbian",
                "Russian",
                "Turkish",
                "Chinese (Simplified)",
                "Chinese (Traditional)",
            ),
            LanguagePreference.entries.map(LanguagePreference::label),
        )
    }

    @Test
    fun relayFixturesCoverSevenStableStatesAndThreeRoles() {
        assertEquals(7, ProfileRelayFixtures.defaults.size)
        assertEquals(RelayConnectionStatus.Connected, ProfileRelayFixtures.defaults.first().status)
        assertEquals(RelayRole.entries.toSet(), ProfileRelayFixtures.defaults.flatMap { it.roles }.toSet())
        assertTrue(ProfileRelayFixtures.defaults.any(ProfileRelay::isReadOnly))
        assertTrue(ProfileRelayFixtures.defaults.any { it.status == RelayConnectionStatus.Reconnecting })
        assertTrue(ProfileRelayFixtures.defaults.any { it.status == RelayConnectionStatus.Disconnected })
    }

    @Test
    fun chatAvailabilityUsesOnlyAssignedOperationalRelays() {
        val defaults = ProfileRelayFixtures.defaults
        assertEquals(
            listOf("wss://relay.primal.net", "wss://relay.damus.io", "wss://nos.lol"),
            ProfileRelayFixtures.chatMessageUrls(defaults),
        )
        val disconnected = defaults.map { relay ->
            if (RelayRole.ChatMessages in relay.roles && relay.status == RelayConnectionStatus.Connected) {
                relay.copy(status = RelayConnectionStatus.Disconnected)
            } else {
                relay
            }
        }
        assertFalse(ProfileSettingsPolicy.hasChatMessageRelay(ProfileSettings(relays = disconnected)))
        assertEquals(
            RelayRoleAvailability.Reconnecting,
            ProfileRelayFixtures.availability(disconnected, RelayRole.ChatMessages),
        )
    }

    @Test
    fun customRelayValidationNormalizesAndDeduplicates() {
        val custom = ProfileRelayFixtures.add(ProfileRelayFixtures.defaults, " WSS://Relay.Example.com/path/ ")!!
        assertEquals("wss://relay.example.com/path", custom.last().url)
        assertEquals(RelayRole.entries.toSet(), custom.last().roles)
        assertEquals(RelayConnectionStatus.Reconnecting, custom.last().status)
        assertNull(ProfileRelayFixtures.add(custom, "wss://relay.example.com/path"))
        assertNull(ProfileRelayFixtures.add(custom, "https://relay.example.com"))
    }

    @Test
    fun distinctCustomRelaysKeepUniqueIdsWhenTheirReadableSlugsCollide() {
        val first = ProfileRelayFixtures.add(emptyList(), "wss://relay.example.com/a-b")!!
        val second = ProfileRelayFixtures.add(first, "wss://relay.example.com/a/b")!!

        assertEquals(
            listOf("custom-relay-example-com-a-b", "custom-relay-example-com-a-b-2"),
            second.map(ProfileRelay::id),
        )
        assertEquals(2, second.map(ProfileRelay::id).distinct().size)
    }

    @Test
    fun addressAndExportPasswordValidationStayDeterministic() {
        assertTrue(ProfileSettingsPolicy.isValidNostrAddress("marmota@whitenoise.example"))
        assertFalse(ProfileSettingsPolicy.isValidNostrAddress("not an address"))
        assertTrue(ProfileSettingsPolicy.isValidExportPassword("eight888", "eight888"))
        assertFalse(ProfileSettingsPolicy.isValidExportPassword("short", "short"))
        assertFalse(ProfileSettingsPolicy.isValidExportPassword("password", "different"))
    }

    @Test
    fun exportPasswordStrengthMatchesPinnedIosThresholds() {
        assertNull(ProfileSettingsPolicy.exportPasswordStrength(""))
        assertEquals(
            ExportPasswordStrength.Low,
            ProfileSettingsPolicy.exportPasswordStrength("abcdefghijk"),
        )
        assertEquals(
            ExportPasswordStrength.Fair,
            ProfileSettingsPolicy.exportPasswordStrength("abcdefghijkl"),
        )
        assertEquals(
            ExportPasswordStrength.Fair,
            ProfileSettingsPolicy.exportPasswordStrength("abcdefghijklmnop"),
        )
        assertEquals(
            ExportPasswordStrength.Strong,
            ProfileSettingsPolicy.exportPasswordStrength("Secure phrase 123!"),
        )
    }

    @Test
    fun notificationPreviewExamplesMatchEveryDeterministicChoice() {
        assertEquals(
            "Maya Chen · Can you send the latest version?",
            NotificationPreviewMode.SenderAndMessage.example,
        )
        assertEquals(
            "Maya Chen · New message",
            NotificationPreviewMode.SenderOnly.example,
        )
        assertEquals(
            "White Noise · New message",
            NotificationPreviewMode.Generic.example,
        )
    }

    @Test
    fun relayRecoverySummaryGroupsEveryUnavailableRoleByCause() {
        val relays = ProfileRelayFixtures.defaults.map { relay ->
            when (relay.id) {
                "primal", "damus", "nos-lol", "nostr-band" -> relay.copy(roles = emptySet())
                "white-noise-profile" -> relay.copy(roles = setOf(RelayRole.ChatMessages))
                else -> relay
            }
        }
        val summary = ProfileRelayFixtures.recoverySummary(relays)!!
        assertTrue(summary.contains("Choose a relay for Profile"))
        assertTrue(summary.contains("No connected relay for Inbox"))
        assertTrue(summary.contains("Relays are reconnecting for Chat Messages"))
        assertTrue(summary.contains("publishing"))
        assertTrue(summary.contains("invitations"))
        assertTrue(summary.contains("new chats"))
    }

    @Test
    fun keyExportsKeepRawAndProtectedRepresentationsSeparate() {
        val raw = ProfileKeyFixtures.rawExport(ProfileFixtures.marmota)
        val protected = ProfileKeyFixtures.encryptedExport(ProfileFixtures.marmota, "password")
        assertTrue(raw.contains(ProfileKeyFixtures.PRIVATE_KEY))
        assertTrue(raw.startsWith("White Noise key export"))
        assertFalse(protected.contains(ProfileKeyFixtures.PRIVATE_KEY))
        assertTrue(protected.startsWith("WHITE NOISE PROTECTED KEY PACKAGE"))
    }

    @Test
    fun generatedProfileCodeRoundTripsThroughQrDecoder() {
        val value = ProfileFixtures.marmota.publicKey
        val matrix = qrMatrix(value)
        val scale = 4
        val width = matrix.width * scale
        val height = matrix.height * scale
        val pixels = IntArray(width * height) { index ->
            val x = (index % width) / scale
            val y = (index / width) / scale
            if (matrix[x, y]) 0xff000000.toInt() else 0xffffffff.toInt()
        }
        val bitmap = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(width, height, pixels)))
        assertEquals(value, QRCodeReader().decode(bitmap).text)
    }

    @Test
    fun shareProfileCodeCanUseOnlyItsExternalFrame() {
        val value = ProfileFixtures.marmota.publicKey
        val defaultMatrix = qrMatrix(value)
        val tightlyFramedMatrix = qrMatrix(value, marginModules = 0)

        assertEquals(defaultMatrix.width - 4, tightlyFramedMatrix.width)
        assertEquals(defaultMatrix.height - 4, tightlyFramedMatrix.height)
    }

    @Test
    fun productionLanguageChoicesUseDistinctAndroidLocaleTags() {
        assertEquals("ru", LanguagePreference.Russian.localeTag)
        assertEquals("tr", LanguagePreference.Turkish.localeTag)
        assertEquals("zh-CN", LanguagePreference.ChineseSimplified.localeTag)
        assertEquals("zh-TW", LanguagePreference.ChineseTraditional.localeTag)
        assertEquals(12, LanguagePreference.entries.map(LanguagePreference::localeTag).distinct().size)
    }
}
