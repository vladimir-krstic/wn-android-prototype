# Documentation instructions

- Keep active Android product decisions in this `docs/` tree. The
  `reference/` tree is evidence, not a place to edit current requirements.
- Update `decisions.md` only for durable choices that affect later work. Do not
  log every visual experiment.
- Preserve exact approved product copy in the selected screen brief. Keep
  platform-neutral intent separate from Android implementation notes.
- Before changing a platform pattern, verify the relevant current official
  Android source through `references/android.md`; do not rely on remembered
  behavior or version numbers.
- Do not translate Apple measurements, component names, or motion recipes
  directly. Use `references/native-ui.md` to document the Android equivalent
  and any necessary custom exception.
- Keep `port/feature-inventory.md` factual. A status requires evidence from the
  Android implementation or explicit user acceptance.
- Treat `screens/` as just-in-time briefs. Do not predesign unselected screens.

