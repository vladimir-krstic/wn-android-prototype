# Reference snapshot instructions

- Everything under `reference/` is read-only source evidence.
- Never edit files inside `wn-ios-prototype-snapshot/` during Android product
  work. Refresh it only as a separate user-approved source-sync task.
- Never add a runtime or build-time dependency from Android production code to
  this tree.
- Swift, SwiftUI, UIKit, SF Symbols, Apple APIs, and Apple-specific metrics are
  evidence of product intent only. Translate them through active Android
  guidance before implementation.
- When source behavior and a copied brief disagree, inspect the related copied
  tests and record the resolution in the Android screen brief or decisions.

