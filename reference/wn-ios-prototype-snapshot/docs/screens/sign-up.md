# Sign Up

## Purpose and navigation

Create a new White Noise profile. **Sign Up** on first-launch Welcome presents a native large sheet. Add Profile pushes it inside the existing onboarding sheet.

## Copy

- Navigation title: **Sign Up**
- Initial dummy name: **Marmota**
- Avatar action before selection: **Add Photo**
- Avatar action after selection: **Change Photo**
- Image-source menu: **Choose from Photos**, **Choose from Files**, **Find Image on Web**
- Web-image modes: **Search**, **URL**
- Search prompt: **Search Images**
- Search privacy title: **Search privacy**
- Search privacy detail: **Your search is sent to DuckDuckGo. Image providers
  can see your IP address when results load.**
- URL field label: **Image URL**
- URL prompt: **https://example.com/image.jpg**
- URL privacy title: **Image privacy**
- URL privacy detail: **The image provider can see your IP address when the
  preview loads.**
- URL helper: **Enter an image URL to preview it below.**
- Invalid URL: **Enter a valid web address.**
- Web-image confirmation: **Done**
- Selected-image removal: **Remove Photo**
- Field labels: **Name**, **About**
- About prompt: **A little about you**
- Image error: **Couldn't use that photo. Choose another image and try again.**
- Primary action: **Sign Up**
- Progress accessibility label: **Signing Up**
- Photo preparation progress: **Preparing Photo**

## Native components

- A first-launch native large sheet with visible drag indicator and a native Close toolbar action.
- Add Profile keeps native Back behavior inside its existing sheet.
- Circular SwiftUI avatar preview with the first name initial or the selected image.
- Native `Menu` for the compact source choice.
- `PhotosPicker` limited to images.
- SwiftUI `fileImporter` limited to image files.
- One native large sheet for finding an image on the web, with Close, Done, and
  a system palette `Picker` in the principal toolbar position for **Search** and
  **URL**.
- A native UIKit `UISearchBar` hosted in a SwiftUI system `safeAreaBar`, and a
  three-column SwiftUI `LazyVGrid` of edge-to-edge 1:1 square image Buttons for
  one-of-21 selection. The search field's system clear control appears while
  it contains text. The field retains UIKit's minimal search style so it never
  draws a separate bar background. At rest it uses standard safe-area padding;
  focus expands it toward the screen edges, matching the native Chats search
  geometry. A separate circular system glass `xmark` appears only while the
  field has focus and dismisses only the keyboard. A single bottom-trailing
  selection badge uses the app's adaptive accent fill with a white outline and
  checkmark to communicate the active selection.
- The bottom search bar leaves the sheet toolbar independent, so Close,
  Search/URL, and Done remain available while search is active.
- A quiet, neutral privacy disclosure precedes Search results and URL entry.
  Search and URL both place it in a native grouped Form section, giving each
  mode the same system top inset and white rounded container. Both use
  `hand.raised`, semantic primary and secondary text, and no warning color,
  modal confirmation, or custom material.
- Native grouped `Form` for entering a URL and previewing its image in
  the same sheet.
- Native grouped `Form` with its scroll background hidden so the canvas matches Welcome.
- Plain native `TextField` controls inside separate grouped Name and About cards.
- Sign Up contains no Verified Nostr Address field. The profile address is
  assigned only after the profile and public key are created.
- Semantic system fill distinguishes the grouped cards from the white canvas while native Form geometry owns their shape and insets.
- Native `glass` avatar action and `glassProminent` primary action.
- Native small `ProgressView` centered inside the primary action while signing up.
- Enabled primary-action content follows the adaptive monochrome tint: white title or spinner on the black Light Mode button, and black title or spinner on the white Dark Mode button.
- If this shared control enters a disabled environment, its content returns to semantic `primary` and lets native `glassProminent` own the disabled material and reduced prominence.

## Important behavior

- The form starts with the deterministic pet name **Marmota** and an **M** monogram.
- Completing initial Sign In uses the bundled, user-supplied marmot photo. Sign Up uses that photo only when no replacement photo was chosen; an explicitly chosen Photos, Files, or web image becomes the active profile avatar.
- The avatar remains a white circle with a black initial in Dark Mode.
- Editing Name immediately updates the monogram to the first non-whitespace character.
- A selected photo replaces the monogram, is normalized off the main actor to a maximum 512-pixel JPEG, and remains in memory only.
- **Find Image on Web** opens one sheet in Search mode. The person can switch
  between Search and URL without leaving the sheet.
- Search starts with a native empty state. Typing a nonempty query immediately
  presents 21 varied,
  locally bundled example images as a regular three-column square thumbnail
  grid with minimal gutters and aspect-fill cropping. Each query deterministically
  reorders the catalog; the prototype represents the production web-search
  experience without making a runtime request or identifying its fixture
  source in product UI. Production-facing privacy copy describes the real
  Search and URL data flow that this interaction represents.
- The results surface is constrained to the available width and scrolls only
  vertically; it does not pan horizontally or move freely in two dimensions.
- Search uses one grouped Form as its strictly vertical scroller. Its privacy
  section uses the same system top spacing as URL, while the result grid is a
  transparent edge-to-edge Form row. Results use Apple's soft bottom
  scroll-edge effect near the system search field while remaining clipped to
  the keyboard-adjusted scroll viewport, so thumbnails never show through the
  system keyboard. URL shows its equivalent disclosure before the field so the
  consequence is visible before entry.
- The web-image sheet places an opaque semantic grouped backing above the
  results and directly beneath the system keyboard. UIKit's system-managed
  keyboard layout guide owns its complete docked region, including the bottom
  safe area. This is a user-approved exception to the default translucent
  substrate: the keyboard remains system-owned, but image results never
  visually participate in its material. When the keyboard leaves, the backing
  collapses completely rather than retaining the guide's resting bottom-safe-
  area height.
- The grid permits exactly one selection. **Done** applies the selected image
  to the Sign Up avatar and the resulting profile.
- Selecting a search result removes focus from the search field, dismisses the
  keyboard, and hides the separate large `xmark`. The entered query, the
  search field's system clear control, and the selected badge remain visible.
  The clear control empties the query and results without undoing the image
  selection; the now-enabled top-right **Done** action is the explicit
  confirmation.
- SwiftUI's default search cancel action clears the query as it dismisses
  search, which conflicts with the approved interaction. The bounded UIKit
  search-field bridge is the user-approved native exception: its built-in
  clear control clears only text, while the separately labeled **Dismiss
  Keyboard** button changes only focus and disappears whenever the field is no
  longer focused.
- The bridge follows the system search hierarchy rather than one fixed layout:
  the inactive field stays compact, while the focused field uses reduced outer
  margins and the native circular keyboard-dismiss control. The focused
  geometry matches Chats and Apple's Messages search reference without
  changing the inactive state or introducing a second bar background.
- URL accepts any syntactically valid HTTP or HTTPS image address and updates
  a square deterministic bundled preview immediately while typing. The preview
  has no selection badge because it is the direct result of the entered URL;
  **Done** applies it. This keeps the prototype functional without runtime
  networking.
- Every web-image choice has a deterministic `example.com` display URL. A
  Search selection immediately seeds that URL, so switching to URL shows the
  same image and its preview. After **Done**, reopening Find Image on Web with
  an existing web image seeds the same display URL and preview regardless of
  whether the image was originally chosen through Search or URL. The initial
  URL view remains empty when no web image has been chosen. The resulting
  profile retains this web provenance; bundled and device-selected avatars do
  not infer a URL from a matching catalog asset.
- **Change Photo** reopens the same source menu; **Remove Photo** restores the monogram.
- **Remove Photo** uses SwiftUI's destructive button role. Its complete native `Label` also receives the semantic red style so the title and standard trash symbol match Apple's destructive-menu example despite the current iOS 27 beta rendering the role on the title only.
- The avatar action uses the regular native control size with one stable semantic system spacing step below the avatar. Changing between the monogram and selected image does not alter this spacing.
- The main Sign Up screen keeps the white system canvas used by Welcome. The
  web-image task uses the system grouped canvas so its privacy group, empty
  state, result grid, and URL Form share one coherent sheet background.
- Native Form sections own the field-card shape, padding, spacing, and focus behavior; no rounded rectangle is drawn by the app.
- The avatar remains visually floating in a transparent Form row above the fields.
- Photos and Files present their system interfaces. No camera or broad photo-library permission is requested.
- The combined Search/URL sheet uses a native Close action and supports swipe
  dismissal.
- Sign Up preserves its normal dimensions, replaces its visible title with a centered spinner, prevents repeat activation, and exposes **Signing Up** and **In progress** to assistive technologies before invoking its callback.
- The deterministic prototype processing state lasts two seconds so the stable loading treatment remains inspectable without slowing the flow.
- Name, About, and the complete avatar choice are committed together. Leaving
  the Sign Up or Sign In
  presentation before processing completes cancels that pending completion and
  never changes the root destination afterward.
- Photo preparation is cancelable. Sign Up remains unavailable while a chosen photo is being prepared, and a failed replacement keeps the last valid draft image while presenting the approved error.
- Initial Sign Up and Add Profile both finish on Chats. Add Profile removes the
  underlying Settings destination without animation while its onboarding sheet
  remains visible. The sheet dismissal begins only on the following render turn,
  after the navigation state has committed, and reveals Chats directly.

## Accessibility

- The initials preview is announced as **Profile photo, M** and updates with the name.
- A selected image is announced as **Profile photo**.
- Persistent labels identify Name and About after their prompts disappear.
- Native controls retain system traits, focus, hit targets, Dynamic Type, menu behavior, keyboard behavior, and motion.
- Every web result is an individual Button announced by its visible subject;
  the active result also has the selected trait and a checkmark, so selection
  never depends on green alone.
- The URL field uses the URL keyboard, disables autocorrection and automatic
  capitalization, and focuses when its sheet opens.
- The screen scrolls and dismisses the keyboard interactively.

## Apple references

- [Text fields](https://developer.apple.com/design/human-interface-guidelines/text-fields)
- [Form](https://developer.apple.com/documentation/swiftui/form)
- [Menus](https://developer.apple.com/design/human-interface-guidelines/menus)
- [Picker](https://developer.apple.com/documentation/swiftui/picker)
- [Palette picker style](https://developer.apple.com/documentation/swiftui/pickerstyle/palette)
- [Search fields](https://developer.apple.com/design/human-interface-guidelines/search-fields)
- [Privacy](https://developer.apple.com/design/human-interface-guidelines/privacy)
- [UISearchBar](https://developer.apple.com/documentation/uikit/uisearchbar)
- [UISearchBarDelegate](https://developer.apple.com/documentation/uikit/uisearchbardelegate)
- [safeAreaBar](https://developer.apple.com/documentation/swiftui/view/safeareabar(edge:alignment:spacing:content:))
- [LazyVGrid](https://developer.apple.com/documentation/swiftui/lazyvgrid)
- [Scroll edge effect style](https://developer.apple.com/documentation/swiftui/view/scrolledgeeffectstyle(_:for:))
- [Adjusting layout with the keyboard layout guide](https://developer.apple.com/documentation/uikit/adjusting-your-layout-with-keyboard-layout-guide)
- [Sheets](https://developer.apple.com/design/human-interface-guidelines/sheets)
- [Populating SwiftUI menus with adaptive controls](https://developer.apple.com/documentation/SwiftUI/Populating-SwiftUI-menus-with-adaptive-controls)
- [PhotosPicker](https://developer.apple.com/documentation/photosui/photospicker)
- [Bringing Photos picker to your SwiftUI app](https://developer.apple.com/documentation/photokit/bringing-photos-picker-to-your-swiftui-app)
- [fileImporter](https://developer.apple.com/documentation/swiftui/view/fileimporter(ispresented:allowedcontenttypes:allowmultipleselection:oncompletion:))
- [SwiftUI accessibility fundamentals](https://developer.apple.com/documentation/swiftui/accessibility-fundamentals)

## Acceptance

- **Sign Up** on Welcome presents a native large sheet; Close or swipe-down returns to Welcome.
- **Sign Up** from Add Profile remains in the existing large onboarding sheet and native Back returns to its Welcome step.
- Completing Sign In or Sign Up from Add Profile shows only the native onboarding-sheet dismissal and reveals Chats directly; Settings never appears or slides away between them.
- The default Sign Up avatar shows **M** for **Marmota** before completion.
- After Sign In completes, Chats and Settings show the bundled Marmota photo. After Sign Up, they show the selected photo, or the bundled Marmota photo when no replacement was chosen.
- Wiping the last profile or erasing app data never changes fixture identity:
  fresh Sign In or Sign Up recreates Marmota with the stable Marmota identity,
  while Add Profile Sign Up creates the separate Pebble identity with its
  stable ID. Sign Up applies an explicitly selected avatar; otherwise each
  identity keeps its bundled avatar.
- The avatar action is visually compact and clearly separated from the avatar.
- The avatar-to-action spacing remains unchanged before and after selecting a photo.
- Sign Up uses the same white system canvas as Welcome.
- Name and About use separate native grouped Form cards with a subtle semantic
  system fill.
- Form owns field-card corner geometry, insets, and spacing; the app does not supply numeric border or corner-radius recipes.
- **Add Photo** opens a menu with working Photos, Files, and one **Find Image on
  Web** choice.
- **Find Image on Web** opens one large sheet with a native Search/URL selector
  in its top toolbar.
- Search initially shows **Search Images** and **Enter a search to find an
  image.** with no results. Entering a nonempty query immediately shows all 21 varied
  example images in a regular 1:1 square three-column grid. Every thumbnail
  uses aspect-fill cropping, minimal equal gutters, and no masonry sizing.
  Results scroll vertically without horizontal panning.
- Search and URL present their approved neutral privacy disclosures in matching
  native grouped Form sections with the same system top inset and container
  geometry. Search keeps its section above the first result row; URL keeps its
  section before the URL field. Neither treatment uses warning color or
  interrupts the task.
- Search results remain in one vertical scroller and transition through a soft
  system scroll edge near the native bottom search field. When the keyboard is
  visible, the result grid remains clipped to its adjusted viewport and does
  not remain visually exposed beneath the keyboard. An opaque semantic grouped
  backing covers the complete keyboard and bottom safe-area region, so
  thumbnails don't show through its material.
- The web-image grid supports one selected item, communicates selection without
  relying on color alone through a white-outlined accent badge and white
  checkmark, and applies only after Done.
- Close, Search/URL, and Done remain visible during active search. While the
  search field is focused, it expands to the same edge margins as the native
  Chats search without drawing an opaque bar behind the field. A separate
  large circular `xmark` is visible and dismisses only the keyboard without
  changing the query.
  Selecting a result performs that same keyboard-only dismissal, restores the
  compact field, preserves the visible query and selection, keeps the in-field
  clear control available, enables Done, hides the large `xmark`, and leaves
  the complete native bottom search field unobscured. Clearing the query with
  the in-field control returns to the Search Images empty state without
  undoing the selection.
- A valid HTTP or HTTPS URL immediately produces a square deterministic preview
  without a selection checkmark and can be applied; a malformed URL keeps
  **Done** disabled in URL mode. The helper reads **Enter an image URL to
  preview it below.** before entry and after a valid preview appears.
- Selecting an image through Search and then opening URL, or reopening Find
  Image on Web after applying an image through either mode, shows a stable
  `example.com` image URL and the same image preview. URL does not return to an
  empty field while a current web image exists.
- Choosing a supported image previews it; removing it restores the current name initial.
- Name, About, and the selected avatar all appear on the resulting profile after
  Sign Up. Its deterministic Verified Nostr Address is assigned only after the
  profile and public key exist. Re-entering Sign Up for a locally stored
  signed-out profile updates the Sign Up values without changing its stored
  Verified Nostr Address, retained chats, or profile-owned settings.
- Photos and Files selections are prepared to a maximum 512-pixel dimension before being retained. A failed replacement preserves the previous valid draft image.
- The destructive Remove Photo menu item displays both its title and trash symbol with the system destructive treatment.
- The bottom **Sign Up** action remains visible above the safe area and keyboard.
- Sign Up shows a centered native spinner for two seconds without changing button dimensions or losing contrast.
- In Light Mode the prominent action has white content on black; in Dark Mode it has black content on white, including normal and loading states.
- The layout remains usable in Light and Dark appearances and at accessibility text sizes.

## Bundled web-image provenance (team only)

The picker contains exactly 21 local example images: 14 existing fictional-
universe fixtures and seven newly bundled web examples. Product UI never names
the image source, and the app never fetches an image at runtime. Search renders
square aspect-fill thumbnails; only the resulting profile avatar applies the
shared circular avatar crop.

Newly bundled source links are retained only for internal provenance. License:
[source license](https://unsplash.com/license).

| Asset | Creator | Source | Retrieved |
| --- | --- | --- | --- |
| AvatarWebAionyHaust | Aiony Haust | [3TLl_97HNJo](https://unsplash.com/photos/3TLl_97HNJo) | 2026-08-07 |
| AvatarWebChristopherCampbell | Christopher Campbell | [rDEOVtE7vOs](https://unsplash.com/photos/rDEOVtE7vOs) | 2026-08-07 |
| AvatarWebIanDooley | Ian Dooley | [d1UPkiFd04A](https://unsplash.com/photos/d1UPkiFd04A) | 2026-08-07 |
| AvatarWebSergioDePaula | Sergio de Paula | [c_GmwfHBDzk](https://unsplash.com/photos/c_GmwfHBDzk) | 2026-08-07 |
| AvatarWebAyoOgunseinde | Ayo Ogunseinde | [sibVwORYqs0](https://unsplash.com/photos/sibVwORYqs0) | 2026-08-07 |
| AvatarWebVinceFleming | Vince Fleming | [j3lf-Jn6deo](https://unsplash.com/photos/j3lf-Jn6deo) | 2026-08-07 |
| AvatarWebPhilipMartin | Philip Martin | [5aGUyCW_PJw](https://unsplash.com/photos/5aGUyCW_PJw) | 2026-08-07 |
