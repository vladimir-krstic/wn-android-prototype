package dev.ipf.whitenoise.model

import kotlin.math.abs

enum class MessageFactStatus { Sending, Sent, Received, Failed, Streaming }
enum class MessageFactTime { Created, Sent, Received }
data class MessageFacts(val status: MessageFactStatus, val timeKind: MessageFactTime, val primaryMillis: Long,
    val senderClaimedMillis: Long?, val expiresMillis: Long?) {
    companion object {
        fun from(message: ChatMessage, profileId: String): MessageFacts {
            val incoming = message.authorId != profileId
            val created = message.createdAtMillis?.takeIf { it > 0 } ?: GlobalSearchClock.timestamp(message)
            val received = message.receivedAtMillis?.takeIf { it > 0 }
            val status = when (message.deliveryState) {
                MessageDeliveryState.Streaming -> MessageFactStatus.Streaming
                MessageDeliveryState.Sending -> if (incoming) MessageFactStatus.Received else MessageFactStatus.Sending
                MessageDeliveryState.Failed -> if (incoming) MessageFactStatus.Received else MessageFactStatus.Failed
                MessageDeliveryState.Sent -> if (incoming) MessageFactStatus.Received else MessageFactStatus.Sent
            }
            val kind = if (incoming) MessageFactTime.Received else when (status) {
                MessageFactStatus.Sending, MessageFactStatus.Failed -> MessageFactTime.Created
                else -> MessageFactTime.Sent
            }
            val original = message.createdAtMillis?.takeIf { incoming && it > 0 && received != null && abs(it - received) > 5_000 }
            return MessageFacts(status, kind, if (incoming) received ?: created else created, original, message.expiresAtMillis?.takeIf { it > 0 })
        }
    }
}
