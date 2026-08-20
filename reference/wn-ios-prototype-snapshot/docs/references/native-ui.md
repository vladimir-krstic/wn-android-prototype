# Native Apple UI evaluation

Use this local process for every material component, interaction, or review
decision. Use `docs/references/apple.md` to open the current official Apple
source; do not rely only on memory.

## Evaluation sequence

1. State the person’s task and the observable outcome.
2. Identify the platform pattern: navigation, list, form, search, toolbar, menu,
   sheet, alert, confirmation, picker, share sheet, permission, or unavailable
   state.
3. Open the relevant indexed Apple source and select the closest public
   SwiftUI/UIKit component.
4. Let the component own typography, padding, shape, material, motion,
   accessibility, focus, and platform adaptation.
5. Check the complete state set required by the feature: loading, disabled,
   success, empty, error, offline, permission, destructive, and recovery.
6. Check accessibility, localization, keyboard behavior, feedback, motion, and
   perceived responsiveness.
7. Record the governing Apple links and observable decision in the current
   screen brief.

If explicit user direction differs from Apple’s default, follow the user and
record the approved exception. If no native component fits, state why before
building custom UI and define its accessibility and acceptance criteria.

## Native component checks

- Preserve familiar hierarchy, Back behavior, safe areas, and system margins.
- Use `NavigationStack`/`NavigationLink` for hierarchy, sheets for bounded
  tasks, menus for compact commands, and alerts only for critical actionable
  interruption.
- Use `Form`, `List`, `Picker`, `Toggle`, `TextField`, and other standard
  controls when their semantics fit; do not imitate them with arbitrary cards
  or gestures.
- Use SF Symbols for familiar interface actions and verify deployment-target
  availability.
- Give destructive actions an exact label, destructive role, proportional
  confirmation, and recovery when feasible.
- Keep important actions discoverable; a gesture must not be their only route.
- Keep progress, validation, success, and recovery feedback close to the action
  or content it describes.
- Use Liquid Glass through system controls and toolbars first. Apply custom
  glass only when the current brief records why no native treatment fits.

## Accessibility and localization

- Support Dynamic Type without clipping, overlap, or unreachable actions.
- Check VoiceOver labels, values, traits, grouping, order, and custom actions.
- Preserve the native 44-by-44-point iOS control target where practical and
  provide adequate separation between controls.
- Do not rely only on color, animation, sound, or haptics to convey state.
- Verify contrast in Light and Dark appearances.
- Respect Reduce Motion and avoid unnecessary depth, blur, bounce, or fixed
  delay choreography.
- Check Voice Control names, keyboard/focus behavior, localization expansion,
  and right-to-left presentation where relevant.
- Follow automated accessibility inspection with hands-on assistive-technology
  testing at milestone reviews.

## Motion, feedback, and performance

- Prefer navigation, presentation, control, and SF Symbol transitions supplied
  by the system.
- Add custom motion only to preserve continuity, explain causality, or reinforce
  direct manipulation; keep it interruptible and state-driven.
- Use haptics sparingly for meaningful selection, success, warning, error, or a
  threshold, and always retain visible feedback.
- Treat delayed feedback, blocked input, repeated layout, dropped frames, and
  visible hitches as product defects when reproducible.

## Review output

Classify observations as:

- **Required correction:** conflicts with the user’s direction, an applicable
  Apple pattern, accessibility, safety, or the screen brief.
- **Quality improvement:** materially strengthens hierarchy, clarity,
  feedback, resilience, or perceived polish.
- **Optional polish:** subjective refinement with no meaningful usability or
  integrity impact.

For each actionable finding, provide the evidence, user impact, recommended
native pattern, required states, accessibility behavior, and acceptance
criteria. Cite Apple only when it materially supports the decision.

## Visual iteration discipline

- When the user supplies a screenshot or recording, inspect the current source,
  identify one concrete cause, and make one bounded change at a time.
- Do not stack speculative visual adjustments.
- Do not claim visual verification without inspecting the current build.
- The user is the final visual and product acceptance authority.
