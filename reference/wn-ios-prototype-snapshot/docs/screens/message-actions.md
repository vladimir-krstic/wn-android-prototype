# Message actions

## Purpose and navigation

Provide the complete press-and-hold message-action flow shared by every direct
and group conversation. A deliberate hold focuses one message and exposes
reactions plus **Reply**, **Forward**, **Copy**, **Select**, **Info**, and
**Delete**. The related reply composer, forwarding sheet, selection mode,
deletion confirmations, emoji picker, and pushed **Message Details** screen are
one bounded flow.

## Context presentation and exact copy

- A nondeleted message uses Signal's deliberate two-phase hold timing: a native
  `UILongPressGestureRecognizer` bridged through
  `UIGestureRecognizerRepresentable` must first remain within ten points for
  0.2 seconds, then the pressed message compresses to 95 percent over 0.2
  seconds before the focused presentation opens. Cancelling during either
  phase restores the message on the same ease-in-out curve. This keeps the
  visible presentation about 0.4 seconds away from touch-down without making
  transcript scrolling wait for a custom timer.
- Interactive bubble content keeps two distinct outcomes. A short tap performs
  the content action, including opening a quoted message, media viewer, link,
  file, person, or playback control. The message hold makes descendant content
  gestures wait for it to fail while explicitly recognizing alongside pan
  recognizers, so the transcript's native vertical pan can begin directly over
  a bubble. Movement cancels the pending hold, while a successful stationary
  hold cancels the child tap and opens the owning message's action presentation
  from every point inside the bubble.
- The source message lifts above a regular-material conversation backdrop. Its
  reaction strip sits above it and its action surface sits below it, aligned to
  the message direction. The group shifts within safe margins when the source
  position would place an action offscreen. Touching the backdrop dismisses it.
- The quick reactions default to **❤**, **🤘**, **🔥**, **😂**, **🦫**, and
  **🚀**, followed by **More Reactions**. The six defaults belong to the active
  profile and remain in memory independently for each profile. If the current
  profile's selected reaction is not one of those six, it is appended as a
  selected seventh reaction immediately before **More Reactions**. Choosing the
  selected reaction in this long-press strip removes it. Any other choice
  replaces the current profile's prior reaction so one person has at most one
  reaction per message. A normal tap on a reaction pill beneath a message only
  selects or replaces the current profile's reaction; tapping an already
  selected pill never removes it.
- **More Reactions** opens a searchable native sheet at the medium or large
  detent. The sheet has no navigation title or Close button; its system drag
  indicator and interactive sheet dismissal remain visible. A search field
  uses UIKit's native internal search-field inset inside an 8-point leading
  sheet inset. The glass **Configure Reactions** gear uses the `.large` system
  control size so its circle matches the visible search-field height. A
  4-point stack gap combines with `UISearchBar`'s own trailing inset to produce
  the same visible control separation used by the app's other field-adjacent
  actions. The gear uses SwiftUI's platform-default trailing safe-area inset
  instead of a hand-tuned edge value. This user-approved Signal-style header
  is an app-owned `UISearchBar` because native `.searchable` cannot place a
  separate settings action in the same search row without also introducing
  the rejected navigation chrome. The content starts with
  deterministic recents followed by the standard categories; there is no
  message-specific section.
- The emoji grid and every section heading share the same native sheet content
  margin on both sides. The grid presents eight dense columns while retaining
  a 44-point target for every emoji. One fixed-width continuous interactive
  Liquid Glass capsule contains all nine category controls: recents plus the
  eight standard categories. It fits within the sheet margins and never
  scrolls or extends past an edge. The chosen category is distinguished inside
  that shared surface rather than giving every category a separate glass
  button. Choosing an emoji applies or replaces the current reaction and
  dismisses the sheet; it never removes a matching reaction. The picker and
  quick strip use Apple-rendered emoji glyphs.
- **Configure Reactions** is available from the picker's gear action. It opens
  a focused native sheet titled **Configure Reactions** with the active
  profile's six quick reactions, the guidance **Tap an emoji to replace it.**,
  **Reset**, and **Done**. Tapping a slot opens the same emoji picker for its
  replacement. Replacing with an emoji already in another slot swaps the two
  positions so the six choices remain unique. **Reset** restores the default
  six in the draft; **Done** applies the draft only to the active profile.
  Dismissing without Done discards the draft.
- Opening a replacement picker preserves the chosen slot as the clear focus:
  over 0.3 seconds the chosen emoji grows to 130 percent while the other five
  shrink to 80 percent and fade to 30-percent opacity. The focused emoji then
  alternates between -0.08 and 0.08 radians every 0.2 seconds until replacement
  or dismissal. This bounded behavior follows Signal's current
  `MessageReactionPicker` configuration animation; Reduce Motion keeps the
  static focused sizing and dimming but suppresses the wiggle and transform
  interpolation.
- The command order is **Reply**, **Forward**, **Copy**, **Select**, **Info**,
  and destructive **Delete**. **Copy** appears only when a message contains
  text. A failed outgoing message keeps **Retry Send** first. The user did not
  request Edit or Pin, so neither is added.
- **Reply** dismisses the presentation, installs the existing reply quote, and
  focuses **Message**. **Copy** writes the rendered message text to the general
  pasteboard. It has no intermediate screen. **Info** pushes **Message
  Details**. **Forward**, **Select**, and **Delete** continue into the states
  below.

## Motion and layout behavior

- Signal's current open-source implementation is bounded comparison evidence,
  not a runtime dependency or copied implementation. The recording and source
  establish distinct coordinated tracks rather than one shared accessory
  animation:
  - backdrop blur and preview shadow arrive over 0.2 seconds;
  - the preview moves from its source frame over a 0.4-second spring with 0.8
    damping and initial velocity 1;
  - the action menu begins directly below the source message at 20-percent
    scale and opacity. Its upper attachment edge remains locked to the moving
    message while both travel to their resolved position, and the Liquid Glass
    surface expands and fades in over the same 0.4-second spring. Incoming
    messages grow from the menu's top-leading corner; outgoing messages grow
    from its top-trailing corner;
  - the reaction surface fades in over 0.2 seconds, while each reaction moves
    upward 24 points and fades over 0.2 seconds with a 0.01-second item stagger;
    this track waits 0.1 seconds only when the preview needs to move;
  - dismissal reverses the preview, menu, reactions, and backdrop over 0.4
    seconds instead of collapsing every track into a 0.2-second exit; and
  - message selection uses a separate 0.2-second ease-in-out transition.
  White Noise recreates those causal tracks with state-driven SwiftUI motion.
- The reaction strip and action menu align to the focused bubble box rather
  than to a screen margin: their leading edges match an incoming bubble and
  their trailing edges match an outgoing bubble. The same vertical gap is used
  above and below the focused message. When reaction pills extend below a
  bubble, the lower gap begins at the visible pill edge rather than at the
  pill's larger invisible hit target.
- The source message retains its real width, direction, contents, reactions,
  avatar, and metadata. Very tall content scales only as much as necessary to
  keep the reaction strip and a scrollable command surface reachable.
- Context presentation and dismissal are interruptible. The focused preview
  and each scaled accessory are composited as unified visual layers so text,
  symbols, glass, and rounded edges do not animate as disconnected subviews.
  Reduce Motion replaces scaling, spring, and directional travel with a short
  opacity transition.
- Long-press movement tolerance remains ten points. Vertical movement before
  recognition fails the hold and scrolls the transcript, including when the
  touch began directly on a bubble. A tap continues to perform the message's
  ordinary action; it never opens the context presentation.

## Selection mode

- **Select** enters selection with the initiating message selected. A 24-point
  leading selection indicator appears inside a 44-point button target for every
  selectable message, and incoming content makes room for that column. Tapping
  anywhere on a message toggles it. Selected state uses a checkmark plus the
  system accent and never depends on color alone.
- The normal Back action is replaced by **Close** at trailing. Selection mode
  has no top-leading delete action. **Close** exits selection without changing
  messages.
- The composer is replaced by a native bottom safe-area bar containing
  **Delete Selected Messages**, **N Selected**, and **Forward Selected
  Messages**. Delete and Forward are disabled at zero selections. Forward is
  also disabled when a selected item is deleted or when more than 32 messages
  are selected.

## Forwarding

- Forward presents a native searchable sheet titled **Forward** at medium and
  large detents. Chat rows use their existing identity, title, and selection
  indicator. A person can select up to five destinations.
- The bottom action reads **Forward** for one destination and **Forward to N
  Chats** for several. Completion appends in-memory copies in source order,
  authored by the current profile, without reply or reaction metadata. It then
  dismisses the sheet and selection mode. There is no backend or network work.

## Message details

- **Message Details** is a pushed native `List`. Its first section shows the
  message in the shared bubble presentation, followed by direction-appropriate
  **Sent** or **Received** time. The bubble uses the same native horizontal row
  inset as that status row, so their leading content edges align.
- Incoming messages include **Sent from** with the sender. Outgoing messages
  group recipients under **Sending**, **Sent**, or **Not Delivered** according
  to the message's deterministic delivery state. Group recipients appear as
  individual rows; a direct chat shows the other person.
- The screen uses the real message, people, attachment labels, and localized
  date/time. It does not invent protocol identifiers, receipt timestamps, or
  backend diagnostics.

## Deletion

- A single message asks **Delete message?**. Selection asks **Delete selected N
  messages?**. Both explain that **Delete for Me** removes the selected content
  from this device.
- **Delete for Me** is available for incoming and outgoing messages and removes
  the selected entries locally. **Delete for Everyone** is additionally
  available only when every selected message is a nondeleted outgoing message.
  It keeps the accepted **You deleted this message.** tombstone and clears its
  attachments, reactions, and reply metadata. **Cancel** is always available.
- Every deleting command uses the destructive role. A message removed for the
  current profile cannot leave a stale pending reply in the composer.

## Native components and custom exception

- Native SwiftUI `Button`, `Label`, `List`, `NavigationStack`, `TextField`
  search, `ScrollView`, `LazyVGrid`, `safeAreaBar`, `sheet`,
  `presentationDetents`, `confirmationDialog`,
  `UIGestureRecognizerRepresentable`, `UILongPressGestureRecognizer`, semantic
  colors/type, SF Symbols, and `sensoryFeedback` own their applicable behavior.
- UIKit's public context-menu API owns a preview and a `UIMenu`, but exposes no
  public reaction-bar accessory. iOS also exposes no public system emoji-picker
  controller and doesn't let an app force the emoji keyboard. The user-approved
  Signal-style reaction accessory therefore requires one bounded custom
  SwiftUI context composition and an app-owned emoji grid. They use only public
  APIs, system-rendered emoji, native sheets/search, `GlassEffectContainer` and
  `glassEffect` for the continuous category surface, 44-point controls, Dynamic
  Type, semantic materials, and Reduce Motion.

## Accessibility

- Every message retains named accessibility actions for Show Actions, Reply,
  Forward, Copy when available, Select, Info, and Delete. VoiceOver never has
  to discover the flow by a long press alone.
- The focused presentation is modal to accessibility. Reactions announce the
  emoji and whether the current profile selected it. The full picker announces
  category headings and each emoji as a button. Category controls announce
  their selected state. The search field, clear action, Configure Reactions,
  and sheet escape action are named. Configuration slots announce their
  position, emoji, and replacement purpose; Reset and Done remain named
  controls.
- Selection indicators announce **Selected** or **Not selected**. The bottom
  count updates as one live value. Disabled Forward explains whether a deleted
  item or the 32-message limit is blocking it.
- Layout supports Dynamic Type, localization expansion, right-to-left
  direction, sufficient contrast, and visible state independent of haptics or
  animation.

## Governing and comparison sources

- [Apple context menus](https://developer.apple.com/design/human-interface-guidelines/context-menus)
- [UIContextMenuInteraction](https://developer.apple.com/documentation/uikit/uicontextmenuinteraction)
- [UIGestureRecognizerRepresentable](https://developer.apple.com/documentation/swiftui/uigesturerecognizerrepresentable)
- [UILongPressGestureRecognizer](https://developer.apple.com/documentation/uikit/uilongpressgesturerecognizer)
- [UIGestureRecognizerDelegate](https://developer.apple.com/documentation/uikit/uigesturerecognizerdelegate)
- [Sheets](https://developer.apple.com/design/human-interface-guidelines/sheets)
- [presentationDetents](https://developer.apple.com/documentation/swiftui/view/presentationdetents(_:))
- [Search modifiers](https://developer.apple.com/documentation/swiftui/view-search)
- [Applying Liquid Glass to custom views](https://developer.apple.com/documentation/SwiftUI/Applying-Liquid-Glass-to-custom-views)
- [GlassEffectContainer](https://developer.apple.com/documentation/swiftui/glasseffectcontainer)
- [LazyVGrid](https://developer.apple.com/documentation/swiftui/lazyvgrid)
- [SensoryFeedback](https://developer.apple.com/documentation/swiftui/sensoryfeedback)
- [Motion](https://developer.apple.com/design/human-interface-guidelines/motion)
- [Accessibility](https://developer.apple.com/design/human-interface-guidelines/accessibility)
- [General pasteboard](https://developer.apple.com/documentation/uikit/uipasteboard/general)
- [Signal custom context menu](https://github.com/signalapp/Signal-iOS/tree/main/Signal/src/ViewControllers/ContextMenus/CustomContextMenus)
- [Signal conversation gesture recognizers](https://github.com/signalapp/Signal-iOS/blob/main/Signal/ConversationView/ConversationViewController%2BGestureRecognizers.swift)
- [Signal context-menu interaction](https://github.com/signalapp/Signal-iOS/blob/main/Signal/src/ViewControllers/ContextMenus/CustomContextMenus/ContextMenuInteraction.swift)
- [Signal reaction picker](https://github.com/signalapp/Signal-iOS/blob/main/Signal/src/ViewControllers/MessageReactionPicker.swift)
- [Signal emoji sheet](https://github.com/signalapp/Signal-iOS/blob/main/Signal/Emoji/EmojiPickerSheet.swift)
- [Signal reaction configuration](https://github.com/signalapp/Signal-iOS/blob/main/Signal/Emoji/EmojiReactionPickerConfigViewController.swift)
- User-supplied Signal selected-nonfavorite reaction screenshot, reviewed
  2026-08-15.
- [Signal message selection](https://github.com/signalapp/Signal-iOS/blob/main/Signal/ConversationView/ConversationViewController%2BSelection.swift)
- [Signal selection toolbar](https://github.com/signalapp/Signal-iOS/blob/main/Signal/src/ViewControllers/MessageActionsToolbar.swift)
- [Signal message details](https://github.com/signalapp/Signal-iOS/blob/main/Signal/src/ViewControllers/MessageDetailViewController.swift)

## Acceptance criteria

- Holding any nondeleted message opens the correctly aligned focused preview,
  quick reactions, More Reactions, and exactly the requested actions.
- Starting a vertical transcript scroll on any part of a message bubble scrolls
  normally and never opens or visibly starts message actions; the action flow
  requires an intentional stationary hold through both timing phases.
- Tapping any interactive quote, media, link, file, person, or playback control
  performs that control's action, while holding the same point opens message
  actions without also performing the tap action.
- A normal reaction-pill tap selects or replaces but never removes. Holding the
  message exposes the selected reaction in the quick strip—even when it is not
  a favorite—and tapping that selected quick reaction removes it. Full-picker
  choices apply or replace and immediately update the compact reaction summary.
- The full picker has one continuous category glass surface, eight dense emoji
  columns with 44-point targets, matching native side margins, no
  message-specific section or navigation chrome, and a working Configure
  Reactions flow whose Reset, replacement, swap, cancel, and Done behavior
  remains local to the active profile.
- Reply, Forward, Copy, Select, Info, Retry Send, Delete for Me, and Delete for
  Everyone produce their documented in-memory results.
- Selection can add and remove multiple bubbles; its top and bottom controls,
  counts, disabled states, transitions, forwarding, and selected-message
  deletion stay coherent through completion or cancellation.
- Message Details accurately reflects incoming/outgoing direction, sender,
  localized times, delivery state, recipients, and shared message content.
- All conversation types use the same flow, no product surface exposes
  implementation-boundary language, and no private API, networking,
  persistence, third-party runtime dependency, or copied Signal code is added.
