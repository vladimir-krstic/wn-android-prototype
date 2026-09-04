package dev.ipf.whitenoise.ui.conversation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.os.ConfigurationCompat
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
internal fun MessageFactsSection(profile: Profile, message: ChatMessage) {
    val facts = MessageFacts.from(message, profile.id)
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.ROOT
    val zone = ZoneId.systemDefault()
    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(locale).withZone(zone)
    fun time(value: Long) = "${formatter.format(Instant.ofEpochMilli(value))} (${zone.id})"
    val status = stringResource(when (facts.status) {
        MessageFactStatus.Sending -> R.string.sending
        MessageFactStatus.Sent -> R.string.sent
        MessageFactStatus.Received -> R.string.received
        MessageFactStatus.Failed -> R.string.not_delivered
        MessageFactStatus.Streaming -> R.string.message_streaming
    })
    Surface(Modifier.fillMaxWidth().testTag("message.facts"), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column {
            FactRow(stringResource(R.string.message_status), status)
            FactRow(stringResource(when (facts.timeKind) {
                MessageFactTime.Created -> R.string.message_created_at
                MessageFactTime.Sent -> R.string.message_sent_at
                MessageFactTime.Received -> R.string.message_received_at
            }), time(facts.primaryMillis))
            facts.senderClaimedMillis?.let { FactRow(stringResource(R.string.message_sender_time), time(it)) }
            facts.expiresMillis?.let { FactRow(stringResource(R.string.message_expires_at), time(it)) }
            FactRow(stringResource(R.string.message_id), message.id, stringResource(R.string.message_copy_id))
            if (message.authorId != profile.id) profile.people.firstOrNull { it.id == message.authorId }?.let { person ->
                FactRow(stringResource(R.string.message_sender_public_key), person.publicKey, stringResource(R.string.message_copy_sender_key))
            }
        }
    }
}

@Composable
private fun FactRow(label: String, value: String, copyLabel: String? = null) {
    val context = LocalContext.current
    ListItem(supportingContent = { Text(value, maxLines = if (copyLabel == null) Int.MAX_VALUE else 2, overflow = TextOverflow.Ellipsis) },
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        trailingContent = if (copyLabel == null) null else ({
            IconButton(onClick = { (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText(label, value)) }) {
                Icon(painterResource(R.drawable.ic_content_copy), contentDescription = copyLabel)
            }
        }),
    ) { Text(label) }
}
