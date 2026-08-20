# White Noise terminology

This is the canonical English glossary for ordinary Android product UI. Revise
an entry only after a product decision; source code or another app is not
authority by itself.

| Concept | Use | Avoid in ordinary UI | Notes |
| --- | --- | --- | --- |
| Existing-profile entry | **Sign In** | Login, import identity, import nsec | Use on Welcome, the credential screen, and its primary action. |
| New profile | **Sign Up** | Create identity, generate identity, create account | Use on Welcome, the screen title, and its primary action. |
| Person's White Noise presence | **Profile** | Account, identity | Use for creation, switching, sharing, and management. |
| Stored-profile actions | **Switch Profile**, **Add Profile**, **Remove Profile** | Switch identity, add account, wipe identity | Explain device-local consequences where relevant. |
| Secret credential | **Private Key** | nsec as the primary label, secret key | Secondary help may say it starts with `nsec`. Never reveal it in logs or accessibility output. |
| Shareable identifier | **Public Key** | npub as the primary label, hex key | Show the `npub` only where copying or sharing helps. |
| Conversation | **Chat** | Session, thread, MLS group | Use **Group** when membership or administration matters. |
| Discoverable participant | **Person**, **People**, or their name | User, peer, member outside a group | **Member** is correct inside a group. |
| Profile address | **Verified Nostr Address** | NIP-05, identifier | Use the full label for editable fields and accessibility. A trailing seal indicates verified state. |
| Message transport settings | **Relays** | NIP-65, outbox relay list | Relays belong to the active profile. Roles are **Profile**, **Inbox**, and **Chat Messages**. Name the unavailable capability rather than saying the whole app is offline. |
| Return-access protection | **Require device authentication** | Require Face ID, Face ID Lock, PIN Lock | Android may offer biometrics, device credential, or both through the system prompt. Final wording is selected with the implemented authenticator policy. |
| Recent-tasks privacy | **Hide Screen in Recents** | Hide Screen in App Switcher, Block Screenshots | Covers the Android Recents snapshot only; it does not promise screenshot or recording prevention. |
| End active session | **Sign Out** | Logout, remove account | State whether local profile data remains. |
| Remove active profile data | **Wipe Data From This Device** within **Sign Out** | Delete account, wipe identity | Selected by default in the accepted flow; explain that local chats do not return after later sign-in. |
| Remove all local app data | **Erase App Data** | Device-wide sign-out, wipe all profiles, reset app | Signs out every profile, permanently removes local White Noise data, and returns to Welcome. |
| Remove another local profile | **Remove Profile** | Delete account, wipe identity | Use for an inactive locally stored profile. |

## Technical-only terms

These are allowed in Developer Tools, Diagnostics, implementation notes, and
evidence. Do not use them as ordinary product copy unless the user approves an
exception:

- NIP numbers and raw event kinds
- `nsec` and `npub` as feature names or primary labels
- MLS, key package, gift wrap, epoch, and group state
- Marmot implementation details
- runtime, stream, subscription, control plane, outbox, and inbox relay list
- raw error names, codes, payloads, and engine messages

An implementation note may use exact terms to constrain behavior or safety.
That does not make those terms approved product copy.

