package dev.ipf.whitenoise.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeveloperModelsTest {
    @Test
    fun developerFixturesStartLockedWithSanitizedArtifacts() {
        val tools = ProfileFixtures.marmota.developerTools
        assertFalse(tools.isEnabled)
        assertFalse(tools.debugMode)
        assertFalse(tools.anonymousTelemetry)
        assertFalse(tools.auditLogging)
        assertEquals(2, tools.auditFiles.size)
        assertEquals(32_000, tools.auditFiles.sumOf(AuditFile::byteCount))
        assertTrue(tools.auditFiles.none { '/' in it.filename || '\\' in it.filename })
    }

    @Test
    fun disablingToolsStopsFeaturesButPreservesArtifacts() {
        val enabled = ProfileFixtures.marmota.developerTools.copy(
            isEnabled = true,
            debugMode = true,
            anonymousTelemetry = true,
            auditLogging = true,
        )
        val disabled = enabled.withEnabled(false)
        assertFalse(disabled.isEnabled)
        assertFalse(disabled.debugMode)
        assertFalse(disabled.anonymousTelemetry)
        assertFalse(disabled.auditLogging)
        assertEquals(enabled.auditFiles, disabled.auditFiles)
        assertEquals(enabled.keyPackage, disabled.keyPackage)
    }

    @Test
    fun conversationDebugAccessNeedsExistingChatAndBothGates() {
        val profile = ProfileFixtures.marmota
        assertEquals(ConversationDebugAccess.Unavailable, ConversationDebugPolicy.access(profile, "missing"))
        assertEquals(ConversationDebugAccess.Disabled, ConversationDebugPolicy.access(profile, "maya-chen"))
        val enabled = profile.copy(developerTools = profile.developerTools.copy(isEnabled = true, debugMode = true))
        assertEquals(ConversationDebugAccess.Enabled, ConversationDebugPolicy.access(enabled, "maya-chen"))
        assertTrue(ConversationDebugPolicy.showsToolbarAction(enabled, "fiatjaf"))
        assertTrue(ConversationDebugPolicy.showsToolbarAction(enabled, ChatFixtures.SUPPORT_CHAT_ID))
        assertFalse(ConversationDebugPolicy.showsToolbarAction(enabled, "maya-chen"))
    }

    @Test
    fun snapshotsDeriveDirectAndGroupFactsWithoutMessageContent() {
        val profile = ProfileFixtures.marmota
        val direct = ConversationDebugPolicy.snapshot(profile, "maya-chen")!!
        val group = ConversationDebugPolicy.snapshot(profile, "weekend-walks")!!
        assertEquals("Active", direct.lifecycle)
        assertNull(direct.memberCount)
        assertEquals(1, direct.relayCount)
        assertEquals("Registered", direct.push.registrationStatus)
        assertEquals(profile.chats.first { it.id == "weekend-walks" }.members.size, group.memberCount)
        assertEquals("Admin", group.currentRole)
        assertEquals(1, group.push.staleTokenCount)
        assertEquals(listOf(32769, 32771, 32772, 32774, 32777, 32779, 32780), group.requiredEventKinds)
    }

    @Test
    fun diagnosticSummaryIsDeterministicAndSensitiveValueFree() {
        val profile = ProfileFixtures.marmota
        val first = ConversationDebugPolicy.snapshot(profile, "maya-chen")!!
        val repeated = ConversationDebugPolicy.snapshot(profile, "maya-chen")!!
        val message = profile.chats.first { it.id == "maya-chen" }.timeline
            .filterIsInstance<ChatTimelineEntry.Message>().first().message.text
        assertEquals(first, repeated)
        assertFalse(first.diagnosticSummary.contains(profile.publicKey))
        assertFalse(first.diagnosticSummary.contains(message))
        assertFalse(first.diagnosticSummary.contains("npub1"))
        assertFalse(first.diagnosticSummary.contains(first.mlsGroupId))
        assertTrue(first.diagnosticSummary.contains("Required Event Kinds"))
    }

    @Test
    fun wipePhraseIsStableLowercaseUniqueAndStrictInsideWhitespace() {
        val ids = listOf("marmota", "open-quill", "cipher-wheel")
        val first = WipeConfirmationPhrase.make(ids)
        val second = WipeConfirmationPhrase.make(ids.reversed())
        assertEquals(first, second)
        assertEquals(3, first.split(' ').distinct().size)
        assertEquals(first.lowercase(), first)
        assertTrue(WipeConfirmationPhrase.matches(" $first\n", first))
        assertFalse(WipeConfirmationPhrase.matches(first.uppercase(), first))
        assertFalse(WipeConfirmationPhrase.matches(first.replace(" ", "  "), first))
    }

    @Test
    fun publishedPackageReplacesRatherThanAdds() {
        assertNotEquals(KeyPackage.Fixture, KeyPackage.PublishedFixture)
        assertEquals("Just now", KeyPackage.PublishedFixture.published)
    }
}
