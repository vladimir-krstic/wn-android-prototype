# QR Scanner

## Purpose and navigation

Scan a private-key QR code from Sign In. Native Back returns without changing the field. A successful scan returns to Sign In and fills the private-key field.

## Copy

- Navigation title: none; the native Back control remains visible over the scanner.
- Simulator permission title: **Allow Camera Access?**
- Simulator permission explanation: **Use the camera to scan a private key QR code.**
- Simulator permission actions: **Don’t Allow**, **Allow Camera**
- Wrong-content title: **Can’t Use This QR Code**
- Wrong-content explanation: **This QR code doesn’t contain a private key.**
- Wrong-content recovery: **Try Again**
- Denied title: **Camera Access Is Off**
- Simulator denied explanation: **Allow camera access to scan a private key QR code.**
- Simulator denied recovery: **Try Again**, which presents the permission choice again.
- Physical denied explanation: **Allow camera access in Settings to scan a private key QR code.**
- Physical recovery: **Open Settings**
- Unavailable title: **QR Scanning Unavailable**

## Native components

- `NavigationStack` destination and native Back behavior.
- Native SwiftUI `alert` for the simulator-only permission choice and wrong-content recovery.
- Native `ContentUnavailableView` for denied, restricted, and unavailable camera states.
- `DataScannerViewController` with QR-only recognition, native guidance, native highlighting, pinch to zoom, and high-frame-rate tracking on supported physical iPhones.
- `AVCaptureDevice` authorization status and request APIs on physical iPhones.
- `UIApplication.openSettingsURLString` for physical-device denial recovery.
- Shared Core Image QR generation using one fixed development-only private key.
- Apple-owned VisionKit guidance and highlighting on supported physical iPhones.

## Important behavior

- The simulator permission choice persists in Sign In for the current app process, matching iOS rather than appearing between scan attempts.
- Before permission is allowed, the simulator doesn’t display the camera scene.
- Denying permission does not advance the scan-outcome loop.
- Allowing permission reveals one static camera scene. The entire camera scene is one plain button.
- Simulator outcomes loop: wrong QR content → valid key → wrong QR content.
- Wrong content presents a native alert. **Try Again** keeps the scanner open for the next deterministic attempt.
- Valid returns immediately and fills the field.
- “Unreadable” is not modeled as an outcome. VisionKit keeps scanning when it recognizes nothing and may display its own contextual guidance.
- The bundled backdrop is an original generated image whose 941-by-1672 dimensions are preserved with photographic JPEG compression. The visible QR is rendered separately with Core Image so its data is deterministic.
- The simulator doesn’t imitate VisionKit’s private recognized-item highlight or invent success, warning, or error colors.
- Physical iPhones let VisionKit own guidance, recognized-item highlighting, geometry, and color.
- Physical iPhones use the real one-time system permission behavior. After denial, the app cannot show the system request again and instead offers Settings recovery.
- Camera permission, scan results, and outcome order remain in memory only.

## Accessibility

- The simulator camera scene is one button labeled **Scan QR Code**.
- Native alerts, unavailable views, navigation, and physical scanner behavior retain their system semantics.

## Apple references

- [Scanning data with the camera](https://developer.apple.com/documentation/visionkit/scanning-data-with-the-camera)
- [DataScannerViewController](https://developer.apple.com/documentation/visionkit/datascannerviewcontroller)
- [RecognizedItem](https://developer.apple.com/documentation/visionkit/recognizeditem)
- [AVCaptureDevice authorization](https://developer.apple.com/documentation/avfoundation/requesting-authorization-to-capture-and-save-media)
- [Open Settings](https://developer.apple.com/documentation/uikit/uiapplication/opensettingsurlstring)
- [Privacy HIG](https://developer.apple.com/design/human-interface-guidelines/privacy)
- [SF Symbols](https://developer.apple.com/sf-symbols/)

## Acceptance

- Simulator denial shows camera-disabled instructions; **Try Again** returns to the deterministic permission choice.
- Simulator approval shows the static scanner without a custom tracker or guidance bar.
- Denial never advances the outcome; completed attempts advance exactly once.
- The first attempt shows the native wrong-content alert and **Try Again** keeps scanning.
- The next attempt returns to Sign In and fills the key.
- No unreadable outcome or custom green, red, or orange scanner feedback appears.
- Supported physical iPhones retain VisionKit scanning and Apple-accurate permission recovery.
