# Verified Nostr Address

## Purpose and scope

Show the email-shaped address people can recognize after a profile and its
public key exist. Profile can edit it; Share & Connect, its Profile Found state,
User Profile, and Group Member display it. All values and verification states
remain deterministic and in memory.

## Copy and native components

- The only product label is **Verified Nostr Address**. **NIP-05** never appears
  in ordinary UI.
- Sign Up does not ask for or display the address because the public key does
  not exist yet. Initial and added profiles receive their deterministic address
  only after Sign Up completes. Re-onboarding an existing stored profile does
  not replace its address.
- Editable Profile uses a native `TextField` in its grouped `Form`. The email
  keyboard, no automatic capitalization, and no autocorrection match the
  address-shaped input.
- Share & Connect, Profile Found, User Profile, and Group Member place the
  address directly below the name as compact secondary text without a section,
  card, capsule, or visible field label. The public-key capsule follows it.
  Freeform About text does not interrupt the name/address/key identity sequence;
  an existing direct chat exposes it from Chat Info's focused About action.
- Every compact read-only presentation uses the same middle abbreviation as
  one-on-one Chat Info. Values longer than 38 characters keep the first 18 and
  final 19 characters around one ellipsis, approximately 20 percent shorter
  than the longest current fixture. This prevents the address from reaching a
  containing list section's rounded clipping edge. Public-key presentation is
  unchanged.
- A verified value has the trailing SF Symbol `checkmark.seal.fill`. This is the
  Apple seal-shaped checkmark requested by the user; an unverified value has no
  trailing symbol.

## Important states

- Completed initial and Add Profile Sign Up create the deterministic verified
  addresses **marmota@whitenoise.example** and
  **pebble@whitenoise.example**, respectively.
- Editing a verified address makes a different draft unverified. Re-entering
  the exact stored verified address during the same edit restores the seal.
  Cancel restores the stored address and verification state.
- A valid value has one nonempty local part, one `@`, and a domain containing a
  dot. A valid unverified value can still be committed.
- An invalid draft shows **Enter an address like name@example.com.** in the
  native section footer and disables Done until corrected.
- The wiggly seal appears only when the stored or draft value is verified; no
  color-only distinction communicates verification.

## Accessibility

- The compact read-only line announces **Verified Nostr Address**, the complete
  address, and either **Verified** or **Not verified**.
- The seal is hidden from the combined read-only announcement. The editable
  text field announces its complete value and either **Verified** or
  **Not verified**, while the visual seal remains decorative to avoid a
  duplicate announcement.
- The address abbreviates and, when space still requires it, truncates through
  the middle visually without changing the value VoiceOver receives. Dynamic
  Type and right-to-left layout remain native.

## Governing Apple sources

- [Text fields](https://developer.apple.com/design/human-interface-guidelines/text-fields)
- [TextField](https://developer.apple.com/documentation/swiftui/textfield)
- [Form](https://developer.apple.com/documentation/swiftui/form)
- [SF Symbols](https://developer.apple.com/design/human-interface-guidelines/sf-symbols)
- [SF Symbols browser and availability](https://developer.apple.com/sf-symbols/)

## Acceptance criteria

- Sign Up contains no Nostr-address field, label, validation, or seal.
- Completing Sign Up creates the profile's deterministic verified address only
  after the profile's public key exists.
- Profile Edit commits or cancels the address and verification state together.
- Deleting or changing a verified Profile address hides its seal immediately;
  entering the original stored address again restores it before Done.
- Profile, Share & Connect and its Profile Found state, User Profile, and Group
  Member show the address. Except for editable Profile, these surfaces use the
  compact inline presentation under the name; verified fixtures show
  `checkmark.seal.fill` and unverified fixtures do not.
- No profile surface displays **NIP-05** or invents network verification.
