package dev.ipf.whitenoise.scenarios

import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.AppRoute
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import dev.ipf.whitenoise.state.*

/** Every variant owns both its explanation and its entry recipe. No selection is stored in the user's profile. */
internal data class ScenarioDestination(val route: AppRoute, val action: String? = null)
internal data class ScenarioVariant(val id: String, val title: String, val description: String,
    val prepare: (AppViewModel) -> ScenarioDestination)
internal data class ScenarioDefinition(val id: String, val group: String, val title: String,
    val description: String, val variants: List<ScenarioVariant>, val deviceNote: String? = null)

internal fun String.scenarioWords() = replace(Regex("([a-z])([A-Z])"), "$1 $2")
private inline fun <reified T : Enum<T>> scenario(id: String, group: String, title: String, instructions: String,
    device: String? = null, noinline label: (T) -> String = { it.name.scenarioWords() },
    noinline detail: (T) -> String = { "Expected result: ${label(it)}. $instructions" },
    noinline prepare: (AppViewModel, T) -> ScenarioDestination) =
    ScenarioDefinition(id, group, title, instructions, enumValues<T>().map { value ->
        ScenarioVariant(value.name, label(value), detail(value)) { prepare(it, value) }
    }, device)

private fun choices(id: String, group: String, title: String, instructions: String,
    vararg values: Pair<String, String>, prepare: (AppViewModel, String) -> ScenarioDestination) =
    ScenarioDefinition(id, group, title, instructions, values.map { (key, description) ->
        ScenarioVariant(key, key.scenarioWords(), description) { prepare(it, key) }
    })

internal object ScenarioCatalog {
    const val device = "This opens the existing Android integration. Permissions, installed apps and device capabilities can affect the result. Exit restores app data, but cannot undo files you export or Android settings you change."
    fun profile(vm: AppViewModel) = checkNotNull(vm.uiState.activeProfile)
    fun direct(vm: AppViewModel) = profile(vm).chats.first { !it.isGroup && it.membership == ChatMembership.Active && it.timeline.isNotEmpty() }.id
    fun group(vm: AppViewModel) = profile(vm).chats.first { it.isGroup && it.hasAuthoritativeGroupAdmin(profile(vm).id) }.id
    fun conversation(vm: AppViewModel, action: String? = null) = ScenarioDestination(AppRoute.Conversation(direct(vm)), action)
    private fun receive(vm: AppViewModel, example: IncomingExample) {
        if (example == IncomingExample.NotificationOtherProfile) {
            vm.completeSignIn(OnboardingOrigin.AddProfile)
            vm.setDeveloperToolsEnabled(true)
            vm.prepareScenarioProfile { it.copy(chats = ProfileFixtures.marmota.chats) }
            vm.selectProfile(ProfileFixtures.MARMOTA_ID)
        }
        vm.incoming.receive(IncomingExamples.entry(example, profile(vm), vm.uiState.signedInProfiles))
    }
    private fun notification(vm: AppViewModel, kind: NotificationActionKind) {
        val entry = IncomingExamples.entry(IncomingExample.NotificationMessage, profile(vm), vm.uiState.signedInProfiles) as IncomingEntry.Notification
        val generation = vm.notificationActions.nextExampleId()
        val card = NotificationCard("scenario-message", generation, entry.target)
        vm.notificationActions.recordCard(card)
        vm.notificationActions.submit(NotificationActionInput("scenario-action-$generation", card, kind,
            if (kind == NotificationActionKind.React) profile(vm).quickReactions.first() else "Thanks, see you there."))
    }

    val all: List<ScenarioDefinition> = listOf(
        choices("account-entry", "Accounts", "Onboarding entry points", "Start at the selected entry point with the matching account state. Complete the form to continue.",
            "FirstSignUp" to "The app has no accounts. Create your first account and continue to Chats.",
            "FirstSignIn" to "The app has no accounts. The example private key is filled in; tap Sign In to run profile setup.",
            "AddSignUp" to "An account is already signed in. Create another account without losing the first.",
            "AddSignIn" to "An account is already signed in. Tap Sign In to add the prepared example account.",
            "RetainedAccount" to "A signed-out account is retained. Choose it on Welcome to sign back in.") { vm, value ->
            when (value) {
                "FirstSignUp", "FirstSignIn" -> {
                    check(vm.eraseAppData(WipeConfirmationPhrase.make(vm.uiState.profiles.map { it.id })))
                    ScenarioDestination(if (value == "FirstSignUp") AppRoute.SignUp(OnboardingOrigin.Initial) else AppRoute.SignIn(OnboardingOrigin.Initial),
                        if (value == "FirstSignIn") "sign-in-form" else null)
                }
                "RetainedAccount" -> { vm.signOutActiveProfile(false); ScenarioDestination(AppRoute.Welcome()) }
                "AddSignUp" -> ScenarioDestination(AppRoute.SignUp(OnboardingOrigin.AddProfile))
                else -> ScenarioDestination(AppRoute.SignIn(OnboardingOrigin.AddProfile), "sign-in-form")
            }
        },
        scenario<ProfileSetupScenario>("setup", "Accounts", "Profile setup", "Complete the required checks, then Open Chats. Optional profile and follows steps may be skipped. Nothing is saved for later.",
            label = { it.developerLabel }, detail = { setupScenarioDescription(it) }) { vm, value ->
            vm.selectSetupScenario(value)
            check(vm.beginPrivateKeySignIn(OnboardingOrigin.AddProfile, LoginPrototypeData.privateKey))
            vm.accessAttempt!!.let { vm.advanceAccess(it.id, it.phase) }
            ScenarioDestination(AppRoute.ProfileSetup(OnboardingOrigin.AddProfile))
        },
        scenario<AccessScenario>("access", "Accounts", "Sign in and recovery", "The sign-in attempt starts immediately with an example account. Follow Retry or the recovery consent prompt when offered.", label = { it.developerLabel }) { vm, value ->
            vm.selectAccessScenario(value)
            if (value.name.startsWith("Amber")) vm.beginAmberSignIn(OnboardingOrigin.AddProfile)
            else vm.beginPrivateKeySignIn(OnboardingOrigin.AddProfile, LoginPrototypeData.privateKey)
            ScenarioDestination(AppRoute.SignIn(OnboardingOrigin.AddProfile))
        },
        choices("sign-up", "Accounts", "Create an account", "Start on the account creation form. Enter a name and create the account.",
            "Success" to "Create a new account successfully and go to Chats.", "Failure" to "Creation fails. Your form stays available so you can retry.") { vm, value ->
            vm.selectAccessScenario(if (value == "Success") AccessScenario.Success else AccessScenario.SignInFailure)
            ScenarioDestination(AppRoute.SignUp(OnboardingOrigin.AddProfile))
        },
        choices("startup", "Accounts", "Startup recovery", "Inspect loading, failure and recovery into an existing account.",
            "Failure" to "Startup fails. Retry loads the app; Choose Profile uses the available example accounts.", "Ready" to "Startup succeeds and opens Chats.") { vm, value ->
            if (value == "Failure") vm.previewStartupFailure()
            ScenarioDestination(AppRoute.SignedIn)
        },
        scenario<ProfileExitScenario>("sign-out", "Accounts", "Sign out and cleanup", "The Sign Out dialog opens. Choose cleanup options and confirm to exercise their results. Only temporary accounts are affected.", label = { it.developerLabel }) { vm, value ->
            vm.selectProfileExitScenario(value); ScenarioDestination(AppRoute.Settings(), "sign-out")
        },
        scenario<ProfileSaveScenario>("profile-save", "Accounts", "Save profile", "The profile editor opens ready to edit. Change the name and tap Save; inspect the result and retry when offered.", label = { it.developerLabel }) { vm, value ->
            vm.selectProfileSaveScenario(value); ScenarioDestination(AppRoute.EditProfile, "edit-profile")
        },
        choices("profile-image", "Accounts", "Profile image", "The profile editor opens. Choose a new image to test image preparation.",
            "Success" to "The selected image is prepared and can be saved.", "Failure" to "The first selected image fails to prepare. Select it again to recover.") { vm, value ->
            vm.selectProfileImageFailure(value == "Failure"); ScenarioDestination(AppRoute.EditProfile, "edit-profile")
        }.copy(deviceNote = device),
        choices("local-key", "Accounts", "Local key availability", "Open the private key controls to inspect availability and recovery.",
            "Available" to "The example private key can be revealed or exported.", "Unavailable" to "The key is unavailable; Retry restores local access.") { vm, value ->
            vm.setLocalKeyAvailable(value == "Available"); ScenarioDestination(AppRoute.ProfileKeys, "reveal-key")
        },
        scenario<PeopleSearchScenario>("people", "People and groups", "People search", "Search opens with an example query. Inspect complete, partial or unavailable results and Retry.", label = { it.developerLabel }) { vm, value ->
            vm.selectPeopleSearchScenario(value); ScenarioDestination(AppRoute.NewChat, "people-search")
        },
        scenario<GroupContactScenario>("group-contact", "People and groups", "Contact group actions", "Choose the groups for the prepared contact and apply the action. Inspect roster and partial-action feedback.", label = { it.developerLabel }) { vm, value ->
            vm.selectGroupContactScenario(value); ScenarioDestination(AppRoute.PersonProfile("maya-chen"), "group-contact")
        },
        choices("created-chat", "People and groups", "Open a new chat", "Start a chat with the prepared contact.",
            "Success" to "The chat opens after creation.", "Unavailable" to "Creation succeeds but opening fails; retry without creating a duplicate.") { vm, value ->
            vm.setCreatedChatUnavailable(value == "Unavailable"); ScenarioDestination(AppRoute.PersonProfile("maya-chen"))
        },
        scenario<GroupRosterScenario>("group-roster", "People and groups", "Group roster", "The member list loads immediately. Inspect its completeness and refresh when offered.", label = { it.developerLabel }) { vm, value ->
            vm.groupWork.chooseRoster(value); ScenarioDestination(AppRoute.GroupMembers(profile(vm).id, group(vm)))
        },
        scenario<GroupMutationScenario>("group-members", "People and groups", "Update group members", "Choose a contact in Add Members and apply the change. Inspect failure or a roster change before commit.", label = { it.developerLabel }) { vm, value ->
            vm.groupWork.chooseMutation(value); ScenarioDestination(AppRoute.AddGroupMembers(group(vm)))
        },
        scenario<GroupImageScenario>("group-images", "People and groups", "Group images and saving", "The group editor opens. Change its image or name and save to exercise the selected result.", label = { it.developerLabel }) { vm, value ->
            vm.groupWork.chooseImage(value); ScenarioDestination(AppRoute.EditGroup(group(vm)))
        },
        scenario<GroupCreateScenario>("group-create", "People and groups", "Create a group", "Members are already selected. Name the group, choose a disappearing-message timer and tap Create. Retry continues the same creation.", label = { it.developerLabel }) { vm, value ->
            vm.groupWork.chooseCreate(value); ScenarioDestination(AppRoute.GroupSetup(listOf("maya-chen", "nora-bennett")))
        },
        scenario<GroupLifecycleScenario>("group-admin", "People and groups", "Group administration", "Group information opens with an administrator account. Use the matching administration action and inspect recovery.", label = { it.developerLabel }) { vm, value ->
            val chatId = group(vm)
            vm.prepareScenarioProfile { profile -> profile.copy(chats = profile.chats.map { chat ->
                if (chat.id != chatId) chat else when(value) {
                    GroupLifecycleScenario.RecoveryFailure -> chat.copy(groupLifecycle = GroupLifecycle.Unrecoverable)
                    GroupLifecycleScenario.DeleteFailure -> chat.copy(groupLifecycle = GroupLifecycle.Disbanded)
                    GroupLifecycleScenario.AcknowledgeFailure -> chat.copy(disbandCapability = chat.disbandCapability.copy(requestFailed = true))
                    GroupLifecycleScenario.DisbandFailure, GroupLifecycleScenario.ConvergenceFailure -> chat.copy(disbandCapability = chat.disbandCapability.copy(enabled = true, blockers = emptySet()))
                    else -> chat.copy(disbandCapability = chat.disbandCapability.copy(blockers = emptySet()))
                }
            }) }
            vm.groupLifecycle.choose(value); ScenarioDestination(AppRoute.ChatInfo(chatId), "group-admin:${value.name}")
        },
        scenario<GroupStateScenario>("group-state", "People and groups", "Group lifecycle", "The prepared group opens in the selected lifecycle state. Inspect available actions and any blocked actions.", label = { it.developerLabel }) { vm, value ->
            vm.groupLifecycle.chooseState(value); ScenarioDestination(AppRoute.ChatInfo(group(vm)))
        },
        scenario<HistoryScenario>("history", "Messages", "Conversation history", "Conversation search opens. Search for an older message or load earlier history to trigger the selected result.", label = { it.developerLabel }) { vm, value ->
            vm.selectHistoryScenario(value); ScenarioDestination(AppRoute.Conversation(direct(vm), openSearch = true))
        },
        scenario<MessageEditScenario>("message-edit", "Messages", "Edit a message", "An outgoing message opens in the editor. Change its text and save. Inspect the result and retry or discard when offered.", label = { it.developerLabel }) { vm, value ->
            vm.selectMessageEditScenario(value); conversation(vm, "message-edit")
        },
        scenario<MessageDeleteScenario>("message-delete", "Messages", "Delete messages", "The deletion confirmation opens for prepared messages. Choose the scope and confirm. Inspect partial results and retry failed items.", label = { it.developerLabel }) { vm, value ->
            vm.selectMessageDeleteScenario(value); conversation(vm, "message-delete")
        },
        scenario<MessageForwardScenario>("message-forward", "Messages", "Forward messages", "The forwarding picker opens with prepared messages. Choose a destination and send; inspect progress and retry.", label = { it.developerLabel }) { vm, value ->
            vm.selectMessageForwardScenario(value); conversation(vm, "message-forward")
        },
        scenario<TranslationScenario>("translation", "Messages", "Translate a message", "A message opens in translation controls. Choose a language and translate; download or retry when offered.", label = { it.developerLabel }) { vm, value ->
            vm.selectTranslationScenario(value); conversation(vm, "translation")
        },
        scenario<WritingScenario>("writing", "Messages", "Writing tools", "Writing tools open with an example draft. Choose an action, review the result and insert it or recover from the error.", label = { it.developerLabel }) { vm, value ->
            vm.selectWritingScenario(value); vm.updateDraftText(direct(vm), "Can we meet tomorrow to discuss our plans?"); conversation(vm, "writing")
        },
        scenario<ChatBatchScenario>("chat-batch", "Chats and search", "Chat bulk actions", "Chats opens with two conversations selected. Choose a bulk action and inspect per-chat results.", label = { it.developerLabel }) { vm, value ->
            vm.selectChatBatchScenario(value); ScenarioDestination(AppRoute.SignedIn, "chat-batch")
        },
        scenario<ChatConnectionScenario>("chat-connection", "Chats and search", "Chat connection", "Chats opens with the selected connection status. Use Retry where available.", label = { it.developerLabel }) { vm, value ->
            vm.selectChatConnectionScenario(value); ScenarioDestination(AppRoute.SignedIn)
        },
        scenario<GlobalVoiceScenario>("voice-search", "Chats and search", "Voice search", "Search opens ready for the microphone action. Tap the microphone to run the selected result.", device = device, label = { it.developerLabel }) { vm, value ->
            vm.selectGlobalVoiceScenario(value); ScenarioDestination(AppRoute.SignedIn, "voice-search")
        },
        scenario<GlobalLibraryScenario>("library", "Chats and search", "Attachment library", "Search opens on the Files category. Open an example file and inspect loading or availability feedback.", label = { it.developerLabel }) { vm, value ->
            vm.selectGlobalLibraryScenario(value); ScenarioDestination(AppRoute.SignedIn, "library")
        },
        scenario<AttachmentTransferScenario>("attachment-transfer", "Attachments", "Attachment transfers", "A conversation opens with transfer examples. Download an attachment and inspect its progress, retry or terminal state.", label = { it.developerLabel }) { vm, value ->
            vm.addAttachmentReadingExamples(profile(vm).id, direct(vm)); vm.selectAttachmentTransferScenario(value); conversation(vm)
        },
        scenario<AttachmentAccessScenario>("attachment-open", "Attachments", "Open files", "A conversation opens with supported file examples. Tap a file to inspect the selected opening outcome.", device = device, label = { it.developerLabel }) { vm, value ->
            vm.addAttachmentReadingExamples(profile(vm).id, direct(vm)); vm.selectAttachmentAccessScenario(value); conversation(vm)
        },
        scenario<PhotoEditorScenario>("photo-editor", "Attachments", "Edit a photo", "The photo editor opens with a prepared image. Make a change and save; inspect source or save failure.", label = { it.developerLabel }) { vm, value ->
            vm.selectPhotoEditorScenario(value)
            vm.addDraftAttachments(direct(vm), listOf(MessageAttachment("scenario-photo", MessageAttachmentKind.Photo, "Trail photo", images = listOf(ProfileAvatar.Asset(AvatarAsset.GardenClub)))))
            vm.openPhotoEditor(profile(vm).id, direct(vm), "scenario-photo", 0); conversation(vm)
        },
        scenario<LocationScenario>("location", "Attachments", "Share a location", "Location controls open immediately. Inspect locating, permission and availability states, then send or retry.", label = { it.developerLabel }) { vm, value ->
            vm.selectLocationScenario(value); vm.openLocation(profile(vm).id, direct(vm)); conversation(vm)
        },
        scenario<DownloadNetworkExample>("download-network", "Attachments", "Automatic download network", "Data Usage opens with a prepared download queue and the selected network. Change download rules or pause downloads to inspect eligibility.", label = { it.developerLabel }) { vm, value ->
            vm.chooseDownloadNetwork(value); vm.loadDownloadQueueExample(); ScenarioDestination(AppRoute.DataUsage)
        },
        choices("download-queue", "Attachments", "Download queue progress", "Data Usage opens with a prepared queue.",
            "Running" to "Transfers progress normally.", "Held" to "Transfer progress is held so queued and active items remain inspectable.", "Paused" to "Automatic downloads are paused. Resume them from Data Usage.") { vm, value ->
            vm.holdDownloadTransfers(value == "Held"); vm.loadDownloadQueueExample()
            if (value == "Paused") vm.pauseAutomaticDownloads(profile(vm).id, true)
            ScenarioDestination(AppRoute.DataUsage)
        },
        scenario<DictationScenario>("dictation", "Voice", "Composer dictation", "Dictation opens for the prepared chat. Start dictation, accept disclosure if shown, then insert the result or retry.", device = device, label = { it.developerLabel }) { vm, value ->
            vm.composerCapture.chooseScenario(value); conversation(vm, "dictation")
        },
        scenario<VoiceCaptureScenario>("voice-recording", "Voice", "Voice message recording", "The chat composer opens ready to record. Hold or tap the microphone, then finish recording to exercise start and finalization results.", device = device, label = { it.developerLabel }) { vm, value ->
            vm.composerCapture.chooseVoiceScenario(value); conversation(vm)
        },
        scenario<SpeechCatalogScenario>("speech-catalog", "Voice", "Read Aloud engines and voices", "Read Aloud settings opens with the selected engine catalog. Choose an engine or voice and inspect discovery and selection feedback.", device = device, label = { it.developerLabel }) { _, value ->
            ScenarioDestination(AppRoute.ReadAloud, "speech-catalog:${value.name}")
        },
        scenario<SpeechEdgeScenario>("speech-history", "Voice", "Read Aloud history", "The message reader opens. Start Read Aloud and move through earlier or later messages to exercise history loading.") { vm, value -> conversation(vm, "speech-history:${value.name}") },
        choices("speech-audio", "Voice", "Read Aloud audio environment", "The message reader opens. Start Read Aloud to inspect audio policy.",
            "Device" to "Use the device audio environment.", "OtherMedia" to "Other media is playing; the configured interruption policy applies.", "Quiet" to "No other media is playing.", "FocusDenied" to "Audio focus is unavailable; speech cannot start.") { vm, value -> conversation(vm, "speech-audio:$value") }.copy(deviceNote = device),
        choices("speech-background", "Voice", "Read Aloud background controls", "A local background-control example opens. Inspect notification, lock timer and stale-command handling without speaking or starting a service.",
            "NotificationStarts" to "The example notification starts successfully.", "NotificationFails" to "Notification startup fails and playback ends.", "Background" to "Background without locking.", "LockAfterMinute" to "Background and advance one minute; locking stops playback.", "LockImmediately" to "Background and lock immediately.", "StaleCommand" to "A previous session's Stop command cannot stop the new session.", "ProfileExits" to "Removing the example profile ends playback.") { _, value -> ScenarioDestination(AppRoute.ScenarioExample("speech-background", value)) },
        scenario<SpeechControlAction>("speech-command", "Voice", "Read Aloud notification commands", "A local background-control example applies the selected command. Inspect playback and source-return state.") { _, value -> ScenarioDestination(AppRoute.ScenarioExample("speech-command", value.name)) },
        scenario<IncomingExample>("incoming-example", "Incoming and notifications", "Incoming content and links", "The incoming request opens immediately. Select its account or destination when required and inspect the resulting chat or error.", label = { it.label }) { vm, value -> receive(vm, value); ScenarioDestination(AppRoute.SignedIn) },
        scenario<IncomingScenario>("incoming-outcome", "Incoming and notifications", "Incoming request outcomes", "A matching share, message notification or invitation is prepared and opened. Follow the chooser or recovery action.", label = { it.developerLabel }) { vm, value ->
            vm.incoming.choose(value)
            receive(vm, when { value.name.startsWith("Invite") -> IncomingExample.NotificationInvite; value == IncomingScenario.NotificationLoadFailure -> IncomingExample.NotificationMessage; else -> IncomingExample.Text })
            ScenarioDestination(AppRoute.SignedIn)
        },
        choices("incoming-lock", "Incoming and notifications", "Deferred incoming requests", "A shared text request is submitted with the selected lock state.",
            "Locked" to "The request stays queued. Use Unlock in the scenario control to continue.", "Unlocked" to "The destination chooser opens immediately.") { vm, value ->
            vm.incoming.chooseLock(value == "Locked"); receive(vm, IncomingExample.Text); ScenarioDestination(AppRoute.SignedIn)
        },
        scenario<NotificationScenario>("notification-settings", "Incoming and notifications", "Notification settings outcomes", "Notifications opens. Change delivery or notification settings and inspect saving, permission and service feedback.", label = { it.developerLabel }) { vm, value ->
            vm.notificationControls.choose(value); ScenarioDestination(AppRoute.Notifications)
        },
        scenario<PushAvailability>("push", "Incoming and notifications", "Push availability", "Notifications opens with the selected push capability. Inspect available delivery choices.") { vm, value ->
            vm.notificationControls.chooseEnvironment(vm.notificationControls.environment.copy(push = value)); ScenarioDestination(AppRoute.Notifications)
        },
        scenario<AndroidVibrationOverride>("vibration", "Incoming and notifications", "Android vibration override", "Conversation notification settings opens. Inspect the effective pattern and preview vibration.", device = device) { vm, value ->
            vm.notificationControls.chooseEnvironment(vm.notificationControls.environment.copy(vibrationOverride = value)); ScenarioDestination(AppRoute.ConversationNotifications(direct(vm)))
        },
        choices("vibration-preview", "Incoming and notifications", "Vibration preview availability", "Conversation notification settings opens. Choose a vibration pattern and preview it.",
            "Available" to "The pattern preview plays.", "Unavailable" to "The preview reports that vibration is unavailable.") { vm, value ->
            vm.notificationControls.chooseEnvironment(vm.notificationControls.environment.copy(previewAvailable = value == "Available")); ScenarioDestination(AppRoute.ConversationNotifications(direct(vm)))
        },
        scenario<NotificationActionScenario>("notification-action", "Incoming and notifications", "Notification action outcomes", "An example inline reply starts immediately. Inspect completion, bounded retries and cleanup feedback.", label = { it.label }) { vm, value ->
            vm.notificationActions.choose(value); notification(vm, NotificationActionKind.Reply); ScenarioDestination(AppRoute.SignedIn)
        },
        scenario<NotificationActionKind>("notification-kind", "Incoming and notifications", "Notification actions", "Apply the selected action to an example notification immediately. Open its chat to inspect the result.") { vm, value -> notification(vm, value); ScenarioDestination(AppRoute.Conversation(direct(vm))) },
        choices("background-connection", "Incoming and notifications", "Background connection", "Notification settings opens to the background delivery controls.",
            "Stopped" to "The background connection has stopped. Enable it again to recover.", "Available" to "Background delivery can be enabled.") { vm, value ->
            if (value == "Stopped") {
                vm.notificationControls.request(NotificationChange.Delivery(NotificationDelivery.Background, true))?.let { vm.notificationControls.advance(it, 0) }
                vm.notificationControls.stopBackground()
            }
            ScenarioDestination(AppRoute.Notifications)
        },
        choices("notification-updates", "Incoming and notifications", "Update notification availability", "Notifications opens with update notifications available or unavailable for this distribution.",
            "Available" to "Update notification preferences are available.", "Unavailable" to "Update notifications are unavailable in this distribution.") { vm, value ->
            vm.notificationControls.chooseEnvironment(vm.notificationControls.environment.copy(updatesAvailable = value == "Available")); ScenarioDestination(AppRoute.Notifications)
        },
        scenario<RetentionScenario>("retention", "Privacy and retention", "Disappearing-message changes", "Chat information opens. Change the disappearing-message timer and inspect confirmation, update and refresh results.", label = { it.developerLabel }) { vm, value ->
            vm.retention.choose(value); ScenarioDestination(AppRoute.ChatInfo(direct(vm)), "retention")
        },
        scenario<RetentionExample>("expiry", "Privacy and retention", "Message expiry", "A conversation opens with the selected expiry example. Advance Time in the scenario control to inspect expiration.", label = { it.developerLabel }) { vm, value ->
            vm.retention.chooseExample(value); conversation(vm)
        },
        scenario<TranscriptScenario>("transcript", "Privacy and retention", "Export conversation", "Transcript controls open for a prepared chat. Choose format and save or share; inspect preparation and export feedback.", device = device, label = { it.developerLabel }) { vm, value ->
            vm.transcript.choose(value); vm.transcript.begin(GroupOwner(profile(vm).id, direct(vm))); ScenarioDestination(AppRoute.ChatInfo(direct(vm)))
        },
        scenario<AppUnlockOutcome>("app-lock", "Privacy and retention", "Unlock the app", "The temporary account is locked. The selected authentication result runs once; use Unlock again to recover.", label = { it.label }) { vm, value ->
            vm.updateProfileSettings(profile(vm).settings.copy(requireDeviceAuthentication = true))
            vm.appLock.credentials(true); vm.appLock.sync(); vm.appLock.choose(value); vm.appLock.lockNow(); ScenarioDestination(AppRoute.SignedIn)
        },
        scenario<AuditLogScenario>("audit", "Diagnostics", "Audit log operations", "Audit Logs opens. Change recording, export or delete files to exercise the matching operation. Only example audit records are affected.", device = device, label = { it.label }) { vm, value ->
            vm.auditLogs.choose(value); ScenarioDestination(AppRoute.AuditLogs)
        },
        scenario<DeveloperOutcome>("inspection", "Diagnostics", "Diagnostics operations", "Diagnostics opens and refreshes health. Inspect the operation result and retry.") { vm, value ->
            vm.developerParity.chooseOutcome(value); ScenarioDestination(AppRoute.Diagnostics())
        },
        scenario<PackageInventoryExample>("packages", "Diagnostics", "Key package inventory", "Key Packages opens with the chosen local and relay inventory. Inspect details, publish or delete example packages.") { vm, value ->
            vm.developerParity.inventoryExample(value); ScenarioDestination(AppRoute.KeyPackages)
        },
        choices("conversation-examples", "Diagnostics", "Conversation examples", "A prepared conversation opens with the selected content already inserted.",
            "Arrival" to "A new incoming message is appended.", "Streaming" to "A streaming arrival is appended with streaming inspection enabled.", "Reading" to "A long message and reading examples are appended.", "Attachments" to "Supported attachment reading examples are appended.", "Agents" to "Agent operation examples are appended.", "NostrEvents" to "Nostr event reference examples are appended.") { vm, value ->
            val p = profile(vm).id; val c = direct(vm)
            when(value) { "Arrival" -> vm.addConversationArrival(p,c); "Streaming" -> { vm.developerParity.streaming(true); vm.addConversationArrival(p,c,true) }; "Reading" -> vm.addMessageReadingExample(p,c); "Attachments" -> vm.addAttachmentReadingExamples(p,c); "Agents" -> vm.addAgentConversationExamples(p,c); "NostrEvents" -> vm.addNostrEventExamples(p,c) }
            conversation(vm)
        },
        scenario<RelayPublicationScenario>("relay-publication", "Relays and updates", "Relay publication", "Relays opens. Change a relay role or publish the configuration and inspect the selected publication outcome.", label = { it.developerLabel }) { vm, value ->
            vm.relayPublication.chooseScenario(value); ScenarioDestination(AppRoute.ProfileRelays)
        },
        choices("relay-import", "Relays and updates", "Imported relay roles", "Relays opens with the imported role example already loaded.", "Imported" to "Inspect the invalid imported relay address, its retained roles and the repair action.") { vm, _ -> vm.loadRelayImportExample(); ScenarioDestination(AppRoute.ProfileRelays) },
        scenario<AppUpdateDistribution>("distribution", "Relays and updates", "Update distribution", "Settings opens with the selected update distribution. Inspect whether in-app update controls are available.", label = { it.developerLabel }) { vm, value ->
            vm.appUpdates.selectDistribution(value); ScenarioDestination(AppRoute.Settings())
        },
        scenario<AppUpdateCheckScenario>("update-check", "Relays and updates", "Check for updates", "Settings opens with the selected release check result. Check again or open the update when available.", label = { it.developerLabel }) { vm, value ->
            vm.appUpdates.previewCheck(value); ScenarioDestination(AppRoute.Settings())
        },
        scenario<AppSelfUpdateScenario>("self-update", "Relays and updates", "Install an update", "Settings opens with an available release. Tap Update to inspect resolution, download, verification and handoff states.", label = { it.developerLabel }) { vm, value ->
            vm.appUpdates.selectSelfUpdateScenario(value); ScenarioDestination(AppRoute.Settings())
        },
    ) + DeveloperOperation.entries.map { operation ->
        scenario<DeveloperOutcome>("operation-${operation.name}", "Diagnostics", operation.name.scenarioWords(),
            "The prepared inspection operation opens immediately. Inspect its result, confirm deletion when requested, or Retry after a failure.") { vm, value ->
            vm.developerParity.chooseOutcome(value)
            vm.setDebugMode(true)
            ScenarioDestination(when (operation) {
                DeveloperOperation.RefreshHealth, DeveloperOperation.SendToSelf -> AppRoute.Diagnostics()
                DeveloperOperation.RefreshPush -> AppRoute.ConversationDebug(direct(vm))
                else -> AppRoute.KeyPackages
            }, "inspection:${operation.name}")
        }
    } + listOf(
        choices("performance", "Diagnostics", "Performance inspection", "Conversation diagnostics opens with the performance timer prepared.",
            "Active" to "Performance inspection is active for its bounded interval.", "Inactive" to "Performance inspection is off; enable it from this screen.") { vm, value ->
            vm.setDebugMode(true); vm.developerParity.performance(value == "Active"); ScenarioDestination(AppRoute.ConversationDebug(direct(vm)))
        },
        choices("streaming", "Diagnostics", "Streaming inspection", "Conversation diagnostics opens with streaming inspection enabled or disabled.",
            "Enabled" to "Streaming details are available.", "Disabled" to "Streaming inspection is off.") { vm, value ->
            vm.setDebugMode(true); vm.developerParity.streaming(value == "Enabled"); ScenarioDestination(AppRoute.ConversationDebug(direct(vm)))
        }
    )
    fun find(id: String) = all.firstOrNull { it.id == id }
}
