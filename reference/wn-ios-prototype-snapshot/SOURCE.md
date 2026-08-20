# Snapshot source

This directory is a read-only capture of the White Noise iOS prototype for the
Android port.

- Source branch: `master`
- Source commit: `58785a4724f33e23135c4dd3f98f231fca6a809d`
- Capture date: 2026-08-15
- Capture includes the dirty working-tree files listed in
  `../../docs/references/ios-prototype.md`.
- Source `AGENTS.md` is stored as `AGENTS.ios.md` so it cannot govern Android
  work by path scope.
- Build output, Git metadata, external agent configuration, and per-user Xcode
  workspace state are excluded.

Do not edit or depend on this directory from production Android code. Use
`../../docs/port/source-map.md` to select evidence and
`../../docs/references/native-ui.md` to translate it.
