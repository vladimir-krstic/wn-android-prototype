# Goal prompt: production Android capability audit

This is the source prompt for the audit executed on 2026-09-03. It requests an
audit and implementation handoff; it does not request implementation.

---

Set a goal to thoroughly audit our production Android app against our Android
prototype and produce an evidence-backed, implementation-ready backlog for
bringing all production Android product capabilities into the prototype.

## Inputs and intended outcome

- Production Android repository:
  `https://github.com/marmot-protocol/whitenoise-android`
- Production baseline: default `master` at
  `319454889f1c2494dec4a69b5577d98017f44eee`
- Android prototype repository:
  `/Users/vladimirkrstic/Workspaces/wn-android-prototype`
- Prototype baseline: the current working tree, including existing tracked and
  untracked work. Record its exact state before auditing.

We have already developed and polished the prototype around iOS product
parity. The production Android app has additional capabilities. We now want
to extend the prototype to cover those capabilities using the UI, UX, visual
language, and Android interaction patterns established here. Later, we will
bring that completed UI/UX back into the production Android app.

Your deliverable is the audit and a practical handoff for a subsequent
implementation agent. Find entire missing flows, missing screens, and missing
functionality, content, actions, and states within existing screens. Organize
the work around complete user outcomes, with screen-level detail where it
helps implementation.

## Authority and boundaries

Read all applicable `AGENTS.md` files and use the local
`white-noise-android-prototype` skill. This request authorizes a repository-wide
capability audit and planning exercise. Subsequent implementation should
remain one selected flow or bounded batch at a time.

For this audit, production Android is the functional reference for capability
coverage. The prototype's current approved decisions and shared components
govern presentation. Read `docs/decisions.md`, `docs/ui-metrics.md`,
`docs/visual-polish.md`, `docs/product-language.md`, `docs/terminology.md`,
`docs/handoff.md`, relevant `docs/screens/` briefs, and `docs/port/` evidence.
Resolve superseded decisions instead of treating every historical description
as current. Preserve existing prototype capabilities even when production
does not have them. Keep the pinned iOS reference for its accepted scope; do
not update that baseline.

Treat differences in product rules as explicit findings. Distinguish an
approved prototype difference from an accidental omission. Propose a
resolution when production behavior conflicts with an accepted prototype
decision; do not silently discard either requirement. Follow the prototype's
approved language while preserving the meaning of production constraints,
security claims, validation, and consequences.

The prototype remains deterministic and in memory. Missing networking,
authentication, cryptography, persistence, or backend infrastructure is not
itself a UI parity gap. Audit its user-visible consequences and specify local
states and transitions that let the prototype demonstrate them faithfully.
Record where real behavior will reconnect during the later production UI
migration. Proposals for new device capabilities, permissions, dependencies,
or changes to that boundary must be identified explicitly.

Keep production read-only. Limit prototype edits to the audit documents.
Preserve existing work; do not change application code, dependencies, approved
decisions, existing parity statuses, or screen briefs. Do not commit, push,
publish, or create issues. Do not use `adb`, emulators, connected tests, device
launches, or device screenshots: this request authorizes source inspection.
Clearly separate source evidence, existing test evidence, and any checks
actually run during this audit. Do not claim visual or runtime verification.

## Audit method

1. **Establish reproducible sources.** Record both repository locations,
   branch/ref names, resolved commit SHAs, audit date, and working-tree
   differences. Include relevant untracked files. Record enough file hashes
   and change provenance to detect drift when implementation starts. If the
   production source is unavailable or ambiguous, ask for that specific
   missing input; do not substitute another repository or guessed branch.

2. **Map the production product surface.** Discover its actual stack and
   architecture. Trace navigation, screens, sheets, dialogs, menus, actions,
   settings, strings, state holders, state transitions, validation, models,
   tests, manifests, permissions, intents, deep links, notifications, and
   relevant background behavior. Follow entry points through their handlers
   and resulting state. Do not infer a working feature from a name, string,
   TODO, mockup, unused class, or backend method alone.

3. **Check reachability and variants.** Record build flavors, feature flags,
   account/role conditions, permission gates, and device/API differences that
   alter the available product. Separate release features, gated features,
   developer tools, incomplete features, and dead/unreachable code. Give each
   relevant area an explicit disposition; do not quietly omit it. Keep
   developer capabilities distinct from consumer flows.

4. **Map the prototype independently.** Trace its actual routes, components,
   state models, fixtures, handlers, strings, and tests. Use the existing
   parity ledger as a starting point, then verify it against current code.
   Similar screen titles or screenshots do not establish behavioral parity.

5. **Compare user outcomes and contracts.** For each production capability,
   identify its prototype equivalent and compare entry/exit paths, content,
   actions, mutations, eligibility, limits, validation, side effects,
   cancellation, recovery, and cross-screen consistency. Inspect happy,
   loading, empty, success, error, retry, offline/unavailable, permission,
   destructive, and role-dependent states where relevant. Include Back,
   lifecycle/restoration, profile isolation, accessibility, and adaptive
   behavior where they affect the capability. Mark unknowns rather than
   inventing behavior or claiming an omission without checking alternatives.

6. **Design the implementation handoff.** Recommend how each gap fits the
   existing prototype information architecture and shared UI. Prefer extending
   an existing screen or component when it serves the same user outcome; add
   a destination when the task warrants one. Reference current component and
   token paths. Preserve the quiet, monochrome Material 3 Expressive direction
   and approved exceptions. For material new platform recommendations, consult
   the current official sources through `docs/references/android.md` and
   `docs/references/native-ui.md`. Do not copy production styling or introduce
   a new visual system.

Use a coverage checklist, not assumptions about what this app contains.
Potential areas include onboarding/identity, discovery and contacts, chat
creation and organization, conversations and message lifecycle, composer and
media, groups and membership, notifications and external entry points,
privacy/security, relays/connectivity, settings, import/export/recovery,
support, diagnostics, and destructive flows. Investigate additional areas
revealed by the code. Mark checklist areas absent when evidence supports it.

## Required output

Create a linked Markdown audit package under
`docs/audits/production-android-parity/`. Use stable capability, flow, and batch
IDs throughout. Keep the capability matrix authoritative; link to details
instead of maintaining duplicate status lists.

### `README.md` — findings and navigation

Explain the scope, source baselines, method, major findings, proposed new
screens versus existing-screen changes, work ordering, unresolved decisions,
and coverage limits. Summarize counts from the matrix with a clear denominator.
Link every detailed deliverable and recommend the first bounded batch.

### `sources-and-coverage.md` — evidence and completeness

Record source provenance and a coverage map of inspected product areas,
navigation entry points, build variants, flags, and relevant source/test paths.
List inaccessible or uninspected areas and explain their impact. Include a
reverse map of prototype-only capabilities to preserve during implementation.

### `capability-matrix.md` — complete comparison ledger

Include covered capabilities as well as gaps. For each capability record:

- Stable ID, flow, user outcome, and production availability/gates.
- Production evidence: repository/ref, path, symbol, and useful line references.
- Prototype evidence and corresponding destination/handler, or documented
  search coverage supporting the finding that an equivalent is missing.
- One status: **covered**, **partial**, **missing**, **behavioral divergence**,
  **unverified**, or **excluded with reason**.
- The exact difference, important missing states/content, and evidence limits.
- Treatment: extend existing surface, add surface/flow, shared capability,
  preserve approved difference, or resolve a decision.
- Linked flow specification and implementation batch, or explicit blocker.

Define statuses. “Covered” means equivalent user-visible capability within the
prototype's approved boundary, supported by source evidence; it does not mean
the user has accepted its visuals. Separate missing functionality from an
intentional offline implementation. Exclusions need a source-backed reason or
an explicit user decision; complexity alone is not an exclusion.

### `flows/<flow-id>-<name>.md` — implementable flow specifications

For every flow requiring work, provide:

- Purpose, linked capability IDs, present behavior, and required outcome.
- Exact existing surfaces to extend, proposed new surfaces, entry points,
  route context, navigation/Back/cancel behavior, and completion destinations.
- A state/action table covering conditions, visible content and controls,
  user events, resulting state, validation, recovery, and side effects.
- Required content and proposed product-ready copy, with uncertain or
  decision-dependent wording identified.
- Deterministic fixtures and a repeatable way to reach each important state;
  keep fixture controls and implementation language off consumer surfaces.
- Shared components/tokens to reuse, hierarchy and interaction guidance,
  accessibility, insets/IME, scaling, RTL, and adaptive considerations.
- Production rules that must survive the eventual UI migration, with source
  pointers to the real state/events/services they depend on. Describe the
  necessary integration boundary without designing a speculative backend.
- Observable acceptance criteria and appropriate host tests. Identify any
  later device/user acceptance checks separately.
- Dependencies, unresolved decisions, and facts versus proposed UX choices.

Keep these as audit specifications. Create or update the actual `docs/screens/`
brief only when a subsequent implementation batch is selected.

### `implementation-plan.md` — ordered, bounded batches

Break the gaps into complete, manageable user flows. Each batch must name
capability IDs, dependencies, likely files to touch, reusable primitives,
necessary fixture/state changes, acceptance criteria, validation, and clear
non-goals. Explain priority using user impact and dependencies; do not invent
precise time estimates. Separate work ready to implement from work awaiting a
product decision. Group shared changes only when actual dependencies justify
them. Include regression criteria for existing prototype behavior.

### `decisions-and-questions.md` — unresolved choices

Record conflicts, uncertainty, evidence, recommended treatment, affected IDs,
and whether each decision blocks implementation. Resolve ordinary technical
choices using established rules. Ask the user only for unavailable source
access or choices that materially change product meaning or scope. Continue
independent audit work while such questions remain open.

### `implementation-agent-prompt.md` — reusable execution handoff

Write a prompt taking one batch ID. It must direct the next agent to verify
source drift, read the linked evidence and local rules, update the selected
screen brief, implement the complete bounded flow in the established design
system, preserve the offline boundary, run relevant host validation, and
update audit and parity records with exact evidence. It must distinguish
implementation, verification, and user visual acceptance. The prompt should
make source pointers available for focused follow-up without requiring the
agent to rediscover the whole product.

## Completion criteria

Before finishing, reconcile coverage against both navigation maps, external
entry points, reachable actions, and relevant state/permission/flag branches.
Every production capability discovered must have a matrix disposition. Every
actionable gap must map to a flow specification and batch; every unresolved
gap must have an explicit blocker. Preserve prototype-only capabilities in
the reverse map. Check IDs, links, counts, evidence references, and dependency
ordering for consistency.

Do not call the audit complete while relevant accessible areas remain
uninspected. Report access limits and unresolved evidence honestly. The result
must let an implementation agent take one ready batch and implement its
observable behavior in our existing UI system without inventing product rules
or doing another repository-wide audit.

Finish with links to the audit package, a concise account of the largest
gaps, proposed screen additions and extensions, the first recommended batch,
and any decisions that require my input. Stop after the audit and handoff.
