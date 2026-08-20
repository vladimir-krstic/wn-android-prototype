# Designer's Guide to Agent-Assisted UI Prototyping

This playbook is for designers building coded UI prototypes with a coding
agent. It is intentionally language- and framework-agnostic. The same workflow
works for web, mobile, desktop, games, or another interactive product surface.

The core idea is simple:

> Put durable working agreements in the repository, select one bounded piece
> of product work, write down what “done” means, and iterate visually in small
> steps.

The agent should do most of the setup. The designer supplies product direction,
references, visual judgment, and acceptance.

## 1. The operating model

Treat the agent as a capable teammate that can inspect the repository, research
official documentation, edit files, run tools, and report results. Do not use it
as a slot machine that receives a huge prompt and produces a finished app.

Use three layers of instruction:

1. **Repository rules** describe how work is always done. Put these in
   `AGENTS.md`.
2. **Product records** preserve language, terminology, accepted decisions,
   reference links, and screen or flow requirements in `docs/`.
3. **The current prompt** names the one outcome to work on now, its boundaries,
   and how it should be checked.

This separation keeps prompts short and prevents decisions from disappearing
inside old conversations.

For Codex specifically, `AGENTS.md` is persistent project guidance. Codex can
combine global instructions with project and nested instructions, with files
closer to the current working directory taking precedence. See
[Custom instructions with AGENTS.md](https://learn.chatgpt.com/docs/agent-configuration/agents-md).
If a different coding agent uses another repository-instruction filename, keep
the same separation of durable rules and task prompts, and avoid maintaining
two conflicting copies.

## 2. Start with almost no manual setup

Create or open the project folder, give the agent access to it, and send the
bootstrap prompt below. The agent should inspect the repository before deciding
which commands, framework rules, or reference sources belong in the files.

### Bootstrap prompt

```text
Set up this repository for fast, designer-led UI prototyping with an agent.

First inspect the repository, README, manifests, existing source structure,
design-system code, and any existing instruction or documentation files. Infer
the language, framework, supported targets, normal run commands, and existing
UI conventions from evidence. Do not change application source yet.

Then create or refine:
- AGENTS.md for durable project rules
- docs/product-language.md for voice and product-copy rules
- docs/terminology.md for canonical product terms
- docs/decisions.md for durable approved decisions
- docs/references/platform-ui.md as an index of current official platform and
  framework UI sources
- docs/references/ui-evaluation.md for the project's UI review method
- docs/screens/ and docs/flows/ only as work selects the first screen or flow

AGENTS.md must establish:
- the project's prototype mission and target platforms
- a clear authority order for conflicting instructions
- the supported language/framework and existing design system
- prototype boundaries and explicit non-goals
- one-screen-or-bounded-flow scope
- brief-before-implementation workflow
- official-source research rules
- production-ready product copy
- deterministic local prototype data unless real integration is requested
- reuse of platform or design-system components before custom controls
- preservation of unrelated work
- static validation by default
- no test creation for styling, layout, copy, fixtures, or straightforward UI
  composition
- no build, emulator, simulator, browser, preview, deployment, commit, or push
  unless I request it or the repository already requires a smaller check
- visual acceptance belongs to me after hands-on inspection

For docs/references/platform-ui.md, search the current official documentation
for the detected platform, UI framework, design system, accessibility model,
navigation, forms, feedback, motion, and testing tools. Open every adopted
source. Prefer first-party documentation and direct pages over search results or
third-party summaries. Record each direct link, what it governs, when to open
it, and the date verified. Do not copy large sections of source text.

Keep every file concise and specific to this repository. Do not invent screens,
architecture, services, dependencies, or product decisions. If one unresolved
question would materially change the setup, ask it; otherwise make conservative
assumptions, record them, and proceed.

Finish by listing the files created or changed, the important assumptions, and
the next best prompt for selecting our first screen.
```

This prompt deliberately asks the agent to discover facts rather than making
the designer manually describe package managers, folder conventions, test
commands, or platform documentation.

## 3. A small repository knowledge system

Start with the smallest useful structure:

```text
project/
├── AGENTS.md
└── docs/
    ├── product-language.md
    ├── terminology.md
    ├── decisions.md
    ├── references/
    │   ├── platform-ui.md
    │   └── ui-evaluation.md
    ├── screens/        # Create briefs only for selected screens
    └── flows/          # Use only for genuinely multi-screen behavior
```

Do not create an elaborate design-ops library on day one. Add a file only when
it resolves recurring ambiguity or supports selected work.

### What belongs where

| File | Purpose | Does not belong here |
| --- | --- | --- |
| `AGENTS.md` | Durable rules for how the agent works | Temporary task details or one screen's copy |
| `product-language.md` | Voice, tone, capitalization, action-label, error, and recovery rules | Screen layout |
| `terminology.md` | Canonical terms, definitions, and forbidden synonyms | Long product strategy |
| `decisions.md` | Approved choices that still govern future work | Every visual experiment |
| `references/platform-ui.md` | Direct, verified official sources and when to use them | Search snippets or copied articles |
| `references/ui-evaluation.md` | The repeatable review method | Screen-specific acceptance criteria |
| `screens/<name>.md` | Exact contract for one screen or tightly owned view | Speculative future screens |
| `flows/<name>.md` | Cross-screen sequence, state transitions, and completion | Duplicated details from screen briefs |

## 4. A framework-agnostic `AGENTS.md` starter

Ask the agent to adapt this template to what it finds. Remove placeholders and
irrelevant rules; a short, true file is stronger than a long generic one.

```md
# [Project name] UI Prototype

## Mission

Build a polished [target] prototype quickly, one designer-approved screen or
tightly related flow at a time. Product UI is for [audience]. Development notes
may use precise team terminology.

## Authority

When guidance conflicts, follow this order:

1. The designer's latest explicit direction.
2. Current official platform and framework documentation indexed in
   `docs/references/platform-ui.md`.
3. `docs/product-language.md` and `docs/terminology.md`.
4. The selected screen or flow brief and approved `docs/decisions.md` entries.
5. The current implementation and established local design-system patterns.
6. External examples as optional comparison evidence.

## Technical boundaries

- Use [language/framework/version] and support [targets/orientations/browsers].
- Reuse the repository's existing components, tokens, assets, and conventions.
- Do not add a backend, persistence, analytics, real authentication, payments,
  networking, third-party runtime dependencies, or new infrastructure unless
  the selected work explicitly requires it.
- Keep prototype state deterministic and local when real integration is not the
  product question being tested.
- Do not create speculative architecture, services, models, screens, or assets.
- Product-surface copy must be final-quality. Never show words such as dummy,
  fake, fixture, simulated, or prototype in user-facing UI.
- Preserve unrelated changes. Do not configure remotes, commit, push, publish,
  or deploy unless explicitly requested.

## UI defaults

- Start with the closest public platform control or established design-system
  component before creating a custom control.
- Let standard components own expected semantics, accessibility, focus,
  keyboard behavior, interaction states, and motion.
- Use semantic typography, color, spacing, and tokens before fixed visual
  values.
- Record why an approved custom component or platform exception is necessary.

## Screen and flow workflow

- Work only on the screen or bounded flow the designer selected.
- Before implementation, create or update one concise brief in `docs/screens/`
  or `docs/flows/`.
- The brief must contain purpose, scope, navigation, exact copy, building
  blocks, behavior, important states, accessibility, governing source links,
  and observable acceptance criteria.
- Record only durable accepted decisions. Do not document every temporary
  styling experiment.
- Before a material UI decision, open the relevant current official source from
  `docs/references/platform-ui.md`. Do not rely on remembered API behavior.
- If explicit designer direction differs from the platform default, follow the
  designer and record the approved exception in the brief.

## Validation and tests

- Use the smallest relevant static validation by default: formatting, parsing,
  linting, type checking, or an equivalent repository check.
- Do not create or expand tests for layout, styling, copy, deterministic
  fixtures, or straightforward view composition.
- Add a test only for meaningful nonvisual logic or a confirmed regression.
- Do not start a dev server, compile a full build, open a browser, emulator,
  simulator, preview, or device tool unless the designer explicitly requests
  inspection or the current task cannot be checked responsibly without it.
- Never claim visual verification without inspecting the current rendered
  result.

## Visual iteration

- When the designer supplies a screenshot or recording, inspect the current
  implementation, identify one concrete cause, and make one bounded change at
  a time.
- Do not stack speculative visual adjustments.
- The designer is the final visual and product acceptance authority.

## Completion

A screen is complete only when its observable acceptance criteria are met and
the designer accepts it after hands-on inspection.
```

### A hard stop for test-heavy agents

If the agent still creates unnecessary tests, replace the balanced test rule
with this stricter version:

```md
## Prototype validation boundary

- Do not create or edit test files unless the designer explicitly asks.
- For UI-only changes, run only the smallest existing formatter, parser, linter,
  or type checker that does not require a full application launch.
- Report any untested nonvisual logic risk instead of silently expanding scope.
```

Use the hard stop for exploratory prototype branches. Restore a balanced policy
before production engineering or whenever correctness-critical logic enters the
work.

## 5. How to write a screen brief

A screen brief is a handoff contract, not a diary and not a pixel specification.
It should let another agent or developer implement the accepted product behavior
without rereading the conversation.

Name it after the product concept, for example
`docs/screens/account-recovery.md`, not after a temporary component filename.

### Screen brief template

```md
# [Screen name]

## Purpose

[One or two sentences describing the person's task and expected outcome.]

## Scope and non-goals

- In scope: [behavior this screen owns]
- Out of scope: [adjacent behavior intentionally deferred]

## Entry, navigation, and exit

- Entry: [where it comes from]
- Presentation: [page, route, sheet, panel, dialog, embedded view, etc.]
- Back/cancel: [what happens and whether state changes]
- Success: [where the person goes and what changes]

## Exact product copy

- Title: **[Exact title]**
- Field label: **[Exact label]**
- Primary action: **[Exact action]**
- Empty state: **[Exact message]**
- Error/recovery: **[Exact message and action]**

## Building blocks

- [Existing design-system or native component and its purpose]
- [Navigation, form, menu, dialog, list, picker, feedback pattern]
- Custom exception: [why no standard equivalent fits, or “None”]

## Behavior

- [What happens on the main action]
- [Validation, focus, keyboard, selection, dismissal, or state rules]
- [What must not happen]

## Important states

| State | Trigger | Visible result | Available actions |
| --- | --- | --- | --- |
| Initial | Screen opens | [Result] | [Actions] |
| In progress | [Trigger] | [Feedback] | [Disabled/available actions] |
| Empty | [Trigger] | [Message] | [Recovery] |
| Error/offline/denied | [Trigger] | [Message] | [Recovery] |
| Success | [Trigger] | [Feedback/navigation] | [Next action] |

Delete irrelevant rows. Add destructive, permission, loading, or disabled
states only when the screen actually needs them.

## Accessibility and adaptation

- Reading/focus order: [rule]
- Labels, values, roles, and selected state: [rule]
- Keyboard or alternative input: [rule]
- Text scaling, contrast, reduced motion, localization, and responsive layout:
  [screen-relevant expectations]
- Meaning is not conveyed by color, position, sound, or motion alone.

## Governing sources

- [Component or pattern name](https://direct-official-source.example) — [what
  this source governs on this screen]

## Acceptance criteria

- [Observable statement a designer can verify in the running prototype]
- [Observable state transition]
- [Observable failure or recovery behavior]
- [Observable accessibility or responsive behavior]
```

Good acceptance criteria describe evidence, not implementation:

- Good: “The primary action remains disabled until the trimmed name is not
  empty.”
- Weak: “Use `isButtonDisabled = name.isEmpty`.”
- Good: “Cancel returns to the previous screen without changing the profile.”
- Weak: “The modal should work correctly.”

## 6. When a flow needs its own brief

Do not make a flow document for every navigation link. Use one when a product
outcome crosses multiple screens, owns shared state, has branching, or has
important cancellation and completion semantics.

### Flow brief template

```md
# [Flow name]

## User goal

[The outcome the person is trying to achieve.]

## Start and completion

- Starts: [entry condition and screen]
- Completes: [observable result and destination]
- Cancels: [where the person returns and what state is preserved]

## Screen sequence

1. [Screen](../screens/screen.md) — [decision or input owned here]
2. [Screen](../screens/screen.md) — [decision or input owned here]
3. [Screen](../screens/screen.md) — [completion]

## Flow state

| Event | Current state | Next state | Mutation/navigation |
| --- | --- | --- | --- |
| [Event] | [State] | [State] | [Observable result] |

## Branches, failure, and recovery

- [Branch condition and result]
- [What happens on failure]
- [How the person retries, goes back, or exits safely]

## Cross-screen rules

- [Shared data ownership]
- [Whether partial input survives Back, refresh, or dismissal]
- [Deduplication, atomicity, or navigation-history rule]

## Governing sources

- [Direct official source](https://direct-official-source.example) — [what it
  governs]

## Acceptance criteria

- [End-to-end observable outcome]
- [Cancellation outcome]
- [Important branch or recovery outcome]
```

Keep detailed copy and component decisions in the linked screen briefs. The flow
brief should not duplicate them.

## 7. Record decisions without creating bureaucracy

Add a decision only when it will govern future work, resolves a repeated
disagreement, or approves an exception. Do not record every color adjustment or
spacing experiment.

```md
## PROTO-0001 — [Short decision title]

- Date: YYYY-MM-DD
- Status: Approved
- Supersedes: [ID or “None”]

[The accepted decision in plain language.]

- [Important consequence]
- [Important boundary]
- [Reason or evidence when it prevents future re-litigation]
```

When the designer says “remember this for the rest of the project,” the agent
should update the appropriate durable file. A useful follow-up is:

```text
This is now an approved durable decision. Record it in the smallest appropriate
local file, update any affected screen brief, and then implement only the
selected change. Do not document the discarded visual experiments.
```

## 8. Prompt by outcome, not by micromanaged procedure

The most reliable prompt structure is:

- **Goal:** the observable result.
- **Context:** relevant files, screenshots, references, and current behavior.
- **Output:** code, a brief, a review, or a specific artifact.
- **Boundaries:** what must stay unchanged and what is out of scope.
- **Verification:** the smallest evidence that would show the result is sound.

This matches current [OpenAI prompting guidance](https://learn.chatgpt.com/docs/prompting),
which recommends stating the result, adding useful context, defining output and
boundaries, and refining with follow-ups.

### Prompt to select and implement one screen

```text
Work only on [screen name / bounded flow].

Goal:
[Describe the person, task, and observable outcome.]

Context:
- Inspect AGENTS.md and the relevant local product, terminology, decision, and
  existing UI files.
- Use [attached screenshot/Figma link/current screen] as visual or behavioral
  evidence for [specific parts].
- Preserve [approved behavior or nearby UI].

Before implementation:
1. Create or update docs/screens/[name].md.
2. Include purpose, navigation, exact production copy, existing/native building
   blocks, behavior, important states, accessibility, direct official source
   links, and observable acceptance criteria.
3. Research only material unresolved platform or framework decisions. Open
   current first-party documentation and add adopted links to the local
   reference index and screen brief.

Implementation boundaries:
- Do not create adjacent screens, speculative abstractions, backend work,
  persistence, new dependencies, or future-state architecture.
- Use deterministic local state and realistic content where integration is not
  part of the product question.
- Reuse existing components and platform patterns before adding custom UI.
- Do not add or edit tests for this UI work.
- Do not start a build or visual runtime unless I explicitly request it.
- Preserve unrelated changes.

Validation:
- Run only the smallest relevant static check.
- Report what changed, what was checked, and what still needs my visual
  inspection.

Ask only if an unresolved choice would materially change the product
experience. Otherwise make a conservative assumption, record it, and proceed.
```

### Prompt for documentation-only handoff

Use this when the designer wants another developer or agent to implement later:

```text
Prepare an implementation-ready brief for [screen/flow]. Do not change
application source.

Inspect the repository so the brief uses existing terminology, design-system
components, navigation conventions, data ownership, and file structure. Resolve
the product copy and all observable behavior that can be determined from the
provided direction.

Research current first-party platform/framework guidance for each material UI
decision. Open the sources and add direct links with a short note describing
what each governs. Do not use search snippets as authority.

Create or update:
- docs/screens/[screen].md for screen-owned behavior
- docs/flows/[flow].md only if behavior genuinely crosses screens
- docs/decisions.md only for durable choices that govern future work
- docs/references/platform-ui.md only for reusable official sources

The brief must contain exact production copy, entry/exit behavior, component
choices, all important states, accessibility and responsive behavior, explicit
non-goals, and observable acceptance criteria. Avoid pixel recipes unless I
approved a custom brand element that requires them.

Finish with a short “ready for implementation” summary and list only genuine
open decisions that require designer input.
```

### Prompt for source research

```text
Research the current official platform and framework guidance for [decision].
Use first-party sources only unless I explicitly request external product
comparison.

Open the relevant pages; do not rely on search snippets or memory. Compare the
closest standard components or patterns against our selected screen's task and
states. Recommend the smallest native/design-system solution, including
accessibility and recovery behavior.

Update docs/references/platform-ui.md with reusable links and the verification
date. Update the selected screen brief with only the sources and decision that
materially govern it. Do not implement yet.
```

### Prompt for a small visual correction

```text
Use the attached screenshot as evidence for one bounded correction to
[screen/region].

Observed problem: [what looks or behaves wrong].
Desired result: [what should be perceptibly different].
Preserve: [layout, behavior, copy, or components that must not change].

Inspect the current implementation and identify the concrete cause before
editing. Make the smallest change that addresses that cause. Do not stack other
polish, refactor nearby code, add tests, or change unrelated UI. Run only a
lightweight static check and tell me exactly what to inspect next.
```

### Prompt for a read-only review

```text
Review [screen/flow] against its local brief, the current implementation, and
the supplied screenshot or recording. Do not edit files.

Classify findings as:
1. Required correction — conflicts with approved behavior, applicable platform
   guidance, accessibility, safety, or the brief.
2. Quality improvement — materially improves hierarchy, clarity, feedback,
   resilience, or perceived polish.
3. Optional polish — subjective refinement with no meaningful usability or
   integrity impact.

For each actionable finding, state the evidence, user impact, recommended
pattern, affected states, accessibility consideration, and an observable
acceptance criterion. Cite an official source only when it materially supports
the finding. Ignore unrelated architecture and missing UI snapshot tests.
```

## 9. Chunk work so the agent can move fast

The best unit of work is normally one screen, one component with all its states,
or one tightly related flow. A chunk should be small enough that the designer
can inspect it in a few minutes and specific enough that “done” is visible.

### Good chunks

- Empty and populated states of one list screen.
- One form plus validation, progress, success, and recovery.
- One dialog or sheet and the action that presents it.
- One responsive header across the agreed breakpoints.
- A short creation flow whose state and navigation must be designed together.
- One concrete visual correction from a screenshot.

### Chunks that are too large

- “Build the whole app.”
- “Implement onboarding, settings, profile, and notifications.”
- “Make every screen responsive and polished.”
- “Create the complete design system before the first screen.”

### A practical sequence

1. **Select:** agree on one screen or bounded flow.
2. **Brief:** lock purpose, exact copy, states, sources, and acceptance.
3. **Structure:** implement navigation, hierarchy, and core behavior.
4. **State pass:** add empty, disabled, progress, error, permission, destructive,
   and recovery states that actually apply.
5. **Accessibility pass:** check semantics, focus, keyboard, scaling, contrast,
   motion, and responsive behavior.
6. **Hands-on inspection:** the designer runs and uses the result.
7. **Polish loop:** one observed cause and one bounded change at a time.
8. **Accept:** record any durable decision and move to the next chunk.

Do not polish a screen whose hierarchy or behavior is still unresolved. Do not
build generalized infrastructure until repeated implementation proves a shared
abstraction is necessary.

## 10. Move fast without becoming careless

### Use deterministic prototype state

If the question is “Does this interaction make sense?”, a real backend usually
slows learning. Use realistic fixed content and in-memory state. Simulate only
the minimum timing or failure needed to inspect a meaningful state.

Keep simulation language out of product UI. A person should see final-quality
copy even when the implementation is intentionally local.

### Start with the platform and the existing system

Use the platform's standard navigation, controls, menus, dialogs, focus,
feedback, and accessibility behavior—or the repository's established design
system—before inventing a custom control. Custom UI creates extra design,
implementation, responsive, accessibility, and review work.

Custom work is justified when it represents product identity or a task no
standard component can express. Record the reason and its acceptance criteria.

### Separate required work from optional polish

Ask the agent to finish required behavior first. Keep quality improvements and
optional polish visible but separate. This prevents a tiny spacing opinion from
blocking a usable prototype.

### Use static checks during iteration

Formatting, parsing, linting, or type checking usually catches accidental code
damage faster than a full build. Build and launch at meaningful visual review
points, not after every edit.

Tests are valuable for state machines, parsers, calculations, reducers,
permissions, security rules, data transformations, and confirmed regressions.
They are usually a poor use of prototype time for static layout, copy, styling,
fixture content, or direct framework composition.

### Preserve the live workspace

Tell the agent that unrelated edits belong to someone else. It should inspect
version-control status, touch only in-scope files, and never reset, commit,
push, install dependencies, or publish without explicit authorization.

### Keep the designer in the visual loop

Static code inspection cannot prove visual quality. The agent may check syntax
and implementation consistency, but the designer should accept the rendered
result after hands-on use.

If the designer manually changes or reverts code, tell the agent. Otherwise it
may restore an old direction from conversation context.

## 11. Polish with evidence

“Make it more polished” is too broad. Give the agent a screenshot or recording,
name the region, and describe the perceptual problem:

- “The primary action competes with the title.”
- “The selected state is too dependent on color.”
- “The bottom action jumps when the keyboard appears.”
- “The empty state looks like an error.”
- “The row rhythm becomes uneven when text wraps.”

Then ask for one correction. Inspect it before requesting another. This makes
cause and effect legible and avoids a pile of speculative offsets.

Useful polish passes are:

1. **Hierarchy:** what is primary, secondary, and supporting?
2. **Rhythm:** do spacing and alignment form a coherent pattern?
3. **States:** does every interaction explain what happened and what to do next?
4. **Adaptation:** does it survive small/large viewports, text scaling, themes,
   localization, and input methods?
5. **Motion:** does motion explain continuity or causality without delaying use?
6. **Restraint:** can borders, colors, cards, labels, or effects be removed?

## 12. Review at the right cadence

Use three levels of review:

### During implementation

Check the current brief and the smallest static validation. Do not launch a
large review process for every small edit.

### At screen acceptance

Inspect the rendered screen, exercise its important states, and compare the
result with observable acceptance criteria. The designer decides whether it is
accepted.

### At a milestone

After several accepted screens form a coherent journey, review cross-screen
navigation, repeated patterns, copy consistency, accessibility, responsive
behavior, and only then consider consolidating proven duplication.

Do not preemptively refactor because two components look vaguely similar. A
shared abstraction is justified when repeated use has revealed the stable
common behavior and the meaningful variations.

## 13. Common failure modes and the correction

| Failure mode | Better instruction |
| --- | --- |
| The agent designs future screens | “Work only on the selected screen; do not create speculative routes, models, or assets.” |
| The agent writes lots of tests | “Do not create or edit test files for this UI task; run the smallest static check.” |
| The agent launches heavy tools constantly | “Do not build or launch until I ask for visual inspection.” |
| The UI exposes fake-data language | “Use final product copy; keep fixture and simulation language in developer-only files.” |
| The brief becomes a diary | “Record durable accepted decisions, not every iteration.” |
| The agent invents a custom control | “Open current official component guidance and explain why the closest standard component cannot fit.” |
| The agent changes unrelated files | “Inspect repository status first; preserve unrelated changes and limit edits to the selected scope.” |
| A screenshot causes many speculative edits | “Identify one concrete cause and make one bounded change.” |
| The review is a list of personal opinions | “Classify required corrections, quality improvements, and optional polish; attach evidence and acceptance criteria.” |
| Old chat context overrides a new decision | “The latest explicit designer direction wins; record the accepted change in the brief or decision log.” |
| Links rot or become cargo cult | “Open current first-party sources, record the verification date, and state what each link governs.” |

## 14. A fast first week

### Day 1: Bootstrap and first brief

- Run the bootstrap prompt.
- Review the generated `AGENTS.md` and correct only material assumptions.
- Select one representative screen, not the entire application.
- Ask the agent to create its brief without implementing.

### Day 2: Structural implementation

- Approve the brief.
- Implement hierarchy, navigation, exact copy, and the happy path.
- Use deterministic data and existing components.
- Run a small static check.

### Day 3: States and accessibility

- Add only applicable empty, progress, disabled, error, permission, destructive,
  and recovery states.
- Check semantics, focus, keyboard, scaling, contrast, responsive behavior, and
  reduced motion.

### Day 4: Hands-on visual loop

- Ask the agent to run or prepare the app for inspection.
- Capture screenshots or a short recording.
- Iterate through bounded corrections, one cause at a time.

### Day 5: Accept and reuse

- Accept the screen or list the remaining required corrections.
- Record durable decisions.
- Reuse the successful prompt and brief structure for the next screen.
- Extract a shared component only if actual repetition has proven it.

## 15. Final checklist

Before starting a task:

- Is there one selected screen or bounded flow?
- Has the agent read the repository rules and relevant local documents?
- Is the desired outcome observable?
- Are non-goals explicit?
- Is visual or behavioral reference evidence attached and explained?

Before implementation:

- Does a concise brief exist?
- Is all product copy exact and final-quality?
- Are important states and recovery described?
- Were material platform decisions checked against current official sources?
- Are acceptance criteria observable rather than code-specific?

Before calling it complete:

- Was only the selected scope changed?
- Were unrelated changes preserved?
- Was the smallest relevant validation run?
- Were unnecessary tests, dependencies, builds, and architecture avoided?
- Was the rendered result actually inspected?
- Did the designer accept it?
- Were durable decisions recorded without documenting every experiment?

The workflow is successful when each next screen becomes easier: prompts stay
shorter, the agent makes fewer incorrect assumptions, handoffs require less
conversation, and polish comes from visible evidence instead of random tweaks.
