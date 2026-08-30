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
- Kotlin compilation reports deprecated Material API usage across list items,
  sheet state creation, and one text-field padding call. These compile today
  but are tracked so the pinned Material version is not carrying avoidable
  migration debt.
- Device environment: a physical Pixel 8a is connected through ADB and the
  `Pixel_10_Pro_XL` emulator definition is available. Connected execution and
  current screenshots have not yet been claimed.

## Findings

| ID | Severity | Area | Evidence and governing expectation | Status | Verification / commit |
| --- | --- | --- | --- | --- | --- |
| H-001 | P2 | Resources | Lint identifies `ic_send` plus ten strings as unused. Exact `R`-reference review confirms the resources are not consumed. Removal must preserve persisted GIF content and only delete unreachable acquisition/review labels. | Open | Pending safe-cleanup checkpoint |
| H-002 | P2 | Conversation state | Lint reports boxed `mutableStateOf(0f)` for a hot composer-offset value in `ConversationScreen.kt`; Compose provides primitive float state. | Open | Pending safe-cleanup checkpoint |
| H-003 | P2 | Shared Material components | Kotlin compilation reports deprecated `ListItem`, sheet-state, and outlined-field-padding overloads in production and one test. The pinned alpha25 replacement APIs must be inspected before mechanical migration. | Open | Pending safe-cleanup checkpoint |
| H-004 | P2 | Documentation | `README.md` says the app declares only camera permission, while the manifest, notification implementation, tests, decisions, and screen brief also deliberately declare and request `POST_NOTIFICATIONS`. | Open | Pending documentation reconciliation |
| H-005 | P2 | Device acceptance | The parity ledger and briefs still mark many flows as device/visual/TalkBack pending. Current goal explicitly authorizes device inspection. | Open | Pending bounded flow inspections |
| H-006 | P1 | Instrumentation reliability | The complete connected suite has not yet been run against the current build; historical briefs record unrelated legacy harness/assertion failures. Current authoritative result is missing. | Open | Pending connected baseline |
| H-007 | P3 | File organization | Several cohesive UI files exceed 1,000 lines, led by `ConversationComposer.kt` at 2,034 lines. Size alone does not justify a split; extraction is allowed only where the responsibility and tests form a real reusable boundary. | Audit only | Deferred unless concrete duplication or ownership defects are found |
| H-008 | P3 | Dependencies | Lint reports newer Navigation, Material, and CameraX artifacts. The goal explicitly forbids dependency upgrades, and Material alpha25 is a recorded API-23 compatibility decision. | Rejected for this goal | Retain pins; no implementation change |

## Checkpoints

| Checkpoint | Scope | Result | Commit |
| --- | --- | --- | --- |
| 1 | Clean baseline, repository map, initial prioritized findings | Complete; clean gate and evidence recorded | `docs: establish codebase hardening audit` |

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
