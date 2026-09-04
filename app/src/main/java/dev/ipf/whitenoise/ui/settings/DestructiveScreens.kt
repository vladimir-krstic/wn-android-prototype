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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.Profile
import dev.ipf.whitenoise.model.ProfileExitAttempt
import dev.ipf.whitenoise.model.ProfileExitStep
import dev.ipf.whitenoise.model.SignOutOptions
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
    onComplete: (SignOutOptions) -> Unit,
    attempt: ProfileExitAttempt? = null,
    onAdvance: (Long, ProfileExitStep) -> Unit = { _, _ -> },
    onRetry: (Long) -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    var wipeData by rememberSaveable(profile.id) { mutableStateOf(true) }
    var deleteConnectionInformation by rememberSaveable(profile.id) { mutableStateOf(true) }
    val confirmation = rememberSaveable(profile.id, saver = TextFieldState.Saver) { TextFieldState() }
    val confirmationValue = confirmation.text.toString()
    val busy = attempt?.isRunning == true
    val dismissEnabled by rememberUpdatedState(!busy)
    val confirmSheetValueChange = remember {
        { target: SheetValue -> target != SheetValue.Hidden || dismissEnabled }
    }
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Expanded,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        confirmValueChange = confirmSheetValueChange,
    )
    val wipeExplanation = if (wipeData) {
        stringResource(R.string.sign_out_wipe_detail)
    } else {
        stringResource(R.string.sign_out_keep_detail)
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(attempt?.id, attempt?.currentStep, lifecycle) {
        val step = attempt?.currentStep ?: return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            delay(600)
            onAdvance(attempt.id, step)
        }
    }
    ModalBottomSheet(
        onDismissRequest = { if (!busy) onDismiss() },
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets.safeDrawing },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f),
        ) {
            WhiteNoiseSheetHeader(
                title = stringResource(R.string.ui_sign_out),
                onClose = onDismiss,
                closeEnabled = !busy,
            )
            if (attempt != null) {
                ProfileExitStatus(attempt, Modifier.weight(1f))
                if (!busy) SettingsBottomAction {
                    DestructiveButton(
                        label = stringResource(R.string.try_again),
                        onClick = { onRetry(attempt.id) },
                        enabled = true,
                        actionDescription = stringResource(R.string.try_again),
                        unavailableDescription = null,
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    SettingsGroup(
                        modifier = Modifier.testTag("sign_out.profile_group"),
                    ) {
                        ListItem(
                            modifier = Modifier.testTag("sign_out.profile"),
                            leadingContent = {
                                ProfileAvatar(
                                    profile.name,
                                    profile.avatar,
                                    Modifier.size(48.dp).testTag("sign_out.profile.avatar"),
                                    contentDescription = null,
                                )
                            },
                            supportingContent = { Text(profile.shortPublicKey) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        ) {
                            Text(profile.name, modifier = Modifier.testTag("sign_out.profile.name"))
                        }
                        SettingsDivider(Modifier.testTag("sign_out.profile.divider"))
                        SettingsSwitch(
                            title = stringResource(R.string.ui_wipe_data_from_this_device),
                            checked = wipeData,
                            onCheckedChange = {
                                wipeData = it
                                if (!it) {
                                    confirmation.edit { replace(0, length, "") }
                                    focusManager.clearFocus()
                                }
                            },
                        )
                        if (!wipeData) {
                            SettingsDivider()
                            SettingsSwitch(
                                title = stringResource(R.string.exit_remove_connection_info),
                                checked = deleteConnectionInformation,
                                onCheckedChange = { deleteConnectionInformation = it },
                            )
                        }
                    }
                    SettingsExplainer(wipeExplanation)
                    if (!wipeData) SettingsExplainer(stringResource(R.string.exit_remove_connection_help))
                    if (wipeData) {
                        SettingsSection(stringResource(R.string.ui_enter_profile_name))
                        WhiteNoiseTextField(
                            state = confirmation,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin),
                            label = { Text(stringResource(R.string.ui_profile_name)) },
                            lineLimits = TextFieldLineLimits.SingleLine,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Done,
                            ),
                            onKeyboardAction = { focusManager.clearFocus() },
                        )
                        SettingsExplainer(
                            stringResource(R.string.remove_profile_confirmation, profile.name),
                        )
                    }
                }
                SettingsBottomAction(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 0.dp,
                ) {
                    DestructiveButton(
                        label = stringResource(R.string.ui_sign_out),
                        onClick = {
                            focusManager.clearFocus()
                            onComplete(SignOutOptions(wipeData, deleteConnectionInformation, confirmationValue))
                        },
                        enabled = !wipeData || WipeConfirmationPhrase.matches(confirmationValue, profile.name),
                        actionDescription = if (wipeData) stringResource(R.string.sign_out_and_wipe) else stringResource(R.string.ui_sign_out),
                        unavailableDescription = if (wipeData) stringResource(R.string.profile_name_required) else null,
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
                title = stringResource(R.string.ui_erase_app_data),
                onClose = onDismiss,
                closeEnabled = !erasing,
            )
            if (erasing) {
                DestructiveProgress(stringResource(R.string.erasing_app_data), Modifier.weight(1f))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    SettingsCallout(
                        modifier = Modifier.testTag("erase.warning"),
                        title = stringResource(R.string.ui_this_cant_be_undone),
                        text = stringResource(R.string.erase_app_data_detail),
                        isError = true,
                        leading = {
                            Icon(
                                painter = painterResource(R.drawable.ic_warning),
                                contentDescription = null,
                            )
                        },
                    )
                    SettingsSection(stringResource(R.string.ui_type_these_words_to_confirm))
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
                        label = { Text(stringResource(R.string.ui_confirmation_phrase)) },
                        lineLimits = TextFieldLineLimits.SingleLine,
                        supportingText = { Text(stringResource(R.string.ui_enter_the_three_words_exactly_to_continue)) },
                    )
                }
                SettingsBottomAction(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 0.dp,
                ) {
                    DestructiveButton(
                        label = stringResource(R.string.erase),
                        onClick = { erasing = true },
                        enabled = WipeConfirmationPhrase.matches(confirmationValue, phrase),
                        actionDescription = stringResource(R.string.ui_erase_app_data),
                        unavailableDescription = stringResource(R.string.confirmation_phrase_required),
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
    SettingsScaffold(title = stringResource(R.string.ui_manage_profiles), onBack = onBack) {
        SettingsList {
            if (removable.isEmpty()) {
                item {
                    WhiteNoiseEmptyState(
                        title = stringResource(R.string.ui_no_other_profiles),
                        detail = stringResource(R.string.no_other_profiles_detail),
                    )
                }
            } else {
                item { SettingsSection(stringResource(R.string.ui_stored_profiles)) }
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
                                        Text(stringResource(R.string.ui_remove), color = MaterialTheme.colorScheme.error)
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
                    stringResource(R.string.ui_removing_another_profile_signs_it_out_and_permanently_),
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
        title = { Text(stringResource(R.string.ui_remove_profile)) },
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
                    label = { Text(stringResource(R.string.ui_profile_name)) },
                    lineLimits = TextFieldLineLimits.SingleLine,
                    supportingText = {
                        Text(stringResource(R.string.remove_profile_permanent_confirmation, profile.name))
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
            ) { Text(stringResource(R.string.ui_remove_profile_2), color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
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
    val readyDescription = stringResource(R.string.ready)
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = WhiteNoiseButtonDefaults.TaskHeight)
            .semantics {
                role = Role.Button
                contentDescription = actionDescription
                stateDescription = if (enabled) readyDescription else unavailableDescription.orEmpty()
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
