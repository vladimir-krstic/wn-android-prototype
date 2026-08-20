# Official Apple reference index

Last full verification: **2026-07-31**. Developer Tools sources were
reverified on **2026-08-06**. Performance and testing sources were reverified
on **2026-08-15**.

This is the routing index for material platform decisions. Open the relevant
live Apple source before choosing or recreating a component, interaction,
system integration, motion treatment, or accessibility behavior. Apply the
evaluation process in `docs/references/native-ui.md`, then record the governing
links and resulting decision in the selected screen brief.

All links below are official Apple sources. The user’s latest direction remains
the highest authority; document any approved exception from Apple’s default
pattern in the screen brief.

## Contents

1. [Design foundations and resources](#1-design-foundations-and-resources)
2. [Interface writing, feedback, and privacy](#2-interface-writing-feedback-and-privacy)
3. [Navigation and presentation](#3-navigation-and-presentation)
4. [Lists, forms, settings, search, and menus](#4-lists-forms-settings-search-and-menus)
5. [Buttons, selection, fields, and standard controls](#5-buttons-selection-fields-and-standard-controls)
6. [Toolbars, Liquid Glass, symbols, and motion](#6-toolbars-liquid-glass-symbols-and-motion)
7. [Photos, files, sharing, camera, and permissions](#7-photos-files-sharing-camera-and-permissions)
8. [Accessibility, text adaptation, and localization](#8-accessibility-text-adaptation-and-localization)
9. [Testing, responsiveness, and performance](#9-testing-responsiveness-and-performance)
10. [Xcode 27 and current SwiftUI](#10-xcode-27-and-current-swiftui)

## 1. Design foundations and resources

| Source | Governs | Open when | Current areas | Verified |
| --- | --- | --- | --- | --- |
| [Human Interface Guidelines](https://developer.apple.com/design/human-interface-guidelines/) | Platform hierarchy, harmony, consistency, patterns, and components | Starting any material UI or interaction decision | All screens | 2026-07-31 |
| [Design principles](https://developer.apple.com/design/human-interface-guidelines/design-principles) | Purpose, familiarity, feedback, recovery, responsibility, and inclusive design | Evaluating product quality or competing design directions | All flows | 2026-07-31 |
| [Apple Design Resources](https://developer.apple.com/design/resources/) | Official templates, fonts, symbols, and production resources | Preparing platform-accurate visual or icon work | Brand and visual foundations | 2026-07-31 |
| [Color](https://developer.apple.com/design/human-interface-guidelines/color) | Adaptive system colors across appearances and accessibility settings | Selecting a semantic fill instead of guessing a fixed Messages color | Conversation bubbles | 2026-08-12 |
| [systemGray](https://developer.apple.com/documentation/uikit/uicolor/systemgray) | Adaptive standard base gray | Keeping a native switch's on track distinct from its white thumb in a grayscale Dark appearance | Privacy & Security | 2026-08-15 |
| [systemGray5](https://developer.apple.com/documentation/uikit/uicolor/systemgray5) | Adaptive fifth-level gray, lighter than `systemGray4` in light appearance | Applying the approved lighter incoming-message fill | Conversation bubbles | 2026-08-12 |
| [Principles of great design](https://developer.apple.com/videos/play/wwdc2026/250/) | Current Apple product-design evaluation | Reviewing whether a flow feels intentional and coherent | Milestone reviews | 2026-07-31 |
| [Communicate your brand identity on iOS](https://developer.apple.com/videos/play/wwdc2026/251/) | Brand expression within native iOS structure | Applying White Noise identity without replacing platform conventions | Brand elements, onboarding | 2026-07-31 |
| [Create UI prototypes using agents in Xcode](https://developer.apple.com/videos/play/wwdc2026/227/) | Realistic content, interaction iteration, and tuning key moments | Planning or refining prototype work | Prototype workflow | 2026-07-31 |

## 2. Interface writing, feedback, and privacy

| Source | Governs | Open when | Current areas | Verified |
| --- | --- | --- | --- | --- |
| [Writing for interfaces](https://developer.apple.com/videos/play/wwdc2022/10037/) | Purposeful, contextual, empathetic interface language | Writing titles, actions, help, empty states, or recovery | All product copy | 2026-07-31 |
| [Alerts](https://developer.apple.com/design/human-interface-guidelines/alerts) | Alert necessity, titles, messages, actions, and tone | Adding interruption, warning, or destructive confirmation | Settings, Chats, onboarding, Developer Tools | 2026-08-10 |
| [Managing accounts](https://developer.apple.com/design/human-interface-guidelines/managing-accounts) | Clear account exit and deletion consequences | Designing sign-out or removal flows | Sign Out, app-data erasure | 2026-08-05 |
| [Feedback](https://developer.apple.com/design/human-interface-guidelines/feedback) | Proportional, accessible status and result feedback | Confirming progress, success, failure, or correction | Copy actions, sending, relay recovery | 2026-08-03 |
| [systemOrange](https://developer.apple.com/documentation/uikit/uicolor/systemorange) | Adaptive system orange for warning states | Presenting warning guidance without fixed RGB values | Relays, Chats recovery, conversation recovery, app-data erasure | 2026-08-05 |
| [systemRed](https://developer.apple.com/documentation/uikit/uicolor/systemred) | Adaptive system red for errors, destructive actions, and critical status | Presenting the user-approved disconnected endpoint status and destructive relay actions without fixed RGB values | Relays | 2026-08-03 |
| [Standard colors](https://developer.apple.com/documentation/uikit/standard-colors) | Adaptive system hues across appearances and accessibility settings | Selecting a system hue instead of a fixed RGB value | Status, recovery, group-author identity, and monogram avatar surfaces | 2026-08-12 |
| [Privacy](https://developer.apple.com/design/human-interface-guidelines/privacy) | Contextual permission requests, data-use transparency, and recovery | Requesting protected access or explaining external data sharing before it occurs | QR scanning, profile media, Sign Up web-image search | 2026-08-07 |
| [Local Authentication](https://developer.apple.com/documentation/localauthentication) | System biometric and device-credential authentication | Designing Require Face ID or access to sensitive content | Privacy & Security | 2026-08-04 |
| [Device-owner authentication](https://developer.apple.com/documentation/localauthentication/lapolicy/deviceownerauthentication) | Face ID-first authentication with iPhone-passcode fallback | Choosing the Require Face ID authentication policy | Privacy & Security | 2026-08-04 |
| [canEvaluatePolicy](https://developer.apple.com/documentation/localauthentication/lacontext/canevaluatepolicy(_:error:)) | Live authentication-policy availability and prerequisite checks | Handling missing device passcode or unavailable authentication | Privacy & Security | 2026-08-04 |
| [Preparing your UI to run in the background](https://developer.apple.com/documentation/uikit/preparing-your-ui-to-run-in-the-background) | Sensitive-content removal before UIKit creates the app-switcher snapshot | Implementing Hide Screen in App Switcher | Privacy & Security | 2026-08-04 |

## 3. Navigation and presentation

| Source | Governs | Open when | Current areas | Verified |
| --- | --- | --- | --- | --- |
| [Navigation and search](https://developer.apple.com/design/human-interface-guidelines/navigation-and-search) | Hierarchy, context preservation, predictable movement, and search | Choosing push, modal, search, or top-level structure | All flows | 2026-07-31 |
| [NavigationStack](https://developer.apple.com/documentation/swiftui/navigationstack) | SwiftUI stack navigation and route ownership | Implementing hierarchical destinations | Chats, conversation, Settings | 2026-07-31 |
| [NavigationLink](https://developer.apple.com/documentation/swiftui/navigationlink) | Native disclosure and value-driven navigation | Making a row or control reveal a destination | Chats, Settings, Developer Tools | 2026-08-06 |
| [ToolbarItemPlacement.primaryAction](https://developer.apple.com/documentation/swiftui/toolbaritemplacement/primaryaction) | Primary toolbar action placement in a pushed destination | Placing Profile's Edit and Done actions without modal confirmation semantics | Profile editing | 2026-08-03 |
| [ToolbarItemPlacement.confirmationAction](https://developer.apple.com/documentation/swiftui/toolbaritemplacement/confirmationaction) | Trailing confirmation placement for a modal task | Applying staged composer-media inclusion changes | Conversation media viewer | 2026-08-14 |
| [ToolbarItemPlacement.cancellationAction](https://developer.apple.com/documentation/swiftui/toolbaritemplacement/cancellationaction) | Leading cancellation placement that dismisses a modal without applying its action | Discarding staged composer-media inclusion changes | Conversation media viewer | 2026-08-14 |
| [navigationBarBackButtonHidden](https://developer.apple.com/documentation/swiftui/view/navigationbarbackbuttonhidden(_:)) | Replacing the system Back action during a bounded edit state | Temporarily providing Cancel instead of leaving with an active draft | Profile editing | 2026-08-03 |
| [Sheets](https://developer.apple.com/design/human-interface-guidelines/sheets) | Scoped modal tasks, system-owned geometry, and interactive dismissal | Deciding whether work belongs in a native sheet | Sign In, Sign Up, switcher, destructive confirmation, conversation camera and media viewer | 2026-08-14 |
| [Modality](https://developer.apple.com/design/human-interface-guidelines/modality) | Focused modal tasks, temporary full-screen media experiences, explicit dismissal, and avoiding data loss | Designing a dedicated confirmation or media-viewing context | Conversation media viewer, Sign Out and wiping | 2026-08-14 |
| [sheet](https://developer.apple.com/documentation/swiftui/view/sheet(ispresented:ondismiss:content:)) | System-owned card presentation and binding-driven interactive dismissal | Presenting the user-approved swipe-dismissible conversation camera | Conversation camera | 2026-08-11 |
| [sheet(item:onDismiss:content:)](https://developer.apple.com/documentation/swiftui/view/sheet%28item%3Aondismiss%3Acontent%3A%29) | Item-driven sheet presentation with system-owned interactive dismissal | Opening the unified media viewer on the exact selected chat item | Conversation, Chat Info | 2026-08-14 |
| [fullScreenCover(item:onDismiss:content:)](https://developer.apple.com/documentation/swiftui/view/fullscreencover%28item%3Aondismiss%3Acontent%3A%29) | Binding-driven full-screen modal presentation from identifiable data | Opening the composer media viewer on the exact selected draft item | Conversation | 2026-08-14 |
| [presentationDetents](https://developer.apple.com/documentation/swiftui/view/presentationdetents(_:selection:)) | Native medium and large sheet heights with controlled selection | Choosing a sheet's supported resting heights | Onboarding, profile switcher, Chat Relays, conversation media viewer | 2026-08-14 |
| [interactiveDismissDisabled](https://developer.apple.com/documentation/swiftui/view/interactivedismissdisabled(_:)) | Conditional control of a sheet's built-in drag-down dismissal | Preventing interactive media-viewer dismissal while the current image is zoomed | Conversation, Chat Info | 2026-08-14 |
| [PresentationContentInteraction](https://developer.apple.com/documentation/swiftui/presentationcontentinteraction) | Sheet resizing versus scrolling priority | Resolving nested sheet/content gestures | Onboarding, profile switcher | 2026-07-31 |
| [Presentation modifiers](https://developer.apple.com/documentation/swiftui/view-presentation) | SwiftUI sheet, cover, detent, background, and interaction APIs | Selecting a presentation API or behavior | Modal flows | 2026-07-31 |
| [confirmationDialog](https://developer.apple.com/documentation/swiftui/view/confirmationdialog(_:ispresented:titlevisibility:presenting:actions:message:)) | Native action-choice and confirmation presentation | Presenting intentional choices without a custom action sheet | Chats, Settings | 2026-07-31 |
| [Action sheets](https://developer.apple.com/design/human-interface-guidelines/action-sheets) | Compact choices related to a deliberate action | Choosing between a confirmation dialog, menu, or alert | Chats | 2026-07-31 |
| [UIAlertController](https://developer.apple.com/documentation/uikit/uialertcontroller) | UIKit alert and action-sheet presentation | Implementing a native confirmation from UIKit | Native Chats | 2026-07-31 |

## 4. Lists, forms, settings, search, and menus

| Source | Governs | Open when | Current areas | Verified |
| --- | --- | --- | --- | --- |
| [Settings](https://developer.apple.com/design/human-interface-guidelines/settings) | Settings hierarchy, restraint, and system preference respect | Adding or reorganizing a preference | Settings, Developer Tools | 2026-08-06 |
| [Lists and tables](https://developer.apple.com/design/human-interface-guidelines/lists-and-tables) | Row hierarchy, selection, editing, disclosure, and scanning | Designing list content or row interactions | Chats, relays, profiles, chat creation | 2026-08-09 |
| [Label](https://developer.apple.com/documentation/swiftui/label) | Standard icon-and-title composition in lists and navigation rows | Pairing a concise action label with an SF Symbol | Chat creation | 2026-08-09 |
| [Form](https://developer.apple.com/documentation/swiftui/form) | Native grouped data entry and settings structure | Building editable fields or preference groups | Sign Up, Settings, Developer Tools, Chat Info | 2026-08-10 |
| [listSectionSpacing](https://developer.apple.com/documentation/swiftui/view/listsectionspacing(_:)) | Native spacing between List sections | Aligning related Chat Info section rhythm without custom containers | Chat Info, Group Info | 2026-08-10 |
| [Section](https://developer.apple.com/documentation/swiftui/section) | Native hierarchy with optional headers and surface footers | Grouping related settings and placing supporting copy outside row containers | Settings, Chat Info | 2026-08-10 |
| [Disclosure controls](https://developer.apple.com/design/human-interface-guidelines/disclosure-controls) | Progressive disclosure, labels, and revealed detail | Deciding whether technical detail belongs inline or in a destination | Developer Tools | 2026-08-06 |
| [List](https://developer.apple.com/documentation/swiftui/list) | Native rows, scrolling, separators, selection, and deletion | Implementing standard single-column collections | Settings, switcher | 2026-07-31 |
| [Displaying data in lists](https://developer.apple.com/documentation/swiftui/displaying-data-in-lists) | SwiftUI identity, hierarchy, and list composition | Structuring dynamic or hierarchical data | Settings, relay lists | 2026-07-31 |
| [Search fields](https://developer.apple.com/design/human-interface-guidelines/search-fields) | Search-field purpose, placement, prompts, and immediate results | Adding search over an image, message, or chat collection | Chats, conversations, Sign Up, Profile, media forwarding | 2026-08-15 |
| [Search modifiers](https://developer.apple.com/documentation/swiftui/view-search) | Native search presentation and activation | Adding searchable content | Chats, conversations, media forwarding | 2026-08-15 |
| [Adding a search interface](https://developer.apple.com/documentation/swiftui/adding-a-search-interface-to-your-app) | Search placement, suggestions, scopes, and programmatic state | Implementing a complete SwiftUI search experience | Chats, conversations | 2026-08-15 |
| [UISearchBar](https://developer.apple.com/documentation/uikit/uisearchbar) | Native search text entry, prompt, and in-field clear behavior | Hosting a search field whose query remains after keyboard dismissal | Sign Up, Profile, conversations | 2026-08-15 |
| [UISearchBarDelegate](https://developer.apple.com/documentation/uikit/uisearchbardelegate) | Native search text and editing-focus callbacks | Binding search text and keyboard focus to SwiftUI state | Sign Up, Profile, conversations | 2026-08-15 |
| [Scroll edge effect style](https://developer.apple.com/documentation/swiftui/view/scrolledgeeffectstyle(_:for:)) | System-owned soft and hard transitions where scrolling content meets an overlay | Letting results continue beneath a search or toolbar surface without a hard visual cutoff | Sign Up, Profile, conversations | 2026-08-07 |
| [Design intuitive search experiences](https://developer.apple.com/videos/play/wwdc2026/292/) | Search entry, result hierarchy, scope, and recovery | Evaluating search behavior beyond API mechanics | Chats and future people search | 2026-07-31 |
| [Menus HIG](https://developer.apple.com/design/human-interface-guidelines/menus) | Menu purpose, organization, labels, and destructive roles | Deciding whether secondary commands belong in a menu | Chats, onboarding, Developer Tools | 2026-08-06 |
| [Menu](https://developer.apple.com/documentation/swiftui/menu) | Native disclosure of a compact group of commands with an app-supplied label | Keeping mutually exclusive voice-message formats behind one selected-value label and downward chevron | Conversation, Developer Tools | 2026-08-15 |
| [menuActionDismissBehavior](https://developer.apple.com/documentation/swiftui/view/menuactiondismissbehavior(_:)) | Whether a menu dismisses after performing an action | Ensuring a selected command closes its menu before another system presentation | Conversation | 2026-08-11 |
| [UIButton menu](https://developer.apple.com/documentation/uikit/uibutton/menu) | A system-owned menu attached to a UIKit button | Presenting the conversation attachment menu as the button's primary action | Conversation | 2026-08-11 |
| [showsMenuAsPrimaryAction](https://developer.apple.com/documentation/uikit/uicontrol/showsmenuasprimaryaction) | Presents a control's context menu from its primary touch interaction | Keeping the voice-format selector native while observing the complete menu lifecycle | Conversation speech messages | 2026-08-15 |
| [UIContextMenuInteraction](https://developer.apple.com/documentation/uikit/uicontextmenuinteraction) | UIKit ownership of contextual menu interaction, preview, and action delivery | Evaluating a message context presentation and coordinating the compact attachment menu | Conversation, message actions | 2026-08-13 |
| [UIContextMenuInteractionDelegate](https://developer.apple.com/documentation/uikit/uicontextmenuinteractiondelegate) | Context-menu preview and display lifecycle callbacks | Evaluating public preview customization and preventing the attachment menu's bottom row from activating the composer beneath it | Conversation, message actions | 2026-08-13 |
| [Menus and commands](https://developer.apple.com/documentation/swiftui/menus-and-commands) | Native compact command and option groups | Exposing secondary actions or filters | Chats, profile actions, Developer Tools | 2026-08-06 |
| [Populating SwiftUI menus with adaptive controls](https://developer.apple.com/documentation/SwiftUI/Populating-SwiftUI-menus-with-adaptive-controls) | Labels, symbols, selection, and destructive menu roles | Building nontrivial menus | Chats filters, avatar actions | 2026-07-31 |
| [EditButton](https://developer.apple.com/documentation/swiftui/editbutton) | Native entry and exit for edit mode | Allowing list deletion or reordering | Relays | 2026-07-31 |
| [LabeledContent](https://developer.apple.com/documentation/swiftui/labeledcontent) | Semantic label/value alignment | Presenting settings values or compact statuses | Settings, relays | 2026-07-31 |

### UIKit list implementation

Open these only when a selected screen genuinely needs UIKit list behavior that
SwiftUI cannot provide cleanly. They do not authorize a parallel component
system.

| Source | Governs | Open when | Current areas | Verified |
| --- | --- | --- | --- | --- |
| [UICollectionLayoutListConfiguration](https://developer.apple.com/documentation/uikit/uicollectionlayoutlistconfiguration) | Native collection-view list appearance and behavior | Configuring the native Chats collection layout | Native Chats | 2026-07-31 |
| [Updating collection views using diffable data sources](https://developer.apple.com/documentation/uikit/updating-collection-views-using-diffable-data-sources) | Stable UIKit collection updates | Applying chat snapshots without manual index mutations | Native Chats | 2026-07-31 |
| [UIHostingConfiguration](https://developer.apple.com/documentation/swiftui/uihostingconfiguration) | SwiftUI content hosted inside UIKit cells | Using local SwiftUI row content in a native collection cell | Native Chats | 2026-07-31 |
| [UICellConfigurationState](https://developer.apple.com/documentation/uikit/uicellconfigurationstate) | Native cell state and custom state keys | Keeping hosted rows synchronized with UIKit state | Native Chats | 2026-07-31 |
| [UIContextualAction](https://developer.apple.com/documentation/uikit/uicontextualaction) | A single contextual row action | Adding an approved swipe action | Native Chats | 2026-07-31 |
| [UISwipeActionsConfiguration](https://developer.apple.com/documentation/uikit/uiswipeactionsconfiguration) | Leading and trailing swipe-action groups | Configuring approved row swipe behavior | Native Chats | 2026-07-31 |
| [UIScrollViewDelegate](https://developer.apple.com/documentation/uikit/uiscrollviewdelegate) | UIKit scrolling lifecycle callbacks | Coordinating an approved navigation or scroll effect | Native Chats | 2026-07-31 |
| [contentInsetAdjustmentBehavior](https://developer.apple.com/documentation/uikit/uiscrollview/contentinsetadjustmentbehavior-swift.property) | Safe-area inset adjustment for UIKit scroll views | Resolving content and navigation-bar insets | Native Chats | 2026-07-31 |
| [topEdgeEffect](https://developer.apple.com/documentation/uikit/uiscrollview/topedgeeffect) | System scroll-edge treatment at the top edge | Integrating the current navigation material with UIKit content | Native Chats | 2026-07-31 |
| [UIScrollEdgeEffect.Style.hard](https://developer.apple.com/documentation/uikit/uiscrolledgeeffect/style-swift.class/hard) | Hard system scroll-edge transition | Choosing the documented hard edge for an approved screen | Native Chats | 2026-07-31 |
| [UIScrollEdgeEffect isHidden](https://developer.apple.com/documentation/uikit/uiscrolledgeeffect/ishidden) | Visibility of a system scroll-edge effect | Suppressing a redundant system edge effect | Native Chats | 2026-07-31 |

## 5. Buttons, selection, fields, and standard controls

| Source | Governs | Open when | Current areas | Verified |
| --- | --- | --- | --- | --- |
| [Buttons HIG](https://developer.apple.com/design/human-interface-guidelines/buttons) | Button hierarchy, labels, roles, prominence, and 44-point hit regions | Choosing or reviewing an action treatment | All screens | 2026-08-14 |
| [Button](https://developer.apple.com/documentation/swiftui/button) | Native activation, feedback, focus, and accessibility | Adding any actionable control | All screens | 2026-07-31 |
| [DragGesture](https://developer.apple.com/documentation/swiftui/draggesture) | Native continuous drag values for direct manipulation | Tracking the approved vertical composer-expansion interaction | Conversation | 2026-08-14 |
| [contentShape(_:eoFill:)](https://developer.apple.com/documentation/swiftui/view/contentshape(_:eofill:)) | Defining the complete hit-testing shape of a view | Ensuring the visible expanded composer, rather than the covered transcript, owns a pull that begins anywhere in its bounds | Conversation | 2026-08-14 |
| [simultaneousGesture(_:including:)](https://developer.apple.com/documentation/swiftui/view/simultaneousgesture(_:including:)) | Simultaneous gesture recognition with explicit subtree participation | Preserving vertical timeline scrolling and message-content controls beside swipe to reply | Conversation | 2026-08-14 |
| [LongPressGesture](https://developer.apple.com/documentation/swiftui/longpressgesture) | Native recognition after a minimum hold duration and within an allowable movement distance | Requiring deliberate press-and-hold activation without competing with the empty composer's pull gesture | Conversation voice recording | 2026-08-15 |
| [onLongPressGesture](https://developer.apple.com/documentation/swiftui/view/onlongpressgesture(minimumduration:maximumdistance:perform:onpressingchanged:)) | Native hold timing, allowable movement, and press-state callbacks | Making voice recording tolerant of ordinary finger drift while preserving intentional activation | Conversation voice recording | 2026-08-15 |
| [UIGestureRecognizerRepresentable](https://developer.apple.com/documentation/swiftui/uigesturerecognizerrepresentable) | Public integration of a UIKit gesture recognizer into a SwiftUI hierarchy | Coordinating message hold and reply gestures explicitly with transcript scrolling | Conversation, message actions | 2026-08-15 |
| [UILongPressGestureRecognizer](https://developer.apple.com/documentation/uikit/uilongpressgesturerecognizer) | Native long-press timing, allowable movement, cancellation, and touch delivery | Requiring an intentional bubble hold without blocking a vertical pan | Message actions | 2026-08-15 |
| [UIPanGestureRecognizer](https://developer.apple.com/documentation/uikit/uipangesturerecognizer) | Native continuous pan translation and velocity | Tracking swipe to reply only after direction gating succeeds | Conversation | 2026-08-15 |
| [highPriorityGesture](https://developer.apple.com/documentation/swiftui/view/highprioritygesture(_:including:)) | Parent gesture precedence over gestures defined by the view and its subviews | Giving the full composer glass one pull owner | Conversation | 2026-08-14 |
| [ButtonBorderShape](https://developer.apple.com/documentation/swiftui/buttonbordershape) | System-owned button geometry | Choosing a supported bordered shape | Onboarding actions, conversation search | 2026-08-15 |
| [Picker](https://developer.apple.com/documentation/swiftui/picker) | Native single-choice selection | Selecting a mode or preference | Settings, Share & Connect, Sign Up | 2026-08-07 |
| [Palette picker style](https://developer.apple.com/documentation/swiftui/pickerstyle/palette) | Compact mutually exclusive palette selection | Presenting closely related modes in a toolbar | Share & Connect, Sign Up | 2026-08-07 |
| [Tabs picker style](https://developer.apple.com/documentation/swiftui/tabspickerstyle) | Semantically announcing a mutually exclusive picker as tabs | Evaluating related content categories inside Chat Info | Chat Info, Group Info | 2026-08-09 |
| [Segmented picker style](https://developer.apple.com/documentation/swiftui/pickerstyle/segmented) | Native mutually exclusive segmented selection for two to five options | Comparing visible category selection inside Chat Info | Chat Info, Group Info | 2026-08-15 |
| [Palette picker style](https://developer.apple.com/documentation/swiftui/palettepickerstyle) | Native row of compact picker elements | Comparing compact public category controls inside Chat Info | Chat Info, Group Info | 2026-08-09 |
| [Menu picker style](https://developer.apple.com/documentation/swiftui/pickerstyle/menu) | Native menu-based single selection whose button shows the current option | Comparing compact single-choice presentation in information categories | Chat Info, Group Info | 2026-08-15 |
| [TabView](https://developer.apple.com/documentation/swiftui/tabview) | Native switching between child views through interactive controls | Building the selectable Chat Info and Group Info category pages | Chat Info, Group Info | 2026-08-09 |
| [PageTabViewStyle](https://developer.apple.com/documentation/swiftui/pagetabviewstyle) | Native paged scrolling and horizontal swipe behavior for a `TabView` | Giving chat information categories system-owned page motion | Chat Info, Group Info | 2026-08-09 |
| [LazyVGrid](https://developer.apple.com/documentation/swiftui/lazyvgrid) | Lazy multicolumn layout within a vertical scroll view | Presenting a regular app-owned thumbnail grid | Sign Up web-image search, Chat Info shared media | 2026-08-10 |
| [Segmented controls](https://developer.apple.com/design/human-interface-guidelines/segmented-controls) | Appropriate use of mutually exclusive segments | Comparing segmented and palette patterns | Share & Connect | 2026-07-31 |
| [Toggle](https://developer.apple.com/documentation/swiftui/toggle) | Native binary preferences and semantics | Adding an on/off setting | Settings, Developer Tools | 2026-08-06 |
| [Entering data](https://developer.apple.com/design/human-interface-guidelines/entering-data) | Clear, minimal, forgiving data-entry tasks | Designing typed confirmation or other consequential input | Sign Out, app-data erasure | 2026-08-05 |
| [Text fields HIG](https://developer.apple.com/design/human-interface-guidelines/text-fields) | Field purpose, labels, affordances, and validation | Designing any text-entry experience | Onboarding, conversation, Settings | 2026-07-31 |
| [TextField](https://developer.apple.com/documentation/swiftui/textfield) | Native editable text, focus, submission, and multiline entry | Adding ordinary text entry | Sign Up, conversation, Settings | 2026-08-13 |
| [SpeechAnalyzer](https://developer.apple.com/documentation/speech/speechanalyzer) | Asynchronous analysis of live or prerecorded spoken audio | Evaluating the production path for voice-message transcription while keeping this prototype deterministic | Conversation speech messages | 2026-08-15 |
| [SpeechTranscriber](https://developer.apple.com/documentation/speech/speechtranscriber) | General-purpose speech-to-text transcription with device and locale availability checks | Evaluating the production transcription module and unsupported-language state | Conversation speech messages | 2026-08-15 |
| [AVSpeechSynthesizer](https://developer.apple.com/documentation/avfaudio/avspeechsynthesizer) | Native text-to-speech playback, pause, stop, state, and delegate events | Reading a received text message aloud | Conversation speech messages | 2026-08-15 |
| [UITextView](https://developer.apple.com/documentation/uikit/uitextview) | Native multiline attributed-text editing, selection, keyboard behavior, and TextKit access | Styling semantic ranges inside the editable conversation composer | Conversation | 2026-08-13 |
| [NSAttributedString.TextHighlightStyle](https://developer.apple.com/documentation/foundation/nsattributedstring/texthighlightstyle) | System-defined inline text emphasis with a background and contrasting foreground | Giving complete composer mentions native rounded emphasis | Conversation | 2026-08-13 |
| [textHighlightAttributes](https://developer.apple.com/documentation/uikit/uitextview/texthighlightattributes) | Adaptive foreground and background attributes used by TextKit highlights | Matching composer mention emphasis to the local semantic gray treatment | Conversation | 2026-08-13 |
| [lineLimit](https://developer.apple.com/documentation/swiftui/view/linelimit(_:reservesspace:)) | Maximum visible lines and native scrolling for a vertical TextField | Capping a growing conversation composer | Conversation | 2026-08-11 |
| [SecureField](https://developer.apple.com/documentation/swiftui/securefield) | Native obscured sensitive entry | Entering private keys or backup passwords | Sign In, Profile Keys | 2026-07-31 |
| [Focus](https://developer.apple.com/documentation/swiftui/focus) | SwiftUI focus state, movement, and focused values | Coordinating keyboard focus or restoration | Sign In, Sign Up, conversation, chat creation | 2026-08-09 |
| [scrollDismissesKeyboard](https://developer.apple.com/documentation/swiftui/scrolldismisseskeyboardmode/interactively) | Interactive keyboard dismissal driven by scrolling | Letting scrollable content move the software keyboard away | Chat creation, conversation | 2026-08-14 |
| [UIGestureRecognizerDelegate](https://developer.apple.com/documentation/uikit/uigesturerecognizerdelegate) | Filtering touches and coordinating recognition between bounded UIKit gestures | Adding approved tap-outside keyboard dismissal, allowing a bubble hold beside transcript panning, or rejecting a vertical reply pan before recognition | Chat creation, conversation, message actions | 2026-08-15 |
| [cancelsTouchesInView](https://developer.apple.com/documentation/uikit/uigesturerecognizer/cancelstouchesinview) | Preserving delivery of recognized touches to their views | Keeping buttons and navigation active while a background tap ends editing | Chat creation | 2026-08-09 |
| [disabled](https://developer.apple.com/documentation/swiftui/view/disabled(_:)) | Native disabled interaction and accessibility state | Preventing an action until its requirements are met | Onboarding and forms | 2026-07-31 |
| [isEnabled](https://developer.apple.com/documentation/swiftui/environmentvalues/isenabled) | Reading enabled state from the environment | Adapting a local component to system-disabled behavior | Onboarding controls | 2026-07-31 |
| [TextFieldStyle](https://developer.apple.com/documentation/swiftui/textfieldstyle) | System-provided field styles | Evaluating whether a standard field treatment fits | Sign In, forms | 2026-07-31 |
| [textInputBorderShape](https://developer.apple.com/documentation/swiftui/view/textinputbordershape(_:)) | System border shape for text input | Reviewing a bordered input treatment | Sign In | 2026-07-31 |
| [Capsule](https://developer.apple.com/documentation/swiftui/capsule) | SwiftUI capsule geometry | Implementing a user-approved capsule with semantic fills | Sign In, Share & Connect | 2026-07-31 |
| [secondarySystemFill](https://developer.apple.com/documentation/uikit/uicolor/secondarysystemfill) | Adaptive subordinate fill color | Toning down a local utility control without custom material | Share & Connect | 2026-07-31 |
| [ControlSize](https://developer.apple.com/documentation/swiftui/controlsize) | Semantic system control sizing | Choosing among system-provided size variants | Conversation, toolbars, actions | 2026-08-11 |
| [ContentUnavailableView](https://developer.apple.com/documentation/swiftui/contentunavailableview) | Native empty, unavailable, and no-results presentation | Showing absent content or capability recovery | Chats, QR scanning, Developer Tools | 2026-08-06 |
| [ProgressView](https://developer.apple.com/documentation/swiftui/progressview) | Native determinate and indeterminate progress | Representing advancing work or playback clearly | Onboarding, relays, support, conversation speech | 2026-08-15 |
| [Linear progress style](https://developer.apple.com/documentation/swiftui/progressviewstyle/linear) | Native horizontal determinate progress bar | Showing Read Aloud progress beneath message text | Conversation speech messages | 2026-08-15 |
| [tint(_:)](https://developer.apple.com/documentation/swiftui/view/tint(_:)) | System tint for controls and indicators | Adding an adaptive semantic color to a standard control | Profile Keys | 2026-08-03 |
| [Progress indicators](https://developer.apple.com/design/human-interface-guidelines/progress-indicators) | Appropriate progress feedback and waiting behavior | Choosing between indeterminate, determinate, and no progress UI | Onboarding, relays | 2026-07-31 |
| [Typography](https://developer.apple.com/design/human-interface-guidelines/typography) | Semantic type hierarchy, legibility, and adaptation | Reviewing text hierarchy or custom type decisions | All screens | 2026-07-31 |
| [TextRenderer](https://developer.apple.com/documentation/swiftui/textrenderer) | Custom drawing for SwiftUI text layout runs | Giving mentions and in-conversation search matches one rounded inline emphasis treatment | Conversation | 2026-08-15 |
| [badge](https://developer.apple.com/documentation/swiftui/view/badge(_:)) | Native list and tab badge values | Displaying an approved unread count | Chats | 2026-07-31 |
| [BadgeProminence](https://developer.apple.com/documentation/swiftui/badgeprominence) | Native badge emphasis | Choosing standard versus decreased badge emphasis | Chats | 2026-07-31 |

## 6. Toolbars, Liquid Glass, symbols, and motion

| Source | Governs | Open when | Current areas | Verified |
| --- | --- | --- | --- | --- |
| [Toolbars](https://developer.apple.com/documentation/swiftui/toolbars) | SwiftUI toolbar placement, content, and customization | Adding or reorganizing navigation and action controls | Chats, conversation, chat creation, Chat Info media preview, Share & Connect, Developer Tools | 2026-08-10 |
| [Adopting Liquid Glass](https://developer.apple.com/documentation/TechnologyOverviews/adopting-liquid-glass) | System-first adoption of Apple’s current visual design | Adding or reviewing glass, bars, or controls | Toolbars, onboarding, composer | 2026-07-31 |
| [Applying Liquid Glass to custom views](https://developer.apple.com/documentation/SwiftUI/Applying-Liquid-Glass-to-custom-views) | Custom glass shapes, tint, and native interactive response when standard components do not fit | Implementing an approved custom glass exception | Conversation composer | 2026-08-14 |
| [GlassEffectContainer](https://developer.apple.com/documentation/swiftui/glasseffectcontainer) | Coordinating multiple custom glass shapes and their spacing | Building the approved continuous emoji-category surface | Message actions | 2026-08-13 |
| [GlassButtonStyle](https://developer.apple.com/documentation/swiftui/glassbuttonstyle) | Native secondary glass buttons | Styling an approved secondary action | Onboarding, scanner recovery, Chat Info, conversation search | 2026-08-15 |
| [glass(_:)](https://developer.apple.com/documentation/swiftui/primitivebuttonstyle/glass(_:)) | Configurable native clear or tinted glass button style | Styling compact secondary icon actions without a custom button implementation | Chat Info | 2026-08-10 |
| [GlassProminentButtonStyle](https://developer.apple.com/documentation/swiftui/glassprominentbuttonstyle) | Native prominent glass actions | Styling the primary action | Onboarding, confirmations | 2026-07-31 |
| [Refining system-provided Liquid Glass in toolbars](https://developer.apple.com/documentation/swiftui/landmarks-refining-the-system-provided-glass-effect-in-toolbars) | Toolbar grouping and automatic glass structure | Organizing adjacent toolbar controls | Chats, Share & Connect | 2026-07-31 |
| [ToolbarSpacer](https://developer.apple.com/documentation/swiftui/toolbarspacer) | Semantic separation between toolbar groups | Controlling toolbar grouping without custom gaps | Chats, Chat Info media preview | 2026-08-10 |
| [sharedBackgroundVisibility](https://developer.apple.com/documentation/swiftui/toolbarcontent/sharedbackgroundvisibility(_:)) | Participation in automatic shared toolbar backgrounds | Keeping approved artwork outside a glass group | Chats avatar | 2026-07-31 |
| [safeAreaBar](https://developer.apple.com/documentation/swiftui/view/safeareabar(edge:alignment:spacing:content:)) | Stationary actions beside scrolling content with matching safe-area and scroll-edge effects | Installing a system-owned bottom action bar or conversation composer | Chats, conversation, Sign Up, Profile, Chat Info media preview | 2026-08-13 |
| [overlay(alignment:content:)](https://developer.apple.com/documentation/swiftui/view/overlay(alignment:content:)) | Foreground layering whose secondary content does not determine the modified view's layout size | Letting the expanded composer cover the transcript while only its compact footprint reserves scroll space | Conversation | 2026-08-14 |
| [safeAreaInset](https://developer.apple.com/documentation/swiftui/view/safeareainset(edge:alignment:spacing:content:)) | Content inset by a view at a safe-area edge | Placing persistent content without covering a scroll view | Onboarding | 2026-08-03 |
| [ignoresSafeArea](https://developer.apple.com/documentation/swiftui/view/ignoressafearea(_:edges:)) | Intentional extension beyond safe areas | Making an approved background or camera preview full-bleed | Chats, QR scanning | 2026-07-31 |
| [Adjusting layout with the keyboard layout guide](https://developer.apple.com/documentation/uikit/adjusting-your-layout-with-keyboard-layout-guide) | System-managed layout that follows the complete docked keyboard region | Keeping keyboard-adjacent content within the changing visible region | Sign Up, Profile, conversation | 2026-08-14 |
| [ScrollView](https://developer.apple.com/documentation/swiftui/scrollview) | Native horizontal and vertical scrolling for content that exceeds its container | Building custom scrolling content or the contained composer media carousel | Conversation, chat creation, Developer Tools | 2026-08-14 |
| [containerRelativeFrame(_:alignment:)](https://developer.apple.com/documentation/swiftui/view/containerrelativeframe(_:alignment:)) | Sizing each scrolling child from the viewport rather than an arbitrary fixed value | Giving every settled composer-media page the full viewport width | Conversation | 2026-08-14 |
| [ViewAlignedScrollTargetBehavior](https://developer.apple.com/documentation/swiftui/viewalignedscrolltargetbehavior) | Settling a scroll view on view-based target geometry with an optional anchor | Settling the composer gallery directly on its precomputed edge-aware item targets | Conversation | 2026-08-14 |
| [scrollPosition(id:anchor:)](https://developer.apple.com/documentation/SwiftUI/View/scrollPosition%28id%3Aanchor%3A%29) | Identity-bound scroll position with an optional alignment anchor | Opening the expanded composer gallery on the exact thumbnail selected | Conversation | 2026-08-14 |
| [listSectionMargins](https://developer.apple.com/documentation/swiftui/view/listsectionmargins(_:_:)) | Per-section margins within a native List | Allowing an uncontained selected-people strip to use the full viewport | Chat creation | 2026-08-09 |
| [contentMargins](https://developer.apple.com/documentation/swiftui/view/contentmargins(_:for:)) | Margins for scroll content independently of its viewport | Preserving selected-strip alignment or symmetric carousel peeks while allowing edge-to-edge scrolling | Conversation, chat creation | 2026-08-14 |
| [defaultScrollAnchor](https://developer.apple.com/documentation/swiftui/view/defaultscrollanchor(_:for:)) | Initial and role-specific scroll positioning | Preserving the intended edge through content or container size changes | Conversation, chat creation | 2026-08-11 |
| [onGeometryChange](https://developer.apple.com/documentation/swiftui/view/ongeometrychange(for:of:action:)) | Efficient observation of derived geometry values | Reacting only when a bounded view dimension actually changes | Conversation | 2026-08-11 |
| [LazyVStack](https://developer.apple.com/documentation/swiftui/lazyvstack) | Lazy vertical layout in a scroll view | Rendering a prototype message timeline | Conversation | 2026-07-31 |
| [Grouping data with lazy stack views](https://developer.apple.com/documentation/swiftui/grouping-data-with-lazy-stack-views) | Native `Section` grouping and pinned section headers in lazy stacks | Keeping transcript date separators inline while pinning the active day | Conversation | 2026-08-12 |
| [ScrollEdgeEffectStyle](https://developer.apple.com/documentation/swiftui/scrolledgeeffectstyle) | Native transitions between scrolling content and controls | Choosing hard, soft, or automatic scroll-edge behavior | Chats, conversation | 2026-08-12 |
| [Motion](https://developer.apple.com/design/human-interface-guidelines/motion) | Purposeful, comfortable, system-consistent motion | Adding or reviewing animation or transition behavior | All animated flows | 2026-07-31 |
| [interactiveSpring](https://developer.apple.com/documentation/swiftui/animation/interactivespring) | Native interruptible spring animation intended for direct manipulation and velocity-preserving settling | Letting the composer follow a pull and complete toward its selected endpoint after release | Conversation | 2026-08-14 |
| [withAnimation(_:completionCriteria:_:completion:)](https://developer.apple.com/documentation/swiftui/withanimation(_:completioncriteria:_:completion:)) | Explicit animation completion for state cleanup after the logical transition finishes | Holding numeric composer geometry through expansion and collapse before restoring its resting layout | Conversation | 2026-08-14 |
| [SensoryFeedback](https://developer.apple.com/documentation/swiftui/sensoryfeedback) | Semantic system haptic feedback | Confirming a meaningful result or threshold | Copy, selection, success | 2026-07-31 |
| [General pasteboard](https://developer.apple.com/documentation/uikit/uipasteboard/general) | Systemwide text and data copy-paste exchange | Copying message text without an intermediate screen | Message actions | 2026-08-13 |
| [SF Symbols HIG](https://developer.apple.com/design/human-interface-guidelines/sf-symbols) | Symbol selection, variants, rendering, localization, and effects | Choosing an interface icon | All screens | 2026-08-09 |
| [Icons](https://developer.apple.com/design/human-interface-guidelines/icons) | Familiar action symbols and optical consistency across glyphs | Adjusting individual icon dimensions when equal nominal sizing looks inconsistent | Chat Info media preview | 2026-08-10 |
| [SF Symbols](https://developer.apple.com/sf-symbols/) | Current symbol browser and availability resources | Verifying a symbol and platform support | All screens | 2026-07-31 |
| [symbolEffect](https://developer.apple.com/documentation/swiftui/view/symboleffect(_:options:isactive:)) | System-owned animated SF Symbol effects and Reduce Motion adaptation | Indicating an active live process without custom animation | Developer Tools | 2026-08-06 |

## 7. Photos, files, sharing, camera, and permissions

| Source | Governs | Open when | Current areas | Verified |
| --- | --- | --- | --- | --- |
| [PhotosPicker](https://developer.apple.com/documentation/photosui/photospicker) | Privacy-preserving system photo selection | Selecting an avatar or message image | Sign Up, Profile, chat creation, conversation | 2026-08-13 |
| [aspectRatio](https://developer.apple.com/documentation/swiftui/view/aspectratio(_:contentmode:)-6j7xz) | Constraining a view to supplied media dimensions with fit or fill behavior | Giving expanded composer media its own aspect-matched page and compact media square fill bounds | Conversation | 2026-08-14 |
| [LPLinkView](https://developer.apple.com/documentation/linkpresentation/lplinkview) | Native metadata-driven rich-link presentation and the boundary of its fixed public layout API | Evaluating whether the approved horizontal composer preview can use the system view directly | Conversation | 2026-08-14 |
| [Bringing Photos picker to your SwiftUI app](https://developer.apple.com/documentation/photokit/bringing-photos-picker-to-your-swiftui-app) | End-to-end SwiftUI photo-picker integration | Implementing selection, loading, and state handling | Sign Up, Profile | 2026-07-31 |
| [fileImporter](https://developer.apple.com/documentation/swiftui/view/fileimporter(ispresented:allowedcontenttypes:allowmultipleselection:oncompletion:)) | System file selection | Importing an image or chat file | Sign Up, Profile, chat creation, conversation | 2026-08-09 |
| [fileExporter](https://developer.apple.com/documentation/swiftui/view/fileexporter(ispresented:document:contenttype:defaultfilename:oncompletion:)) | System destination picker for exported files | Saving a private-key, encrypted-backup document, or shared chat media without a custom browser | Profile Keys, Chat Info media preview | 2026-08-10 |
| [FileDocument](https://developer.apple.com/documentation/swiftui/filedocument) | Value-type serialization for exported documents | Preparing deterministic content for `fileExporter` | Profile Keys, Chat Info media preview | 2026-08-10 |
| [File management](https://developer.apple.com/design/human-interface-guidelines/file-management) | Familiar Files destinations and system-owned save behavior | Designing a file export or save flow | Profile Keys | 2026-08-03 |
| [ShareLink](https://developer.apple.com/documentation/swiftui/sharelink) | Native system sharing from SwiftUI | Sharing a profile, export value, or prepared chat-media file | Share & Connect, Profile Keys, Chat Info media preview | 2026-08-10 |
| [Activity views](https://developer.apple.com/design/human-interface-guidelines/activity-views) | Share-sheet purpose and presentation | Evaluating native sharing behavior | Share & Connect, exports | 2026-07-31 |
| [QR code generator](https://developer.apple.com/documentation/coreimage/cifilter/qrcodegenerator()) | Local Core Image QR generation | Producing deterministic shareable QR imagery | Sign In, Share & Connect, Donate | 2026-07-31 |
| [DataScannerViewController](https://developer.apple.com/documentation/visionkit/datascannerviewcontroller) | Native live data scanning | Implementing physical-iPhone QR scanning | Sign In, Share & Connect | 2026-07-31 |
| [RecognizedItem](https://developer.apple.com/documentation/visionkit/recognizeditem) | Typed results from VisionKit recognition | Interpreting scanner output without custom camera recognition | Sign In, Share & Connect | 2026-07-31 |
| [Scanning data with the camera](https://developer.apple.com/documentation/visionkit/scanning-data-with-the-camera) | Scanner availability, guidance, highlighting, and recognition | Designing the complete scanning lifecycle | Sign In, Share & Connect | 2026-07-31 |
| [Camera authorization](https://developer.apple.com/documentation/avfoundation/requesting-authorization-to-capture-and-save-media) | Camera permission status and system request | Requesting or recovering camera access | QR scanning, conversation | 2026-08-11 |
| [AVCam: Building a camera app](https://developer.apple.com/documentation/avfoundation/avcam-building-a-camera-app) | Responsive SwiftUI photo and movie capture architecture | Building the conversation camera | Conversation | 2026-08-11 |
| [AVCapturePhotoOutput](https://developer.apple.com/documentation/avfoundation/avcapturephotooutput) | Public still-photo capture output | Capturing a message photo | Conversation | 2026-08-11 |
| [AVCaptureMovieFileOutput](https://developer.apple.com/documentation/avfoundation/avcapturemoviefileoutput) | Public movie-file recording output | Recording a message video while the shutter is held | Conversation | 2026-08-11 |
| [Asking permission to use notifications](https://developer.apple.com/documentation/usernotifications/asking-permission-to-use-notifications) | Contextual notification authorization and response handling | Adding or reviewing notification permission behavior | Notification settings | 2026-07-31 |
| [User Notifications](https://developer.apple.com/documentation/usernotifications) | Local notification creation and remote APNs delivery | Distinguishing on-device notifications from remote push | Notification settings | 2026-08-03 |
| [UNNotificationSettings](https://developer.apple.com/documentation/usernotifications/unnotificationsettings) | Live notification authorization and per-interaction settings | Enabling or disabling app options based on iOS notification access | Notification settings | 2026-08-03 |
| [Pushing background updates to your app](https://developer.apple.com/documentation/usernotifications/pushing-background-updates-to-your-app) | Content-free remote wake-up behavior and delivery limits | Explaining or implementing background push | Notification settings | 2026-08-03 |
| [Open notification settings](https://developer.apple.com/documentation/uikit/uiapplication/opennotificationsettingsurlstring) | Direct URL to this app's notification settings | Offering recovery after notification access is denied | Notification settings | 2026-08-03 |
| [Open Settings](https://developer.apple.com/documentation/uikit/uiapplication/opensettingsurlstring) | System Settings recovery URL | Offering recovery after other denied permissions | QR scanning | 2026-07-31 |
| [UIGraphicsImageRenderer](https://developer.apple.com/documentation/uikit/uigraphicsimagerenderer) | Bounded local image preparation | Resizing selected profile imagery before holding it in memory | Sign Up, Profile | 2026-07-31 |

## 8. Accessibility, text adaptation, and localization

| Source | Governs | Open when | Current areas | Verified |
| --- | --- | --- | --- | --- |
| [Accessibility HIG](https://developer.apple.com/design/human-interface-guidelines/accessibility) | Inclusive visual, motor, hearing, speech, and cognitive design | Designing or reviewing any interactive screen | All screens | 2026-08-13 |
| [SwiftUI accessibility fundamentals](https://developer.apple.com/documentation/swiftui/accessibility-fundamentals) | Labels, values, grouping, actions, and standard-control behavior | Implementing or reviewing SwiftUI accessibility | All screens | 2026-07-31 |
| [Supporting VoiceOver in your app](https://developer.apple.com/documentation/uikit/supporting-voiceover-in-your-app) | VoiceOver auditing, labels, grouping, order, and custom elements | Reviewing spoken navigation or a custom composition | All screens | 2026-07-31 |
| [Performing accessibility audits](https://developer.apple.com/documentation/accessibility/performing-accessibility-audits-for-your-app) | Inspector checks for descriptions, hit regions, contrast, clipping, and traits | Running milestone accessibility review | Milestone reviews | 2026-07-31 |
| [Dynamic Type](https://developer.apple.com/documentation/swiftui/environmentvalues/dynamictypesize) | Content-size adaptation | Checking semantic typography and layout growth | All screens | 2026-07-31 |
| [Reduce Motion](https://developer.apple.com/documentation/swiftui/environmentvalues/accessibilityreducemotion) | Motion adaptation from the environment | Adding or reviewing custom motion | Animated flows | 2026-07-31 |
| [Localization](https://developer.apple.com/documentation/xcode/localization) | String catalogs, language and region adaptation, and localization testing | Preparing production copy or checking expansion and right-to-left behavior | All product copy | 2026-07-31 |
| [Supporting multiple languages in your app](https://developer.apple.com/documentation/xcode/supporting-multiple-languages-in-your-app) | Internationalization and system language adaptation | Adding or reviewing supported app languages | Appearance and all product copy | 2026-08-03 |
| [Localizing and varying text with a string catalog](https://developer.apple.com/documentation/xcode/localizing-and-varying-text-with-a-string-catalog) | String-catalog setup, translations, plurals, and language variants | Turning a planned language into a complete localization | Appearance and all product copy | 2026-08-03 |

## 9. Testing, responsiveness, and performance

| Source | Governs | Open when | Current areas | Verified |
| --- | --- | --- | --- | --- |
| [Xcode testing](https://developer.apple.com/documentation/xcode/testing) | Unit, integration, UI, and performance test capabilities | Adding a justified regression or logic test | Test targets | 2026-07-31 |
| [Testing and performance](https://developer.apple.com/documentation/technologyoverviews/testing-and-performance) | Responsiveness, stalls, hitches, energy, and concurrency | Investigating a reproducible quality issue | Chats, conversation | 2026-07-31 |
| [Writing and running performance tests](https://developer.apple.com/documentation/xcode/writing-and-running-performance-tests) | Repeatable performance baselines | Protecting a proven performance-critical path | Native chat list | 2026-07-31 |
| [Improving app responsiveness](https://developer.apple.com/documentation/xcode/improving-app-responsiveness) | Rough interaction budgets, main-thread work, hangs, hitches, and appropriate diagnostic tools | Investigating delayed taps, animation stalls, or expensive synchronous work | Project-wide hardening | 2026-08-15 |
| [Diagnosing performance issues early](https://developer.apple.com/documentation/xcode/diagnosing-performance-issues-early) | Thread Performance Checker findings, priority inversions, and non-UI main-thread work | Separating a project-owned stall from a runtime or toolchain issue | Project-wide hardening | 2026-08-15 |
| [Understanding and improving SwiftUI performance](https://developer.apple.com/documentation/xcode/understanding-and-improving-swiftui-performance) | Long-running view bodies, frequent updates, dependency scope, and SwiftUI Instruments evidence | Reviewing repeated work in a SwiftUI view graph | Chats, conversation | 2026-08-15 |
| [Performance tests](https://developer.apple.com/documentation/xctest/performance-tests) | XCTest metrics, measurement options, and regression baselines | Adding a durable performance regression test | Unit and UI test targets | 2026-08-15 |
| [XCTApplicationLaunchMetric](https://developer.apple.com/documentation/xctest/xctapplicationlaunchmetric) | First-frame and responsive launch duration | Comparing repeated launches under the same harness | App launch | 2026-08-15 |
| [XCTOSSignpostMetric](https://developer.apple.com/documentation/xctest/xctossignpostmetric) | Duration of system or custom signposted regions | Isolating navigation-transition time from UI-automation wall time | Conversation navigation | 2026-08-15 |
| [navigationTransitionMetric](https://developer.apple.com/documentation/xctest/xctossignpostmetric/navigationtransitionmetric) | System navigation-transition duration | Comparing first and repeated conversation opens | Conversation navigation | 2026-08-15 |
| [OSSignposter](https://developer.apple.com/documentation/os/ossignposter) | Low-overhead custom interval and event signposts | Adding temporary attribution only when system metrics are insufficient | Project-wide diagnostics | 2026-08-15 |

## 10. Xcode 27 and current SwiftUI

| Source | Governs | Open when | Current areas | Verified |
| --- | --- | --- | --- | --- |
| [Xcode 27 release notes](https://developer.apple.com/documentation/xcode-release-notes/xcode-27-release-notes) | Current toolchain, SDK, compiler, and beta behavior | A build, API, simulator, preview, or test behavior is uncertain | Project-wide | 2026-07-31 |
| [SwiftUI documentation](https://developer.apple.com/documentation/swiftui/) | Primary framework API reference | Implementing or checking a SwiftUI API | Project-wide | 2026-07-31 |
| [SwiftUI updates](https://developer.apple.com/documentation/updates/swiftui) | Current framework additions and changes | Considering a new or changed SwiftUI capability | Project-wide | 2026-07-31 |
| [WWDC26 SwiftUI guide](https://developer.apple.com/wwdc26/guides/swiftui/) | Curated current SwiftUI sessions and Xcode 27 guidance | Starting research on a current SwiftUI topic | Project-wide | 2026-07-31 |
| [What’s new in SwiftUI](https://developer.apple.com/videos/play/wwdc2026/269/) | Current presentation, toolbar, data-flow, and performance changes | Evaluating iOS 27 implementation options | Project-wide | 2026-07-31 |
| [Xcode, agents, and you](https://developer.apple.com/videos/play/wwdc2026/259/) | Apple’s agent workflow and review expectations | Improving repository agent collaboration | Agent workflow | 2026-07-31 |

## Maintenance

- Add a source only when an implemented or selected screen needs it.
- Prefer a direct HIG page, API page, WWDC session, or official sample over a
  search result, forum answer, or third-party summary.
- Update the verification date when a source is reopened and confirmed.
- Remove duplicates and superseded links instead of retaining historical API
  advice.
- If a live source is temporarily unavailable, do not invent a platform claim;
  state the limitation and use already recorded local decisions until the
  source can be checked.
