package dev.ipf.whitenoise.ui.chats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import dev.ipf.whitenoise.ui.components.WhiteNoiseModalBottomSheet as ModalBottomSheet
import dev.ipf.whitenoise.ui.components.WhiteNoiseSheetHeader
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
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
import dev.ipf.whitenoise.ui.settings.profileSwitcherPresentation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSwitcherSheet(
    profiles: List<Profile>,
    activeProfileId: String?,
    onDismiss: () -> Unit,
    onSelectProfile: (String) -> Unit,
    onAddProfile: () -> Unit,
) {
    val presentedProfiles = profileSwitcherPresentation(profiles, activeProfileId)
    val currentProfileDescription = stringResource(R.string.current_profile)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            WhiteNoiseSheetHeader(stringResource(R.string.switch_profile), onClose = onDismiss)
            LazyColumn(modifier = Modifier.weight(1f, fill = false).heightIn(max = 520.dp)) {
                items(presentedProfiles, key = { it.profile.id }) { item ->
                    val profile = item.profile
                    val unreadDescription = if (item.unreadCount > 99) {
                        stringResource(R.string.unread_count_capped)
                    } else {
                        pluralStringResource(
                            R.plurals.unread_count,
                            item.unreadCount,
                            item.unreadCount,
                        )
                    }
                    ListItem(
                        headlineContent = {
                            Text(
                                text = profile.name,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        },
                        supportingContent = { Text(profile.shortPublicKey) },
                        leadingContent = {
                            ProfileAvatar(
                                name = profile.name,
                                avatar = profile.avatar,
                                modifier = Modifier.size(48.dp),
                                contentDescription = null,
                            )
                        },
                        trailingContent = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (!item.isActive && item.unreadCount > 0) {
                                    Badge(
                                        modifier = Modifier.semantics {
                                            contentDescription = unreadDescription
                                        },
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                    ) {
                                        Text(if (item.unreadCount > 99) "99+" else item.unreadCount.toString())
                                    }
                                }
                                if (item.isActive) {
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
                            containerColor = if (item.isActive) {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            } else {
                                Color.Transparent
                            },
                        ),
                        modifier = Modifier
                            .clickable { onSelectProfile(profile.id) }
                            .testTag("profile_switcher.profile.${profile.id}")
                            .semantics(mergeDescendants = true) {
                                selected = item.isActive
                            },
                    )
                }
            }
            WhiteNoiseButton(
                onClick = onAddProfile,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("profile_switcher.add_profile"),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings_person_add),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
                androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.add_profile))
            }
        }
    }
}
