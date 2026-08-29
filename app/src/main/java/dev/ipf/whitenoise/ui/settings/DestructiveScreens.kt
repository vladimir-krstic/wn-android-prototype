package dev.ipf.whitenoise.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import dev.ipf.whitenoise.ui.components.WhiteNoiseAlertDialog as AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import dev.ipf.whitenoise.ui.components.WhiteNoiseModalBottomSheet as ModalBottomSheet
import dev.ipf.whitenoise.ui.components.WhiteNoiseSheetHeader
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.Profile
import dev.ipf.whitenoise.model.WipeConfirmationPhrase
import dev.ipf.whitenoise.ui.components.ProfileAvatar
import dev.ipf.whitenoise.ui.components.WhiteNoiseButtonDefaults
import dev.ipf.whitenoise.ui.components.WhiteNoiseEmptyState
import dev.ipf.whitenoise.ui.components.WhiteNoiseTextField
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignOutSheet(
    profile: Profile,
    onDismiss: () -> Unit,
    onComplete: (wipeData: Boolean) -> Unit,
) {
    var wipeData by rememberSaveable { mutableStateOf(true) }
    val confirmation = rememberSaveable(profile.id, saver = TextFieldState.Saver) { TextFieldState() }
    val confirmationValue = confirmation.text.toString()
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
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f),
        ) {
            WhiteNoiseSheetHeader(
                title = "Sign Out",
                onClose = onDismiss,
                closeEnabled = progress == null,
            )
            if (progress != null) {
                DestructiveProgress(progress!!, Modifier.weight(1f))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    SettingsGroup {
                        ListItem(
                            headlineContent = { Text(profile.name) },
                            supportingContent = { Text(profile.shortPublicKey) },
                            leadingContent = {
                                ProfileAvatar(
                                    profile.name,
                                    profile.avatar,
                                    Modifier.size(48.dp),
                                    contentDescription = null,
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                        SettingsSwitch(
                            title = "Wipe Data From This Device",
                            checked = wipeData,
                            onCheckedChange = {
                                wipeData = it
                                if (!it) confirmation.edit { replace(0, length, "") }
                            },
                            subtitle = if (wipeData) {
                                "This profile and all local data will be permanently removed. Previous chats won’t return."
                            } else {
                                "This profile and its local data will stay on this device."
                            },
                        )
                    }
                    if (wipeData) {
                        SettingsSection("Enter Profile Name")
                        WhiteNoiseTextField(
                            state = confirmation,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin),
                            label = { Text("Profile name") },
                            lineLimits = TextFieldLineLimits.SingleLine,
                            supportingText = {
                                Text("Type “${profile.name}” to confirm permanent removal of this profile and its local data.")
                            },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                            ),
                        )
                    }
                }
                SettingsBottomAction {
                    DestructiveButton(
                        label = "Sign Out",
                        onClick = {
                            progress = if (wipeData) {
                                "Signing out and wiping data…"
                            } else {
                                "Signing out…"
                            }
                        },
                        enabled = !wipeData || WipeConfirmationPhrase.matches(confirmationValue, profile.name),
                        actionDescription = if (wipeData) "Sign Out and Wipe Data" else "Sign Out",
                        unavailableDescription = if (wipeData) "Profile name required" else null,
                    )
                }
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
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Expanded,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    val phrase = remember(profileIds) { WipeConfirmationPhrase.make(profileIds) }
    val confirmation = rememberSaveable(saver = TextFieldState.Saver) { TextFieldState() }
    val confirmationValue = confirmation.text.toString()
    var erasing by remember { mutableStateOf(false) }
    LaunchedEffect(erasing) {
        if (!erasing) return@LaunchedEffect
        delay(600)
        onErase(confirmationValue)
    }
    ModalBottomSheet(
        onDismissRequest = { if (!erasing) onDismiss() },
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets.safeDrawing },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f),
        ) {
            WhiteNoiseSheetHeader(
                title = "Erase App Data",
                onClose = onDismiss,
                closeEnabled = !erasing,
            )
            if (erasing) {
                DestructiveProgress("Erasing app data…", Modifier.weight(1f))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    SettingsCallout(
                        modifier = Modifier.testTag("erase.warning"),
                        title = "This can’t be undone",
                        text = "Every profile and all local chats, media, drafts, keys, and settings will be removed from this device.",
                        isError = true,
                        leading = {
                            Icon(
                                painter = painterResource(R.drawable.ic_warning),
                                contentDescription = null,
                            )
                        },
                    )
                    SettingsSection("Type these words to confirm")
                    SettingsGroup(
                        modifier = Modifier.testTag("erase.phrase"),
                    ) {
                        SelectionContainer {
                            Text(
                                text = phrase,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(WhiteNoiseSpacing.FormField),
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                    WhiteNoiseTextField(
                        state = confirmation,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = WhiteNoiseSpacing.CompactScreenMargin,
                                vertical = WhiteNoiseSpacing.FormField,
                            )
                            .testTag("erase.confirmation"),
                        label = { Text("Confirmation phrase") },
                        lineLimits = TextFieldLineLimits.SingleLine,
                        supportingText = { Text("Enter the three words exactly to continue.") },
                    )
                }
                SettingsBottomAction(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 0.dp,
                ) {
                    DestructiveButton(
                        label = "Erase",
                        onClick = { erasing = true },
                        enabled = WipeConfirmationPhrase.matches(confirmationValue, phrase),
                        actionDescription = "Erase App Data",
                        unavailableDescription = "Confirmation phrase required",
                    )
                }
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
                item {
                    WhiteNoiseEmptyState(
                        title = "No other profiles",
                        detail = "There are no other stored profiles to remove.",
                    )
                }
            } else {
                item { SettingsSection("Stored profiles") }
                item {
                    SettingsGroup {
                        removable.forEach { profile ->
                            ListItem(
                                headlineContent = { Text(profile.name) },
                                supportingContent = { Text(profile.shortPublicKey) },
                                leadingContent = {
                                    ProfileAvatar(
                                        profile.name,
                                        profile.avatar,
                                        Modifier.size(48.dp),
                                        contentDescription = null,
                                    )
                                },
                                trailingContent = {
                                    TextButton(onClick = { target = profile }) {
                                        Text("Remove", color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            )
                        }
                    }
                }
            }
            item {
                SettingsExplainer(
                    "Removing another profile signs it out and permanently deletes its local chats, drafts, keys, settings, and developer artifacts from this device.",
                )
            }
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
    val confirmation = rememberSaveable(profile.id, saver = TextFieldState.Saver) { TextFieldState() }
    val confirmationValue = confirmation.text.toString()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remove profile?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.FormField)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.FormField),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ProfileAvatar(
                        profile.name,
                        profile.avatar,
                        Modifier.size(48.dp),
                        contentDescription = null,
                    )
                    Column {
                        Text(profile.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            profile.shortPublicKey,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                WhiteNoiseTextField(
                    state = confirmation,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Profile name") },
                    lineLimits = TextFieldLineLimits.SingleLine,
                    supportingText = {
                        Text("Type “${profile.name}” to permanently remove this profile and its local data.")
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                    ),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onRemove(confirmationValue) },
                enabled = WipeConfirmationPhrase.matches(confirmationValue, profile.name),
            ) { Text("Remove Profile", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun DestructiveProgress(text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = text
                liveRegion = LiveRegionMode.Polite
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Text(text, modifier = Modifier.padding(top = WhiteNoiseSpacing.FormField))
    }
}

@Composable
private fun DestructiveButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    actionDescription: String,
    unavailableDescription: String?,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = WhiteNoiseButtonDefaults.TaskHeight)
            .semantics {
                role = Role.Button
                contentDescription = actionDescription
                stateDescription = if (enabled) "Ready" else unavailableDescription.orEmpty()
            },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
        ),
        contentPadding = WhiteNoiseButtonDefaults.TaskContentPadding,
    ) {
        Text(label)
    }
}
