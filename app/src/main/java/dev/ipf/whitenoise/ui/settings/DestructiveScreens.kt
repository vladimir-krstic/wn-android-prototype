package dev.ipf.whitenoise.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.model.Profile
import dev.ipf.whitenoise.model.WipeConfirmationPhrase
import dev.ipf.whitenoise.ui.components.ProfileAvatar
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignOutSheet(
    profile: Profile,
    onDismiss: () -> Unit,
    onComplete: (wipeData: Boolean) -> Unit,
) {
    var wipeData by rememberSaveable { mutableStateOf(true) }
    var confirmation by rememberSaveable { mutableStateOf("") }
    var progress by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(progress) {
        if (progress == null) return@LaunchedEffect
        delay(600)
        onComplete(wipeData)
    }
    ModalBottomSheet(
        onDismissRequest = { if (progress == null) onDismiss() },
        contentWindowInsets = { WindowInsets.safeDrawing },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.94f).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Sign Out", style = MaterialTheme.typography.headlineSmall)
                TextButton(onClick = onDismiss, enabled = progress == null) { Text("Close") }
            }
            if (progress != null) {
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                    Text(progress!!, modifier = Modifier.padding(top = 16.dp))
                }
            } else {
                ListItem(
                    headlineContent = { Text(profile.name) },
                    supportingContent = { Text(profile.shortPublicKey) },
                    leadingContent = { ProfileAvatar(profile.name, profile.avatar, Modifier.size(48.dp), contentDescription = null) },
                )
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text("Wipe Data From This Device") },
                    trailingContent = {
                        Switch(
                            checked = wipeData,
                            onCheckedChange = {
                                wipeData = it
                                if (!it) confirmation = ""
                            },
                        )
                    },
                )
                Text(
                    if (wipeData) {
                        "This profile and all local data will be permanently removed. Previous chats won’t return."
                    } else {
                        "This profile and its local data will stay on this device."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (wipeData) {
                    Text("Enter Profile Name", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = confirmation,
                        onValueChange = { confirmation = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Profile name") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    )
                    Text("Type “${profile.name}” to confirm permanent removal of this profile and its local data.")
                }
                Button(
                    onClick = {
                        progress = if (wipeData) "Signing out and wiping data…" else "Signing out…"
                    },
                    enabled = !wipeData || WipeConfirmationPhrase.matches(confirmation, profile.name),
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) { Text("Sign Out") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EraseAppDataSheet(
    profileIds: Collection<String>,
    onDismiss: () -> Unit,
    onErase: (String) -> Unit,
) {
    val phrase = remember(profileIds) { WipeConfirmationPhrase.make(profileIds) }
    var confirmation by rememberSaveable { mutableStateOf("") }
    var erasing by remember { mutableStateOf(false) }
    LaunchedEffect(erasing) {
        if (!erasing) return@LaunchedEffect
        delay(600)
        onErase(confirmation)
    }
    ModalBottomSheet(
        onDismissRequest = { if (!erasing) onDismiss() },
        contentWindowInsets = { WindowInsets.safeDrawing },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.94f).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Erase App Data", style = MaterialTheme.typography.headlineSmall)
                TextButton(onClick = onDismiss, enabled = !erasing) { Text("Close") }
            }
            if (erasing) {
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                    Text("Erasing app data…", modifier = Modifier.padding(top = 16.dp))
                }
            } else {
                ListItem(
                    headlineContent = { Text("This can’t be undone") },
                    supportingContent = { Text("Every profile and all local chats, media, drafts, keys, and settings will be removed from this device.") },
                    leadingContent = {
                        Text("⚠", modifier = Modifier.clearAndSetSemantics { }, color = MaterialTheme.colorScheme.tertiary)
                    },
                )
                Text("Type these words to confirm", style = MaterialTheme.typography.titleMedium)
                Text(phrase, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                OutlinedTextField(
                    value = confirmation,
                    onValueChange = { confirmation = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Confirmation phrase") },
                    singleLine = true,
                )
                Text("Enter the three words exactly to continue.")
                Button(
                    onClick = { erasing = true },
                    enabled = WipeConfirmationPhrase.matches(confirmation, phrase),
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) { Text("Erase") }
            }
        }
    }
}

@Composable
fun ManageProfilesScreen(
    profiles: List<Profile>,
    activeProfileId: String,
    onBack: () -> Unit,
    onRemove: (String, String) -> Boolean,
) {
    var target by remember { mutableStateOf<Profile?>(null) }
    val removable = profiles.filterNot { it.id == activeProfileId }
    SettingsScaffold(title = "Manage Profiles", onBack = onBack) {
        SettingsList {
            if (removable.isEmpty()) {
                item { SettingsExplainer("There are no other stored profiles to remove.") }
            } else {
                items(removable.size, key = { removable[it].id }) { index ->
                    val profile = removable[index]
                    ListItem(
                        headlineContent = { Text(profile.name) },
                        supportingContent = { Text(profile.shortPublicKey) },
                        leadingContent = { ProfileAvatar(profile.name, profile.avatar, Modifier.size(48.dp), contentDescription = null) },
                        trailingContent = {
                            TextButton(onClick = { target = profile }) {
                                Text("Remove", color = MaterialTheme.colorScheme.error)
                            }
                        },
                    )
                    HorizontalDivider()
                }
            }
            item { SettingsExplainer("Removing another profile signs it out and permanently deletes its local chats, drafts, keys, settings, and developer artifacts from this device.") }
        }
    }
    target?.let { profile ->
        RemoveProfileDialog(
            profile = profile,
            onDismiss = { target = null },
            onRemove = { confirmation ->
                if (onRemove(profile.id, confirmation)) target = null
            },
        )
    }
}

@Composable
private fun RemoveProfileDialog(
    profile: Profile,
    onDismiss: () -> Unit,
    onRemove: (String) -> Unit,
) {
    var confirmation by rememberSaveable(profile.id) { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remove profile?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Type “${profile.name}” to permanently remove this profile and its local data.")
                OutlinedTextField(
                    value = confirmation,
                    onValueChange = { confirmation = it },
                    label = { Text("Profile name") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onRemove(confirmation) },
                enabled = WipeConfirmationPhrase.matches(confirmation, profile.name),
            ) { Text("Remove Profile", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
