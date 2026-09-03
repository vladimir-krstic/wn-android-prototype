# Implementation-agent prompt

Replace `BATCH_ID` with one batch from [the implementation plan](implementation-plan.md), then use this prompt for a new goal task.

---

Set a goal to implement **BATCH_ID** from the production Android parity audit in `/Users/vladimirkrstic/Workspaces/wn-android-prototype`.

Read every applicable `AGENTS.md`, the repository-local `white-noise-android-prototype` skill, `docs/decisions.md`, `docs/ui-metrics.md`, `docs/visual-polish.md`, `docs/product-language.md`, `docs/terminology.md`, `docs/references/native-ui.md`, and the selected batch in `docs/audits/production-android-parity/implementation-plan.md`. Read its linked capability rows, flow specification, decisions and existing `docs/screens/` brief. Production is read-only functional evidence at `marmot-protocol/whitenoise-android` `master` commit `319454889f1c2494dec4a69b5577d98017f44eee`; use the matrix's permanent source links. Confirm the production branch still resolves to that commit and the prototype files relevant to the batch still match `baseline-manifest.json`. If either source drifted, inspect only the affected diff and update audit evidence before implementation.

The production app defines the capability outcome and edge cases. This prototype defines presentation: quiet monochrome Material 3 Expressive, semantic status/error roles, shared tokens/components, Android Back and system surfaces, accessible/adaptive behavior and approved exceptions. Preserve all prototype-only behavior named in `sources-and-coverage.md`. Resolve superseded decisions and do not copy production styling, navigation structure or architecture.

Implement only the chosen bounded batch and its required shared dependency. Before code, update or split the relevant `docs/screens/` brief with exact behavior, entry/exit/Back, deterministic fixtures, states, proposed final copy, Android composition, accessibility/adaptation and observable acceptance criteria. Ask only if a linked Q-item blocks the exact slice; finish unblocked work first. Do not silently choose a product-conflict outcome.

Keep state deterministic, profile/chat-owned and in memory. Do not add backend, Marmot, networking, real authentication/crypto, persistence, notification delivery, services or new permissions unless the current user request explicitly expands that selected capability. Use capability wrappers and local fixtures for visible production results. Name the cited production controller/state/event seam in documentation so the later production migration reconnects to its SQLite/Marmot authority rather than copying fixture state as a second production database. Keep fixture/simulation language out of consumer UI.

Use the closest existing prototype component and Material/platform pattern. System-owned pickers, Sharesheet, Settings, security prompts and permission dialogs keep platform appearance. Support system/predictive Back, recreation ownership, IME/insets, RTL/localization, 200% type, compact/expanded windows, TalkBack, keyboard and non-color state cues. Re-check eligibility at mutation time. Async completion must not target a new profile/chat after navigation or account switching.

Add meaningful unit tests for state transitions, filters, ownership and regressions, and Compose tests for durable navigation, actions and semantics. Run targeted host checks during iteration, then the exact static gate in `README.md` after the batch. Do not use `adb`, an emulator, connected tests, install/launch the app or capture screenshots unless explicitly requested in the current task. Compiling instrumentation APKs remains host-side and does not imply visual verification.

Update the selected screen brief, `docs/port/feature-inventory.md`, the audit capability rows and batch status only with exact implementation/test evidence. Keep implementation, host verification, device verification and user visual acceptance separate. Do not mark a capability visually accepted without the user's hands-on acceptance.

Finish the complete batch. Report the changed behavior, files, validation, remaining limitations, linked unresolved decisions, and the next dependency-ready batch. Do not commit, push or publish unless explicitly requested.
