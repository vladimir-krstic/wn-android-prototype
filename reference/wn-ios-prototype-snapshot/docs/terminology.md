# White Noise terminology

This is the canonical English glossary for ordinary product UI. Add or revise
an entry only after a product decision; current code or another app is not
authority by itself.

## Preferred terms

| Concept | Use | Avoid in ordinary UI | Notes |
| --- | --- | --- | --- |
| Existing-profile entry | **Sign In** | Login, import identity, import nsec | Use on Welcome, the credential screen, and its primary action. |
| New profile | **Sign Up** | Create identity, generate identity, create account | Use on Welcome, the screen title, and the primary action. |
| Person’s White Noise presence | **Profile** | Account, identity | Use for creation, switching, sharing, and management. |
| Stored-profile actions | **Switch Profile**, **Add Profile**, **Remove Profile** | Switch identity, add account, wipe identity | Explain device-local consequences where relevant. |
| Secret credential | **Private Key** | nsec as the primary label, secret key | Secondary help may explain that it starts with `nsec`. Never reveal it. |
| Shareable identifier | **Public Key** | npub as the primary label, hex key | Show the actual `npub` value only when copying or sharing helps the task. |
| Conversation | **Chat** | Session, thread, MLS group | Use **Group** when membership or administration matters. |
| Discoverable participant | **Person**, **People**, or their name | User, peer, member outside a group | **Member** is correct inside a group. |
| Profile address | **Verified Nostr Address** | NIP-05, identifier | Use the complete label for editable fields and accessibility. Compact identity headers may show the address alone beneath the name. A trailing seal indicates a verified value; its absence means the value is not verified. |
| Message transport settings | **Relays** | NIP-65, outbox relay list | Relays and role assignments belong to the active profile. Relay Details uses **Profile**, **Inbox**, and **Chat Messages**. **Inbox** receives invitations; **Chat Messages** supplies the defaults for chats this profile creates. A role is available only while at least one assigned read/write relay is connected. Recovery names whether a role is unassigned, reconnecting, or disconnected and names the unavailable capability rather than calling the complete app offline. |
| Return-access protection | **Require Face ID** | Face ID Lock, PIN Lock | Uses system device-owner authentication: Face ID first with the iPhone passcode as fallback. There is no separate White Noise PIN. |
| App-switcher privacy | **Hide Screen in App Switcher** | Block Screenshots, Hide During Screen Capture | This setting affects the app-switcher snapshot only. It doesn’t promise screenshot or recording prevention. |
| Stop using active profile while retaining it | **Sign Out** | Logout, remove account | State that the profile remains on this device when the consequence matters. |
| Remove active profile and local data | **Wipe Data From This Device** within **Sign Out** | Delete account, wipe identity | Selected by default in the Sign Out sheet; explain that all local data is removed and previous chats don’t return after a later sign-in. |
| Remove every profile and all local White Noise data | **Erase App Data** | Device-wide sign-out, wipe all profiles, reset app | Privacy & Security device action. Always destructive, requires the generated three-word confirmation phrase, and returns to Welcome. |
| Remove another stored profile | **Remove Profile** | Delete account, wipe identity | Use for an inactive locally stored profile. |

## Technical-only terms

These are allowed in Developer Tools, Diagnostics, implementation notes, and
evidence. Do not use them as ordinary product copy unless the user explicitly
approves an exception.

- NIP numbers and raw event kinds
- `nsec` and `npub` as feature names or primary labels
- MLS, key package, gift wrap, epoch, and group state
- Marmot implementation details
- runtime, stream, subscription, control plane, outbox, and inbox relay list
- raw error names, codes, payloads, and engine messages

An implementation note may use exact terms to constrain behavior or safety.
That does not make those terms approved product copy. When precision is
necessary in ordinary UI, introduce the human term first and keep the technical
form secondary.
