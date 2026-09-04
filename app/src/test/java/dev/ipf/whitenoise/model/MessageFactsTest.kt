package dev.ipf.whitenoise.model

import org.junit.Assert.*
import org.junit.Test

class MessageFactsTest {
    private val message = ChatMessage("m", "me", 3, "Today", 600, "10:00", "Hello", createdAtMillis = 100_000)
    @Test fun failedAndPendingOutgoingUseCreatedWhileSentUsesSent() {
        listOf(MessageDeliveryState.Sending, MessageDeliveryState.Failed).forEach {
            assertEquals(MessageFactTime.Created, MessageFacts.from(message.copy(deliveryState = it), "me").timeKind)
        }
        assertEquals(MessageFactTime.Sent, MessageFacts.from(message, "me").timeKind)
        assertEquals(MessageFactStatus.Failed, MessageFacts.from(message.copy(deliveryState = MessageDeliveryState.Failed), "me").status)
    }
    @Test fun incomingPrefersLocalReceiptAndOnlyShowsMeaningfulSenderTimeSkew() {
        val incoming = message.copy(authorId = "friend", receivedAtMillis = 105_000)
        val facts = MessageFacts.from(incoming, "me")
        assertEquals(MessageFactStatus.Received, facts.status); assertEquals(MessageFactTime.Received, facts.timeKind)
        assertEquals(105_000, facts.primaryMillis); assertNull(facts.senderClaimedMillis)
        assertEquals(100_000L, MessageFacts.from(incoming.copy(receivedAtMillis = 105_001), "me").senderClaimedMillis)
        assertNull(MessageFacts.from(incoming.copy(createdAtMillis = null), "me").senderClaimedMillis)
    }
    @Test fun streamingAndExpiryAreIndependentFactsWithoutReadReceipt() {
        val facts = MessageFacts.from(message.copy(authorId = "friend", deliveryState = MessageDeliveryState.Streaming, expiresAtMillis = 200_000), "me")
        assertEquals(MessageFactStatus.Streaming, facts.status); assertEquals(MessageFactTime.Received, facts.timeKind); assertEquals(200_000L, facts.expiresMillis)
        assertNull(MessageFacts.from(message.copy(expiresAtMillis = 0), "me").expiresMillis)
    }
    @Test fun legacyLabelsHaveFixedFallbackAndMissingReceiptDoesNotInventAnotherTimestamp() {
        val legacy = message.copy(authorId = "friend", createdAtMillis = null, receivedAtMillis = 0)
        val facts = MessageFacts.from(legacy, "me")
        assertEquals(GlobalSearchClock.timestamp(legacy), facts.primaryMillis); assertNull(facts.senderClaimedMillis)
    }
}
