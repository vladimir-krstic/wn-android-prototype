package dev.ipf.whitenoise.ui.chats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.Profile
import dev.ipf.whitenoise.ui.components.ProfileAvatar
import dev.ipf.whitenoise.ui.components.WhiteNoiseButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSwitcherSheet(
    profiles: List<Profile>,
    activeProfileId: String?,
    onDismiss: () -> Unit,
    onSelectProfile: (String) -> Unit,
    onAddProfile: () -> Unit,
) {
    val orderedProfiles = profiles.sortedBy { if (it.id == activeProfileId) 0 else 1 }
    val currentProfileDescription = stringResource(R.string.current_profile)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.switch_profile),
                    modifier = Modifier.padding(start = 16.dp),
                    style = MaterialTheme.typography.titleLarge,
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.close),
                    )
                }
            }
            LazyColumn(modifier = Modifier.heightIn(max = 520.dp)) {
                items(orderedProfiles, key = Profile::id) { profile ->
                    val unreadCount = profile.chats
                        .filter { !it.isArchived && !it.hasEndedMembership }
                        .sumOf { chat -> chat.unreadCount.coerceAtLeast(if (chat.isMarkedUnread) 1 else 0) }
                    val unreadDescription = pluralStringResource(
                        R.plurals.unread_count,
                        unreadCount,
                        unreadCount,
                    )
                    ListItem(
                        headlineContent = { Text(profile.name) },
                        supportingContent = { Text(profile.shortPublicKey) },
                        leadingContent = {
                            ProfileAvatar(
                                name = profile.name,
                                avatar = profile.avatar,
                                modifier = Modifier.size(56.dp),
                                contentDescription = null,
                            )
                        },
                        trailingContent = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (unreadCount > 0) {
                                    Badge(
                                        modifier = Modifier.semantics {
                                            contentDescription = unreadDescription
                                        },
                                    ) { Text(if (unreadCount > 99) "99+" else unreadCount.toString()) }
                                }
                                if (profile.id == activeProfileId) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_check),
                                        contentDescription = null,
                                        modifier = Modifier.semantics {
                                            contentDescription = currentProfileDescription
                                        },
                                    )
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = if (profile.id == activeProfileId) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                Color.Transparent
                            },
                        ),
                        modifier = Modifier
                            .clickable { onSelectProfile(profile.id) }
                            .semantics(mergeDescendants = true) {
                                selected = profile.id == activeProfileId
                            },
                    )
                }
            }
            WhiteNoiseButton(
                onClick = onAddProfile,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                Text(stringResource(R.string.add_profile))
            }
        }
    }
}
