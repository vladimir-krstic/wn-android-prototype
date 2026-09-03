# Signal Android emoji asset provenance

White Noise reaction surfaces use an exact, offline snapshot of Signal
Android's bundled emoji artwork by explicit user direction. This is an artwork
and generated-data import only; production rendering, state, search, and UI are
app-owned Kotlin/Compose code.

## Pinned source

- Repository: `signalapp/Signal-Android`
- Commit: `879651dc47a7b18b67e7aea52a25197875024680`
- Source directory: `lib/emoji/src/main/assets/emoji/`
- Android destination: `app/src/main/assets/signal_emoji/`
- Included upstream legal files: `LICENSE-Signal-Android.txt` and
  `NOTICE-Signal-Android.txt`

The 20 WebP pages and `emoji_data.json` are copied without modification. The
app does not include Signal's `EmojiProvider`, source-selection, downloader,
database, or update code and performs no emoji network access.

## Rendering contract

The app-owned renderer reads the generated manifest, locates a 66×66 px tile
in a 16-column page, removes the one-pixel atlas gutter on every edge, and
bitmap-filters the remaining square into the requested Compose bounds. This
matches the observable geometry of the pinned Signal renderer while avoiding
text-font metrics and the clipping they caused.

## SHA-256 manifest

```text
d54af836e00fa226e6dad0a848e56f752f13c61d7a733a8b16a5e0866bf2d249  Activity.webp
9ddd1ed8218a9553ca2ea98af81387d397f3fb0ff889fd1b1cda5d40951d73c5  Flags_0.webp
4fc81404973b613d8fe7a050199b91c01062cc14c9e666ce8184d2574729fb5b  Flags_1.webp
0fa4ae1ecd831fda591c799e3052cf2cf5b909767180fe8abe5356d0ff882bc2  Foods.webp
a5fa65ab3423ebd9add2acda3a4d308c88da9da796dbcefe1f36f0314d0d97c9  Nature.webp
ea9c9dda5b1dea817b6f85cd8a1d6c2327c9314ffb3b787968b050a9a649ec51  Objects_0.webp
e36bdabde84ce7e901e3cb9ba54dd589d2ff745b28683c764a204b9734f8f227  Objects_1.webp
1b95c3e8c125ea3c2f259e700806281957b995dd8a50d4134cfcbc1ca24fac31  People_0.webp
f72f3107a3ca02352e445d123a57b7dd338abdaf2869b8d25f234d83009633df  People_1.webp
a9394554dc6569247f0714e24353b5e23dd68e505fc9df1e233874abc1a21443  People_2.webp
8ffdb2c6adf2e67d840b403dc2b26c8ef90fdd027c27b5f5a38b36cc2173bd1d  People_3.webp
441925a810d3078359a9260fcfc3062c25fa52b478c1fa4b41cf812d2b639b9b  People_4.webp
49543192ffae7f03b8ac548d3130dcb31a9c1fef6cb08c82c7cda2e911363240  People_5.webp
9d93ec8273fed9215a36ac0f4f0c7f05519718d39d0a5ce5691fbbf968b1c855  People_6.webp
ba93fd238269e1de5f0fe48a7649c9f852d621fe52104d0407a95770282b073c  People_7.webp
93328274702b84a2134aceb4115060b14c8cd6a611a49c30efa845679e90fd5c  People_8.webp
7e67074dd5d7698d9e6ae9d9a195ddfaa1bf9802e6e3e007496fc2e9de591a12  People_9.webp
49bf78c258b80e6e6fbb10df68527c3ec5cb491e08387294823e1b6a3acc4c86  People_10.webp
ebcc6f1e06f74d0ae59622a5698b17f629fc065166e5fccf968b068bdf99b1f3  Places.webp
bf0838160bc545c9d8222665ed5057de9023bf926dbeecf5ba0087beeadb91c7  Symbols.webp
e4e6003586df4aaf2eaf90dea705090b32c153633317f1a64a6bae6e6b29da73  emoji_data.json
4df3c306dddaaf4baffdff5ca820cc679ac8cd6dc263c6a74517783e42fa7a3b  LICENSE-Signal-Android.txt
37d30610371645854da9aaab282f6a96ad612fa29b1f013fe18841644b423262  NOTICE-Signal-Android.txt
```
