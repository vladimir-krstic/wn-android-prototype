# Verified event cards and readers

## Purpose

Let people recognize public profile references inside messages and inspect
shared public events without leaving the conversation or losing the author’s
original reference.

## Scope and non-goals

B30 covers C114 and C115: checksum-valid `npub`/`nprofile` message references,
member-aware in-app identity presentation, typed public-event cards, recovery,
article/document reading and local referenced-video playback.

The prototype does not query relays, verify real signatures, download media,
fetch profiles, persist resolved events, sign or encrypt, or hand Nostr
identity/event references to another app. Typed Loaded fixtures represent the
result of production verification. The original reference remains available
for copy and later production wiring.

## Production evidence

The pinned production baseline is
[`319454889f1c2494dec4a69b5577d98017f44eee`](https://github.com/marmot-protocol/whitenoise-android/commit/319454889f1c2494dec4a69b5577d98017f44eee).

- [`NostrProfileReference.kt`](https://github.com/marmot-protocol/whitenoise-android/blob/319454889f1c2494dec4a69b5577d98017f44eee/app/src/main/java/dev/ipf/whitenoise/android/core/NostrProfileReference.kt)
  strictly decodes public profile and event references.
- [`MarkdownRenderer.kt`](https://github.com/marmot-protocol/whitenoise-android/blob/319454889f1c2494dec4a69b5577d98017f44eee/app/src/main/java/dev/ipf/whitenoise/android/ui/MarkdownRenderer.kt)
  reserves `@Name` for a known group member, renders a known non-member as an
  inline profile link, and keeps identity taps in-app.
- [`NostrEventCards.kt`](https://github.com/marmot-protocol/whitenoise-android/blob/319454889f1c2494dec4a69b5577d98017f44eee/app/src/main/java/dev/ipf/whitenoise/android/ui/conversation/nostr/NostrEventCards.kt),
  [`NostrEventCardResolver.kt`](https://github.com/marmot-protocol/whitenoise-android/blob/319454889f1c2494dec4a69b5577d98017f44eee/app/src/main/java/dev/ipf/whitenoise/android/ui/conversation/nostr/NostrEventCardResolver.kt),
  and
  [`NostrEventCardModelMapper.kt`](https://github.com/marmot-protocol/whitenoise-android/blob/319454889f1c2494dec4a69b5577d98017f44eee/app/src/main/java/dev/ipf/whitenoise/android/ui/conversation/nostr/NostrEventCardModelMapper.kt)
  define typed cards, authored-reference copy, bounded card count and recovery.
- [`NostrEventViewers.kt`](https://github.com/marmot-protocol/whitenoise-android/blob/319454889f1c2494dec4a69b5577d98017f44eee/app/src/main/java/dev/ipf/whitenoise/android/ui/conversation/nostr/NostrEventViewers.kt)
  owns article and video presentation.

## Parity contract

- Only checksum-valid public `npub` and `nprofile` forms become identity links.
  Private, malformed, mixed-case and bad-checksum values remain ordinary text.
- `@npub`/`@nprofile` for a known current group member renders as a highlighted
  **@Name**. A known non-member renders as an underlined **Name** without `@`.
- A plain `nostr:npub` or `nostr:nprofile` URI is an in-app profile link, not
  an external URI.
- A known profile opens the existing Person Profile destination. An unresolved
  public profile opens a local unavailable dialog with Retry and preserves the
  compact public reference.
- Rendered identity names map every visible character to the complete unchanged
  authored reference. B09 selection, copy, editing, speech highlighting and
  revision history continue to use authored source.
- Public event references accept strict `note`, `nevent` and `naddr` shapes.
  Profile and private-key forms never become event cards.
- Loaded fixtures cover Note, Article, Image, Video, Document and Event. Other
  fixtures cover Loading, Not Found, Invalid, Unavailable and Failed.
- Cards keep the exact authored reference for **Copy event reference** and show
  a compact version only as secondary recognition text.
- Retry requires the exact active profile, chat, message, reference and revision.
  Ended membership, deleted messages, stale revisions and profile switches make
  no change.
- A message containing only its event reference hides the duplicate body while
  the card is visible. The authored source remains available through message
  actions and search.
- At most three cards render in one message, matching the production bound.
- Article and Document open the existing structured Markdown reader. Video
  opens the existing Media3 Compose player with the bundled local clip.
- Note, Image and Event open a Material details dialog. No event action invokes
  an external app or starts network work.

## Entry, navigation, Back, and exit

Conversation Debug → **Add event card examples** inserts or replaces stable
local rows in that exact active chat. The action is visible only through the
existing Developer Tools gate. Returning to the conversation shows the rows as
ordinary content; the cards and readers remain available when Developer Tools
is later disabled.

Card **Read**, **Play**, and **Open event** actions open route-local modal
presentation. App-bar Back, system Back, and Close dismiss that presentation
and return to the same conversation position. No open or retry action survives
a profile/chat owner change.

## Exact product copy

Ordinary copy is resource-backed: **Loading event…**, **Event not found**,
**This event reference isn’t valid.**, **This event is unavailable right now.**,
**Couldn’t load this event.**, **Copy event reference**, **Open event**, **Read**,
**Play**, **Note**, **Article**, **Image**, **Video**, **Document**, **Event**,
**Profile unavailable**, **Retry**, **Checked again**, and **Close**.

The fixture insertion labels remain on the developer-only Conversation Debug
screen. Raw event kinds, hex IDs, relay hints and verifier errors do not appear
on ordinary surfaces.

## Android composition

Event cards use the established 256 dp rich-message canvas, 6 dp rich-content
spacing, a neutral transparent surface and an outline derived from the bubble’s
content color. Text communicates kind and status; icons are reinforcement.
Material buttons retain their native target, focus, ripple and keyboard
behavior. Card actions use a wrapping row for localization and 200% text.

Readers use a full-window Material `Dialog`, `Scaffold`, safe insets and app-bar
Back. The article body reuses `MessageDocumentContent`, including headings,
lists, selection and safe link behavior. Referenced video reuses
`MediaViewerVideo`, Media3’s Material controls and the packaged
`chat_trail_clip.mp4`; it never accepts a remote URL.

## State and ownership

`NostrProfileReferences` validates and resolves encoded public identities.
`NostrProfileTextProjection` owns the visible-to-authored range mapping.
`NostrPublicEventReferences` strictly decodes public event pointers.
`NostrEventReference` holds immutable presentation state, retry output and
revision. `NostrEventExamples` inserts/replaces its own rows and applies retry
only to an exact live reference.

`AppViewModel.addNostrEventExamples` enforces Developer Tools plus exact active
profile/chat ownership. `retryNostrEvent` does not require Developer Tools
because recovery belongs to ordinary content, but it does require an active
signed-in owner and exact revision.

## Accessibility and adaptation

- Every state and kind has visible text and a semantic state description.
- Copy, Retry, Open, Read, Play, Back and Close are named Material actions.
- Identity links use their displayed name or compact public reference and stay
  keyboard/Voice Access reachable through Compose link semantics.
- Event images are decorative because adjacent title/summary text carries the
  meaning; no key or raw event ID is placed in a content description.
- Cards and dialogs use logical source order, wrapping actions, adaptive reader
  width and scrollable content at large type.
- The Media3 player retains native playback, focus and lifecycle semantics.

## Governing Android guidance

- [Navigation](https://developer.android.com/guide/navigation/) and
  [type-safe Navigation Compose routes](https://developer.android.com/guide/navigation/design/type-safety)
  govern the existing Person Profile handoff and Back ownership.
- [Compose semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics)
  governs state descriptions and link/action meaning.
- [Accessibility principles](https://developer.android.com/guide/topics/ui/accessibility/principles)
  governs equivalent named actions and logical reading order.
- [Media3 Compose customization](https://developer.android.com/media/media3/ui/compose-customization)
  governs reuse of Material player controls.
- [Compose progress indicators](https://developer.android.com/develop/ui/compose/components/progress)
  governs the indeterminate Loading presentation.

## Acceptance

- Known `npub` and `nprofile` references open the matching local profile and
  never call an external URI handler.
- Group-member and non-member presentation differs by `@`, weight/background
  and underline as specified without changing authored source.
- Unresolved profile presentation offers Retry and keeps the reference.
- Every six event kind and all five loading/failure outcomes have deterministic
  developer fixtures.
- Copy returns the exact `nostr:` authored reference.
- Retry changes only the exact eligible reference and rejects stale/foreign work.
- Article/document content uses the rich reader and Back returns safely.
- Video uses the bundled local clip in the existing Media3 viewer.
- Existing message selection, editing, search, actions, read state and agent
  operations remain intact.

## Implementation evidence

B30 implementation uses `NostrReferences.kt`, `NostrEventUi.kt`, the existing
message-document renderer and Media3 viewer, exact-profile AppViewModel
mutations and a developer-only fixture action. `NostrReferenceTest` adds nine
validation, display/source-mapping, fixture, ownership and retry regressions.
`NostrEventInteractionTest` adds seven compiled card, reader, player, copy,
identity, recovery and large-type cases.

The complete host gate
`./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`
passes 826 unit tests with zero failures/errors/skips, zero lint errors, and
both APKs. All seven new Compose cases compile only. Device execution,
screenshots and visual acceptance were not requested and are not claimed.
