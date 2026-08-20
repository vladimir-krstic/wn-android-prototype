# Disappearing-message indicators

## Purpose

Keep an active disappearing-message timer visible in Chats and in the open
conversation without requiring a visit to Chat Info. This is a bounded status
presentation for the existing in-memory setting; it does not change timer
behavior or add new duration choices.

## Navigation and exact presentation

- Chats keeps its existing row navigation and actions. When disappearing
  messages are on, the title row shows the secondary `timer` SF Symbol.
- A muted chat with disappearing messages shows the existing mute symbol first,
  followed by the timer symbol. A chat with only disappearing messages shows
  only the timer symbol.
- The conversation's principal toolbar button keeps the avatar and title. An
  active timer adds a compact secondary subtitle consisting of the timer symbol
  and **1d**, **1w**, or **4w**. By explicit user direction, the inline symbol
  has a 9-point default size and scales relative to `caption2` for Dynamic
  Type.
- A group with an active timer keeps its member count and appends a centered dot
  and the timer status, for example **6 members · [timer] 4w**.
- A direct chat with the timer off has no subtitle. A group with the timer off
  continues to show only its member count.

## Native components and states

- Continue using the existing `ToolbarItem` with `.principal`, `Button`,
  `VStack`, `HStack`, semantic caption typography, secondary foreground style,
  and the public `timer` SF Symbol.
- Do not introduce a custom icon, badge surface, hard-coded color, or motion.
- Timer changes made in Chat Info update both indicators from the same
  profile-owned `PrototypeChat` state.
- The developer catalog includes three stable chat scenarios:
  - **Direct - Disappearing** — 1 Day, not muted.
  - **Direct - Disappearing & Muted** — 1 Week, muted.
  - **Group - Disappearing** — 4 Weeks with the member-count subtitle.

## Accessibility

- A Chats-row timer symbol exposes **Disappearing messages on** and never relies
  on the glyph alone.
- The conversation info button exposes the chat title, group member count when
  applicable, and the full active duration, such as **Disappearing messages,
  4 Weeks**. Compact visible duration copy must not reduce VoiceOver clarity.
- Existing mute, membership, row, and button semantics remain intact.

## Governing sources

- Apple places a principal toolbar item in the center of the iOS navigation bar:
  [ToolbarItemPlacement.principal](https://developer.apple.com/documentation/swiftui/toolbaritemplacement/principal).
- Apple recommends explicit labels and values when SwiftUI's inferred
  accessibility does not fully describe a status:
  [Accessibility fundamentals](https://developer.apple.com/documentation/swiftui/accessibility-fundamentals).
- Apple's current symbol guidance governs use of a familiar adaptive interface
  glyph: [SF Symbols](https://developer.apple.com/design/human-interface-guidelines/sf-symbols).
- Signal's current iOS conversation header composes a timer icon with a short
  duration and keeps multiple statuses in one subtitle:
  [ConversationViewController+UI.swift](https://github.com/signalapp/Signal-iOS/blob/main/Signal/ConversationView/ConversationViewController%2BUI.swift).
- Signal's current timer indicator uses the same compact icon-plus-short-duration
  structure:
  [DisappearingMessagesChatIndicatorView.swift](https://github.com/signalapp/Signal-iOS/blob/main/SignalUI/Views/DisappearingMessagesChatIndicatorView.swift).
- Signal supports a custom time up to four weeks and shows a timer icon in an
  enabled chat header:
  [Set and manage disappearing messages](https://support.signal.org/hc/en-us/articles/360007320771-Set-and-manage-disappearing-messages).

The adopted Signal conclusion is limited to compact active-state presentation.
White Noise retains its existing approved Off, 1 Day, 1 Week, and 4 Weeks
choices; changing the picker is outside this task.

## Acceptance criteria

- Off, disappearing-only, muted-and-disappearing, and group-disappearing rows
  are deterministic and visibly distinct in the developer catalog.
- Chats shows no timer icon while the setting is off and one timer icon while
  it is on; mute and timer icons can appear together without replacing either.
- A direct conversation shows the active compact duration below its title.
- A group conversation shows member count, centered dot, timer icon, and active
  compact duration on one secondary line.
- Changing the timer in Chat Info updates Chats and the conversation header and
  turning it off removes the timer presentation.
- Static validation passes. Completion still requires user acceptance after
  hands-on inspection.
