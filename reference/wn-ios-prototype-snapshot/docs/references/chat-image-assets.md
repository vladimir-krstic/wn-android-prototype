# Chat image assets

## Purpose

Record the bundled Unsplash images used by deterministic conversation members,
catalog rows, and message-media fixtures. The prototype performs no runtime
download. Member fixtures select from this pool deterministically by stable ID;
the image is an identity aid and does not claim to depict the fictional person.

All files are stored as size-optimized 512-by-512 JPEG assets and clipped by the
relevant SwiftUI presentation. The source license is the
[Unsplash License](https://unsplash.com/license).

## Member and catalog identity pool

| Asset | Photographer | Unsplash source |
| --- | --- | --- |
| `AvatarMayaChen` | Christina @ wocintechchat.com | [SJvDxw0azqw](https://unsplash.com/photos/SJvDxw0azqw) |
| `AvatarEliasMoreno` | Albert Dera | [ILip77SbmOE](https://unsplash.com/photos/ILip77SbmOE) |
| `AvatarMinaPark` | Jake Nackos | [IF9TK5Uy-KI](https://unsplash.com/photos/IF9TK5Uy-KI) |
| `AvatarLeoMartins` | Jurica Koletić | [7YVZYZeITc8](https://unsplash.com/photos/7YVZYZeITc8) |
| `AvatarNoraBennett` | Good Faces | [xmSWVeGEnJw](https://unsplash.com/photos/xmSWVeGEnJw) |
| `AvatarTheoGrant` | Joseph Gonzalez | [iFgRcqHznqg](https://unsplash.com/photos/iFgRcqHznqg) |
| `AvatarAishaRahman` | Clay Elliot | [mpDV4xaFP8c](https://unsplash.com/photos/mpDV4xaFP8c) |
| `AvatarLenaOrtiz` | Michael Dam | [mEZ3PoFGs_k](https://unsplash.com/photos/mEZ3PoFGs_k) |
| `AvatarJonahReed` | Ryan Hoffman | [Ft4p5E9HjTQ](https://unsplash.com/photos/Ft4p5E9HjTQ) |
| `AvatarTessaMorgan` | Christina @ wocintechchat.com | [Zpzf7TLj_gA](https://unsplash.com/photos/Zpzf7TLj_gA) |
| `AvatarMarcusBell` | Alex Suprun | [ZHvM3XIOHoE](https://unsplash.com/photos/ZHvM3XIOHoE) |
| `AvatarSofiaAlvarez` | Štefan Štefančík | [QXevDflbl8A](https://unsplash.com/photos/QXevDflbl8A) |
| `AvatarDanielKim` | Ludovic Migneault | [EZ4TYgXPNWk](https://unsplash.com/photos/EZ4TYgXPNWk) |
| `AvatarWebAionyHaust` | Aiony Haust | [3TLl_97HNJo](https://unsplash.com/photos/3TLl_97HNJo) |
| `AvatarWebChristopherCampbell` | Christopher Campbell | [rDEOVtE7vOs](https://unsplash.com/photos/rDEOVtE7vOs) |
| `AvatarWebIanDooley` | Ian Dooley | [d1UPkiFd04A](https://unsplash.com/photos/d1UPkiFd04A) |
| `AvatarWebSergioDePaula` | Sergio de Paula | [c_GmwfHBDzk](https://unsplash.com/photos/c_GmwfHBDzk) |
| `AvatarWebAyoOgunseinde` | Ayo Ogunseinde | [sibVwORYqs0](https://unsplash.com/photos/sibVwORYqs0) |
| `AvatarWebVinceFleming` | Vince Fleming | [j3lf-Jn6deo](https://unsplash.com/photos/j3lf-Jn6deo) |
| `AvatarWebPhilipMartin` | Philip Martin | [5aGUyCW_PJw](https://unsplash.com/photos/5aGUyCW_PJw) |

## Catalog media pool

The message gallery fixtures reuse `AvatarWebAionyHaust`,
`AvatarWebAyoOgunseinde`, `AvatarWebChristopherCampbell`,
`AvatarWebIanDooley`, `AvatarWebPhilipMartin`, `AvatarWebSergioDePaula`, and
`AvatarWebVinceFleming`. `AvatarGardenClub` supplies the botanical image and is
credited to alex starnes at
[WYE2UhXsU1Y](https://unsplash.com/photos/WYE2UhXsU1Y).

These square crops intentionally exercise portrait, botanical, bright, dark,
and high-contrast media in one deterministic gallery without introducing a
network or caching dependency.
