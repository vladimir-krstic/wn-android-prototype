# F02 — Discovery, profiles and private contact details

## Purpose and current composition

Enter from New Message, member/profile taps, direct-chat details or QR/link. Back returns to the exact origin. Keep published profile data separate from private nickname/notes and role-gated group actions.

Prototype surface: `ui/chats/ChatCreationScreens.kt; ui/settings/ProfileSettingsScreens.kt; model/Profile.kt`. Reuse `WhiteNoiseScaffold`, tonal Settings groups/rows, shared sheets/dialogs, `WhiteNoiseTextField`, `WhiteNoiseButton`, adaptive content bounds, message action/reaction components, MediaViewer and established empty/loading/error content as applicable. Keep 48 dp minimum targets, label-above forms, semantic error colors, bounded state layers, RTL, 200% text, IME/inset ownership and compact/expanded behavior.

## Required content and proposed copy

Use **Profile**, **Display name**, **About**, **Profile banner**, **Lightning address**, **Nickname & notes**, **Follow/Unfollow**, **Message**, **Add to groups**, and task-specific unavailable/retry copy. Explain that nickname and notes are private to this device.

These labels are the audit recommendation and follow current prototype terminology. Validate exact surrounding help/error copy in the selected screen brief against each matrix source link; preserve production security and destructive consequences without exposing implementation terms.

## Capability and state contract

| Capability | Initial state | Event / Back behavior | Observable result |
| --- | --- | --- | --- |
| C012 · Direct-chat creation and existing-chat reuse | Existing equivalent | Trigger its named entry/action; cancel with Back where available | Preserve behavior |
| C013 · People search by name, public identifier or address | Implemented; host verified | Trigger its named entry/action; cancel with Back where available | Search distinguishes local/chats/following/network results; resolving, invalid public key/address, address-not-found, no-profile, partial/unavailable and retry paths exist. Local rows remain available, and unresolved people can receive a standard invitation share. |
| C014 · Creation succeeds but conversation projection is unavailable | Implemented; host verified | Trigger its named entry/action; cancel with Back where available | Direct/group creation records a canonical ID before opening. An unavailable projection retries that ID without duplicate creation; close returns to Chats, and navigation/profile changes reject stale completion. |
| C015 · Own profile display name/about/avatar/verified address | Existing equivalent | Trigger its named entry/action; cancel with Back where available | Preserve behavior |
| C016 · Profile banner and large avatar viewer | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Add banner choice/URL/removal and loading/failure states; open avatar image from own and other profiles without entering edit. |
| C017 · Lightning address field and validation | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Add syntactic validation, checking, unresolved and no-connection outcomes and read-only display in person profile; use a deterministic non-payment fixture. |
| C018 · Generate a new suggested display name | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Offer a name suggestion action that changes only the draft, with deterministic suggestions and explicit Save. |
| C019 · Private nickname and contact notes | Implemented; host verified | Trigger its named entry/action; cancel with Back where available | Profile-owned nickname/notes editing cleans and caps nickname at 80 characters. Clearing restores published name. Display names propagate to chats, search, authors, mentions, contacts/shared content and roster; published metadata and private notes stay separate. |
| C020 · Follow/unfollow and groups in common | Existing equivalent | Trigger its named entry/action; cancel with Back where available | Preserve behavior |
| C021 · Profile-to-group invitation and promotion across groups | Implemented; host verified | Trigger its named entry/action; cancel with Back where available | Start group preselects the person. Multi-group invite/admin promotion checks active authoritative roles, resolving/partial/unavailable rosters, confirmation and retry. Partial application preserves successes and retains only failed selections. |

## Production integration seam

Production evidence for each row is linked in the [matrix](../capability-matrix.md). During prototype work, add the smallest profile-owned immutable fixture/state transitions and callbacks needed to render every named result. Do not add Marmot, networking, signing, persistence, notification delivery, background services or cryptography. Name production events and ownership in the selected screen brief so the eventual migration reconnects to the cited controller/state methods rather than copying prototype fixtures into production storage.

## Copy, accessibility and adaptation

Use the approved product language and terminology. Production strings in the matrix are evidence of meaning, not automatic final copy. Keep raw keys, event IDs, MLS and engine errors off ordinary surfaces; developer surfaces may be exact. State must be conveyed by text/semantics as well as icon/color. Provide accessible equivalents for gestures, logical focus and Back order, and preserve action eligibility at large type and narrow height.

## Acceptance and host validation

- Every linked capability has a deterministic route/fixture and every mutation yields the specified success, cancellation, unavailable and failure outcomes relevant to it.
- Back, profile switching and restored state cannot commit work to the wrong profile/chat or repeat a completed mutation.
- Existing capabilities in this flow retain their current model and UI tests.
- Add unit tests for rules/ownership and Compose tests for durable navigation/actions/semantics. Run targeted host tests while iterating and the repository static gate after a meaningful batch. Compile instrumentation tests only; device execution and visual acceptance require a separate current request.

## Dependencies and decisions

Batches: B03, B04. Decisions: None. Facts are the matrix's cited production behavior and current prototype evidence. UI placement and proposed copy remain recommendations until the selected screen brief records them.

## B03 implementation evidence

C013, C014, C019 and C021 are implemented and host-verified 2026-09-04. The
[selected brief](../../../screens/people-discovery-and-private-details.md#implementation-evidence)
records source/result states, private details, canonical chat-open recovery and
role-checked group actions. The clean gate passed 231 unit tests, zero lint
errors and both APKs; seven new UI cases compile. B04 owns the remaining profile
banner/avatar/Lightning/name-suggestion additions. Device/visual acceptance pending.
