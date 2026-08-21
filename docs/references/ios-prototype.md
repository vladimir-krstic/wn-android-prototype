# iOS prototype reference

## Accepted parity baseline

- Repository: [`vladimir-krstic/wn-ios-prototype`](https://github.com/vladimir-krstic/wn-ios-prototype)
- Source branch at capture: `master`
- Accepted baseline commit: `0bd7cbae56c92f07c7639be78b9bb62f8e5297cb`
- Original capture date: 2026-08-15 in Europe/Belgrade
- Local source copy removed: 2026-08-21 after the scoped Android port reached
  its complete static implementation gate

The original Android-repository snapshot was recorded as commit
`58785a4724f33e23135c4dd3f98f231fca6a809d` plus eight working-tree files.
Those eight files are exactly the complete diff committed by `0bd7cba`, and
their captured contents match that commit. The former snapshot is therefore
fully reproducible from the accepted baseline above; removing the copied tree
does not discard unique parity evidence.

## Authority and drift

Android briefs, decisions, tests, and `docs/port/feature-inventory.md` are the
working authority for the completed port. Consult the pinned iOS baseline only
when those local records do not resolve a product-behavior, copy, fixture, or
state question.

The upstream `master` branch may advance. A newer commit does not automatically
become Android scope and must not silently supersede accepted Android decisions.
Comparing or syncing newer iOS behavior is a separate, explicitly requested
task.

## Read-only access

Start with `docs/port/source-map.md` and inspect only the relevant upstream
paths. When the sibling checkout exists, read a file without checking out or
modifying the iOS working tree:

```bash
git -C ../wn-ios-prototype show \
  0bd7cbae56c92f07c7639be78b9bb62f8e5297cb:<path>
```

The same commit can be inspected through the private GitHub repository when
the sibling checkout is unavailable. Do not clone, fetch, switch branches, or
change the iOS working tree merely to answer a parity question when a read-only
Git or GitHub lookup is sufficient.

Swift, SwiftUI, UIKit, SF Symbols, Apple APIs, and Apple-specific metrics are
evidence of product intent only. Translate them through
`docs/references/native-ui.md` and current official Android guidance. Never add
a runtime or build-time dependency from Android production code to the iOS
repository.

## Future parity sync

Only sync newer iOS behavior after explicit user approval. Before changing the
accepted baseline:

1. record the proposed branch and commit;
2. diff relevant product docs, behavior, tests, and resources from `0bd7cba`;
3. identify whether each difference is already covered, intentionally
   Android-specific, or genuinely new scope;
4. update the parity ledger, source map, affected Android briefs, and this
   baseline only after the decision is accepted.
