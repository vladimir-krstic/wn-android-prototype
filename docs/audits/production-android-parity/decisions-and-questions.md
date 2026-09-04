# Decisions and questions

These questions affect only the linked capability slices. The recommendation is first. Independent ready batches may proceed while they are open.

<a id="q01"></a>

### Q01 — Color customization versus monochrome identity

- **Conflict:** C102 exposes production full-spectrum accent and per-side/global/per-chat bubble colors; the approved prototype identity is monochrome.
- **Recommendation:** Preserve monochrome defaults and app chrome. If full capability is required, allow optional bubble/content customization and a restrained semantic action accent without changing neutral surfaces; verify contrast per chosen color.
- **Resolved in B26:** The user selected the recommendation. Every theme/profile
  can optionally set the primary action and global sent/received bubble colors;
  each chat can override both bubble sides. Defaults and neutral surfaces remain
  monochrome, text uses the verified contrast policy and Reset removes the
  relevant override.

<a id="q02"></a>

### Q02 — Screenshot blocking as a separate privacy control

- **Conflict:** C097 production labels one secure-window control as blocking screenshots and hiding Recents; the prototype deliberately promises only Recents snapshot privacy.
- **Recommendation:** Keep **Hide Screen in Recents** and add **Block screenshots in chats** as a separate setting with an exact scope and non-color state.
- **Resolved in B24:** The user selected the recommendation. **Hide Screen in Recents** secures the paused task preview, while **Block screenshots in chats** secures conversation and chat-detail routes during use. App-lock protection remains independent.

<a id="q03"></a>

### Q03 — Retroactive disappearing-message deletion

- **Resolved in B20:** The authorized production parity scope and source distinguish two operations. Enabling/shortening prunes older plaintext after destructive confirmation; Off/longer does not. Ordinary expiry uses each row's saved deadline/duration, with first-read deferral for received messages and send-time anchoring for outgoing messages. Unpinned history never borrows the current timer; group events remain.
- **Copy and behavior:** Explain timer-change pruning separately from message countdowns. Preserve accepted policy if history refresh fails; retry refresh only. Later settings do not rewrite old deadlines or restore removed content.
- **Evidence:** [B20 brief](../../screens/disappearing-timers-and-expiry.md#implementation-evidence) and WN-ANDROID-0140 record production seams, exact copy and host verification. No remaining B20 decision block.

<a id="q04"></a>

### Q04 — Relay and key-package information architecture

- **Conflict:** C080, C108 and C110 expose technical group/relay inspection, managed posting/inbox relay lists and Key Packages at Settings root; the prototype places key packages in Developer Tools and supports broad secure URLs with Profile/Inbox/Chat Messages roles.
- **Recommendation:** Preserve the prototype relay capabilities and developer-only protocol language. Add production publication/readiness states. Keep Key Packages in Developer Tools unless ordinary users have a recovery task that requires the raw package list.
- **Blocks:** moving Key Packages to the consumer Settings root and narrowing relay editing; state coverage can proceed.

<a id="q05"></a>

### Q05 — Sign-out default and cleanup choices

Resolved for B02 on 2026-09-04 from the already-approved prototype direction
in `docs/terminology.md` and `docs/screens/developer-and-destructive-flows.md`:
retain the checked wipe default and exact-name confirmation; add the separate
cleanup choice and independent results. This changes neither the approved
default nor the scope of real external operations.

- **Conflict:** C009–C010 production distinguishes non-wiping sign-out, optional relay key-package cleanup and staged wipe; the accepted prototype sign-out flow defaults to wiping local data.
- **Recommendation:** Keep both outcomes and the prototype's consequence language, but make the safe default an explicit product choice in the selected brief. Never conflate failed relay cleanup with local sign-out failure.
- **Blocks:** none; the existing approved default governs B02.

<a id="q06"></a>

### Q06 — Real Android platform capabilities in the prototype

- **Scope:** C049, C058, C059, C066, C070, C084, C086, C090, C094, C095 and C121 touch media permissions, location/maps, background services, shortcuts/share targets, notification delivery/actions, app authentication, or APK installation.
- **Recommendation:** First implement deterministic app-owned states and launch contracts where already approved. Keep networking/services/installers off. Approve each real permission or service only with its bounded implementation batch; prefer permissionless system surfaces.
- **Blocks:** device-executed capability, not the audit specs or deterministic UI/state implementations. B11 implements C049 with bundled access states and a standard Photo Picker; real library permission/MediaStore access remains outside the prototype scope. B14 implements C058 through coordinate entry, one-shot local outcomes and explicit permissionless external Maps handoff; GPS, map tiles/geocoding, new permissions and device execution remain outside scope. B16 implements C066 as a developer-only lifecycle/notification-controls example; actual native speech stops in the background. B17 implements C070 with exact selected-service and permission/failure outcomes, disclosure and retained review; it starts no microphone, recognizer or provider Activity. Real speech services, notification delivery and hardware validation remain outside scope. B32 implements C121 as app-wide deterministic checking, resolution, download, verification, ready, permission and installer-handoff states; it performs no release lookup, network/file/digest, package-install permission, notification or installer operation.

<a id="q07"></a>

### Q07 — Media save destination

- **Conflict:** C053 production often writes to gallery/media storage, while the prototype uses Android document creation for explicit placement.
- **Recommendation:** Preserve the prototype's system-owned save flow until the selected media brief establishes that gallery placement is part of the outcome. Keep Save and Share available in the viewer either way.
- **B11 choice (2026-09-04):** retain explicit Android document destinations for Save, including per-item multi-attachment outcomes. This preserves the approved prototype integration without new storage permissions. See `docs/screens/composer-attachment-actions.md`. No batch remains blocked by Q07.

<a id="q08"></a>

### Q08 — Forensic audit logs versus sanitized diagnostics

- **Conflict:** C098 production audit files may contain identities, messages and device details; prototype diagnostic export promises sanitization.
- **Recommendation:** Treat them as two features. Keep sanitized diagnostics and add a developer-only forensic log control with explicit recording state, sensitive-export consent and Delete.
- **Blocks:** ordinary-settings placement in B24, not deterministic sensitive-log states.

<a id="q09"></a>

### Q09 — Font assets, defaults and localization scope

- **Conflict:** C101/C104 production offers multiple bundled families and additional languages; the prototype has an approved typography system and different language list.
- **Recommendation:** Keep the existing typeface/scale and language default. Add optional choices only after verifying font licensing/resources, and preserve every current language while adding production languages with real translated resources in separate bounded work.
- **Resolved in B26:** The user selected the recommendation and complete locale
  coverage. System remains the approved font and device-locale default. Pinned
  OFL metadata, full family licenses and reproducible API-23-compatible static
  instances are documented in [font assets](../../references/font-assets.md).
  Every existing language remains, and Russian, Turkish, Simplified Chinese and
  Traditional Chinese each provide all 1,762 translatable resources with
  verified format-token and plural parity.

## Nonblocking evidence uncertainties

- Live Marmot/SQLite, relay, push and crypto results were not exercised. The selected implementation agent must use fixed fixtures for their visible states and preserve cited production integration seams.
- Production test and screenshot files were inventoried but not run. A capability's source status is not a runtime or visual acceptance claim.
- Production includes `Coming soon` fallbacks when an attachment callback is absent. The matrix classifies only wired Gallery/Camera/Document/Location/User/Contact entry paths; it does not count an unavailable placeholder as a separate feature.
