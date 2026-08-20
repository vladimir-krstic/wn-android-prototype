# Decisions

This file records only material decisions that continue to govern the prototype.

## WN-PROTOTYPE-0001 — Minimal clean restart

- Date: 2026-07-23
- Status: Approved

The repository is a fast native iPhone prototype workspace. It started with a clean development placeholder and builds one user-agreed screen or tightly related flow at a time. WN-PROTOTYPE-0002 supersedes the original per-batch build and inspection cadence below.

- Native Apple components and APIs are the default.
- Each selected screen gets one concise brief; future screens are not predesigned.
- Architecture, state, fixtures, permissions, assets, and tests are introduced only when current work needs them.
- Every batch is built, previewed, launched, and inspected directly by the user.
- The user alone accepts product and visual results.
- Claude is never invoked automatically; a neutral prompt is supplied only when the user requests a major review.
- Existing Git history is preserved. The pre-reset working tree was archived outside the repository before cleanup.

## WN-PROTOTYPE-0002 — Fast iteration and inspection workflow

- Date: 2026-07-30
- Status: Approved

The prototype now contains implemented product flows, but the clean-restart
working style remains authoritative.

- Work on one user-agreed screen or bounded flow at a time.
- Screen briefs record durable accepted decisions rather than every iteration.
- Small changes use static validation; build once at the end of a meaningful
  batch.
- Device Hub inspection occurs only when requested or explicitly approved.
- The iPhone Air running iOS 27 is the single default inspection device. Reuse
  it rather than booting another simulator.
- Never claim visual verification without inspecting the current build.
- The user alone accepts visual and product results.
- Claude and Fable are manual major-batch reviewers only.

## WN-PROTOTYPE-0003 — Consolidate only proven duplication

- Date: 2026-07-30
- Status: Approved

- Keep the empty unit-test target and its tracked `.gitkeep` as a normal Xcode
  project placeholder; add a test only when meaningful nonvisual logic
  justifies one.
- Keep mutable chat state with its owning profile so list actions and profile
  unread badges share one source of truth.
- Use one shared profile-avatar renderer, one QR-image generator, and one
  physical VisionKit scanner after repeated implementations proved those
  abstractions necessary.
- Bundle profile imagery at a maximum 512-pixel dimension. Preserve the
  scanner backdrop dimensions while using efficient photographic compression.
- Raw public-key QR values and key-shaped export fixtures must not expose
  prototype terminology through shareable product surfaces.

## WN-PROTOTYPE-0004 — Self-contained project knowledge

- Date: 2026-07-31
- Status: Approved

- All White Noise product knowledge, terminology, screen requirements,
  decisions, and workflow rules required for normal work live in this
  repository.
- Do not read, invoke, or depend on project-specific files, source code,
  instructions, or skills from another local project unless the user explicitly
  requests a bounded cross-project investigation.
- Current official Apple design and developer documentation is an intentional
  live authority. Use `docs/references/apple.md` to open the relevant source
  before a material platform decision and apply
  `docs/references/native-ui.md` when evaluating the result.
- The user’s latest direction outranks Apple’s default pattern. Record an
  approved exception in the selected screen brief.
- User-supplied Figma links, GitHub issues, shipped-app examples, and asset
  sources may remain as optional provenance or comparison evidence, but no
  implementation may require them.

## WN-PROTOTYPE-0005 — Per-profile relay roles

- Date: 2026-08-03
- Status: Approved

- Each profile owns an independent union list of configured relay endpoints.
- Ordinary Settings shows that single list. Relay Details assigns any capable
  relay to one or more human-facing roles: **Profile**, **Inbox**, and **Chat
  Messages**. The main list summarizes those assignments with semantic role
  symbols so a separate Advanced destination is unnecessary.
- **Profile** represents the account relay behavior used to publish profile and
  connection information. Key-package publication uses the appropriate
  profile-relay behavior internally and is not a fourth product setting.
- **Inbox** receives invitations to new chats and groups.
- **Chat Messages** is the per-profile default relay set copied into chats that
  profile creates. Existing chats retain their signed routing, and incoming
  chats carry their own routing. Per-chat editing is a separate future flow.
- At least one capable relay remains assigned to every role. Read-only relays
  can remain configured but cannot be assigned to these write-requiring roles.
- A new relay’s roles are chosen before it becomes active and default to all
  three. Initial onboarding relay selection remains a future requirement.
- The production transport layer currently accepts default group-routing
  relays at process construction. Production translation therefore requires a
  per-profile default-routing input or public setter; this local decision is
  complete and does not require access to another repository.

## WN-PROTOTYPE-0006 — Relay Details owns removal

- Date: 2026-08-03
- Status: Approved

- The main Relays list is a quiet two-line endpoint overview. It has no Edit
  mode, inline role controls, or swipe-to-delete behavior.
- Every relay row opens Relay Details. That destination owns the three role
  toggles and a destructive **Remove Relay** action at the bottom.
- Removal always requires the native consequence-aware confirmation. A normal
  removal says that this profile will stop using the relay; a removal that
  empties roles also names the capabilities that become unavailable.
- **Add Relay** remains directly available from the main list.

## WN-PROTOTYPE-0007 — Relay degraded mode and recovery

- Date: 2026-08-03
- Status: Approved; supersedes the minimum-role invariant in
  WN-PROTOTYPE-0005

- A person may intentionally leave **Profile**, **Inbox**, or **Chat Messages**
  without a relay. Relay choices belong to the active profile; leaving a role
  empty never assigns that profile to an unwanted endpoint.
- Assignment alone does not make a role operational. A role is **Available**
  only while at least one assigned read/write relay is connected. Otherwise it
  is **Reconnecting**, **Disconnected**, or **Unassigned**. Read-only relays
  never satisfy availability.
- Every unavailable state degrades only the role's dependent capability:
  Profile editing is unavailable without an available Profile relay,
  invitations cannot arrive without an available Inbox relay, and new chats
  cannot be created without an available Chat Messages relay. A connected
  sibling relay keeps the role available when another assigned relay fails.
  Existing chats keep their own routing and history remains available; while
  any profile relay role is unavailable, the empty Fiatjaf composer
  prioritizes the direct recovery route.
- Turning off the final relay for a role and removing a relay that empties a
  role are allowed after a native consequence-aware confirmation.
- Relays owns the complete recovery explanation through its inline status
  callout. While setup needs attention, Chats replaces its New Message toolbar
  symbol with a compact outlined orange warning, and an open conversation uses
  its complete empty composer as the same recovery link. Both push the active
  profile's Relays destination; the native Back action returns to the
  originating screen. Other signed-in destinations don't repeat the warning.
- Relay connection feedback uses green for Connected, a neutral spinner for
  Reconnecting, and red for Disconnected. The user approved red as the clearer
  concrete endpoint-failure status even though the condition is recoverable.
  Aggregate recovery links and callouts remain orange so they read as guidance
  rather than destructive actions or field validation. Symbols, text, and
  accessibility values carry the meaning without relying on color.
- **Restore Default Relays** performs a confirmed full reset for the active
  profile. It removes custom endpoints and restores the original seven-relay
  list and role assignments.

## WN-PROTOTYPE-0008 — App Security

- Date: 2026-08-04
- Status: Approved

- Privacy & Security uses separate native Form sections for **Hide Screen in
  App Switcher** and **Require Face ID**, allowing each preference to carry its
  own concise explanation. **App Security** is their shared visible heading.
- Require Face ID uses device-owner authentication: Face ID first with the
  iPhone passcode as the system fallback. White Noise does not add a separate
  PIN. If no iPhone passcode is configured, Require Face ID is unavailable
  until that device requirement is resolved.
- Hide Screen in App Switcher covers the UIKit app snapshot. It does not claim
  to prevent screenshots or active screen recording.
- The prototype models these states deterministically and does not use
  LocalAuthentication or real snapshot replacement.
- Anonymous Telemetry and Audit Logging are developer controls and live in
  Developer Tools rather than Privacy & Security.

## WN-PROTOTYPE-0009 — Profile exit and local data removal

- Date: 2026-08-04
- Status: Approved

- A profile’s stored local data and its active signed-in session are separate.
  **Sign Out** ends the session but preserves the profile, chats, and settings
  on this device for a later sign-in.
- Settings exposes one neutral **Sign Out** action. It opens a native Form
  sheet whose **Wipe Data From This Device** Toggle is off by default. The
  sheet is the complete confirmation task; it doesn't stack a second alert or
  wipe sheet.
- With wiping off, the primary **Sign Out** action keeps local data. With
  wiping on, the same action becomes destructive and requires exact
  profile-name confirmation. Previous chats don't return after a later sign-in
  on this or another device.
- **Wipe All Profiles** uses a pushed review list followed by a native Form
  sheet requiring a generated three-word lowercase confirmation phrase. It
  removes every locally stored profile and returns to Welcome. Its neutral
  navigation row lives in Privacy & Security under **Device Data**, not in the
  Sign Out flow.
- Sign Out and Wipe All entry rows remain neutral. The Sign Out sheet uses the
  primary confirmation treatment whether local data is retained or wiped; the
  selected Toggle, exact-name gate, consequence copy, and accessibility label
  communicate the wipe. The final Wipe All confirmation remains red.
- Typed profile and all-profile wipe confirmations remain disabled until their
  required text matches.
- After a single-profile exit, the app presents the profile switcher as the root
  view when another signed-in profile remains and Welcome when none remain.
  Selecting a profile from either switcher opens Settings for that profile.
- The prototype models these outcomes deterministically in memory and performs
  no real authentication, persistence, cryptography, or remote account
  deletion.

## WN-PROTOTYPE-0010 — Unified single-profile and all-profile sign out

- Date: 2026-08-05
- Status: Approved; supersedes WN-PROTOTYPE-0009 only for the placement and
  presentation of device-wide sign out and wipe

- Settings keeps session-management actions together in one native section.
  **Sign Out** affects the active profile. **Sign Out All Profiles** appears as
  a separate sibling row only when multiple profiles are signed in.
- The two scopes are separate before presentation; no scope picker is embedded
  inside a destructive task.
- Sign Out All Profiles uses the same native Form-sheet model as single-profile
  Sign Out. **Wipe Data From This Device** starts off, so signing out all
  profiles preserves their local data and returns to Welcome.
- Enabling the wipe choice requires the deterministic three-word lowercase
  confirmation phrase. The final action uses the destructive role, removes
  every stored profile and its local data, and returns to Welcome.
- The previous **Wipe All Profiles** destination is removed from Privacy &
  Security. Sign Out is the single owner of both session scopes and their
  optional local-data removal.
- Wipe-enabled completion actions use the destructive role. Data-preserving
  sign-out actions use the primary role; all entry rows remain neutral.

## WN-PROTOTYPE-0011 — Separate profile sign out from app data erasure

- Date: 2026-08-05
- Status: Approved; supersedes WN-PROTOTYPE-0010 and the device-wide
  placement and presentation decisions in WN-PROTOTYPE-0009

- Settings exposes only **Sign Out** for the active profile. Its native Form
  sheet retains the optional **Wipe Data From This Device** choice and the
  existing current-profile routing behavior.
- Device-wide sign-out without erasure is unsupported. The app does not offer
  **Sign Out All Profiles**.
- Privacy & Security owns the uncommon, irreversible **Erase All App Data**
  action under **Device Data**. Its footer states that every profile is signed
  out and all local White Noise data is permanently removed from this iPhone.
- The action opens a native Form sheet with concise consequences, the stable
  three-word lowercase confirmation phrase, a **Confirmation phrase** field,
  **Cancel**, and a destructive **Erase Data** action. The destructive action
  remains disabled until the phrase matches exactly.
- The erasure flow does not show a profile list or a keep-data choice. Success
  removes every stored and signed-in profile, all profile-owned chats, media,
  drafts, keys, settings, and transient app state, then returns to Welcome.
- The entry and final action use the destructive role. **Erase Data** is not
  presented as a black primary action.

## WN-PROTOTYPE-0012 — Concise app data erasure warning

- Date: 2026-08-05
- Status: Approved; supersedes only the product label and consequence
  presentation in WN-PROTOTYPE-0011

- Privacy & Security and the confirmation sheet use **Erase App Data**. The
  consequence copy still states explicitly that every profile and all local
  White Noise data are removed.
- The confirmation sheet presents the irreversible consequence as a native
  warning callout with a semantic orange warning symbol, a primary **This
  can’t be undone** title, and secondary detail. The final **Erase Data** action
  remains destructive red and phrase-gated.

## WN-PROTOTYPE-0013 — Prominent app data erasure action

- Date: 2026-08-05
- Status: Approved; supersedes only the final action label and treatment in
  WN-PROTOTYPE-0011 and WN-PROTOTYPE-0012

- The confirmation sheet's final action is **Erase**.
- It uses the same native prominent red toolbar treatment as the wipe-enabled
  Sign Out action and remains disabled until the confirmation phrase matches.

## WN-PROTOTYPE-0014 — Stable destructive Sign Out treatment

- Date: 2026-08-05
- Status: Approved; supersedes the Sign Out default and action-color decisions
  in WN-PROTOTYPE-0009 through WN-PROTOTYPE-0011

- **Wipe Data From This Device** starts on in the Sign Out sheet.
- The final **Sign Out** action uses the same native prominent red toolbar
  treatment whether wiping is on or off. Changing the Toggle does not change
  the button's color or prominence.
- Wiping still requires the exact profile-name confirmation. Turning wiping
  off preserves local data and removes the confirmation field.

## WN-PROTOTYPE-0015 — Standard destructive toolbar actions

- Date: 2026-08-05
- Status: Approved; supersedes only the prominent visual treatment in
  WN-PROTOTYPE-0013 and WN-PROTOTYPE-0014

- **Sign Out** and **Erase** remain compact trailing toolbar actions. They are
  not duplicated as large buttons inside their Form sheets.
- Both actions use the native destructive role without a prominent button
  style, manual tint, or manual font weight. The system owns their
  regular-weight enabled-red and subdued disabled appearance.
- **Sign Out** keeps the same destructive treatment whether wiping is on or
  off. **Erase** and wipe-enabled **Sign Out** remain disabled until their
  confirmation input matches.
- A large in-sheet action was rejected because it would overemphasize an
  irreversible outcome and compete with the confirmation content.

## WN-PROTOTYPE-0016 — Full-width destructive sheet actions

- Date: 2026-08-05
- Status: Approved; supersedes WN-PROTOTYPE-0015 and only the final-action
  placement and visual treatment in WN-PROTOTYPE-0013 and WN-PROTOTYPE-0014

- **Sign Out** and **Erase** are full-width red destructive buttons at the
  bottom of their Form sheets. They are not trailing toolbar actions.
- Their labels use regular body weight rather than bold emphasis. Native
  `borderedProminent` buttons own control geometry, pressed behavior,
  accessibility, and disabled presentation.
- **Cancel** remains the only toolbar action in both sheets.
- Wipe-enabled **Sign Out** and **Erase** remain disabled until their required
  confirmation input matches. **Sign Out** keeps the same red destructive
  treatment when wiping is off.

## WN-PROTOTYPE-0017 — Large confirmation sheets with Close controls

- Date: 2026-08-05
- Status: Approved; supersedes only sheet detents, dismissal controls, and
  action-width details in WN-PROTOTYPE-0016

- Sign Out and Erase App Data always use the large sheet detent. They don't
  offer a compact medium presentation.
- A native leading `xmark` button is each sheet's sole toolbar action. The
  system toolbar supplies its Liquid Glass treatment; the app adds no custom
  background or geometry.
- Sign Out and Erase remove the standard Form row inset around their final
  actions so each visible destructive button spans the complete grouped
  content width.
- Profile-name confirmation uses local TextField state and native focus. Its
  enabled state updates directly as the person types the matching name.

## WN-PROTOTYPE-0018 — Developer tools ownership and sanitized diagnostics

- Date: 2026-08-06
- Status: Approved

- Developer Tools is an always-visible team surface in this prototype and
  begins with an explicit development-and-testing warning.
- **Debug Mode** owns optional conversation-level inspection. It exposes a
  native `ladybug` action in implemented conversations and never inserts raw
  events into a normal conversation timeline.
- **Diagnostics** separately owns live local technical events, runtime facts,
  aggregate relay health, and the deterministic self check. **Streaming
  Debug** is intentionally omitted because it duplicates Diagnostics and
  pollutes product timelines with developer information.
- Anonymous Telemetry, Audit Logging, and Debug Mode are independent settings.
  Audit files are sanitized, remain available after logging is turned off,
  and can be deleted without changing the logging preference. Sensitive-data
  logging is not offered.
- Audit Logs owns file inventory and destructive cleanup. Profile Keys remains
  the owner of key information; Developer Tools does not repeat profile keys
  or a fictional MarmotKit connection state.
- The About section reads the app version and build from the bundle and records
  the prototype integration baseline as **MarmotKit (790eb860)** on **iOS 27**.

## WN-PROTOTYPE-0019 — Profile-scoped Developer Tools gate

- Date: 2026-08-06
- Status: Approved; supersedes the ownership, visibility, About placement, and
  Key Packages details in WN-PROTOTYPE-0018

- Each profile owns an independent Developer Tools state. Entering the page
  first presents a master **Developer Tools** Toggle; technical sections remain
  hidden until it is enabled for the active profile.
- Disabling the master Toggle stops Debug Mode, Anonymous Telemetry, and Audit
  Logging for that profile. Existing sanitized audit files and the current key
  package remain intact because disabling tools is not deletion.
- Diagnostics describes only the active profile and no longer reports how many
  other profiles are stored on the device.
- Key Packages is a separate Developer Tools destination with exactly one
  current package and one available operation: **Publish Key Package**.
- About is the final section and contains only the bundle version/build and
  **MarmotKit (790eb860)**. The Developer Tools UI does not show an iOS SDK
  version.

## WN-PROTOTYPE-0020 — Inline audit files and content clearing

- Date: 2026-08-06
- Status: Approved; supersedes the Audit Logs destination and file-deletion
  details in WN-PROTOTYPE-0018 and WN-PROTOTYPE-0019

- Audit files appear inline in the profile's **Audit Logging** Form section
  only while Audit Logging is enabled. Turning logging off hides the file rows
  and stops new logging without removing existing files.
- **Clear Audit Logs** clears the recorded contents of every audit file but
  preserves the file records, filenames, dates, profile ownership, and Audit
  Logging preference. The files remain visible with zero-byte sizes after the
  operation.
- Clearing uses a native destructive confirmation because recorded diagnostic
  history cannot be recovered. There is no separate Audit Logs destination and
  no Delete Audit Log Files operation.

## WN-PROTOTYPE-0021 — Focused diagnostics and key-package status

- Date: 2026-08-06
- Status: Approved; supersedes the Diagnostics and Key Packages presentation
  details in WN-PROTOTYPE-0018 and WN-PROTOTYPE-0019

- Diagnostics is an event-only console. It presents deterministic local
  events with an animated system **Live** indicator plus **Test** and
  **Clear Events** actions. Runtime, relay-health, and self-check summary
  sections are intentionally omitted.
- **Test** appends one deterministic passing event to the same list rather
  than creating a separate result card. **Clear Events** empties the list.
- Key Packages continues to expose exactly one current package. Its row shows
  the package identifier, published time, and size; it does not show a
  separate **Synced** or location value.
- The sole package action is **Publish New Key Package**, represented with
  Apple’s shipping-box-and-arrow SF Symbol. Publishing replaces the existing
  package and updates its published time.

## WN-PROTOTYPE-0022 — Persistent diagnostics console and toolbar commands

- Date: 2026-08-06
- Status: Approved; refines the Diagnostics presentation in
  WN-PROTOTYPE-0021

- Diagnostics dedicates its available body to one persistent adaptive
  system-background event group with continuous rounded corners. Events scroll
  within that group. When no events exist, the same group remains visible and
  contains the native **No Events** presentation.
- The **Events** and **Live** header aligns with the event rows' content margins,
  not the rounded group's outer edges.
- A native toolbar Menu owns **Test** and **Clear Events**. Clearing ephemeral
  console output is a normal command, not a destructive action, and is disabled
  using the system state when the console is already empty.
- The animated **Live** symbol uses semantic system green while the visible
  label and VoiceOver description continue to communicate state without color.

## WN-PROTOTYPE-0023 — Unified profile-owned chats and complete chat flows

- Date: 2026-08-08
- Status: Approved; completes the per-chat editing deferral in
  WN-PROTOTYPE-0005

- Each profile owns one authoritative People directory and chat collection.
  A chat owns its metadata, list state, independent routing, messages, draft,
  membership, and reactions; chat-list rows are projections of that state.
- Every chat row opens one shared direct/group conversation architecture.
  Maya Chen is the exhaustive direct showcase and Weekend Walks is the
  exhaustive admin group showcase. Fiatjaf and White Noise Support retain their
  accepted visible stories while moving to the shared state.
- New direct chats use a person-profile **Message** step and deduplicate by
  person. New groups require at least one other person, make the creator an
  admin, and copy the creator profile's available Chat Messages relays.
- Per-chat relay editing now lives in Chat Info and Group Info. Editing a chat
  never rewrites profile defaults or another chat. An empty chat relay list
  disables sending but preserves history.
- The functional composer sends text, links, images, videos, files, replies,
  mentions, and a simulated press-and-hold voice message. Voice uses one
  bundled locally generated recording and no microphone API. Contact and GIF
  remain deterministic showcase renderers. Shared-location and sticker
  messages are not supported and do not appear in fixtures or catalog coverage.

## WN-PROTOTYPE-0024 — Record once and choose the speech-message format

- Date: 2026-08-15
- Status: Approved

- The resting composer keeps its existing press-and-hold waveform with no new
  permanent speech control. Stopping a recording keeps the voice review and
  centers the optional **Transcribe** action there with secondary emphasis.
- After transcription, one native Menu offers **Voice**, **Text**, and **Both**,
  followed by one Send action. Its compact label shows the selected format and
  one downward chevron. Both is the default. Text is an editable ordinary
  message. Both is one message with a voice attachment and ordinary message
  text, never duplicate timeline entries or a nested transcript surface. The
  review waveform uses a fully opaque semantic primary foreground for stronger
  contrast. While the Menu is visible and dismissing, the composer beneath it
  remains inert so selecting its lowest row cannot activate an underlying
  control. Its selected value and chevron use the semantic secondary label
  color, and its native button fills an available 44-point rectangular row.
  Text and Both reuse the conversation's pull-to-expand gesture across the
  complete composer, including that row. A normal movement threshold separates
  a stationary Menu tap from a deliberate pull. Voice remains compact. The
  resting empty composer does not attach that competing pull gesture while its
  hold-to-record control is visible. Expanded review content is top-aligned in
  format, voice, then text order. The 44-point format button
  never stretches vertically, and opening its Menu blocks taps without
  collapsing or hiding the expanded composer behind it. The Menu uses an
  invisible targeted-preview
  anchor so neither compact nor expanded composer is lifted, hidden, or
  morphed during native presentation and dismissal; only its selected content
  changes.
- Received text offers **Read Aloud** in message actions. Received voice offers
  **Transcribe** when no transcript exists and **Show Transcript** or **Hide
  Transcript** afterward. Any visible transcript paired with a voice attachment
  uses a small secondary **Transcribed** provenance label above plain message
  typography. Recipient-created transcripts keep additional separation from
  the voice row, remain local, and are not forwarded. Authored voice transcripts
  use **Copy Transcript** instead of generic **Copy**. Compact transcript review
  grows from its actual text without reserving empty rows below it and matches
  the ordinary composer's bottom text inset. Choosing Voice clears expansion;
  returning to Text or Both starts compact until the user pulls again.
- The custom message action presentation never scrolls internally; it sizes to
  show every relevant command at once. Read Aloud shows determinate native
  progress beneath the message text while speech is active.
- The prototype keeps recording and transcription deterministic and in memory.
  It adds no microphone, Speech framework, network, persistence, or backend
  dependency. Native system speech synthesis may read received text aloud.

## WN-PROTOTYPE-0025 — Content-independent composer pull

- Date: 2026-08-15
- Status: Approved; supersedes only the empty-composer pull restriction in
  WN-PROTOTYPE-0024

- The resting composer accepts the same pull for empty, one-line, and multiline
  drafts. Expansion never depends on whether the draft is sendable.
- Compact and expanded presentations retain one stable composer hierarchy
  throughout direct manipulation and settling. Entering flexible layout never
  replaces the gesture-owning subtree, so the first pull continues to its
  selected endpoint instead of stopping at an intermediate height.
- The movement threshold separates a directional pull from the waveform's
  stationary press-and-hold. Active recording still disables expansion, and
  voice review retains its existing format-specific behavior.
