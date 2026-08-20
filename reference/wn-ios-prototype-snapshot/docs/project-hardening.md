# Project Hardening

## Scope and method

This is the evidence ledger for the one-time full-project hardening pass started
on 2026-08-15. The review covered application source, tests, Xcode settings,
resources, product copy, screen briefs, supporting documentation,
accessibility, launch and first-use performance, runtime behavior, memory and
temporary-file handling, and repository hygiene.

The implementation remains a deterministic, in-memory iPhone prototype. The
review did not add networking, persistence, real authentication, cryptography,
third-party runtime dependencies, or speculative architecture.

Apple's current guidance was the evaluation authority for platform decisions:

- [Improving app responsiveness](https://developer.apple.com/documentation/xcode/improving-app-responsiveness)
- [Diagnosing performance issues early](https://developer.apple.com/documentation/xcode/diagnosing-performance-issues-early)
- [Understanding and improving SwiftUI performance](https://developer.apple.com/documentation/xcode/understanding-and-improving-swiftui-performance)
- [XCTApplicationLaunchMetric](https://developer.apple.com/documentation/xctest/xctapplicationlaunchmetric)
- [Performing accessibility testing for your app](https://developer.apple.com/documentation/accessibility/performing-accessibility-testing-for-your-app)

Simulator measurements are regression evidence for this prototype, not claims
about shipping-device performance.

## Reviewed inventory

- 214 intentional repository files after generated-render cleanup.
- 55 application Swift files and 13 unit/UI test Swift files.
- 31 Markdown documents, including 23 screen briefs.
- One Xcode project, three targets, and one shared scheme.
- 97 asset-catalog files across 47 image sets, four PDF fixtures, one video
  fixture, and one Icon Composer asset.
- Apple frameworks only; no package or third-party runtime dependency graph.

Every source and documentation file was included in the static review. The
asset catalog, icon JSON, bundled PDFs, video reference, project file, build
settings, app permission copy, product-surface copy, and repository-generated
artifacts were reviewed separately.

## Findings and corrections

### Required fixes

| ID | Finding | Correction and evidence |
| --- | --- | --- |
| H-001 | The Welcome path eagerly created every profile, person, chat, event, and message fixture before the first screen was useful. | Profile fixtures are now lazy. The Welcome screen no longer pays for the signed-in graph, while sign-in and UI-test entry points still construct the same deterministic state. Fixture construction fell from a 15.512 ms median to 1.999 ms. |
| H-002 | Fixture construction repeatedly created date formatters, parsed constant ISO dates, and resolved bundled files. | Constant dates, resource lookups, voice data, and formatters are cached or parsed once. A unit performance regression test protects full graph construction. |
| H-003 | Chat projection and message rendering repeated avoidable scans and setup, including link detection, mention regex creation, reply lookup, date work, and Core Image context creation. | Projection work was consolidated, reusable detectors/regex/context were cached, and per-render timeline/reply calculations were reduced without changing product behavior. |
| H-004 | Photo, video, file, image-processing, and temporary-file work could remain on the main actor or outlive its presentation. | Transfer and processing work now crosses explicit asynchronous boundaries, observes cancellation, cleans discarded temporary files, and removes queued temporary attachments when cancelled or sent. |
| H-005 | First voice playback activated the session asynchronously, but `AVAudioPlayer` still started and released hardware synchronously on the main actor. | Session activation/deactivation uses the iOS 27 asynchronous APIs; player prepare/play/pause/stop operations run in order on a dedicated user-initiated serial queue with cancellation identity and deterministic cleanup. The focused voice result bundle is runtime-warning-free. |
| H-006 | The compact composer could reserve its height too late, causing the initial bottom message to sit underneath it; overlay geometry also intercepted the voice control. | The compact and flexible layouts now use separate hit-testing geometry, report their measured reservation, and re-anchor the transcript once on first measurement. Focused composer and voice regressions pass. |
| H-007 | Creating the first direct chat could update model state without navigating to the newly created conversation. | Navigation now consumes the returned chat identifier after creation. A UI regression verifies creation, navigation, send, list preview, and deduplication. |
| H-008 | Existing Swift Testing assertions failed to compile under Xcode 27 macro isolation rules. | Mutating operations are evaluated before assertions and throwing ambiguity was removed. The complete unit suite now compiles and passes. |
| H-009 | Release builds exposed an actor-isolation diagnostic and deprecated `Text` concatenation. | The gesture producer has correct main-actor isolation and highlighted text uses current interpolation/attributed construction. Debug and Release builds are warning-free. |
| H-010 | Several controls lacked durable accessibility semantics, the custom voice control had an incorrect hit region, and large Dynamic Type could make the conversation header unusable. | Labels, values, identifiers, hints, status meaning, header adaptation, focus behavior, and native button hit testing were corrected. A primary-surface system accessibility audit and focused interaction regressions were added. |
| H-011 | A notice projection assumed an impossible nonempty value with a crash path, and a UIKit decoding initializer crashed despite having no useful decode behavior. | Impossible or unsupported inputs now fail safely; the one remaining unavailable coder initializer is deliberately unreachable by API contract. |
| H-012 | `UISearchBar` delegate callbacks wrote the focus binding synchronously while SwiftUI was updating its representable. | Delegate writes are now idempotent and occur only for an actual state transition. The complete search flow passes with no state-update runtime warning. |

### Documentation and product consistency

| ID | Finding | Correction |
| --- | --- | --- |
| D-001 | Chat Info's shared-media acceptance criterion contradicted its chat-wide paging behavior. | The brief now requires paging across the available chat history. |
| D-002 | Chat Catalog reused an action identifier. | Catalog identifiers are unique and stable. |
| D-003 | Older Fiatjaf and shared-conversation text conflicted about shared bubble/composer mechanics and voice cancellation. | Story-specific requirements remain local; common mechanics now defer to the shared brief and use one accepted threshold. |
| D-004 | Settings described implemented per-chat relay editing as deferred. | Settings, Chat Info, and the relevant decision record now agree. |
| D-005 | README, permission strings, catalog terms, and implementation boundaries were incomplete or inconsistent. | README now reflects the current app, build/test workflow, self-contained product authority, and prototype boundaries; product copy no longer exposes implementation language. |

### Reviewed invariants retained

The review did not mechanically replace every forced cast or route lookup when
doing so would weaken a proven framework or navigation invariant:

- `ConversationCameraPreviewView.previewLayer` casts its backing layer only
  after overriding `layerClass` to `AVCaptureVideoPreviewLayer`.
- Person, chat, and member route children index model collections only after
  their parent views validate the route identifier and stop rendering when it
  is absent.
- `AvatarWebImagePicker` retains one `@available(*, unavailable)` coder
  initializer because the programmatic view cannot be decoded from a storyboard.
- Invalid bundled voice data has a development assertion plus a safe empty-data
  fallback; it is not a product crash path.

No `TODO`, `FIXME`, `HACK`, `try!`, `unsafeBitCast`, app debug print, or custom
production telemetry remains. No telemetry system was added: this local-only
prototype has no server, consent model, or persistence boundary. Reproducible
XCTest metrics and local profiling are the appropriate evidence surface.

## Organization and design review

The project retains a feature-oriented structure: process-local models and
fixture operations live under `App`, shared native surfaces under `Components`
and `Screens/Shared`, and screen-specific behavior under its owning screen
folder. Test targets mirror behavior boundaries rather than implementation
files. Model mutations remain explicit on the profile/chat values and no
service, repository, dependency-injection, or persistence layer was introduced
without a real production need.

`ConversationView` and `ChatInfoView` are the largest remaining source files.
They own unusually broad, already accepted screen behavior, while reusable
message, composer, attachment, camera, media, and action surfaces are separated
into neighboring files. A line-count-only split would create new cross-file
state APIs without reducing runtime work or product risk, so this pass kept the
screen coordinators intact and reduced their repeated computation and unsafe
work in place. Future extraction should be driven by the next accepted screen
boundary, not by speculative architecture.

## Performance evidence

All comparisons use the same iPhone Air simulator on iOS 27.0. Samples are
small, deliberately local, and interpreted as regression evidence.

| Measurement | Baseline | Hardened | Interpretation |
| --- | ---: | ---: | --- |
| Full fixture graph, median | 15.512 ms | 1.999 ms | 87.1% faster; the app-owned work most consistent with the reported first-use pause was removed. |
| Full fixture graph, worst sample | 16.294 ms | 2.115 ms | Stable improvement with a performance regression test. |
| Welcome launch, five-run average | about 7.556 s | 7.493 s | XCTest/simulator launch overhead dominates; no regression. Welcome no longer builds signed-in data. |
| Signed-in launch, five-run average | about 8.066 s | 8.061 s | No regression; deterministic signed-in fixture cost is lower. |
| First conversation-open signpost | 0.751 s | 0.710 s | Improved first navigation on the same simulator harness. |
| Repeated conversation-open signpost | Not separately protected | 0.528 s average, 0.83% RSD | Repeat behavior is stable and now has an XCTest metric. |
| Welcome resident memory | 303,040 kB median, 306,368 kB worst | 300,240 kB median, 302,832 kB worst | Small repeatable reduction; no growth regression. |

The absolute launch numbers include the Xcode UI-test runner, simulator process
startup, and beta-tool overhead. The fixture benchmark isolates the app-owned
work and is the more actionable explanation of the user's cold/first-use
report. Time Profiler command-line recording was also attempted, but this Xcode
27 beta's `xctrace` repeatedly stalled during simulator dyld startup; no trace
was treated as valid evidence. XCTest launch/application signposts, repeated
RSS/physical-footprint sampling, static main-thread review, and a five-second
local process sample were used instead.

## Validation matrix

| Area | Evidence | Result |
| --- | --- | --- |
| Debug build | Generic iOS Simulator, fresh Derived Data | Passed; no project warnings |
| Release build | Generic iOS Simulator, fresh Derived Data | Passed; no project warnings |
| Static analyzer | Debug, generic iOS Simulator, fresh Derived Data | Passed; no analyzer findings |
| Unit and model tests | Full unit target on iPhone Air | 102 passed, 0 failed, 0 skipped; no runtime warnings |
| UI behavior | Authentication/profile exit, direct/group/support chats, composer, attachments, forwarding, relays, message actions, voice, pinning | 24 passed, 0 failed, 0 skipped; post-fix search/voice rerun 2 passed with no runtime warnings |
| UI performance | Welcome/signed-in launch and first/repeated conversation open | Four metric tests pass with low sample variation |
| Accessibility | Primary-surface system audit plus focused labels, Dynamic Type, contrast, clipping, and hit-region checks | System audit passes in the definitive full suite |
| Resources | JSON parsing, reference scan, duplicate-payload scan, image-set coverage, PDF extraction/rendering | Passed |
| Documentation | Every Markdown file read; local-link scan excludes fenced examples; copy/term/catalog checks | Passed |
| Static hygiene | `git diff --check`; unsafe-pattern, debug-output, stale-term, generated-artifact, link, JSON, and SVG scans | Passed after final cleanup; 214 intentional files |
| Memory/resource lifecycle | Repeated RSS/footprint, process sample, cancellation/temp-file inspection | Installed build held a stable 247,974,576-byte physical footprint across five samples; main thread was idle in `CFRunLoop` for all 3,710 sampled main-thread intervals |

## Toolchain limitations

- The final full UI suite reports one framework-owned warning when UIKit
  presents the native `UIButton.menu` embedded by `UIViewRepresentable`:
  `_UIReparentingView` is attached to the hosting controller. The public native
  menu path remains functionally correct in its passing ordering, blocking,
  selection, and document-picker regression. Replacing it with a custom menu
  solely to silence this Xcode 27 beta diagnostic would violate the project's
  native-component rule.
- `xctrace` repeatedly stalled while the beta simulator loaded dyld, and both
  `leaks` and `vmmap` terminated with signal 10 when attached to the simulator
  app. No output from those failed tools is treated as evidence. The working
  `footprint` and `sample` tools, XCTest metrics, analyzer, runtime-warning
  audit, resource cleanup review, and lifecycle regressions provide the
  available local evidence instead; this ledger does not claim a device-level
  leak proof.
- Xcode's App Intents metadata processor emits its own informational warning
  that extraction was skipped because this app has no App Intents dependency.
  Application compilation and both generic builds are otherwise warning-free.

## Resource review

All asset and icon JSON parses successfully. Every image set has a source
reference, all 46 raster payloads decode with valid dimensions, the catalog and
icon SVGs parse, no asset payloads are byte-identical, and no unreferenced image
set was found. The bundled clip is a valid 8-second, 720-by-720 H.264 MP4. The
four bundled PDF fixtures were extracted and rendered at 120 DPI: each is a
single unencrypted US Letter page without JavaScript, a form, or suspicious
metadata, and all rendered without clipping, overlap, missing glyphs, or
corruption. Rendered PNGs are review artifacts and are not retained in the
repository.

## Environment and reproducibility

- Xcode 27.0 beta, build 27A5209h.
- iOS 27.0 simulator, build 24A5370g.
- Booted iPhone Air: `8E7FA142-44E8-4F89-A315-112298937399`.
- Every Xcode build and test command uses
  `DEVELOPER_DIR=/Applications/Xcode-beta.app/Contents/Developer`.
- Device Hub was attempted first but was unavailable during this pass. The
  explicitly authorized `simctl`/XCTest fallback reused the existing booted
  iPhone Air; no second simulator was started.

## Completion audit

The technical hardening pass is complete: intentional fixes are implemented,
the final repository checks pass, generated review artifacts are removed, and
the signed Debug build is installed and running on the same iPhone Air used for
validation. The remaining framework/tool limitations are bounded above rather
than hidden. Product and visual completion still belongs to the user's
hands-on acceptance.
