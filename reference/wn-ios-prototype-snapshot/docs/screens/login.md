# Sign In

## Purpose and navigation

Use an existing White Noise profile by entering its private key. **Sign In** on first-launch Welcome presents this screen in a native sheet. Add Profile pushes it inside the existing onboarding sheet. The QR action pushes a native camera scanner and returns the scanned value to Sign In.

## Copy

- Navigation title: **Sign In**
- Field label: **Private Key**
- Field prompt: **Enter private key**
- Format help: **It starts with nsec.**
- Invalid key: **That private key isn't valid. Check it and try again.**
- Primary action: **Sign In**
- Progress: **Signing In…**
- Unavailable scanner title: **QR Scanning Unavailable**

## Native components

- A first-launch native sheet starts at the medium detent, supports expansion to large, shows the system drag indicator, and uses a native Close toolbar action.
- Add Profile keeps native Back behavior inside its existing sheet.
- Native `SecureField` so the private key is never shown as plain text.
- User-approved Login-specific SwiftUI composition using `secondarySystemFill`, native `Capsule`, system horizontal padding, and a 50-point height matching the accepted Sign Up field and large circular glass action.
- SF Symbol buttons for Paste, Clear, and Scan QR Code.
- Paste or Clear occupies a reserved 44-point trailing field-accessory region.
- The separate circular `glass` action uses the large system control size. It displays Scan QR Code while an empty field is idle and Close while the field has keyboard focus.
- Native `glassProminent` primary action.
- Native small `ProgressView` centered inside the primary action while signing in.
- Enabled primary-action content follows the adaptive monochrome tint: white title or spinner on the black Light Mode button, and black title or spinner on the white Dark Mode button.
- Disabled content uses Apple’s adaptive `tertiaryLabel` color in a noninteractive overlay while native `glassProminent` owns the disabled material and reduced prominence underneath. Separating label contrast from the button style prevents the system material from dimming the text twice when unfocused and keeps focused and unfocused disabled states visually identical.
- VisionKit `DataScannerViewController` for QR scanning on supported iPhones.
- Native `ContentUnavailableView` for denied, restricted, unsupported, and temporarily unavailable camera states.

## Important behavior

- The label and guidance or validation text align with the editable text inset inside the capsule, following the accepted Sign Up field hierarchy.
- Tapping the filled field gives the native `SecureField` focus and presents the keyboard.
- An empty, unfocused field shows Paste and Scan QR Code.
- While the field has focus, the outside action becomes Close and dismisses the keyboard without changing the entered value.
- Any entered value replaces Paste with Clear. After the keyboard is dismissed, the outside action disappears and the capsule expands.
- Clear empties the field without dismissing the keyboard.
- The keyboard also uses its native Go key and dismisses when the scanner or Sign In flow is opened.
- Tapping the field selects the sheet's large native detent in the tap transaction, before focus changes and the keyboard begins presenting. The bottom Sign In action then follows the keyboard safe area with the same system motion as Sign Up. Dismissing the keyboard returns the direct Sign In sheet to its medium detent.
- Paste inserts one fixed fictional `nsec` value with the normal 63-character length, so the pasted state is always usable.
- Manual and scanned values are trimmed, then accepted when they begin with lowercase `nsec`. Other values are invalid.
- The prototype performs no length check, decoding, authentication, or cryptography.
- The filled capsule is an intentional custom composition because SwiftUI has no stock filled secure field with a configurable trailing accessory. All constituent controls, colors, shapes, symbols, spacing, and motion use public Apple APIs.
- The iOS 27 bordered text-field style is explicitly rejected here because its outlined appearance conflicts with the accepted filled onboarding fields and rounded actions.
- The primary action remains disabled until the prototype check passes.
- Invalid input shows the recovery message below the field.
- Paste and Clear use the system SF Symbol replacement transition. Scan QR Code fades as the field crosses between empty and nonempty.
- Guidance and validation use native footnote typography with semantic secondary and error colors.
- Sign In preserves its normal dimensions, replaces its visible title with a centered spinner, prevents repeat activation, and exposes **Signing In** and **In progress** to assistive technologies before invoking its callback.
- While Sign In is processing, the entire field enters SwiftUI's native disabled environment so its native `SecureField` receives the system disabled appearance and interaction behavior; both its internal Paste or Clear action and its external Close or Scan action disappear.
- The deterministic prototype processing state lasts two seconds so its stable spinner and disabled-field treatment remain easy to inspect without slowing the flow.
- A scanned QR payload returns to Sign In and is checked by the same prototype validator.
- Opening QR Scanner expands a medium Sign In sheet to large. Returning to Sign In restores the medium detent, unless the person has manually expanded the direct sheet.
- The live or simulated camera surface fills the scanner sheet behind the transparent navigation bar. The system Liquid Glass Back control floats over the camera instead of occupying a separate header surface.

## Accessibility

- The private key value is marked sensitive and is never included in accessibility labels.
- Paste, Clear, and Scan QR Code use explicit action labels.
- Native controls retain system focus, keyboard, hit-target, disabled, progress, and navigation behavior.

## Apple references

- [SecureField](https://developer.apple.com/documentation/swiftui/securefield)
- [SwiftUI Focus](https://developer.apple.com/documentation/swiftui/focus)
- [SwiftUI disabled](https://developer.apple.com/documentation/swiftui/view/disabled(_:))
- [SwiftUI isEnabled](https://developer.apple.com/documentation/swiftui/environmentvalues/isenabled)
- [Text fields HIG](https://developer.apple.com/design/human-interface-guidelines/text-fields)
- [secondarySystemFill](https://developer.apple.com/documentation/uikit/uicolor/secondarysystemfill)
- [Capsule](https://developer.apple.com/documentation/swiftui/capsule)
- [GlassButtonStyle](https://developer.apple.com/documentation/swiftui/glassbuttonstyle)
- [ButtonBorderShape](https://developer.apple.com/documentation/swiftui/buttonbordershape)
- [ControlSize](https://developer.apple.com/documentation/swiftui/controlsize)
- [Rejected bordered TextFieldStyle](https://developer.apple.com/documentation/swiftui/textfieldstyle)
- [Rejected textInputBorderShape](https://developer.apple.com/documentation/swiftui/view/textinputbordershape(_:))
- [Menus and actions](https://developer.apple.com/design/human-interface-guidelines/menus)
- [Progress indicators](https://developer.apple.com/design/human-interface-guidelines/progress-indicators)
- [DataScannerViewController](https://developer.apple.com/documentation/visionkit/datascannerviewcontroller)
- [SF Symbols](https://developer.apple.com/sf-symbols/)

## Acceptance

- **Sign In** on Welcome presents a medium native sheet; Close or swipe-down returns to Welcome.
- **Sign In** from Add Profile appears at the medium detent inside the existing onboarding sheet and native Back returns to its Welcome step.
- The field label and helper or error copy align with the editable text inset.
- Tapping the field focuses the native SecureField and opens the keyboard.
- The sheet expands through its native large detent as focus begins, and the Sign In action moves with the keyboard safe area without an intermediate jump.
- Empty and unfocused shows Paste and Scan QR Code; focused shows the outside Close action; populated and unfocused shows Clear without an outside action.
- Close dismisses the keyboard without clearing the field.
- The primary action is disabled for empty values and values that do not begin with `nsec`.
- Focusing the private-key field does not alter the primary action’s disabled appearance.
- Invalid input displays the approved recovery copy.
- A prototype-valid value enables Sign In.
- The field is a 50-point filled capsule that visually belongs with the accepted Name/About fields and aligns with the circular QR action.
- The trailing action never overlaps or obscures the secure value.
- Sign In replaces its visible label with a centered native spinner without changing button dimensions or losing contrast.
- In Light Mode the enabled action has white content on black; in Dark Mode it has black content on white. The disabled Sign In action uses semantic label color with the native disabled glass treatment instead of white text on light gray.
- During the two-second processing state, the field uses SwiftUI's system disabled treatment, accepts no input, and shows no internal or external accessory actions.
- Scan QR Code pushes the scanner screen and a scanned payload returns to Sign In.
- The scanner viewport fills the sheet and the native Back control overlays it.
- The screen uses the same white onboarding canvas as Sign Up.
- The medium detent removes the unused full-screen whitespace while preserving room for keyboard presentation; no custom fitted height or handmade modal surface is used.
