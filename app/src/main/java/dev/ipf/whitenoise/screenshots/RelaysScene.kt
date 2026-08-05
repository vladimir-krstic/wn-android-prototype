package dev.ipf.whitenoise.screenshots

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun RelaysScene(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            MaterialTopBar(
                title = "Relays",
                onBack = onBack,
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Relay actions",
                        )
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = Dimens.spaceLg),
        ) {
            Text(
                text = "Profile relays",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = Dimens.spaceLg, bottom = Dimens.spaceSm),
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column {
                    relayFixtures.forEachIndexed { index, relay ->
                        RelayListItem(relay)
                        if (index != relayFixtures.lastIndex) {
                            InsetDivider(Modifier.padding(start = 16.dp, end = 16.dp))
                        }
                    }
                }
            }

            Text(
                text = "Relays help White Noise find profiles and exchange messages.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Dimens.spaceLg, vertical = Dimens.spaceMd),
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                ListItem(
                    colors = TransparentListItemColors,
                    leadingContent = {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    headlineContent = {
                        Text("Advanced", fontWeight = FontWeight.SemiBold)
                    },
                    supportingContent = {
                        Text("Choose how each relay is used.")
                    },
                    trailingContent = {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun RelayListItem(relay: RelayFixture) {
    ListItem(
        colors = TransparentListItemColors,
        headlineContent = {
            Text(
                text = relay.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
            )
        },
        supportingContent = {
            Text(
                text = relay.url,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
            RelayStatusIndicator(relay.status)
        },
    )
}

@Composable
private fun RelayStatusIndicator(status: RelayStatus) {
    when (status) {
        RelayStatus.Connected ->
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Connected",
                tint = Success,
                modifier = Modifier.size(24.dp),
            )

        RelayStatus.Reconnecting ->
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { 0.72f },
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeWidth = 3.dp,
                )
            }

        RelayStatus.Disconnected ->
            Icon(
                Icons.Default.Cancel,
                contentDescription = "Disconnected",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp),
            )
    }
}
