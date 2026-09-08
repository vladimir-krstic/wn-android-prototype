package dev.ipf.whitenoise.scenarios

import dev.ipf.whitenoise.model.ProfileSetupScenario

internal fun setupScenarioDescription(scenario: ProfileSetupScenario): String = when (scenario) {
        ProfileSetupScenario.Ready -> "Checks pass. Review the one-device notice and tap Continue. Secure messaging finishes, then Open Chats becomes available."
        ProfileSetupScenario.Attention -> "Edit your profile or skip it. People you follow is skipped, and relay settings need attention. Choose default relays or find existing settings, then finish the device check."
        ProfileSetupScenario.Recovery -> "Profile lookup fails; retry offers editing, where the first save also fails. Relay discovery is inconclusive, inbox settings need defaults, another installation may exist, and secure messaging needs a retry."
        ProfileSetupScenario.ProfileSaveFailure -> "Open Edit Profile and tap Save. The first save fails and keeps your draft. Tap Save again to succeed, or go back and skip the optional profile step."
        ProfileSetupScenario.ProfileLookupFailure -> "Your profile cannot load. Try Again opens the optional profile-editing choice; Not Now skips it. The remaining checks follow the ready-profile path."
        ProfileSetupScenario.FollowsSkipped -> "People you follow is marked Skipped without publishing a replacement list. This optional result does not prevent you from opening Chats."
        ProfileSetupScenario.MissingRelays -> "The relay check needs attention. Explicitly choose Use Default Relays or enter a relay and find existing settings. This required check must pass before Chats opens."
        ProfileSetupScenario.InconclusiveRelays -> "Relay lookup cannot finish. Defaults are unavailable while the result is uncertain. Try Again or enter another relay and tap Find My Settings to recover."
        ProfileSetupScenario.UnknownDevice -> "The app cannot tell whether another installation exists. Review the consequences and tap Continue Anyway to acknowledge them and continue."
        ProfileSetupScenario.PartialRelayFailure -> "One relay endpoint fails, but another configured endpoint satisfies the requirement. Relay checks still pass; a single endpoint failure does not block setup."
        ProfileSetupScenario.MessagingFailure -> "After the device acknowledgment, secure messaging fails once. Open that row and tap Try Again. Setup completes without repeating the acknowledgment."
    }

