# Optional application fonts

B26 preserves **System** as the default and adds Manrope, Outfit, Urbanist and
Figtree as optional profile-owned typefaces. No runtime download or third-party
application library is involved. Material weights, sizes and line heights remain
authoritative; explicit monospace surfaces retain their own family.

## Provenance and notices

The input fonts come from production Android baseline
`319454889f1c2494dec4a69b5577d98017f44eee`, under `app/src/main/res/font/`.
Each font's embedded name table identifies its author and SIL Open Font License
1.1. Accompanying full OFL texts were verified against the respective family in
Google Fonts commit `dbe3d528ea82cfefb82ebc2f8e2ca55af7a1060f`:

- [Manrope](https://github.com/google/fonts/blob/dbe3d528ea82cfefb82ebc2f8e2ca55af7a1060f/ofl/manrope/OFL.txt)
- [Outfit](https://github.com/google/fonts/blob/dbe3d528ea82cfefb82ebc2f8e2ca55af7a1060f/ofl/outfit/OFL.txt)
- [Urbanist](https://github.com/google/fonts/blob/dbe3d528ea82cfefb82ebc2f8e2ca55af7a1060f/ofl/urbanist/OFL.txt)
- [Figtree](https://github.com/google/fonts/blob/dbe3d528ea82cfefb82ebc2f8e2ca55af7a1060f/ofl/figtree/OFL.txt)

The complete licenses and the exact embedded copyright notices are packaged
under `app/src/main/assets/font-licenses/`. This also retains Manrope's embedded
2019 notice alongside the Google Fonts license's 2018 notice. Those files are
available for B31's license inventory. The headers contain no Reserved Font Name
declarations. [OFL redistribution guidance](https://openfontlicense.org/how-to-modify-ofl-fonts/)
governs the notice and license retention.

## API 23 compatibility and reproducibility

Manrope's medium, semibold and bold binaries are copied without modification,
preserving production's three-weight mapping. For the other families, static
400/500/600/700 instances are generated from their pinned variable fonts. This
avoids relying on API 26 variable-font support on the prototype's API 23 minimum.
Glyph maps and author/license records are retained. Text needing glyphs absent
from a chosen family still relies on Android's font fallback; device/font-script
inspection remains pending, not inferred from compilation.

[The manifest](font-assets.json) records every source/output SHA-256, weight,
glyph count, author notice and license source/hash. Reproduce the resources with
host-only `fonttools==4.64.0` and [materialize-fonts.py](../../scripts/materialize-fonts.py):

```sh
python scripts/materialize-fonts.py --source /path/to/pinned/android/archive --licenses /path/to/family-OFL-files
```

The license input directory must contain `manrope-OFL.txt`, `outfit-OFL.txt`,
`urbanist-OFL.txt` and `figtree-OFL.txt` from the pinned URLs above. The script
makes no network calls, preserves source timestamps and validates the static
weight, lack of variable axes and unchanged character map for each output.
It is build preparation only; do not add FontTools to the Android runtime.
