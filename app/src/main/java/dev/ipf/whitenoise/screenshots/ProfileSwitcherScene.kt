package dev.ipf.whitenoise.screenshots

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileSwitcherScene(onBack: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    SettingsBackdrop(onBack = onBack)

    LaunchedEffect(Unit) {
        sheetState.show()
    }
    ModalBottomSheet(
        onDismissRequest = onBack,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f),
        tonalElevation = 3.dp,
    ) {
        ProfileSwitcherSheet()
    }
}

@Composable
private fun SettingsBackdrop(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            MaterialTopBar(title = "Settings", onBack = onBack)
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = Dimens.spaceLg, vertical = Dimens.spaceLg),
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceLg),
        ) {
            Text(
                text = "Account",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = Dimens.spaceLg),
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                ListItem(
                    colors = TransparentListItemColors,
                    leadingContent = {
                        PhotoAvatar(
                            resource = R.drawable.profile_avatar_marmota,
                            size = 52.dp,
                            contentDescription = "Marmota",
                        )
                    },
                    headlineContent = {
                        Text("Marmota", fontWeight = FontWeight.SemiBold)
                    },
                    supportingContent = {
                        Text(
                            "npub1m8z7q4k…8x4k",
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    trailingContent = {
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.QrCode, contentDescription = "Profile QR code")
                        }
                    },
                )
                InsetDivider(Modifier.padding(start = 84.dp, end = Dimens.spaceLg))
                ListItem(
                    colors = TransparentListItemColors,
                    leadingContent = {
                        ProfileAvatarStack()
                    },
                    headlineContent = {
                        Text("Switch profile", fontWeight = FontWeight.SemiBold)
                    },
                    supportingContent = {
                        Text("5 profiles on this device")
                    },
                    trailingContent = {
                        Icon(Icons.Default.ExpandMore, contentDescription = null)
                    },
                )
            }

            Text(
                text = "Profile",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = Dimens.spaceLg),
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                SettingsListItem(Icons.Default.Person, "Edit profile")
                InsetDivider(Modifier.padding(start = 56.dp, end = Dimens.spaceLg))
                SettingsListItem(Icons.Default.Key, "Profile keys")
            }
        }
    }
}

@Composable
private fun SettingsListItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
) {
    ListItem(
        colors = TransparentListItemColors,
        leadingContent = {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        headlineContent = {
            Text(label)
        },
    )
}

@Composable
private fun ProfileAvatarStack() {
    Row(
        horizontalArrangement = Arrangement.spacedBy((-12).dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.width(92.dp),
    ) {
        profileFixtures.drop(1).take(3).forEach { profile ->
            PhotoAvatar(
                resource = profile.avatar,
                size = 34.dp,
                contentDescription = null,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.surfaceContainerLow),
            )
        }
    }
}

@Composable
private fun ProfileSwitcherSheet() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = Dimens.spaceXl, end = Dimens.spaceXl, bottom = Dimens.spaceXl),
    ) {
        Text(
            text = "Switch profile",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = Dimens.spaceLg),
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth().height(360.dp),
            userScrollEnabled = true,
        ) {
            itemsIndexed(profileFixtures) { _, profile ->
                ProfileListItem(profile)
            }
        }

        Spacer(Modifier.height(Dimens.spaceLg))
        FilledTonalButton(
            onClick = {},
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(Dimens.spaceSm))
            Text("Add profile")
        }
    }
}

@Composable
private fun ProfileListItem(profile: ProfileFixture) {
    ListItem(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
        colors =
            ListItemDefaults.colors(
                containerColor =
                    if (profile.selected) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        Color.Transparent
                    },
            ),
        leadingContent = {
            PhotoAvatar(
                resource = profile.avatar,
                size = 48.dp,
                contentDescription = profile.name,
            )
        },
        headlineContent = {
            Text(
                text = profile.name,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                text = profile.npub,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
            if (profile.selected) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Check, contentDescription = "Selected", modifier = Modifier.size(20.dp))
                    }
                }
            }
        },
    )
}
