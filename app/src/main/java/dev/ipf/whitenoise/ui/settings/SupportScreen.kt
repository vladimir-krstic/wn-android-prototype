package dev.ipf.whitenoise.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.model.Profile

@Composable
fun SupportScreen(
    profile: Profile,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onRelays: () -> Unit,
) {
    val hasExistingChat = profile.chats.any { it.id == "white-noise-support" }
    val canStart = hasExistingChat || profile.chatRelayUrls.isNotEmpty()

    SettingsScaffold(title = "Chat with support", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("White Noise Support", style = MaterialTheme.typography.headlineSmall)
                    Text("Questions, problems, and suggestions", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Ask how something works, report a problem, or share a suggestion.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (!canStart) {
                Text(
                    "Profile relays need attention. Choose a connected Chat Messages relay before starting a support chat.",
                    color = MaterialTheme.colorScheme.tertiary,
                )
                OutlinedButton(onClick = onRelays, modifier = Modifier.fillMaxWidth()) {
                    Text("Open Relays")
                }
            }
            Button(
                onClick = onStart,
                enabled = canStart,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Start Chat")
            }
        }
    }
}
