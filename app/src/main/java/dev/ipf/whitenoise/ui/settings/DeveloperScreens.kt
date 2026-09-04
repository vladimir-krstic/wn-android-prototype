package dev.ipf.whitenoise.ui.settings

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
    if (recordingOpen && capture != null) ScenarioChoiceDialog("Voice recording outcomes", dev.ipf.whitenoise.model.VoiceCaptureScenario.entries,
        capture.voiceScenario, { it.developerLabel }, { capture.chooseVoiceScenario(it) }, { recordingOpen = false })
    if (dictationOpen && capture != null) ScenarioChoiceDialog("Dictation outcomes", dev.ipf.whitenoise.model.DictationScenario.entries,
        capture.scenario, { it.developerLabel }, { capture.chooseScenario(it) }, { dictationOpen = false })
    var speechPreferencesOpen by remember { mutableStateOf(false) }
    if (speechPreferencesOpen && speech != null) SpeechDeveloperDialog(profile, speech) { speechPreferencesOpen = false }
    if (speechOpen && speech != null) ScenarioChoiceDialog("Read Aloud history", dev.ipf.whitenoise.model.SpeechEdgeScenario.entries,
        speech.edgeScenario, { it.name }, { speech.setEdgeScenario(profile.id, it) }, { speechOpen = false })
    var locationOpen by remember { mutableStateOf(false) }
    if (locationOpen) ScenarioChoiceDialog("Location sharing", dev.ipf.whitenoise.model.LocationScenario.entries,
        locationScenario, { it.developerLabel }, onLocationScenario, { locationOpen = false })
    var attachmentAccessOpen by remember { mutableStateOf(false) }
    if (attachmentAccessOpen) ScenarioChoiceDialog("File opening", dev.ipf.whitenoise.model.AttachmentAccessScenario.entries,
        attachmentAccessScenario, { it.developerLabel }, onAttachmentAccessScenario, { attachmentAccessOpen = false })
    if (photoEditorOpen) ScenarioChoiceDialog("Photo editor", dev.ipf.whitenoise.model.PhotoEditorScenario.entries,
        photoEditorScenario, { it.developerLabel }, onPhotoEditorScenario, { photoEditorOpen = false })
    if (recentOpen) ScenarioChoiceDialog("Recent media access", dev.ipf.whitenoise.model.RecentMediaAccess.entries,
        recentMediaAccess, { it.name }, onRecentMediaAccess, { recentOpen = false })
    if (transferOpen) ScenarioChoiceDialog("Attachment transfer", dev.ipf.whitenoise.model.AttachmentTransferScenario.entries,
        attachmentTransferScenario, { it.developerLabel }, onAttachmentTransferScenario, { transferOpen = false })
    var forwardOpen by remember { mutableStateOf(false) }
    if (deleteOpen) ScenarioChoiceDialog("Message deletion", dev.ipf.whitenoise.model.MessageDeleteScenario.entries,
        messageDeleteScenario, { it.developerLabel }, onMessageDeleteScenario, { deleteOpen = false })
    if (forwardOpen) ScenarioChoiceDialog("Message forwarding", dev.ipf.whitenoise.model.MessageForwardScenario.entries,
        messageForwardScenario, { it.developerLabel }, onMessageForwardScenario, { forwardOpen = false })
    if (editOpen) ScenarioChoiceDialog("Message editing", dev.ipf.whitenoise.model.MessageEditScenario.entries,
        messageEditScenario, { it.developerLabel }, onMessageEditScenario, { editOpen = false })
    if (historyOpen) ScenarioChoiceDialog("Conversation history", dev.ipf.whitenoise.model.HistoryScenario.entries,
        historyScenario, { it.developerLabel }, onHistoryScenario, { historyOpen = false })
    var globalVoiceOpen by remember { mutableStateOf(false) }
    if (globalVoiceOpen) ScenarioChoiceDialog("Voice search", dev.ipf.whitenoise.model.GlobalVoiceScenario.entries,
        globalVoiceScenario, { it.developerLabel }, onGlobalVoiceScenario, { globalVoiceOpen = false })
    var chatBatchOpen by remember { mutableStateOf(false) }
    var chatConnectionOpen by remember { mutableStateOf(false) }
    if (chatBatchOpen) ScenarioChoiceDialog("Chat actions", dev.ipf.whitenoise.model.ChatBatchScenario.entries,
        chatBatchScenario, { it.developerLabel }, onChatBatchScenario, { chatBatchOpen = false })
    if (chatConnectionOpen) ScenarioChoiceDialog("Chat connection", dev.ipf.whitenoise.model.ChatConnectionScenario.entries,
        if (profile.chatConnection.retryFails) dev.ipf.whitenoise.model.ChatConnectionScenario.RetryFailure
        else dev.ipf.whitenoise.model.ChatConnectionScenario.entries.first { it.name == profile.chatConnection.phase.name || (it == dev.ipf.whitenoise.model.ChatConnectionScenario.RetryFailure && profile.chatConnection.phase == dev.ipf.whitenoise.model.ChatConnectionPhase.Failed) },
        { it.developerLabel }, onChatConnectionScenario, { chatConnectionOpen = false })
    var profileSaveScenariosOpen by remember { mutableStateOf(false) }
    if (profileSaveScenariosOpen) ScenarioChoiceDialog("Profile save", dev.ipf.whitenoise.model.ProfileSaveScenario.entries,
        profileSaveScenario, { it.developerLabel }, onProfileSaveScenario, { profileSaveScenariosOpen = false })
    var peopleScenariosOpen by remember { mutableStateOf(false) }
    var groupScenariosOpen by remember { mutableStateOf(false) }
    val incoming = dev.ipf.whitenoise.ui.share.LocalIncoming.current
    val notificationControls = LocalNotificationControls.current
    val notificationActions = LocalNotificationActions.current
    var actionChoice by rememberSaveable(profile.id) { mutableStateOf<String?>(null) }
    if (notificationActions != null && actionChoice == "outcome") ScenarioChoiceDialog("Notification action outcome",
        dev.ipf.whitenoise.state.NotificationActionScenario.entries,notificationActions.scenario,{it.label},notificationActions::choose,{actionChoice = null})
    if (notificationActions != null && actionChoice == "action") ScenarioChoiceDialog("Notification action",
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
            "outcome" -> ScenarioChoiceDialog("Notification settings outcomes",dev.ipf.whitenoise.state.NotificationScenario.entries,
                notificationControls.scenario,{it.developerLabel},notificationControls::choose,{notificationChoice = null})
            "push" -> ScenarioChoiceDialog("Native push availability",dev.ipf.whitenoise.model.PushAvailability.entries,
                notificationControls.environment.push,{it.name},{notificationControls.chooseEnvironment(notificationControls.environment.copy(push = it))},{notificationChoice = null})
            "vibration" -> ScenarioChoiceDialog("Android vibration override",dev.ipf.whitenoise.model.AndroidVibrationOverride.entries,
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
    if (incoming != null && incomingExampleOpen) ScenarioChoiceDialog("Incoming request", dev.ipf.whitenoise.model.IncomingExample.entries,
        dev.ipf.whitenoise.model.IncomingExample.Text, { it.label }, { example -> incoming.receive(dev.ipf.whitenoise.model.IncomingExamples.entry(example,profile,incoming.signedProfiles())) }, { incomingExampleOpen = false })
    if (incoming != null && incomingOutcomeOpen) ScenarioChoiceDialog("Incoming request outcome", dev.ipf.whitenoise.state.IncomingScenario.entries,
        incoming.scenario, { it.developerLabel }, incoming::choose, { incomingOutcomeOpen = false })
    if (retention != null && groupWorkChoice == "retention") ScenarioChoiceDialog("Retention update", dev.ipf.whitenoise.state.RetentionScenario.entries, retention.scenario, { it.developerLabel }, retention::choose, { groupWorkChoice = null })
    if (retention != null && groupWorkChoice == "expiry") ScenarioChoiceDialog("Retention example", dev.ipf.whitenoise.state.RetentionExample.entries, retention.example, { it.developerLabel }, retention::chooseExample, { groupWorkChoice = null })
    if (groupLifecycle != null && groupWorkChoice == "lifecycle") ScenarioChoiceDialog("Group administration", dev.ipf.whitenoise.model.GroupLifecycleScenario.entries, groupLifecycle.scenario, { it.developerLabel }, groupLifecycle::choose, { groupWorkChoice = null })
    if (groupLifecycle != null && groupWorkChoice == "groupState") ScenarioChoiceDialog("Group lifecycle", dev.ipf.whitenoise.model.GroupStateScenario.entries, groupLifecycle.stateScenario, { it.developerLabel }, groupLifecycle::chooseState, { groupWorkChoice = null })
    if (transcript != null && groupWorkChoice == "transcript") ScenarioChoiceDialog("Transcript export", dev.ipf.whitenoise.model.TranscriptScenario.entries, transcript.scenario, { it.developerLabel }, transcript::choose, { groupWorkChoice = null })
    if (groupWork != null) when (groupWorkChoice) {
        "roster" -> ScenarioChoiceDialog("Group roster", dev.ipf.whitenoise.model.GroupRosterScenario.entries, groupWork.rosterScenario, { it.developerLabel }, groupWork::chooseRoster, { groupWorkChoice = null })
        "members" -> ScenarioChoiceDialog("Group member updates", dev.ipf.whitenoise.model.GroupMutationScenario.entries, groupWork.mutationScenario, { it.developerLabel }, groupWork::chooseMutation, { groupWorkChoice = null })
        "images" -> ScenarioChoiceDialog("Group images", dev.ipf.whitenoise.model.GroupImageScenario.entries, groupWork.imageScenario, { it.developerLabel }, groupWork::chooseImage, { groupWorkChoice = null })
        "create" -> ScenarioChoiceDialog("Group creation", dev.ipf.whitenoise.model.GroupCreateScenario.entries, groupWork.createScenario, { it.developerLabel }, groupWork::chooseCreate, { groupWorkChoice = null })
    }

    if (peopleScenariosOpen) ScenarioChoiceDialog("People search", dev.ipf.whitenoise.model.PeopleSearchScenario.entries,
        peopleSearchScenario, { it.developerLabel }, onPeopleSearchScenario, { peopleScenariosOpen = false })
    if (groupScenariosOpen) ScenarioChoiceDialog("Group contact actions", dev.ipf.whitenoise.model.GroupContactScenario.entries,
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
    SettingsScaffold(title = "Developer Tools", onBack = onBack) {
        SettingsList {
            item {
                SettingsCallout(
                    title = "For development and testing only",
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
                        title = "Developer Tools",
                        checked = tools.isEnabled,
                        onCheckedChange = { onEnabled(it) },
                    )
                }
                SettingsExplainer("Enable technical tools for this profile.")
            }
            if (tools.isEnabled) {
                item { SettingsSection("Access testing") }
                item {
                    SettingsGroup {
                        if (speech != null) {
                            SettingsLink("Read Aloud history outcomes", speech.edgeScenario.name, { speechOpen = true })
                            SettingsLink("Read Aloud engine, audio and background outcomes", onClick = { speechPreferencesOpen = true })
                        }
                        if (capture != null) {
                            SettingsLink("Dictation outcomes", capture.scenario.developerLabel, { dictationOpen = true })
                            SettingsLink("Voice recording outcomes", capture.voiceScenario.developerLabel, { recordingOpen = true })
                        }
                        SettingsLink("History loading scenarios", historyScenario.developerLabel, { historyOpen = true })
                        SettingsDivider()
                        SettingsLink("Message edit outcomes", messageEditScenario.developerLabel, { editOpen = true })
                        SettingsLink("Message deletion outcomes", messageDeleteScenario.developerLabel, { deleteOpen = true })
                        SettingsLink("Recent media access", recentMediaAccess.name, { recentOpen = true })
                        SettingsDivider()
                        SettingsLink("Attachment transfer outcomes", attachmentTransferScenario.developerLabel, { transferOpen = true })
                        SettingsDivider()
                        SettingsLink("Photo editor outcomes", photoEditorScenario.developerLabel, { photoEditorOpen = true })
                        SettingsDivider()
                        SettingsLink("Location sharing outcomes", locationScenario.developerLabel, { locationOpen = true })
                        SettingsLink("File opening outcomes", attachmentAccessScenario.developerLabel, { attachmentAccessOpen = true })
                        SettingsDivider()
                        SettingsLink("Message forwarding outcomes", messageForwardScenario.developerLabel, { forwardOpen = true })
                        SettingsDivider()
                        SettingsLink("Voice search scenarios", globalVoiceScenario.developerLabel, { globalVoiceOpen = true })
                        SettingsDivider()
                        SettingsLink("Chat action scenarios", chatBatchScenario.developerLabel, { chatBatchOpen = true })
                        SettingsDivider()
                        SettingsLink("Chat connection scenarios", profile.chatConnection.phase.name, { chatConnectionOpen = true })
                        SettingsDivider()
                        SettingsLink("Profile save scenarios", profileSaveScenario.developerLabel, { profileSaveScenariosOpen = true })
                        SettingsDivider()
                        SettingsSwitch("Next profile image fails", profileImageFails, onProfileImageFails)
                        SettingsDivider()
                        SettingsLink("People search scenarios", peopleSearchScenario.developerLabel, { peopleScenariosOpen = true })
                        SettingsDivider()
                        SettingsLink("Group contact scenarios", groupContactScenario.developerLabel, { groupScenariosOpen = true })
                        if (groupWork != null) {
                            SettingsDivider(); SettingsLink("Group roster", groupWork.rosterScenario.developerLabel, { groupWorkChoice = "roster" })
                            SettingsDivider(); SettingsLink("Group member updates", groupWork.mutationScenario.developerLabel, { groupWorkChoice = "members" })
                            SettingsDivider(); SettingsLink("Group images", groupWork.imageScenario.developerLabel, { groupWorkChoice = "images" })
                            SettingsDivider(); SettingsLink("Group creation", groupWork.createScenario.developerLabel, { groupWorkChoice = "create" })
                            if (groupLifecycle != null) {
                                SettingsDivider(); SettingsLink("Group administration", groupLifecycle.scenario.developerLabel, { groupWorkChoice = "lifecycle" })
                                SettingsDivider(); SettingsLink("Group lifecycle", groupLifecycle.stateScenario.developerLabel, { groupWorkChoice = "groupState" })
                            }
                            if (transcript != null) { SettingsDivider(); SettingsLink("Transcript export", transcript.scenario.developerLabel, { groupWorkChoice = "transcript" }) }
                            if (notificationActions != null) {
                                SettingsDivider(); SettingsLink("Notification action", "Reply, react or mark a message read", { actionChoice = "action" })
                                SettingsDivider(); SettingsLink("Notification action outcome", notificationActions.scenario.label, { actionChoice = "outcome" })
                            }
                            if (incoming != null) {
                                SettingsDivider(); SettingsLink("Incoming request", "Open an owned share, notification, shortcut or profile link", { incomingExampleOpen = true })
                                SettingsDivider(); SettingsLink("Incoming request outcome", incoming.scenario.developerLabel, { incomingOutcomeOpen = true })
                                SettingsDivider(); SettingsLink("Defer incoming requests", if (incoming.locked) "Locked" else "Unlocked", { incoming.chooseLock(!incoming.locked) })
                            }
                            if (notificationControls != null) {
                                SettingsDivider(); SettingsLink("Notification outcomes",notificationControls.scenario.developerLabel,{notificationChoice = "outcome"})
                                SettingsDivider(); SettingsLink("Native push availability",notificationControls.environment.push.name,{notificationChoice = "push"})
                                SettingsDivider(); SettingsLink("Android vibration override",notificationControls.environment.vibrationOverride.name,{notificationChoice = "vibration"})
                                SettingsDivider(); SettingsSwitch("Vibration preview available",notificationControls.environment.previewAvailable,{notificationControls.chooseEnvironment(notificationControls.environment.copy(previewAvailable = it))})
                                SettingsDivider(); SettingsSwitch("App update distribution",notificationControls.environment.updatesAvailable,{notificationControls.chooseEnvironment(notificationControls.environment.copy(updatesAvailable = it))})
                                SettingsDivider(); SettingsLink("Stop background connection","Report a runtime stop",notificationControls::stopBackground,enabled = notificationControls.backgroundConnection)
                            }
                            if (retention != null) {
                                SettingsDivider(); SettingsLink("Retention update", retention.scenario.developerLabel, { groupWorkChoice = "retention" })
                                SettingsDivider(); SettingsLink("Retention example", retention.example.developerLabel, { groupWorkChoice = "expiry" })
                                SettingsDivider(); SettingsLink("Advance expiry clock", "Add one minute to the local clock", { retention.advanceExampleClock(60_000) })
                            }
                        }
                        SettingsDivider()
                        SettingsSwitch("Next created chat cannot open", createdChatUnavailable, onCreatedChatUnavailable)
                        SettingsDivider()
                        SettingsLink("Access scenarios", accessScenario.developerLabel, { showAccessScenarios = true })
                        SettingsDivider()
                        SettingsAction("Preview startup failure", onClick = onStartupFailure)
                        SettingsDivider()
                        SettingsLink("Sign-out scenarios", exitScenario.developerLabel, { showExitScenarios = true })
                        if (profile.signingMode == dev.ipf.whitenoise.model.ProfileSigningMode.LocalKey) {
                            SettingsDivider()
                            SettingsSwitch("Local key available", profile.localKeyAvailable, onLocalKeyAvailable)
                        }
                    }
                    SettingsExplainer("Choose a result, then use Add Profile or sign out without wiping to test retained-profile entry.")
                }
                item { SettingsSection("Debugging") }
                item {
                    SettingsGroup {
                        SettingsSwitch(
                            title = "Debug Mode",
                            checked = tools.debugMode,
                            onCheckedChange = { onDebugMode(it) },
                        )
                        SettingsDivider()
                        SettingsLink("Diagnostics", "Sanitized event console", onDiagnostics)
                    }
                    SettingsExplainer(
                        "Debug Mode adds technical details to supported conversations.",
                    )
                }
                item {
                    SettingsGroup(modifier = Modifier.padding(top = WhiteNoiseSpacing.Section)) {
                        SettingsLink("Key Packages", "One current package", onKeyPackages)
                    }
                }
                item { SettingsSection("Diagnostic Logs") }
                item {
                    val nonemptyRecords = profile.diagnostics.records.filter { it.byteCount > 0 }
                    SettingsGroup {
                        SettingsMetadata(
                            title = "Diagnostic Logging",
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
                                title = "Export Diagnostic Logs",
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
                        "Configure or clear diagnostic logs in Privacy & Security. " +
                            "Existing sanitized files remain available here after logging is turned off.",
                    )
                }
            }
            item { SettingsSection("About") }
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
            title = { Text("Couldn’t Save Diagnostic Logs") },
            text = { Text("Choose another location and try again.") },
            confirmButton = {
                TextButton(onClick = { saveErrorDialog = false }) {
                    Text("Dismiss")
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
) {
    val context = LocalContext.current
    val events = profile.developerTools.diagnosticEvents
    var actionsExpanded by remember { mutableStateOf(false) }
    SettingsScaffold(
        title = "Diagnostics",
        onBack = onBack,
        topBarActions = {
            Box {
                IconButton(
                    onClick = { actionsExpanded = true },
                    modifier = Modifier.testTag("diagnostics.actions"),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_vert),
                        contentDescription = "Diagnostic actions",
                    )
                }
                WhiteNoiseDropdownMenu(
                    expanded = actionsExpanded,
                    onDismissRequest = { actionsExpanded = false },
                    modifier = Modifier.testTag("diagnostics.actions.menu"),
                    items = buildList {
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
                    "Events",
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
                            title = "No Events",
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
            .clearAndSetSemantics { contentDescription = "Live event stream" },
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
fun KeyPackagesScreen(
    profile: Profile,
    onBack: () -> Unit,
    onPublish: () -> Boolean,
) {
    val keyPackage = profile.developerTools.keyPackage
    SettingsScaffold(
        title = "Key Packages",
        onBack = onBack,
    ) {
        SettingsList {
            item { SettingsSection("Current key package") }
            item {
                SettingsGroup {
                    ListItem(
                        headlineContent = {
                            Text(keyPackage.id, fontFamily = FontFamily.Monospace)
                        },
                        supportingContent = {
                            Text(if (profile.connectionInformationPublished) "Published ${keyPackage.published} · ${keyPackage.size}" else "Not published · ${keyPackage.size}")
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }
            item {
                SettingsExplainer(
                    if (profile.connectionInformationPublished) "This profile uses the current package to receive group invitations."
                    else "No key package is currently published. Publish one to receive new group invitations.",
                )
            }
            item {
                WhiteNoiseFilledTonalButton(
                    onClick = { onPublish() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = WhiteNoiseSpacing.CompactScreenMargin,
                            top = WhiteNoiseSpacing.Section,
                            end = WhiteNoiseSpacing.CompactScreenMargin,
                        )
                        .testTag("key_packages.publish"),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings_key),
                        contentDescription = null,
                    )
                    Spacer(Modifier.size(WhiteNoiseSpacing.Related))
                    Text("Publish New Key Package")
                }
                SettingsExplainer(
                    "Publishing replaces the current package so this profile can receive new group invitations.",
                )
            }
        }
    }
}

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
) {
    val context = LocalContext.current
    SettingsScaffold(title = "Conversation Debug", onBack = onBack) {
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
                        title = "Conversation Debugging Is Off",
                        detail = "Turn on Developer Tools and Debug Mode for this profile to inspect this chat.",
                    )
                    WhiteNoiseButton(
                        onClick = onOpenDeveloperTools,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Open Developer Tools") }
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
                    item { SettingsSection("Conversation") }
                    item {
                        SettingsGroup {
                            DebugValue("State", info.lifecycle)
                            DebugValue("Epoch", info.epoch.toString())
                            info.memberCount?.let { DebugValue("MLS members", it.toString()) }
                            info.adminCount?.let { DebugValue("Admins", it.toString()) }
                            info.currentRole?.let { DebugValue("Your role", it) }
                            DebugValue("Event kinds", "${info.requiredEventKinds.size} required")
                            CopyableDebugValue("MLS group ID", info.mlsGroupId) {
                                copyToClipboard(context, "MLS group ID", info.mlsGroupId)
                            }
                            CopyableDebugValue("Nostr group ID", info.nostrGroupId) {
                                copyToClipboard(context, "Nostr group ID", info.nostrGroupId)
                            }
                        }
                    }
                    item { SettingsSection("History examples") }
                    item {
                        SettingsGroup {
                            SettingsLink("Add unread mention", "Return to the chat to inspect the preserved unread boundary", { onAddArrival(false) })
                            SettingsDivider()
                            SettingsLink("Add streaming message", "Includes receipt, sender time and expiry metadata", { onAddArrival(true) })
                            SettingsDivider()
                            SettingsLink("Add long document", "Markdown, selection and revision history", onAddReadingExample)
                            SettingsDivider()
                            SettingsLink("Add file examples", "Text, Markdown, audio and package outcomes", onAddAttachmentExamples)
                        }
                    }
                    item { SettingsSection("Delivery & notifications") }
                    item {
                        SettingsGroup {
                            DebugValue("Chat relays", info.relayCount.toString())
                            DebugValue("Notifications", if (info.push.notificationsEnabled) "On" else "Off")
                            DebugValue("Push", info.push.registrationStatus)
                            if (info.push.staleTokenCount > 0) {
                                DebugValue("Push tokens", "${info.push.staleTokenCount} stale")
                            }
                            if (info.push.missingRelayHintCount > 0) {
                                DebugValue("Relay hints", "${info.push.missingRelayHintCount} missing")
                            }
                        }
                    }
                    item { SettingsSection("Diagnostics") }
                    item {
                        SettingsGroup {
                            SettingsLink("Diagnostics", "Copy a sanitized summary or inspect events", onDiagnostics)
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
