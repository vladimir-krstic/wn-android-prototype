# Codebase hardening audit

Status: Active repository-wide audit started 2026-08-31 on
`codex/overnight-hardening-20260831`.

This report tracks evidence, bounded fixes, verification, and intentionally
deferred work. A finding is not considered resolved until its implementation
and relevant verification are recorded. Visual acceptance remains the user's
decision even when device inspection passes.

## Baseline

- Android working tree was clean before the audit branch was created.
- The accepted read-only iOS commit
  `0bd7cbae56c92f07c7639be78b9bb62f8e5297cb` is available in the sibling
  repository; that repository is clean and remains unchanged.
- Repository inventory: 65 production Kotlin files, 20 host-unit-test files,
  20 instrumentation-test files, 132 Android resources, and 34 documentation
  files.
- Clean static gate:
  `./gradlew clean testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`
  passed in 40 seconds.
- Unit result: 137 tests, zero failures, zero errors, zero skipped.
- Both debug APKs assemble successfully.
- Lint reports 18 non-fatal findings: 11 `UnusedResources`, six deliberately
  deferred dependency-version warnings, and one `AutoboxingStateCreation`
  hint.
- Kotlin compilation initially reported deprecated Material API usage across
  list items, sheet state creation, and one text-field padding call. The
  behavior-equivalent state and padding replacements are complete. The list
  replacement is a design-sensitive expressive component and remains scoped
  to flow-level visual work.
- Device environment: a physical Pixel 8a is connected through ADB and the
  `Pixel_10_Pro_XL` emulator definition is available. The first connected run
  executed all 166 instrumentation tests but exposed test-host and device-
  wakefulness failures; it is not accepted as product evidence.

## Findings

| ID | Severity | Area | Evidence and governing expectation | Status | Verification / commit |
| --- | --- | --- | --- | --- | --- |
| H-001 | P2 | Resources | Lint identified `ic_send` plus ten strings as unused. Exact `R`-reference review confirmed the resources were not consumed. Removal preserves persisted GIF content and deletes only unreachable acquisition/review labels. | Fixed | `refactor: remove lint-proven dead resources`; lint no longer reports any unused resource |
| H-002 | P2 | Conversation state | Lint reported boxed `mutableStateOf(0f)` for a hot composer-offset value in `ConversationScreen.kt`; Compose provides primitive float state. | Fixed | `refactor: remove lint-proven dead resources`; migrated to `mutableFloatStateOf`; lint hint cleared |
| H-003 | P2 | Shared Material components | Kotlin compilation reported deprecated sheet-state and no-label outlined-field-padding overloads. Pinned alpha25 source provides explicit replacements. | Fixed | Sheet states now declare their allowed endpoints with `rememberBottomSheetState`; the external-label field uses `contentPaddingWithoutLabel`; static gate passes |
| H-003b | P3 | List components | The remaining deprecated `ListItem` replacement is a different expressive component with configurable padding, alignment, shapes, state colors, and elevation. A bulk rename could silently alter accepted geometry and semantics. | Deferred by design | Migrate only inside a bounded visually inspected flow; do not perform a repository-wide mechanical rewrite |
| H-004 | P2 | Documentation | `README.md` and the current handoff said the app declares only camera permission, while the manifest, notification implementation, tests, decisions, and screen brief also deliberately declare and request `POST_NOTIFICATIONS`. Historical decision and batch records correctly describe their earlier permission boundaries and remain unchanged. | Fixed | Current README and handoff now distinguish the just-in-time camera permission from the explicit Android 13+ notification-access gate and state that the prototype does not create or deliver notifications |
| H-005 | P2 | Device acceptance | The parity ledger and briefs still mark many flows as device/visual/TalkBack pending. Current goal explicitly authorizes device inspection. | Open | Pending bounded flow inspections |
| H-006 | P1 | Instrumentation reliability | The first full Pixel 8a run executed 166 tests: seven non-Compose tests passed and 159 Compose tests failed before producing valid product evidence. Fifteen pure component-test classes launched content-owning `MainActivity` and then called the rule's `setContent`; one keyboard class mixed custom and activity-owned content. The physical device also entered its 30-second doze timeout. Pure component tests now use `EmptyTestActivity`, the mixed keyboard test was split by host ownership, and a kept-awake rerun passed 115 of 166 tests. Removing all remaining multiple-`setContent` tests and correcting popup/visual geometry raised the second full baseline to 134 of 166. Authentic system Back and screen-coordinate outside taps, Material-expanded touch bounds, required menu edge clamping, and stable sheet geometry raised the next complete baseline to 151 of 166. Adaptive-width, chat-creation, diagnostics, and settings assertions now use accepted copy and geometry, lazy-list semantics, real native window input, device-density units, and explicit viewport positioning. A 165/166 run isolated one remaining order-sensitive incremental text-input assertion; whole-field replacement passed three consecutive focused runs and the next uninterrupted complete suite. | Fixed | Full Pixel 8a result: 166 tests, zero failures, zero errors, zero skipped, completed in 4m25s |
| H-007 | P3 | File organization | Several cohesive UI files exceed 1,000 lines, led by `ConversationComposer.kt` at 2,034 lines. Size alone does not justify a split; extraction is allowed only where the responsibility and tests form a real reusable boundary. | Audit only | Deferred unless concrete duplication or ownership defects are found |
| H-008 | P3 | Dependencies | Lint reports newer Navigation, Material, and CameraX artifacts. The goal explicitly forbids dependency upgrades, and Material alpha25 is a recorded API-23 compatibility decision. | Rejected for this goal | Retain pins; no implementation change |
| H-009 | P1 | Clipboard privacy | The explicit Copy Private Key action used the same ordinary clipboard payload as public identifiers. Android requires sensitive clipboard content to carry `ClipDescription.EXTRA_IS_SENSITIVE` so supporting system UI can redact the preview. | Fixed | The shared clipboard helper accepts an explicit sensitive flag, only the private-key action sets it, and a connected assertion reads the resulting clip metadata |
| H-010 | P1 | Navigation restoration | Navigation Compose can restore a signed-in destination after process recreation, but the prototype deliberately keeps profiles only in process memory. A new `AppViewModel` therefore had no authoritative content for the restored route and rendered a blank destination. | Fixed | The root graph now detects a non-onboarding destination without an active profile, clears the orphaned stack, and returns to Welcome; focused connected coverage drives the mismatch directly |
| H-011 | P1 | Relay identity | Custom relay IDs were derived only from a punctuation-stripped URL slug. Distinct valid URLs such as `/a-b` and `/a/b` could therefore share an ID, causing ID-targeted role or removal mutations to affect more than one row. | Fixed | Custom relay creation now adds the first available deterministic numeric suffix and a host-unit regression covers the exact slug collision |
| H-012 | P1 | Conversation search | Search projected every timeline message before matching sender metadata. Deleted messages with cleared text could still match their sender, and a deleted fixture retaining text could expose that content as a result. | Fixed | Deleted messages are removed before any searchable projection; host-unit coverage verifies both text and sender queries return no result |
| H-013 | P2 | Link drafts | The HTTPS detector used a case-insensitive regular expression but removed the scheme case-sensitively, so a valid uppercase `HTTPS://` URL never produced the promised preview. | Fixed | Domain extraction now uses `URI.host`, normalizes case, rejects malformed hosts, and has a host-unit uppercase-scheme regression |
| H-014 | P1 | Media preparation lifecycle | Five avatar/composer paths wrapped the suspending image decoder in `runCatching`, which also catches `CancellationException`. Replacing a selection or leaving a screen could therefore let stale work continue, overwrite the newer image, or mutate progress/error state after cancellation. | Fixed | The shared decoder returns ordinary I/O/decode failures as `null` but rethrows cancellation; every replaceable preparation job carries a generation token, updates only while current, and clears progress in a guarded `finally`; the profile editor also cancels an in-flight request when Edit is cancelled |
| H-015 | P1 | Activity Result restoration | The external camera output `Uri` and the private-key export payload were held only in ordinary remembered state. Activity recreation while either system-owned destination was open could return a successful camera result with no source to decode or export an empty file. | Fixed | Camera output is saveable per chat and launch failures enter the existing attachment-error state; key export saves only a non-secret request kind, derives its payload after the destination returns, clears encrypted credentials on completion/cancellation, and reports launcher/write failures through the existing save-error dialog |
| H-016 | P1 | Text-to-speech lifecycle | Every visible incoming text bubble eagerly constructed its own `TextToSpeech` engine. A conversation could therefore initialize many platform services at once, and `speak()` failure still forced that row into a false speaking state. | Fixed | One lifecycle-owned controller now serves the conversation, tracks the active message and range progress, flushes cleanly when another row starts, validates locale support and `speak()` success, ignores late callbacks after shutdown, and releases the single engine when the screen leaves composition |
| H-017 | P1 | Composer attachment menu | The focusable Material popup correctly anchored above the separate `+` action, but opening it transferred window focus and dismissed the keyboard even though the accepted composer behavior keeps the editor and IME active while choosing an attachment source. | Fixed | The shared menu retains its focusable default, while this one composer menu uses a non-focusable Material popup plus an activity-owned Back handler; a connected test verifies editor focus and real IME visibility, and Pixel 8a inspection confirms the elevated menu remains anchored above `+` with the keyboard visible |
| H-018 | P2 | External attachment handoff | Document and video Open actions caught every file, URI, security, and missing-viewer exception but discarded the result, leaving an apparently dead button on devices without a compatible external app. | Fixed | Both content-URI and bundled-resource paths retain recoverable exception handling and now show a concise native long-duration message explaining that the attachment could not be opened and suggesting another app; static gate passes |

## Checkpoints

| Checkpoint | Scope | Result | Commit |
| --- | --- | --- | --- |
| 1 | Clean baseline, repository map, initial prioritized findings | Complete; clean gate and evidence recorded | `docs: establish codebase hardening audit` |
| 2a | Lint-proven dead resources and boxed composer travel state | Complete; 11 resources removed and lint reduced from 18 findings to the six intentionally deferred dependency warnings | `refactor: remove lint-proven dead resources` |
| 2b | Behavior-equivalent pinned Material API cleanup | Complete; sheet endpoint state and external-label text-field padding use their current explicit APIs; unit, lint, and both assemble gates pass | `refactor: migrate safe Material APIs` |
| 3a | Compose instrumentation host ownership | Complete; 15 component classes use the empty host and the mixed onboarding class is split; meaningful Pixel 8a execution improves from 7/166 to 115/166 passing | `test: isolate Compose component hosts` |
| 3b | Invalid connected-test mechanics and semantics geometry | Complete; no instrumentation test calls `setContent` more than once, merged/unmerged selectors are explicit, popup gaps use shared screen coordinates, and visual tags expose outer visual bounds; all 16 affected tests pass in focused Pixel 8a runs | `test: repair connected test mechanics` |
| 3c | Native window input, Material touch bounds and sheet geometry | Complete; popup/dialog Back and outside taps reach their real windows, touch-target checks distinguish interaction bounds from visible containers, edge-clamped menus retain their minimum anchor gap, and sheet assertions use unchanged Material child geometry; all 38 tests across the affected six classes pass on Pixel 8a | `test: harden native overlay assertions` |
| 3d | Adaptive layout and chat-creation assertions | Complete; expanded chats assert the shared 680dp content cap instead of a removed title, lazy lists scroll selected members and people into semantics before interaction, merged rows expose child geometry deliberately, and action surfaces are sampled at explicit resting and scrolled states; all 24 tests across both classes pass on Pixel 8a | `test: reconcile adaptive creation flows` |
| 3e | Diagnostics prompt and settings mechanics | Complete; the sheet receives native scrim, drag-handle, and system-Back input, switch geometry remains individually observable without duplicating accessibility semantics, lazy stored-log content scrolls into composition, and spacing is asserted in device-density pixels; all seven tests pass on Pixel 8a | `test: harden diagnostics prompt coverage` |
| 3f | Settings copy, mode and dialog sequencing | Complete; hub assertions use accepted “Chat with support” copy, edit-mode photo acquisition uses the canonical capitalized action, and dialog fields no longer claim a nonexistent scrolling ancestor; each underlying settings action is repositioned after dialog dismissal; all 21 settings tests pass on Pixel 8a | `test: stabilize settings flow assertions` |
| 3g | Order-sensitive group-name input and complete connected baseline | Complete; replace the complete group-name field, verify its value before submitting, and report the callback value on failure instead of relying on incremental IME insertion; the test passes three consecutive focused Pixel 8a runs and the uninterrupted complete suite passes all 166 tests in 4m25s | `test: complete connected reliability baseline` |
| 4a | Private-key clipboard privacy | Complete; the private-key copy path marks its clip sensitive without changing ordinary public/donation/debug copy behavior; static gate and focused Pixel 8a integration assertion pass | `fix: protect private key clipboard preview` |
| 4b | Process-local navigation recovery | Complete; restored signed-in routes without restored business state clear back to Welcome instead of rendering an empty screen; static gate and focused Pixel 8a graph assertion pass | `fix: recover orphaned navigation state` |
| 4c | Collision-safe custom relay identity | Complete; punctuation-equivalent URL slugs receive deterministic unique IDs so row-targeted mutations remain isolated; host-unit regression and static gate pass | `fix: isolate custom relay identities` |
| 4d | Deleted-search and HTTPS projection correctness | Complete; deleted messages are excluded before matching any text/sender metadata and HTTPS host parsing honors the detector's case-insensitive contract; host-unit regressions and static gate pass | `fix: harden message search projections` |
| 4e | Cancellable media preparation | Complete; shared image preparation preserves coroutine cancellation, superseded requests cannot publish stale results, progress cleanup is generation-safe, and cancelling profile edit cancels active decoding; static gate passes | `fix: preserve cancellable media preparation` |
| 4f | Restorable external camera and key export | Complete; camera capture retains its output destination across Activity recreation and reports launch failure inline; raw/encrypted key export retains a request descriptor rather than transient or saveable secret payload, derives content only after destination return, and preserves the existing write-error UI; static gate passes | `fix: restore external result requests` |
| 4g | Conversation-owned TextToSpeech | Complete; all incoming bubbles share one locale-validated engine and one active utterance state instead of eagerly owning one engine each; failures and late callbacks cannot strand progress; static gate and all 32 `ConversationScreenTest` cases pass on Pixel 8a | `fix: share conversation speech lifecycle` |
| 5a | Composer attachment-menu focus | Complete; the composer-specific Material popup keeps the editor focused and the IME visible without weakening the focusable default for other menus; focused connected coverage and direct Pixel 8a inspection pass | `fix: preserve composer focus for attachments` |
| 5b | External attachment failure feedback | Complete; missing viewer, invalid URI, cache-copy and handoff failures now produce native user feedback instead of an inert Open action; static gate passes | Pending commit |

## Remaining audit coverage

- Production state/model derivations, profile isolation, and destructive
  consequences.
- Navigation graph, Back priority, restoration, lifecycle, and Activity Result
  ownership.
- System boundaries: CameraX/ML Kit, Photo Picker, Files, external camera,
  document/media handoff, Sharesheet, clipboard, notifications, TextToSpeech,
  and secure surfaces.
- Full unit/instrumentation test quality and connected execution.
- Resource/manifest/privacy review beyond lint findings.
- Device inspection of the highest-impact flows still lacking current visual
  evidence, including light/dark, IME, large type, RTL, and adaptive width.
- Final documentation and parity-ledger reconciliation.
