# Visual-polish preparation

Status: **Chats + Settings direction accepted; all app-owned rollout batches
implemented at the static gate; device inspection remains pending**

Last verified against current official guidance: **2026-08-26**

Expressive is the approved direction, not a claim that every component already
uses the newest API family. The 2026-08-26 menu audit found baseline dropdowns,
medium task-button containers with baseline label/pressed-shape behavior, and
standard global motion. All six dropdown entry points now use official
Expressive menu APIs; the button and theme findings are documented follow-up,
not silently changed. See `screens/app-menus.md` and WN-ANDROID-0040.

## Implementation progress

- The shared Material theme now provides the approved typography and shape
  scales, standard vector action icons, destination top-bar hierarchy, empty
  state composition, and settings row/group primitives.
- Chats and the main Settings screen now implement the approved pilot
  direction. Their product state, mutations, and typed destinations remain
  unchanged except for moving account entry from Chats chrome to Settings.
- The accepted Settings refinement now uses compact one-line destination rows,
  consistent rounded Material Symbols, and disclosure chevrons. Redundant
  descriptions are gone; only the active Appearance value and an actionable
  unavailable Profile reason remain secondary. Group headings align to the
  32 dp internal content grid shared by overview icons and icon-free detail
  rows rather than to the tonal container boundary.
- The user accepted the cleaner Chats and Settings direction on 2026-08-21.
  This approves the visual language for rollout without claiming emulator or
  physical-device coverage by this implementation session.
- Welcome, Sign In, Sign Up, Add Profile, and the avatar-source task now use
  that language: restrained task hierarchy, bounded form measure, a clear
  tonal QR alternative, task-appropriate pinned or inline primary actions,
  compact progress, shared empty-state composition, and vector selected-state
  treatment. Sign Up's inline action and IME-aware scroll surface reflect its
  accepted Pixel 8a polish exception.
- New Message, Person Profile, New Group, and Set Up Group now extend the same
  system through search-first people lists, native selection chips, tonal
  relationship grouping, bounded forms/actions, photo feedback, validation,
  and direct relay recovery.
- The conversation shell now uses a small identity app bar, bounded transcript
  and composer measure, tonal sticky day hierarchy, distinct notice/event
  treatment, standard selection state, named retry recovery, and coordinated
  invitation, ended, blocked, and missing-relay panels.
- Composer and rich content now use one cohesive tonal task region, semantic
  attachment sheets and shelves, exact-page media viewers, standard media and
  file/contact icons, code-native voice waveforms, transcript provenance, and
  accessible playback/read-aloud progress. Platform pickers remain untouched.
- Message interactions and in-conversation search now use a contextual message
  preview, semantic command icons, adaptive reaction/emoji targets, native
  forwarding selection, compact selection/search bars, focused search with
  clear navigation, explicit current-result treatment, and tonal Message
  Details grouping.
- Chat and Group Info now use open identity headers, quiet tonal quick actions,
  grouped disclosure/action surfaces, native member selection, shared forms,
  and consequence-aware relay and membership states.
- Consumer Settings details now use the same quiet hierarchy: grouped switch,
  choice, value, and action rows; label-above forms; high-contrast technical QR
  surfaces; explanatory disabled/recovery states; and pinned primary tasks for
  Profile, Relays, Support, and Donate. Android-owned scanners, pickers,
  Sharesheet, document creation, and Settings remain visually untouched.
- Developer and destructive flows preserve technical density and exact model
  gates inside deliberate groups, one adaptive Diagnostics console, complete
  unavailable/empty states, label-above typed confirmation, and full-height
  sheets with pinned semantic-error actions and named progress.
- The clean gate passed on 2026-08-21 with 94 unit tests, 74 compiled Compose
  tests, zero lint issues, and both APKs. Coverage includes the shared tonal
  form-field content line and accessibility error semantics, on-demand search,
  menu scopes, avatar-to-Settings behavior, Settings-owned profile switching,
  onboarding alternate/avatar-source actions, creation validation and relay
  recovery, conversation identity/retry/lifecycle states, composer attachment
  and voice review semantics, message action/reaction/selection/forwarding and
  search controls, Settings dependency/private-data/QR/recovery semantics,
  developer empty/Live states, destructive typed gates, and large-text RTL
  composition.
- No emulator/device inspection was requested or performed. Rollout steps
  1–10 are statically implemented; device acceptance remains step 11.

## Feasibility verdict

The app can be modernized successfully without changing its established
features, state model, navigation graph, or information architecture. The
existing implementation already uses Compose, Material 3, semantic color
roles, typed navigation, standard Android system surfaces, and deterministic
state. The visual-polish work is therefore a presentation and composition
migration, not a product rewrite.

The appropriate target is a **quiet Material 3 Expressive** system: clear
hierarchy, selective scale, deliberate tonal containment, rounded shapes, and
native motion, while retaining White Noise's black, white, and neutral-gray
identity. Expressiveness must clarify tasks rather than add decoration.

## Fixed constraints

- Preserve product capabilities, accepted copy, navigation outcomes,
  deterministic data, state transitions, and the offline prototype boundary.
- Keep Kotlin, Jetpack Compose, Material 3, AndroidX, public platform APIs,
  one activity, and the existing unidirectional state ownership.
- Keep app-owned light and dark monochrome schemes. Dynamic color remains off.
- Keep semantic error and destructive roles; do not flatten them to gray.
- Keep system-owned Photo Picker, Files, camera, scanner, Sharesheet, document
  viewer, notification settings, and security settings visually system-owned.
- Do not reproduce iOS geometry, Pixel screenshots, Google branding, or Google
  product-specific typefaces.
- Do not treat this document as permission to add features, remove tested
  behavior, or introduce speculative architecture.
- Figma is not an implementation dependency or acceptance authority. The
  approved direction is recorded here and in the selected screen briefs.

## Approved pilot decisions

These decisions are settled for the first implementation batch:

- Chats uses a clean top app bar with active-profile avatar, active-scope
  title, filter action, and search action.
- The avatar opens Settings. Account switching lives in the Settings profile
  header, alongside Add Profile and profile management.
- Search is collapsed until the search action is invoked. A persistent search
  bar is reserved for screens where search is the primary task.
- Chats, Unread, Archived, and Left live in a Material dropdown menu. The
  current scope is conveyed by the visible title, menu selection semantics,
  and a tonal filter action for a non-default scope.
- Read All is not a persistent Chats app-bar action. The existing model
  operation is not deleted during visual work.
- The conversation list is a direct list surface without a card or rounded
  container around the whole list.
- Conversation rows use spacing, typography, avatars, metadata, and unread
  badges for hierarchy; they do not become individual decorative cards.
- Settings uses restrained tonal grouping and a prominent active-profile
  header without changing its destination hierarchy. Its overview rows are
  compact and icon-led; supporting text is reserved for current state or an
  actionable unavailable reason rather than repeating the destination label.

## Current implementation assessment

### What is already strong

- Thirty-two typed destinations preserve a coherent product structure.
- One authoritative `AppViewModel` owns profile, chat, settings, and developer
  state; presentation changes do not require a data-layer migration.
- Light and dark `ColorScheme` values cover the complete semantic Material
  role set and remain intentionally monochrome.
- Edge-to-edge scaffolds, safe insets, IME handling, bounded content width,
  RTL composition, and 200% font-scale coverage already exist.
- Material components and Android system contracts already own most controls,
  dialogs, sheets, pickers, sharing, and Back behavior.
- Product states and consequences have substantial unit and compiled Compose
  coverage, so visual changes can be checked against durable behavior.

### Largest gaps

- `MaterialTheme` supplies custom colors but no app typography or shape scale,
  so those two core design-system axes currently use uncoordinated defaults and
  screen-local overrides.
- Top bars are mostly center-aligned and screen-specific; primary, child,
  search, selection, and modal-task bars do not yet form one hierarchy.
- Settings rows rely on dividers, forced uppercase section labels, manual
  disabled alpha, and a text chevron instead of coordinated containment and
  native icon/state treatment.
- Chats still exposes the superseded tab/search/Read All composition in code.
- Several actions use Unicode glyphs for search, settings, overflow, add,
  warning, check, play, close, and chevrons instead of Material Symbols.
- The UI contains many legitimate component measurements but also 289 direct
  `dp` references, making relationship spacing and screen-local tuning hard to
  distinguish.
- Shape values cluster around several local 8/12/18/24 dp choices instead of
  a documented scale.
- Motion is mostly implicit or absent; state and mode changes do not yet share
  a deliberate transition language.
- Loading, empty, error, recovery, disabled, selected, pressed, and focused
  states exist, but their visual composition is not governed by a shared
  contract.
- Expanded-width behavior is bounded rather than fully adaptive; Chats ↔
  conversation and Settings category ↔ detail are candidates for native
  list-detail treatment during their own batches.

## Visual-system contract

### Color and surfaces

Retain the existing semantic monochrome schemes and migrate screens to roles,
not raw values:

| Purpose | Material role | Treatment |
| --- | --- | --- |
| Window and ordinary list background | `surface` / `background` | Quiet base with generous open space |
| Subtle grouped content | `surfaceContainerLow` | Settings groups, bounded supporting regions, form fill |
| Standard contained region | `surfaceContainer` | Secondary grouped content where low is insufficient |
| Expressive menus and ordinary sheets | `surfaceContainerLow` | Native standard menu surface and approved shared sheet surface; component-owned shadow only |
| Other raised transient content | Component default | Preserve each native dialog/tooltip's own hierarchy instead of forcing one surface role |
| Strong selected or primary emphasis | `primaryContainer` + `onPrimaryContainer` | Selected filter action, strong active states, limited hero containment |
| Primary task action | `primary` + `onPrimary` | One obvious task action per screen or task boundary |
| Supporting content | `onSurfaceVariant` | Summaries, timestamps, metadata, helper text |
| Boundaries | `outlineVariant` | Use sparingly; grouping should not depend on a divider after every row |
| Failure/destruction | `error` roles | Preserve visible text/icon/consequence; never color-only |

Tonal surfaces establish hierarchy before shadow. Ordinary lists remain flat.
Cards are used only when containment communicates a real group or object, not
as a universal wrapper.

### Typography

Use the Android/Material default Roboto family. Do not bundle Google Sans or a
brand-imitating substitute. Define one app `Typography` object and consume
semantic roles rather than ad hoc weights:

| Use | Role | Intended character |
| --- | --- | --- |
| Primary screen title | `headlineMedium` | 28/36, medium; reserved for top-level destinations |
| Child destination/app-bar title | `titleLarge` | 22/28, medium |
| Section or prominent object title | `titleMedium` | 16/24, medium |
| Row headline | `titleMedium` or `bodyLarge` | Weight changes only for real state such as unread |
| Body and row summary | `bodyMedium` | 14/20, regular |
| Long-form/body emphasis | `bodyLarge` | 16/24, regular |
| Button and menu label | `labelLarge` | Material component-owned |
| Timestamp, badge support, metadata | `labelMedium` / `labelSmall` | Compact but scalable |

Large display styles are reserved for onboarding or a true hero moment. Text
must wrap or recompose at large font scale; fixed-height text containers are
not allowed.

### Shape

Provide a single Material `Shapes` scale and let components consume it:

| Scale | Baseline radius | Typical use |
| --- | ---: | --- |
| Extra small | 4 dp | Tiny internal details only |
| Small | 8 dp | Thumbnails and compact contained details |
| Medium | 12 dp | Compact cards and message attachments |
| Large | 16 dp | Grouped settings surfaces and supporting containers |
| Extra large | 28 dp | Text inputs, dialogs, sheets, prominent expressive containers |
| Full | 50% | Avatars, icon buttons, pills, badges |

Use larger/full shapes selectively to emphasize a task or state. Do not place
rounded containers around every list or row. If the pinned Material library
offers stable expanded expressive shape roles, adopt them through the theme
rather than screen-local constants.

### Spacing and density

The existing 4/8 dp rhythm remains authoritative:

- 16 dp compact horizontal margin;
- 8 dp for closely related controls;
- 16 dp for peer fields or ordinary row-group separation;
- 24 dp between distinct sections;
- 16 dp pinned-action inset plus system and IME insets.

Material components own internal padding, target size, state layers, and
density. Add a spacing token only after a repeated relationship is proven;
do not turn every numeric measurement into a global token.

### Icons

- Use Material Symbols or existing brand-specific vectors, never Unicode as an
  icon substitute.
- Icon-only actions use the standard Material icon-button family and a minimum
  48 dp interactive target.
- Default icon buttons open search, menus, and navigation. Filled or filled
  tonal icon buttons communicate a selected or emphasized state, not mere
  decoration.
- Use one semantic icon for each action across the app. Mirror directional
  icons in RTL and supply localized accessible names.
- Decorative icons use null/cleared semantics; visible text is not repeated in
  a redundant content description.

### Elevation and containment

- Prefer tonal elevation for persistent hierarchy.
- Use component-owned shadow for menus, dialogs, sheets, and genuinely raised
  controls.
- Avoid stacked shadows, card-in-card compositions, and elevation on ordinary
  list rows.
- Keep scrims and modal elevation under Material component ownership.

### Motion

- Use Material component and Navigation Compose motion first.
- Animate meaningful state changes: entering/leaving search, filter selection,
  contextual selection mode, expanding task controls, list insertion/removal,
  and feedback after a recoverable action.
- Keep transitions short, spatially coherent, and interruptible. Do not add
  looping decorative motion or movement that competes with scrolling, IME, or
  predictive Back.
- Preserve stable lazy-list keys and animate only where identity is stable.
- Honor the system animation scale. Information and status must never depend
  on motion alone.

## Shared-component migration map

| Existing area | Treatment | Implementation target |
| --- | --- | --- |
| `WhiteNoiseTheme` colors | Retain | Add explicit typography and shapes; keep semantic monochrome roles and appearance switching |
| `WhiteNoiseSpacing` | Retain | Keep relationship tokens; replace repeated screen-local relationship values gradually |
| `AdaptiveContent` | Retain, then recompose where needed | Continue compact width bounds; adopt canonical panes only in Chats/conversation and Settings/detail batches |
| `ProfileAvatar` | Retain and restyle | Standardize sizes, monogram typography, selected/state treatment, and semantics |
| `WhiteNoiseButton` wrappers | Extend | Use Material's 56 dp medium height and matched padding for filled, tonal, and outlined task actions; primary loading actions retain semantic emphasis with contrasting progress and stable status text; keep contextual actions compact |
| `WhiteNoiseTextField` / `WhiteNoiseSecureTextField` | Shared Material-based form controls | Use a 28 dp full-rounded `surfaceContainerHigh` rest surface, transparent resting border, 2 dp focus/error ring, and one 16 dp directional line for label, input/icon artwork, and supporting text |
| `WhiteNoiseTopBar` | Recompose | Root, child, search, selection, and modal-task variants with Material icons and consistent hierarchy |
| Chats scope tabs | Replace | App-bar filter icon + shared Expressive menu; only non-default scopes show a title, with native selected-item semantics |
| All app-owned dropdowns | Migrated 2026-08-26 | `WhiteNoiseDropdownMenu` composes actual Google popup/group/new item APIs for all six entry points; WN-ANDROID-0040 |
| Chats persistent search field | Replace | App-bar search action entering a dedicated search mode |
| Chats Settings glyph/Read All | Replace/remove from chrome | Avatar opens Settings; no persistent Read All action; use Material Symbols |
| `ChatRow` | Recompose | Flat list row, clearer text/meta columns, exclusive status, long-press anchored menu and TalkBack actions; no visible ellipsis or horizontal swipe |
| Settings profile header | Recompose | Prominent active identity with Share & Connect and Switch Profile actions |
| `SettingsSection` | Replace | Sentence-case section label and tonal grouping; no forced uppercase |
| `SettingsLink` | Recompose | Material icon/chevron, native disabled semantics, optional value/summary, group-aware shape |
| `SettingsSwitch` | Recompose | One coherent toggleable row semantics; no redundant divider or duplicate TalkBack stop |
| `ChoiceRow` | Retain and recompose | Native radio semantics, visible selection, grouped containment |
| Modal sheets | Retain and restyle | Standard sheet behavior with consistent title, action rows, insets, and destructive treatment |
| Alert dialogs | Retain and restyle | Focused consequence copy, safe dismissal, semantic action hierarchy |
| Conversation shell | Recompose | Consistent top bar, restrained message surfaces, clearer day/event hierarchy, integrated composer |
| Composer/media/message actions | Recompose selectively | Preserve complex behavior; unify shapes, icons, state feedback, and action containment |
| Unicode action glyphs | Replace | Material Symbols or existing vector resources with localized semantics |

## Cross-app state contract

| State | Required treatment |
| --- | --- |
| Loading | Preserve surrounding layout; use component progress, useful status text, blocked duplicate submission, and accessible state description. A processing primary task retains `primary`/`onPrimary`; gray disabled treatment means genuinely unavailable, not merely busy. |
| Empty | One clear title, one supporting sentence, and at most one relevant recovery/creation action; no decorative empty card by default |
| Search empty | Keep the query and active scope visible; distinguish no results from a scope with no content |
| Error | Use semantic error role plus plain-language message and recovery; never show raw exception or rely on red alone |
| Recovery/offline capability | Name the unavailable capability and provide the accepted route to recover it without implying the entire app is offline |
| Disabled | Use Material's disabled treatment and semantics; retain supporting explanation when the reason is not obvious |
| Selected | Use semantic selected state plus shape/icon/text or check, not color alone |
| Pressed/focused/hovered | Keep Material state layers and visible keyboard/D-pad focus; do not override them with static custom fills |
| Destructive | Use consequence-aware copy, `error` roles, safe button ordering, and confirmation proportional to irreversibility |
| Success/transient feedback | Prefer Snackbar or stable inline confirmation; provide Undo for reversible mutations when the flow supports it |

## Screen and flow rollout

Each batch remains behavior-preserving and independently verifiable:

1. **Foundation — implemented at the static gate:** add theme
   typography/shapes, Material Symbol resources, top-bar variants, settings
   row primitives, state components, and focused theme/component tests. No
   product screen is declared visually complete.
2. **Chats + Settings roots — accepted direction and static gate:** implement the
   approved pilot composition, profile-to-Settings navigation, Settings
   account switching, scope menu, search mode, list rows, FAB,
   empty/search/recovery states, and dark theme.
3. **Onboarding + profile entry — implemented at the static gate:** Welcome,
   Sign In, Sign Up, scanner return, avatar sources, form/loading/error states,
   and Add Profile variants. Device-owned picker and scanner surfaces remain
   pending hands-on execution.
4. **Chat creation + person/group setup — implemented at the static gate:**
   New Message, Person Profile, member selection, group setup, validation,
   relay recovery, and completion states. System picker execution remains
   pending hands-on inspection.
5. **Conversation shell + lifecycle — implemented at the static gate:** app
   bar, timeline, day/event/notice hierarchy, invitation/ended/recovery states,
   composer boundary, and Back.
6. **Composer + rich content — implemented at the static gate:** text, reply,
   links, attachments, exact-page media viewers, files, contacts, GIF, voice
   review, loading/error feedback, transcript provenance, and playback/read-
   aloud progress. System picker and media handoff execution remain pending
   hands-on inspection.
7. **Message interactions + search — implemented at the static gate:**
   reaction surfaces, action sheet, selection, forwarding, details, deletion,
   focused contextual search, result highlighting/navigation, and accessible
   state feedback. Clipboard and keyboard behavior remain pending hands-on
   system inspection.
8. **Chat/group information — implemented at the static gate:** uncontained
   identity headers, tonal quick actions and grouped rows, shared-content
   states, native member selection, role-aware profile/group actions, group
   editing, isolated relay recovery, and destructive membership consequences.
9. **Settings details — implemented at the static gate:** Share & Connect,
   Profile, Keys, Notifications, Appearance, Privacy & Security, Data Usage,
   Relays, Support, and Donate, including selected, disabled, recovery, form,
   QR, and pinned-action states.
10. **Developer + destructive flows — implemented at the static gate:**
    Developer Tools, Diagnostics, Key Packages, Conversation Debug, Sign Out,
    Manage Profiles, and Erase App Data, keeping technical density distinct
    from consumer settings and typed consequences explicit.
11. **Device acceptance:** reference-device screenshots and interaction,
    TalkBack, keyboard/D-pad, compact/expanded, rotation/resizing, 200% font,
    display scale, light/dark, RTL, gesture/three-button navigation, IME,
    system surfaces, and motion-scale checks.

## Implementation gates

Every batch must satisfy all of the following before the next batch becomes
the visual reference:

- Product copy, mutations, deterministic states, navigation, and Back behavior
  remain unchanged unless explicit user direction says otherwise.
- New composition uses semantic theme roles and the shared shape/type/spacing
  system; no unexplained local color, icon glyph, radius, or relationship
  spacing is introduced.
- Primary action and active state are visually obvious without relying only on
  color, position, or motion.
- Loading, empty, search-empty, error/recovery, disabled, selected, pressed,
  focused, and destructive states relevant to the batch are implemented.
- Interactive targets are at least 48 dp and remain usable with touch,
  keyboard, mouse, Switch Access, Voice Access, and TalkBack.
- Layout works at compact width, 200% font, dark theme, and RTL. Relevant
  list/detail flows are checked at expanded width without simply stretching.
- Unit and Compose tests cover durable behavior and semantics rather than
  screenshot coordinates.
- `testDebugUnitTest`, `lintDebug`, `assembleDebug`, and
  `assembleDebugAndroidTest` pass for meaningful batches.
- Visual completion is claimed only after explicit device/emulator inspection
  and user acceptance.

## Risks and controls

- **Large change surface:** keep batches small and migrate shared primitives
  before consumers; do not perform a blind global replacement.
- **Behavior regressions from recomposition:** retain callback/state contracts
  and add tests before removing old composition.
- **Experimental expressive APIs:** prefer stable Material components in the
  pinned BOM. Adopt an experimental API only when it materially improves an
  approved need and the risk is recorded in that batch.
- **Over-containment:** use surface hierarchy to group related settings and
  tasks, while keeping lists and conversation timelines visually open.
- **Over-expressiveness:** reserve large shape, scale, and motion for primary
  actions, selected state, and meaningful task transitions.
- **Accessibility loss from custom rows:** favor native components and inspect
  the merged/unmerged semantics tree before adding manual semantics.
- **Adaptive scope creep:** canonical panes may recompose presentation at
  expanded widths but must not add destinations or change compact navigation.

## Current official sources

- [Material Design 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3)
- [Compose design systems](https://developer.android.com/develop/ui/compose/designsystems)
- [Material app bars](https://developer.android.com/develop/ui/compose/components/app-bars)
- [Material menus](https://developer.android.com/develop/ui/compose/components/menu)
- [Material search bar](https://developer.android.com/develop/ui/compose/components/search-bar)
- [Material icon buttons](https://developer.android.com/develop/ui/compose/components/icon-button)
- [Material 3 insets](https://developer.android.com/develop/ui/compose/system/material-insets)
- [Compose accessibility](https://developer.android.com/develop/ui/compose/accessibility)
- [Compose accessibility defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults)
- [Adaptive apps](https://developer.android.com/develop/adaptive-apps)
- [Canonical adaptive layouts](https://developer.android.com/develop/adaptive-apps/guides/canonical-layouts)
- [Google's Material 3 Expressive research](https://design.google/library/expressive-material-design-google-research)
