package dev.ipf.whitenoise.ui.settings

import androidx.compose.ui.res.stringResource

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.Chat
import dev.ipf.whitenoise.model.ConversationDebugAccess
import dev.ipf.whitenoise.model.ConversationDebugPolicy
import dev.ipf.whitenoise.model.ConversationDebugSnapshot
import dev.ipf.whitenoise.model.Profile
import dev.ipf.whitenoise.ui.components.WhiteNoiseButton
import dev.ipf.whitenoise.ui.components.WhiteNoiseAlertDialog as AlertDialog
import dev.ipf.whitenoise.ui.components.WhiteNoiseDropdownMenu
import dev.ipf.whitenoise.ui.components.WhiteNoiseEmptyState
import dev.ipf.whitenoise.ui.components.WhiteNoiseFilledTonalButton
import dev.ipf.whitenoise.ui.components.WhiteNoiseLazyColumn as LazyColumn
import dev.ipf.whitenoise.ui.components.WhiteNoiseMenuItem
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing

@Composable
fun DeveloperToolsScreen(
    profile: Profile,
    onBack: () -> Unit,
    onEnabled: (Boolean) -> Boolean,
    onDebugMode: (Boolean) -> Boolean,
    onDiagnostics: () -> Unit,
    onKeyPackages: () -> Unit,
    parityController: dev.ipf.whitenoise.state.DeveloperParityController? = null,
    onAuditLogs: () -> Unit = {},
    historyScenario: dev.ipf.whitenoise.model.HistoryScenario = dev.ipf.whitenoise.model.HistoryScenario.Success,
    onHistoryScenario: (dev.ipf.whitenoise.model.HistoryScenario) -> Unit = {},
    messageEditScenario: dev.ipf.whitenoise.model.MessageEditScenario = dev.ipf.whitenoise.model.MessageEditScenario.Success,
    onMessageEditScenario: (dev.ipf.whitenoise.model.MessageEditScenario) -> Unit = {},
    messageDeleteScenario: dev.ipf.whitenoise.model.MessageDeleteScenario = dev.ipf.whitenoise.model.MessageDeleteScenario.Success,
    onMessageDeleteScenario: (dev.ipf.whitenoise.model.MessageDeleteScenario) -> Unit = {},
    messageForwardScenario: dev.ipf.whitenoise.model.MessageForwardScenario = dev.ipf.whitenoise.model.MessageForwardScenario.Success,
    recentMediaAccess: dev.ipf.whitenoise.model.RecentMediaAccess = dev.ipf.whitenoise.model.RecentMediaAccess.Full,
    onRecentMediaAccess: (dev.ipf.whitenoise.model.RecentMediaAccess) -> Unit = {},
    attachmentTransferScenario: dev.ipf.whitenoise.model.AttachmentTransferScenario = dev.ipf.whitenoise.model.AttachmentTransferScenario.Success,
    onAttachmentTransferScenario: (dev.ipf.whitenoise.model.AttachmentTransferScenario) -> Unit = {},
    downloadExampleControls: @Composable () -> Unit = {},
    relayPublicationControls: @Composable () -> Unit = {},
    updateControls: @Composable () -> Unit = {},
    photoEditorScenario: dev.ipf.whitenoise.model.PhotoEditorScenario = dev.ipf.whitenoise.model.PhotoEditorScenario.Success,
    onPhotoEditorScenario: (dev.ipf.whitenoise.model.PhotoEditorScenario) -> Unit = {},
    locationScenario: dev.ipf.whitenoise.model.LocationScenario = dev.ipf.whitenoise.model.LocationScenario.Unavailable,
    onLocationScenario: (dev.ipf.whitenoise.model.LocationScenario) -> Unit = {},
    attachmentAccessScenario: dev.ipf.whitenoise.model.AttachmentAccessScenario = dev.ipf.whitenoise.model.AttachmentAccessScenario.Success,
    onAttachmentAccessScenario: (dev.ipf.whitenoise.model.AttachmentAccessScenario) -> Unit = {},
    onMessageForwardScenario: (dev.ipf.whitenoise.model.MessageForwardScenario) -> Unit = {},
    globalVoiceScenario: dev.ipf.whitenoise.model.GlobalVoiceScenario = dev.ipf.whitenoise.model.GlobalVoiceScenario.Success,
    onGlobalVoiceScenario: (dev.ipf.whitenoise.model.GlobalVoiceScenario) -> Unit = {},
    chatBatchScenario: dev.ipf.whitenoise.model.ChatBatchScenario = dev.ipf.whitenoise.model.ChatBatchScenario.Success,
    onChatBatchScenario: (dev.ipf.whitenoise.model.ChatBatchScenario) -> Unit = {},
    onChatConnectionScenario: (dev.ipf.whitenoise.model.ChatConnectionScenario) -> Unit = {},
    profileSaveScenario: dev.ipf.whitenoise.model.ProfileSaveScenario = dev.ipf.whitenoise.model.ProfileSaveScenario.Success,
    onProfileSaveScenario: (dev.ipf.whitenoise.model.ProfileSaveScenario) -> Unit = {},
    profileImageFails: Boolean = false,
    onProfileImageFails: (Boolean) -> Unit = {},
    peopleSearchScenario: dev.ipf.whitenoise.model.PeopleSearchScenario = dev.ipf.whitenoise.model.PeopleSearchScenario.Success,
    onPeopleSearchScenario: (dev.ipf.whitenoise.model.PeopleSearchScenario) -> Unit = {},
    groupContactScenario: dev.ipf.whitenoise.model.GroupContactScenario = dev.ipf.whitenoise.model.GroupContactScenario.Success,
    onGroupContactScenario: (dev.ipf.whitenoise.model.GroupContactScenario) -> Unit = {},
    createdChatUnavailable: Boolean = false,
    onCreatedChatUnavailable: (Boolean) -> Unit = {},
    accessScenario: dev.ipf.whitenoise.model.AccessScenario = dev.ipf.whitenoise.model.AccessScenario.Success,
    onAccessScenario: (dev.ipf.whitenoise.model.AccessScenario) -> Unit = {},
    onStartupFailure: () -> Unit = {},
    exitScenario: dev.ipf.whitenoise.model.ProfileExitScenario = dev.ipf.whitenoise.model.ProfileExitScenario.Success,
    onExitScenario: (dev.ipf.whitenoise.model.ProfileExitScenario) -> Unit = {},
    onLocalKeyAvailable: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val tools = profile.developerTools
    var exportContent by rememberSaveable(profile.id) { mutableStateOf("") }
    var saveErrorDialog by rememberSaveable(profile.id) { mutableStateOf(false) }
    var historyOpen by remember { mutableStateOf(false) }
    var editOpen by remember { mutableStateOf(false) }
    var deleteOpen by remember { mutableStateOf(false) }
    var recentOpen by remember { mutableStateOf(false) }
    var transferOpen by remember { mutableStateOf(false) }
    var photoEditorOpen by remember { mutableStateOf(false) }
    val speech = dev.ipf.whitenoise.ui.conversation.LocalReadAloudController.current
    var speechOpen by remember { mutableStateOf(false) }
    val capture = dev.ipf.whitenoise.ui.conversation.LocalComposerCapture.current
    var dictationOpen by remember { mutableStateOf(false) }
    var recordingOpen by remember { mutableStateOf(false) }
    if (recordingOpen && capture != null) ScenarioChoiceDialog(stringResource(R.string.developer_voice_recording_outcomes), dev.ipf.whitenoise.model.VoiceCaptureScenario.entries,
        capture.voiceScenario, { it.developerLabel }, { capture.chooseVoiceScenario(it) }, { recordingOpen = false })
    if (dictationOpen && capture != null) ScenarioChoiceDialog(stringResource(R.string.developer_dictation_outcomes), dev.ipf.whitenoise.model.DictationScenario.entries,
        capture.scenario, { it.developerLabel }, { capture.chooseScenario(it) }, { dictationOpen = false })
    var speechPreferencesOpen by remember { mutableStateOf(false) }
    if (speechPreferencesOpen && speech != null) SpeechDeveloperDialog(profile, speech) { speechPreferencesOpen = false }
    if (speechOpen && speech != null) ScenarioChoiceDialog(stringResource(R.string.developer_read_aloud_history), dev.ipf.whitenoise.model.SpeechEdgeScenario.entries,
        speech.edgeScenario, { it.name }, { speech.setEdgeScenario(profile.id, it) }, { speechOpen = false })
    var locationOpen by remember { mutableStateOf(false) }
    if (locationOpen) ScenarioChoiceDialog(stringResource(R.string.developer_location_sharing), dev.ipf.whitenoise.model.LocationScenario.entries,
        locationScenario, { it.developerLabel }, onLocationScenario, { locationOpen = false })
    var attachmentAccessOpen by remember { mutableStateOf(false) }
    if (attachmentAccessOpen) ScenarioChoiceDialog(stringResource(R.string.developer_file_opening), dev.ipf.whitenoise.model.AttachmentAccessScenario.entries,
        attachmentAccessScenario, { it.developerLabel }, onAttachmentAccessScenario, { attachmentAccessOpen = false })
    if (photoEditorOpen) ScenarioChoiceDialog(stringResource(R.string.developer_photo_editor), dev.ipf.whitenoise.model.PhotoEditorScenario.entries,
        photoEditorScenario, { it.developerLabel }, onPhotoEditorScenario, { photoEditorOpen = false })
    if (recentOpen) ScenarioChoiceDialog(stringResource(R.string.developer_recent_media_access), dev.ipf.whitenoise.model.RecentMediaAccess.entries,
        recentMediaAccess, { it.name }, onRecentMediaAccess, { recentOpen = false })
    if (transferOpen) ScenarioChoiceDialog(stringResource(R.string.developer_attachment_transfer), dev.ipf.whitenoise.model.AttachmentTransferScenario.entries,
        attachmentTransferScenario, { it.developerLabel }, onAttachmentTransferScenario, { transferOpen = false })
    var forwardOpen by remember { mutableStateOf(false) }
    if (deleteOpen) ScenarioChoiceDialog(stringResource(R.string.developer_message_deletion), dev.ipf.whitenoise.model.MessageDeleteScenario.entries,
        messageDeleteScenario, { it.developerLabel }, onMessageDeleteScenario, { deleteOpen = false })
    if (forwardOpen) ScenarioChoiceDialog(stringResource(R.string.developer_message_forwarding), dev.ipf.whitenoise.model.MessageForwardScenario.entries,
        messageForwardScenario, { it.developerLabel }, onMessageForwardScenario, { forwardOpen = false })
    if (editOpen) ScenarioChoiceDialog(stringResource(R.string.developer_message_editing), dev.ipf.whitenoise.model.MessageEditScenario.entries,
        messageEditScenario, { it.developerLabel }, onMessageEditScenario, { editOpen = false })
    if (historyOpen) ScenarioChoiceDialog(stringResource(R.string.developer_conversation_history), dev.ipf.whitenoise.model.HistoryScenario.entries,
        historyScenario, { it.developerLabel }, onHistoryScenario, { historyOpen = false })
    var globalVoiceOpen by remember { mutableStateOf(false) }
    if (globalVoiceOpen) ScenarioChoiceDialog(stringResource(R.string.developer_voice_search), dev.ipf.whitenoise.model.GlobalVoiceScenario.entries,
        globalVoiceScenario, { it.developerLabel }, onGlobalVoiceScenario, { globalVoiceOpen = false })
    var chatBatchOpen by remember { mutableStateOf(false) }
    var chatConnectionOpen by remember { mutableStateOf(false) }
    if (chatBatchOpen) ScenarioChoiceDialog(stringResource(R.string.developer_chat_actions), dev.ipf.whitenoise.model.ChatBatchScenario.entries,
        chatBatchScenario, { it.developerLabel }, onChatBatchScenario, { chatBatchOpen = false })
    if (chatConnectionOpen) ScenarioChoiceDialog(stringResource(R.string.developer_chat_connection), dev.ipf.whitenoise.model.ChatConnectionScenario.entries,
        if (profile.chatConnection.retryFails) dev.ipf.whitenoise.model.ChatConnectionScenario.RetryFailure
        else dev.ipf.whitenoise.model.ChatConnectionScenario.entries.first { it.name == profile.chatConnection.phase.name || (it == dev.ipf.whitenoise.model.ChatConnectionScenario.RetryFailure && profile.chatConnection.phase == dev.ipf.whitenoise.model.ChatConnectionPhase.Failed) },
        { it.developerLabel }, onChatConnectionScenario, { chatConnectionOpen = false })
    var profileSaveScenariosOpen by remember { mutableStateOf(false) }
    if (profileSaveScenariosOpen) ScenarioChoiceDialog(stringResource(R.string.developer_profile_save), dev.ipf.whitenoise.model.ProfileSaveScenario.entries,
        profileSaveScenario, { it.developerLabel }, onProfileSaveScenario, { profileSaveScenariosOpen = false })
    var peopleScenariosOpen by remember { mutableStateOf(false) }
    var groupScenariosOpen by remember { mutableStateOf(false) }
    val incoming = dev.ipf.whitenoise.ui.share.LocalIncoming.current
    val auditLogs = LocalAuditLogs.current
    var auditChoice by rememberSaveable(profile.id) { mutableStateOf(false) }
    if (auditLogs != null && auditChoice) ScenarioChoiceDialog(stringResource(R.string.developer_audit_log_outcome),dev.ipf.whitenoise.model.AuditLogScenario.entries,
        auditLogs.scenario,{it.label},auditLogs::choose,{auditChoice = false})
    val appLock = LocalAppLock.current
    var unlockChoice by rememberSaveable(profile.id) { mutableStateOf(false) }
    if (appLock != null && unlockChoice) ScenarioChoiceDialog(stringResource(R.string.developer_app_unlock_outcome),dev.ipf.whitenoise.state.AppUnlockOutcome.entries,
        appLock.scenario,{it.label},appLock::choose,{unlockChoice = false})
    val notificationControls = LocalNotificationControls.current
    val notificationActions = LocalNotificationActions.current
    var actionChoice by rememberSaveable(profile.id) { mutableStateOf<String?>(null) }
    if (notificationActions != null && actionChoice == "outcome") ScenarioChoiceDialog(stringResource(R.string.developer_notification_action_outcome),
        dev.ipf.whitenoise.state.NotificationActionScenario.entries,notificationActions.scenario,{it.label},notificationActions::choose,{actionChoice = null})
    if (notificationActions != null && actionChoice == "action") ScenarioChoiceDialog(stringResource(R.string.notification_action_title),
        dev.ipf.whitenoise.model.NotificationActionKind.entries,dev.ipf.whitenoise.model.NotificationActionKind.Reply,{it.name},{ kind ->
            val entry = dev.ipf.whitenoise.model.IncomingExamples.entry(dev.ipf.whitenoise.model.IncomingExample.NotificationMessage,profile,listOf(profile)) as dev.ipf.whitenoise.model.IncomingEntry.Notification
            val generation = notificationActions.nextExampleId()
            val card = dev.ipf.whitenoise.model.NotificationCard("developer-message",generation,entry.target)
            notificationActions.recordCard(card)
            notificationActions.submit(dev.ipf.whitenoise.model.NotificationActionInput("developer-action-$generation",card,kind,
                if (kind == dev.ipf.whitenoise.model.NotificationActionKind.React) profile.quickReactions.firstOrNull().orEmpty() else "Thanks, see you there."))
        },{actionChoice = null})
    var notificationChoice by rememberSaveable(profile.id) { mutableStateOf<String?>(null) }
    if (notificationControls != null) {
        when (notificationChoice) {
            "outcome" -> ScenarioChoiceDialog(stringResource(R.string.developer_notification_settings_outcomes),dev.ipf.whitenoise.state.NotificationScenario.entries,
                notificationControls.scenario,{it.developerLabel},notificationControls::choose,{notificationChoice = null})
            "push" -> ScenarioChoiceDialog(stringResource(R.string.developer_native_push_availability),dev.ipf.whitenoise.model.PushAvailability.entries,
                notificationControls.environment.push,{it.name},{notificationControls.chooseEnvironment(notificationControls.environment.copy(push = it))},{notificationChoice = null})
            "vibration" -> ScenarioChoiceDialog(stringResource(R.string.developer_android_vibration_override),dev.ipf.whitenoise.model.AndroidVibrationOverride.entries,
                notificationControls.environment.vibrationOverride,{it.name},{notificationControls.chooseEnvironment(notificationControls.environment.copy(vibrationOverride = it))},{notificationChoice = null})
        }
    }
    var incomingExampleOpen by remember { mutableStateOf(false) }
    var incomingOutcomeOpen by remember { mutableStateOf(false) }
    val retention = dev.ipf.whitenoise.ui.conversation.LocalRetention.current
    val groupLifecycle = dev.ipf.whitenoise.ui.conversation.LocalGroupLifecycle.current
    val transcript = dev.ipf.whitenoise.ui.conversation.LocalTranscript.current
    val groupWork = dev.ipf.whitenoise.ui.conversation.LocalGroupWork.current
    var groupWorkChoice by rememberSaveable(profile.id) { mutableStateOf<String?>(null) }
    if (incoming != null && incomingExampleOpen) ScenarioChoiceDialog(stringResource(R.string.developer_incoming_request), dev.ipf.whitenoise.model.IncomingExample.entries,
        dev.ipf.whitenoise.model.IncomingExample.Text, { it.label }, { example -> incoming.receive(dev.ipf.whitenoise.model.IncomingExamples.entry(example,profile,incoming.signedProfiles())) }, { incomingExampleOpen = false })
    if (incoming != null && incomingOutcomeOpen) ScenarioChoiceDialog(stringResource(R.string.developer_incoming_request_outcome), dev.ipf.whitenoise.state.IncomingScenario.entries,
        incoming.scenario, { it.developerLabel }, incoming::choose, { incomingOutcomeOpen = false })
    if (retention != null && groupWorkChoice == "retention") ScenarioChoiceDialog(stringResource(R.string.developer_retention_update), dev.ipf.whitenoise.state.RetentionScenario.entries, retention.scenario, { it.developerLabel }, retention::choose, { groupWorkChoice = null })
    if (retention != null && groupWorkChoice == "expiry") ScenarioChoiceDialog(stringResource(R.string.developer_retention_example), dev.ipf.whitenoise.state.RetentionExample.entries, retention.example, { it.developerLabel }, retention::chooseExample, { groupWorkChoice = null })
    if (groupLifecycle != null && groupWorkChoice == "lifecycle") ScenarioChoiceDialog(stringResource(R.string.developer_group_administration), dev.ipf.whitenoise.model.GroupLifecycleScenario.entries, groupLifecycle.scenario, { it.developerLabel }, groupLifecycle::choose, { groupWorkChoice = null })
    if (groupLifecycle != null && groupWorkChoice == "groupState") ScenarioChoiceDialog(stringResource(R.string.developer_group_lifecycle), dev.ipf.whitenoise.model.GroupStateScenario.entries, groupLifecycle.stateScenario, { it.developerLabel }, groupLifecycle::chooseState, { groupWorkChoice = null })
    if (transcript != null && groupWorkChoice == "transcript") ScenarioChoiceDialog(stringResource(R.string.developer_transcript_export), dev.ipf.whitenoise.model.TranscriptScenario.entries, transcript.scenario, { it.developerLabel }, transcript::choose, { groupWorkChoice = null })
    if (groupWork != null) when (groupWorkChoice) {
        "roster" -> ScenarioChoiceDialog(stringResource(R.string.developer_group_roster), dev.ipf.whitenoise.model.GroupRosterScenario.entries, groupWork.rosterScenario, { it.developerLabel }, groupWork::chooseRoster, { groupWorkChoice = null })
        "members" -> ScenarioChoiceDialog(stringResource(R.string.developer_group_member_updates), dev.ipf.whitenoise.model.GroupMutationScenario.entries, groupWork.mutationScenario, { it.developerLabel }, groupWork::chooseMutation, { groupWorkChoice = null })
        "images" -> ScenarioChoiceDialog(stringResource(R.string.developer_group_images), dev.ipf.whitenoise.model.GroupImageScenario.entries, groupWork.imageScenario, { it.developerLabel }, groupWork::chooseImage, { groupWorkChoice = null })
        "create" -> ScenarioChoiceDialog(stringResource(R.string.developer_group_creation), dev.ipf.whitenoise.model.GroupCreateScenario.entries, groupWork.createScenario, { it.developerLabel }, groupWork::chooseCreate, { groupWorkChoice = null })
    }

    if (peopleScenariosOpen) ScenarioChoiceDialog(stringResource(R.string.developer_people_search), dev.ipf.whitenoise.model.PeopleSearchScenario.entries,
        peopleSearchScenario, { it.developerLabel }, onPeopleSearchScenario, { peopleScenariosOpen = false })
    if (groupScenariosOpen) ScenarioChoiceDialog(stringResource(R.string.developer_group_contact_actions), dev.ipf.whitenoise.model.GroupContactScenario.entries,
        groupContactScenario, { it.developerLabel }, onGroupContactScenario, { groupScenariosOpen = false })
    var showAccessScenarios by rememberSaveable(profile.id) { mutableStateOf(false) }
    var showExitScenarios by rememberSaveable(profile.id) { mutableStateOf(false) }
    if (showAccessScenarios && tools.isEnabled) AccessScenarioDialog(
        accessScenario, onAccessScenario, onDismiss = { showAccessScenarios = false },
    )
    if (showExitScenarios && tools.isEnabled) ProfileExitScenarioDialog(
        exitScenario, onExitScenario, onDismiss = { showExitScenarios = false },
    )
    val exportLogs = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        if (uri != null) {
            val result = runCatching {
                checkNotNull(context.contentResolver.openOutputStream(uri))
                    .bufferedWriter()
                    .use { writer -> writer.write(exportContent) }
            }
            saveErrorDialog = result.isFailure
        }
        exportContent = ""
    }
    SettingsScaffold(title = stringResource(R.string.developer_tools), onBack = onBack) {
        SettingsList {
            item {
                SettingsCallout(
                    title = stringResource(R.string.developer_for_development_and_testing_only),
                    text = "These tools can expose technical information and change how the app behaves.",
                    modifier = Modifier.padding(top = WhiteNoiseSpacing.Section),
                    leading = {
                        Icon(
                            painter = painterResource(R.drawable.ic_warning),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }
            item {
                SettingsGroup(modifier = Modifier.padding(top = WhiteNoiseSpacing.Section)) {
                    SettingsSwitch(
                        title = stringResource(R.string.developer_tools),
                        checked = tools.isEnabled,
                        onCheckedChange = { onEnabled(it) },
                    )
                }
                SettingsExplainer(stringResource(R.string.developer_enable_technical_tools_for_this_profile))
            }
            if (tools.isEnabled) {
                if (parityController != null) item { DeveloperParityControls(profile, parityController) }
                item { SettingsSection(stringResource(R.string.developer_access_testing)) }
                item {
                    SettingsGroup {
                        if (speech != null) {
                            SettingsLink(stringResource(R.string.developer_read_aloud_history_outcomes), speech.edgeScenario.name, { speechOpen = true })
                            SettingsLink(stringResource(R.string.developer_read_aloud_engine_audio_and_background_outcomes), onClick = { speechPreferencesOpen = true })
                        }
                        if (capture != null) {
                            SettingsLink(stringResource(R.string.developer_dictation_outcomes), capture.scenario.developerLabel, { dictationOpen = true })
                            SettingsLink(stringResource(R.string.developer_voice_recording_outcomes), capture.voiceScenario.developerLabel, { recordingOpen = true })
                        }
                        SettingsLink(stringResource(R.string.developer_history_loading_scenarios), historyScenario.developerLabel, { historyOpen = true })
                        SettingsDivider()
                        SettingsLink(stringResource(R.string.developer_message_edit_outcomes), messageEditScenario.developerLabel, { editOpen = true })
                        SettingsLink(stringResource(R.string.developer_message_deletion_outcomes), messageDeleteScenario.developerLabel, { deleteOpen = true })
                        SettingsLink(stringResource(R.string.developer_recent_media_access), recentMediaAccess.name, { recentOpen = true })
                        SettingsDivider()
                        SettingsLink(stringResource(R.string.developer_attachment_transfer_outcomes), attachmentTransferScenario.developerLabel, { transferOpen = true })
                        downloadExampleControls()
                        relayPublicationControls()
                        updateControls()
                        SettingsDivider()
                        SettingsLink(stringResource(R.string.developer_photo_editor_outcomes), photoEditorScenario.developerLabel, { photoEditorOpen = true })
                        SettingsDivider()
                        SettingsLink(stringResource(R.string.developer_location_sharing_outcomes), locationScenario.developerLabel, { locationOpen = true })
                        SettingsLink(stringResource(R.string.developer_file_opening_outcomes), attachmentAccessScenario.developerLabel, { attachmentAccessOpen = true })
                        SettingsDivider()
                        SettingsLink(stringResource(R.string.developer_message_forwarding_outcomes), messageForwardScenario.developerLabel, { forwardOpen = true })
                        SettingsDivider()
                        SettingsLink(stringResource(R.string.developer_voice_search_scenarios), globalVoiceScenario.developerLabel, { globalVoiceOpen = true })
                        SettingsDivider()
                        SettingsLink(stringResource(R.string.developer_chat_action_scenarios), chatBatchScenario.developerLabel, { chatBatchOpen = true })
                        SettingsDivider()
                        SettingsLink(stringResource(R.string.developer_chat_connection_scenarios), profile.chatConnection.phase.name, { chatConnectionOpen = true })
                        SettingsDivider()
                        SettingsLink(stringResource(R.string.developer_profile_save_scenarios), profileSaveScenario.developerLabel, { profileSaveScenariosOpen = true })
                        SettingsDivider()
                        SettingsSwitch(stringResource(R.string.developer_next_profile_image_fails), profileImageFails, onProfileImageFails)
                        SettingsDivider()
                        SettingsLink(stringResource(R.string.developer_people_search_scenarios), peopleSearchScenario.developerLabel, { peopleScenariosOpen = true })
                        SettingsDivider()
                        SettingsLink(stringResource(R.string.developer_group_contact_scenarios), groupContactScenario.developerLabel, { groupScenariosOpen = true })
                        if (groupWork != null) {
                            SettingsDivider(); SettingsLink(stringResource(R.string.developer_group_roster), groupWork.rosterScenario.developerLabel, { groupWorkChoice = "roster" })
                            SettingsDivider(); SettingsLink(stringResource(R.string.developer_group_member_updates), groupWork.mutationScenario.developerLabel, { groupWorkChoice = "members" })
                            SettingsDivider(); SettingsLink(stringResource(R.string.developer_group_images), groupWork.imageScenario.developerLabel, { groupWorkChoice = "images" })
                            SettingsDivider(); SettingsLink(stringResource(R.string.developer_group_creation), groupWork.createScenario.developerLabel, { groupWorkChoice = "create" })
                            if (groupLifecycle != null) {
                                SettingsDivider(); SettingsLink(stringResource(R.string.developer_group_administration), groupLifecycle.scenario.developerLabel, { groupWorkChoice = "lifecycle" })
                                SettingsDivider(); SettingsLink(stringResource(R.string.developer_group_lifecycle), groupLifecycle.stateScenario.developerLabel, { groupWorkChoice = "groupState" })
                            }
                            if (transcript != null) { SettingsDivider(); SettingsLink(stringResource(R.string.developer_transcript_export), transcript.scenario.developerLabel, { groupWorkChoice = "transcript" }) }
                            if (auditLogs != null) {
                                SettingsDivider(); SettingsLink(stringResource(R.string.developer_audit_log_outcome),auditLogs.scenario.label,{auditChoice = true})
                            }
                            if (appLock != null) {
                                SettingsDivider(); SettingsLink(stringResource(R.string.developer_app_unlock_outcome),appLock.scenario.label,{unlockChoice = true})
                                SettingsDivider(); SettingsLink(stringResource(R.string.developer_lock_now),stringResource(R.string.developer_lock_now_help),appLock::lockNow)
                            }
                            if (notificationActions != null) {
                                SettingsDivider(); SettingsLink(stringResource(R.string.notification_action_title), stringResource(R.string.developer_notification_action_help), { actionChoice = "action" })
                                SettingsDivider(); SettingsLink(stringResource(R.string.developer_notification_action_outcome), notificationActions.scenario.label, { actionChoice = "outcome" })
                            }
                            if (incoming != null) {
                                SettingsDivider(); SettingsLink(stringResource(R.string.developer_incoming_request), stringResource(R.string.developer_incoming_request_help), { incomingExampleOpen = true })
                                SettingsDivider(); SettingsLink(stringResource(R.string.developer_incoming_request_outcome), incoming.scenario.developerLabel, { incomingOutcomeOpen = true })
                                SettingsDivider(); SettingsLink(stringResource(R.string.developer_defer_incoming_requests), if (incoming.locked) "Locked" else "Unlocked", { incoming.chooseLock(!incoming.locked) })
                            }
                            if (notificationControls != null) {
                                SettingsDivider(); SettingsLink(stringResource(R.string.developer_notification_outcomes),notificationControls.scenario.developerLabel,{notificationChoice = "outcome"})
                                SettingsDivider(); SettingsLink(stringResource(R.string.developer_native_push_availability),notificationControls.environment.push.name,{notificationChoice = "push"})
                                SettingsDivider(); SettingsLink(stringResource(R.string.developer_android_vibration_override),notificationControls.environment.vibrationOverride.name,{notificationChoice = "vibration"})
                                SettingsDivider(); SettingsSwitch(stringResource(R.string.developer_vibration_preview_available),notificationControls.environment.previewAvailable,{notificationControls.chooseEnvironment(notificationControls.environment.copy(previewAvailable = it))})
                                SettingsDivider(); SettingsSwitch(stringResource(R.string.developer_app_update_distribution),notificationControls.environment.updatesAvailable,{notificationControls.chooseEnvironment(notificationControls.environment.copy(updatesAvailable = it))})
                                SettingsDivider(); SettingsLink(stringResource(R.string.developer_stop_background_connection),stringResource(R.string.developer_stop_background_help),notificationControls::stopBackground,enabled = notificationControls.backgroundConnection)
                            }
                            if (retention != null) {
                                SettingsDivider(); SettingsLink(stringResource(R.string.developer_retention_update), retention.scenario.developerLabel, { groupWorkChoice = "retention" })
                                SettingsDivider(); SettingsLink(stringResource(R.string.developer_retention_example), retention.example.developerLabel, { groupWorkChoice = "expiry" })
                                SettingsDivider(); SettingsLink(stringResource(R.string.developer_advance_expiry_clock), stringResource(R.string.developer_advance_expiry_help), { retention.advanceExampleClock(60_000) })
                            }
                        }
                        SettingsDivider()
                        SettingsSwitch(stringResource(R.string.developer_next_created_chat_cannot_open), createdChatUnavailable, onCreatedChatUnavailable)
                        SettingsDivider()
                        SettingsLink(stringResource(R.string.developer_access_scenarios), accessScenario.developerLabel, { showAccessScenarios = true })
                        SettingsDivider()
                        SettingsAction(stringResource(R.string.developer_preview_startup_failure), onClick = onStartupFailure)
                        SettingsDivider()
                        SettingsLink(stringResource(R.string.developer_sign_out_scenarios), exitScenario.developerLabel, { showExitScenarios = true })
                        if (profile.signingMode == dev.ipf.whitenoise.model.ProfileSigningMode.LocalKey) {
                            SettingsDivider()
                            SettingsSwitch(stringResource(R.string.developer_local_key_available), profile.localKeyAvailable, onLocalKeyAvailable)
                        }
                    }
                    SettingsExplainer(stringResource(R.string.developer_choose_a_result_then_use_add_profile_or_sign_out_witho))
                }
                item { SettingsSection(stringResource(R.string.developer_debugging)) }
                item {
                    SettingsGroup {
                        SettingsSwitch(
                            title = stringResource(R.string.developer_debug_mode),
                            checked = tools.debugMode,
                            onCheckedChange = { onDebugMode(it) },
                        )
                        SettingsDivider()
                        SettingsLink(stringResource(R.string.developer_diagnostics), stringResource(R.string.developer_diagnostics_help), onDiagnostics)
                        SettingsDivider(); SettingsLink(stringResource(R.string.audit_logs_title),stringResource(R.string.audit_logs_sensitive),onAuditLogs)
                    }
                    SettingsExplainer(
                        stringResource(R.string.developer_debug_mode_adds_technical_details_to_supported_convers),
                    )
                }
                item {
                    SettingsGroup(modifier = Modifier.padding(top = WhiteNoiseSpacing.Section)) {
                        SettingsLink(stringResource(R.string.developer_key_packages), stringResource(R.string.developer_key_packages_help), onKeyPackages)
                    }
                }
                item { SettingsSection(stringResource(R.string.developer_diagnostic_logs)) }
                item {
                    val nonemptyRecords = profile.diagnostics.records.filter { it.byteCount > 0 }
                    SettingsGroup {
                        SettingsMetadata(
                            title = stringResource(R.string.developer_diagnostic_logging),
                            value = if (profile.diagnostics.loggingEnabled) "On" else "Off",
                        )
                        SettingsDivider()
                        if (nonemptyRecords.isEmpty()) {
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            ) {
                                Text(
                                    text = "There are no logs.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        nonemptyRecords.forEachIndexed { index, file ->
                            if (index > 0) SettingsDivider()
                            ListItem(
                                supportingContent = { Text("${fileSize(file.byteCount)} · ${file.createdLabel} · ${file.profileName}") },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            ) {
                                Text(file.filename, fontFamily = FontFamily.Monospace)
                            }
                        }
                        if (nonemptyRecords.isNotEmpty()) {
                            SettingsDivider()
                            SettingsAction(
                                title = stringResource(R.string.developer_export_diagnostic_logs),
                                onClick = {
                                    exportContent = profile.diagnostics.diagnosticLogExportText
                                    exportLogs.launch("White Noise Diagnostic Logs.txt")
                                },
                                leading = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_download),
                                        contentDescription = null,
                                    )
                                },
                            )
                        }
                    }
                    SettingsExplainer(
                        stringResource(R.string.developer_configure_or_clear_diagnostic_logs_in_privacy_security) +
                            "Existing sanitized files remain available here after logging is turned off.",
                    )
                }
            }
            item { SettingsSection(stringResource(R.string.about)) }
            item {
                SettingsGroup {
                    SettingsMetadata("Version", "0.1 (1)")
                    SettingsDivider()
                    SettingsMetadata("Built on", "MarmotKit (790eb860)")
                }
            }
        }
    }
    if (saveErrorDialog) {
        AlertDialog(
            onDismissRequest = { saveErrorDialog = false },
            title = { Text(stringResource(R.string.developer_couldnt_save_diagnostic_logs)) },
            text = { Text(stringResource(R.string.developer_choose_another_location_and_try_again)) },
            confirmButton = {
                TextButton(onClick = { saveErrorDialog = false }) {
                    Text(stringResource(R.string.batch_dismiss))
                }
            },
        )
    }
}

@Composable
fun DiagnosticsScreen(
    profile: Profile,
    diagnosticSummary: String?,
    onBack: () -> Unit,
    onTest: () -> Boolean,
    onClear: () -> Boolean,
    parityController: dev.ipf.whitenoise.state.DeveloperParityController? = null,
) {
    if (parityController != null) DeveloperOperationHost(profile, "diagnostics", parityController)
    var showHealth by rememberSaveable(profile.id) { mutableStateOf(false) }
    if (showHealth && parityController != null && profile.developerTools.isEnabled) DiagnosticHealthSheet(profile, parityController) {
        showHealth = false
        parityController.work?.let { if (it.phase == dev.ipf.whitenoise.model.DeveloperPhase.Running) parityController.dismiss(it.id) }
    }
    if (!profile.developerTools.isEnabled) {
        SettingsScaffold(title = stringResource(R.string.developer_diagnostics), onBack = onBack) { SettingsExplainer(stringResource(R.string.developer_disabled)) }
        return
    }
    val context = LocalContext.current
    val events = profile.developerTools.diagnosticEvents
    var actionsExpanded by remember { mutableStateOf(false) }
    SettingsScaffold(
        title = stringResource(R.string.developer_diagnostics),
        onBack = onBack,
        topBarActions = {
            Box {
                IconButton(
                    onClick = { actionsExpanded = true },
                    modifier = Modifier.testTag("diagnostics.actions"),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_vert),
                        contentDescription = stringResource(R.string.developer_diagnostic_actions),
                    )
                }
                WhiteNoiseDropdownMenu(
                    expanded = actionsExpanded,
                    onDismissRequest = { actionsExpanded = false },
                    modifier = Modifier.testTag("diagnostics.actions.menu"),
                    items = buildList {
                        if (parityController != null && profile.developerTools.isEnabled) add(WhiteNoiseMenuItem(
                            label = stringResource(R.string.developer_health), icon = R.drawable.ic_bug_report,
                            onClick = { showHealth = true }, modifier = Modifier.testTag("diagnostics.action.health"),
                        ))
                        diagnosticSummary?.let { summary ->
                            add(
                                WhiteNoiseMenuItem(
                                    label = "Copy Diagnostic Summary",
                                    icon = R.drawable.ic_content_copy,
                                    onClick = {
                                        copyToClipboard(context, "Diagnostic summary", summary)
                                    },
                                    modifier = Modifier.testTag("diagnostics.action.copy_summary"),
                                ),
                            )
                        }
                        add(
                            WhiteNoiseMenuItem(
                                label = "Test",
                                icon = R.drawable.ic_check,
                                onClick = { onTest() },
                                modifier = Modifier.testTag("diagnostics.action.test"),
                            ),
                        )
                        add(
                            WhiteNoiseMenuItem(
                                label = "Clear Events",
                                icon = R.drawable.ic_delete,
                                onClick = { onClear() },
                                enabled = events.isNotEmpty(),
                                modifier = Modifier.testTag("diagnostics.action.clear"),
                            ),
                        )
                    },
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(WhiteNoiseSpacing.CompactScreenMargin),
            verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.FormField),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = WhiteNoiseSpacing.FormField),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(R.string.developer_events),
                    modifier = Modifier.testTag("diagnostics.events_title"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                )
                DiagnosticLiveIndicator()
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = MaterialTheme.shapes.large,
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (events.isEmpty()) {
                        WhiteNoiseEmptyState(
                            title = stringResource(R.string.developer_no_events),
                            detail = "Run a diagnostic test to add a sanitized event.",
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                horizontal = WhiteNoiseSpacing.FormField,
                                vertical = WhiteNoiseSpacing.Related,
                            ),
                        ) {
                            itemsIndexed(events, key = { _, event -> event.id }) { index, event ->
                                Column {
                                    Text(
                                        event.text,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("diagnostics.event.$index")
                                            .padding(vertical = WhiteNoiseSpacing.FormField),
                                        fontFamily = FontFamily.Monospace,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    if (index != events.lastIndex) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticLiveIndicator() {
    val liveEventStreamDescription = stringResource(R.string.developer_live_event_stream)
    val infiniteTransition = rememberInfiniteTransition(label = "diagnostics_live")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.42f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "diagnostics_live_alpha",
    )
    Row(
        modifier = Modifier
            .testTag("diagnostics.live_indicator")
            .clearAndSetSemantics { contentDescription = liveEventStreamDescription },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_settings_cell_tower),
            contentDescription = null,
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer { alpha = pulseAlpha },
            tint = DiagnosticLiveGreen,
        )
        Text(
            text = "Live",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

private val DiagnosticLiveGreen = Color(0xFF188038)

@Composable
fun ConversationDebugScreen(
    profile: Profile,
    chat: Chat?,
    snapshot: ConversationDebugSnapshot?,
    onBack: () -> Unit,
    onOpenDeveloperTools: () -> Unit,
    onDiagnostics: () -> Unit,
    onAddArrival: (Boolean) -> Unit = {},
    onAddReadingExample: () -> Unit = {},
    onAddAttachmentExamples: () -> Unit = {},
    onAddAgentExamples: () -> Unit = {},
    onAddNostrEventExamples: () -> Unit = {},
    parityController: dev.ipf.whitenoise.state.DeveloperParityController? = null,
) {
    val context = LocalContext.current
    if (chat != null && parityController != null) DeveloperOperationHost(profile, "push:${chat.id}", parityController)
    SettingsScaffold(title = stringResource(R.string.conversation_debug), onBack = onBack) {
        when (chat?.let { ConversationDebugPolicy.access(profile, it.id) } ?: ConversationDebugAccess.Unavailable) {
            ConversationDebugAccess.Unavailable -> DebugUnavailable("Chat unavailable", "This conversation is no longer available for inspection.")
            ConversationDebugAccess.Disabled -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(WhiteNoiseSpacing.CompactScreenMargin),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    WhiteNoiseEmptyState(
                        title = stringResource(R.string.developer_conversation_debugging_is_off),
                        detail = "Turn on Developer Tools and Debug Mode for this profile to inspect this chat.",
                    )
                    WhiteNoiseButton(
                        onClick = onOpenDeveloperTools,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.developer_open_developer_tools)) }
                }
            }
            ConversationDebugAccess.Enabled -> {
                val info = snapshot
                if (info == null) {
                    DebugUnavailable(
                        "Debug data unavailable",
                        "Technical details for this conversation could not be prepared.",
                    )
                } else {
                    SettingsList {
                    item { SettingsSection(stringResource(R.string.developer_conversation)) }
                    item {
                        SettingsGroup {
                            DebugValue("State", info.lifecycle)
                            DebugValue("Epoch", info.epoch.toString())
                            info.memberCount?.let { DebugValue("MLS members", it.toString()) }
                            info.adminCount?.let { DebugValue("Admins", it.toString()) }
                            info.currentRole?.let { DebugValue("Your role", it) }
                            DebugValue("Event kinds", info.requiredEventKinds.joinToString())
                            DebugValue("Required components", dev.ipf.whitenoise.model.DeveloperInspection.requiredComponents.joinToString("\n"))
                            CopyableDebugValue("MLS group ID", info.mlsGroupId) {
                                copyToClipboard(context, "MLS group ID", info.mlsGroupId)
                            }
                            CopyableDebugValue("Nostr group ID", info.nostrGroupId) {
                                copyToClipboard(context, "Nostr group ID", info.nostrGroupId)
                            }
                        }
                    }
                    item { SettingsSection(stringResource(R.string.developer_history_examples)) }
                    item {
                        SettingsGroup {
                            SettingsLink(stringResource(R.string.developer_add_unread_mention), stringResource(R.string.developer_unread_mention_help), { onAddArrival(false) })
                            SettingsDivider()
                            SettingsLink(stringResource(R.string.developer_add_streaming_message), stringResource(R.string.developer_streaming_message_help), { onAddArrival(true) })
                            SettingsDivider()
                            SettingsLink(
                                stringResource(R.string.agent_examples_add),
                                stringResource(R.string.agent_examples_add_detail),
                                onAddAgentExamples,
                            )
                            SettingsDivider()
                            SettingsLink(
                                stringResource(R.string.nostr_event_examples_add),
                                stringResource(R.string.nostr_event_examples_add_detail),
                                onAddNostrEventExamples,
                            )
                            SettingsDivider()
                            SettingsLink(stringResource(R.string.developer_add_long_document), stringResource(R.string.developer_long_document_help), onAddReadingExample)
                            SettingsDivider()
                            SettingsLink(stringResource(R.string.developer_add_file_examples), stringResource(R.string.developer_file_examples_help), onAddAttachmentExamples)
                        }
                    }
                    if (parityController != null) item {
                        SettingsGroup { TextButton(onClick = { parityController.begin(dev.ipf.whitenoise.model.DeveloperOperation.RefreshPush) },
                            enabled = parityController.work?.phase != dev.ipf.whitenoise.model.DeveloperPhase.Running) { Text(stringResource(R.string.developer_refresh)) } }
                        DeveloperResult(parityController)
                    }
                    if (parityController == null || parityController.work?.phase == dev.ipf.whitenoise.model.DeveloperPhase.Complete) {
                    item { SettingsSection(stringResource(R.string.developer_delivery_notifications)) }
                    item {
                        SettingsGroup {
                            DebugValue("Chat relays", info.relayCount.toString())
                            DebugValue("Notifications", if (info.push.notificationsEnabled) "On" else "Off")
                            DebugValue("Push", info.push.registrationStatus)
                            DebugValue("Total tokens", info.push.totalTokenCount.toString())
                            DebugValue("Active tokens", info.push.activeTokenCount.toString())
                            DebugValue("Local notifications", info.push.localNotifications.toString())
                            DebugValue("Shareable", info.push.shareable.toString())
                            DebugValue("Local leaf", info.push.localLeaf?.toString() ?: "Unavailable")
                            DebugValue("Local token cached", info.push.localTokenCached.toString())
                            DebugValue("Token list updated", info.push.updatedAt)
                            if (info.push.staleTokenCount > 0) {
                                DebugValue("Push tokens", "${info.push.staleTokenCount} stale")
                            }
                            if (info.push.missingRelayHintCount > 0) {
                                DebugValue("Relay hints", "${info.push.missingRelayHintCount} missing")
                            }
                        }
                    }
                    info.push.members.forEach { token -> item(key = "push-${token.memberId}") {
                        SettingsSection(stringResource(R.string.developer_member_token, token.leaf))
                        SettingsGroup {
                            CopyableDebugValue("Member", token.memberId) { copyToClipboard(context, "Member", token.memberId) }
                            DebugValue("Platform", token.platform)
                            DebugValue("Fingerprint", token.fingerprint)
                            CopyableDebugValue("Push server public key", token.serverKey) { copyToClipboard(context, "Push server public key", token.serverKey) }
                            DebugValue("Relay hint", token.relayHint.toString())
                            DebugValue("Active leaf", token.activeLeaf.toString())
                            DebugValue("Matches active leaf", token.matchesLeaf.toString())
                            DebugValue("Local member", token.localMember.toString())
                            DebugValue("Updated", token.updatedAt)
                        }
                    } }
                    }
                    item { SettingsSection(stringResource(R.string.developer_diagnostics)) }
                    item {
                        SettingsGroup {
                            SettingsLink(stringResource(R.string.developer_diagnostics), stringResource(R.string.developer_diagnostics_conversation_help), onDiagnostics)
                        }
                    }
                    }
                }
            }
        }
    }
}

@Composable
private fun DebugUnavailable(title: String, detail: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        WhiteNoiseEmptyState(title = title, detail = detail)
    }
}

@Composable
private fun DebugValue(label: String, value: String) {
    SettingsValue(label, value)
}

@Composable
private fun CopyableDebugValue(label: String, value: String, onCopy: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = { Text(shorten(value), fontFamily = FontFamily.Monospace) },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_content_copy),
                contentDescription = null,
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCopy)
            .semantics { role = Role.Button },
    )
}

private fun shorten(value: String): String =
    if (value.length <= 22) value else "${value.take(12)}…${value.takeLast(6)}"

private fun fileSize(bytes: Int): String = when {
    bytes == 0 -> "0 B"
    bytes >= 1_000 -> "${bytes / 1_000} KB"
    else -> "$bytes B"
}
