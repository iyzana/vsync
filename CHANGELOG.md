# Changelog

## [3.0.1](https://github.com/iyzana/vsync/compare/v3.0.0...v3.0.1) (2025-09-03)


### Bug Fixes

* search term adding and favicon loading ([e7be2b6](https://github.com/iyzana/vsync/commit/e7be2b65f8142d73e4aa683c535e7283abc2c3f1))

## [3.0.0](https://github.com/iyzana/vsync/compare/v2.1.0...v3.0.0) (2024-09-12)


### ⚠ BREAKING CHANGES

* simplify docker setup

### Features

* add logging on if there are active rooms ([23616e8](https://github.com/iyzana/vsync/commit/23616e8fe81dfc90e14dc22d94f620306312756c))
* add more info about which sites are supported ([7997a6d](https://github.com/iyzana/vsync/commit/7997a6da9a4c89a91f760a4114aa2074579fb195))
* delay playback permission request ([9397cf4](https://github.com/iyzana/vsync/commit/9397cf440e8e7f16de3a1d0d0528d81a382f32f1))
* exit fullscreen on connection loss ([a6184dc](https://github.com/iyzana/vsync/commit/a6184dc7decb31d614d259d5dc7fd1f4278796bb))
* inform videojs about the video content type ([f6068e9](https://github.com/iyzana/vsync/commit/f6068e97ba63dd2ccce2967c9f644a9d29fc4027))
* lazy-load video players ([ed129fc](https://github.com/iyzana/vsync/commit/ed129fc12959b05fdd83995cf3d3c1e826412e35))
* mark message logging as debug, add statistic logs ([1f318ed](https://github.com/iyzana/vsync/commit/1f318ed4055dd06d1bb66cf65b0ccfd3b05e5e72))
* request interaction on missing autoplay permission, cleanup html/css ([df0641d](https://github.com/iyzana/vsync/commit/df0641d38bd2a7ae6f72d06f4223c32c523b48c1))
* show loading indicator in queue list, proper favicon loading ([cc1ad28](https://github.com/iyzana/vsync/commit/cc1ad288665ce0770f608ca42c12f2dd1824a5d3))
* support video start timestamps ([a263165](https://github.com/iyzana/vsync/commit/a263165154aa5fff704e22c8e7622f98fb6df9f5))
* vendor react-youtube with custom patch ([b61df8d](https://github.com/iyzana/vsync/commit/b61df8dfde7af8bae3588567a7cc01afacf72a75))


### Bug Fixes

* also clear sync timeout when last video ends ([143b3f4](https://github.com/iyzana/vsync/commit/143b3f40e6848011cc1d25ee305fbb3fe7d1f603))
* always keep some threads alive for video info fetching ([45bbc78](https://github.com/iyzana/vsync/commit/45bbc78359d65133423862bcbe1e0c1dc1e9d2b5))
* catch correct exception for parsing URIs ([b8dffef](https://github.com/iyzana/vsync/commit/b8dffefa3e9dc5948b3a73b2e63685b09e1dff59))
* detekt lints ([494ec22](https://github.com/iyzana/vsync/commit/494ec22fb9de4134e1269e3836894a179e891017))
* remove unnecessary host env ([43d23c1](https://github.com/iyzana/vsync/commit/43d23c167e680db1a79c42e3a7959b8001b0f0a4))
* reset state and ready checks on video switch ([feaf447](https://github.com/iyzana/vsync/commit/feaf447b9d1daa4279431d53deb1ae4f7f6c5742)), closes [#55](https://github.com/iyzana/vsync/issues/55)
* typo in manifest-src ([85e57b2](https://github.com/iyzana/vsync/commit/85e57b272fbf906aa2e56f874072a83bcc0f7586))
* update frontend dependencies ([fae1b2f](https://github.com/iyzana/vsync/commit/fae1b2f07a9a73cc7fe12fa1b5490bcba784a53a))
* use jcenter repo ([47b2072](https://github.com/iyzana/vsync/commit/47b207281e118392ea8855e7e527ef91bcc902d7))


### Code Refactoring

* simplify docker setup ([d972464](https://github.com/iyzana/vsync/commit/d972464771ff14f19f52c48a2c4666e20c52c1d2))

## [2.1.0](https://github.com/iyzana/yt-sync/compare/v2.0.0...v2.1.0) (2023-03-07)


### Features

* add about dialog, nicer queue site indicator ([a1d298f](https://github.com/iyzana/yt-sync/commit/a1d298f886be0e0be0e2e6ec52ef1ce574e36d47))
* add css for noscript ([c22dcef](https://github.com/iyzana/yt-sync/commit/c22dcef69eafa879aad08e9fb47895c0db2f1780))
* allow null thumbnail and title in frontend ([59ccb7b](https://github.com/iyzana/yt-sync/commit/59ccb7b78b82a92a0b7a68f3e639bfbb079a525a))
* autofocus input in new rooms ([b149fd7](https://github.com/iyzana/yt-sync/commit/b149fd79e148be18bb89fed81dd238c5126fad8b))
* better notification management ([2122b48](https://github.com/iyzana/yt-sync/commit/2122b485d93f78d394960c33a12da7e203beaffe))
* fetch video info from oembed if possible ([9e7c707](https://github.com/iyzana/yt-sync/commit/9e7c70766d338b8ee258668ceb2a02f0f02eeeee))
* improve accessibility and keyboard navigation ([c2e90cb](https://github.com/iyzana/yt-sync/commit/c2e90cb39f40e006ef2671ba6987f94919b0678c))
* improve input styling and some texts ([24bf04c](https://github.com/iyzana/yt-sync/commit/24bf04cd738846a2b2517d490d417512167713c5))
* inform clients when last video ended ([8fff741](https://github.com/iyzana/yt-sync/commit/8fff7411a3c6dd409f2202ce25cfe3b4cf4e0492))
* player shortcuts, fix thumbnails, yt-shorts ([68274e2](https://github.com/iyzana/yt-sync/commit/68274e2b6c75b1d909c4ec222ad0f87ae37e727a))
* slight redesign, improve architecture ([99adb17](https://github.com/iyzana/yt-sync/commit/99adb17e7c54205e73f137fbf017a3ac658fc96c))
* update text of empty video player ([e551a69](https://github.com/iyzana/yt-sync/commit/e551a69dce5f05608498b254559f495986e2599b))


### Bug Fixes

* add space between shortcut and description ([1db0159](https://github.com/iyzana/yt-sync/commit/1db015985f47b0572fef12e79e3d1a9f7f17c62f))
* backend build ([7564366](https://github.com/iyzana/yt-sync/commit/75643667e57fc0c85c816a3ca757b809344ee660))
* backend build ([180b219](https://github.com/iyzana/yt-sync/commit/180b219de826458ef93fdf521517dce6faea923b))
* consistent favicon size in queue ([c725f81](https://github.com/iyzana/yt-sync/commit/c725f81fa1f7c4ed540d7dca66e210bd1294f719))
* dialog css on small devices ([e9bd099](https://github.com/iyzana/yt-sync/commit/e9bd099e09f48c38a02ad4c142016e93aeefb4a6))
* don't perform global action when input active ([27fd98b](https://github.com/iyzana/yt-sync/commit/27fd98b27b4c24866f101bd5ba915b3cdb422858))
* fast message handling, improve volume sync ([a91556d](https://github.com/iyzana/yt-sync/commit/a91556da8c308fc59c632468f5aef9e62c273aae))
* improve queue remove button height ([ba7a7af](https://github.com/iyzana/yt-sync/commit/ba7a7af702844eed862ab225b0c6a14ae857adde))
* make about icon properly selectable ([8ad9a75](https://github.com/iyzana/yt-sync/commit/8ad9a759883f332e1818f7bb68888a86ce28b47a))
* queue border rounding ([2f31fad](https://github.com/iyzana/yt-sync/commit/2f31fad4bd33f104f5b5e99ec5a5842cf3c3ee99))
* queuing youtube shorts, embeds or youtu.be links ([89fcaf7](https://github.com/iyzana/yt-sync/commit/89fcaf7e2ab6349012f0cf69203c8b575ac201cb))
* remove superfluous ignoreEndTill check ([8702ce3](https://github.com/iyzana/yt-sync/commit/8702ce3fcaafedf4ba19431effce415a1373df8c))
* set queue working to false on new video ([39add11](https://github.com/iyzana/yt-sync/commit/39add1113e51ae60d75cc69cc31773b3793918b7))
* update frontend dependencies ([861da3f](https://github.com/iyzana/yt-sync/commit/861da3f9609d700705d8209d46180293d3f9bee0))
* use youtube player for youtu.be urls ([02dae81](https://github.com/iyzana/yt-sync/commit/02dae81c5f703d744ab51e95d0f7fd7ff63d1501))

## [2.0.0](https://github.com/iyzana/yt-sync/compare/v1.1.1...v2.0.0) (2022-10-23)


### ⚠ BREAKING CHANGES

* use yt-dlp, add video.js player, use ids and urls in queue

### Features

* update noscript text ([8516ce7](https://github.com/iyzana/yt-sync/commit/8516ce7e1cb242d8adb50ef7934f205569efa3d1))
* use yt-dlp, add video.js player, use ids and urls in queue ([42ec7bf](https://github.com/iyzana/yt-sync/commit/42ec7bfef6784d6dfbded56db7e9a6e1080a975b))


### Bug Fixes

* possible disconnect when pausing early after video switch ([24fb547](https://github.com/iyzana/yt-sync/commit/24fb5470b4579876f6c20544fb59f3e812820e5b))
* update frontend dependencies ([30f50ee](https://github.com/iyzana/yt-sync/commit/30f50ee80b5af7c511d7ad5a1393c61442a5e006))
* videojs player volume syncing when muted ([7ef64a5](https://github.com/iyzana/yt-sync/commit/7ef64a5b81dc0b6ef8f1bc9177660d41217efdd4))

## [1.1.1](https://github.com/iyzana/yt-sync/compare/v1.1.0...v1.1.1) (2022-08-07)


### Bug Fixes

* player incorrectly believed it is paused ([f88570a](https://github.com/iyzana/yt-sync/commit/f88570a918899e8af845393dd6837997004d075d))
* update frontend dependencies ([6679b3e](https://github.com/iyzana/yt-sync/commit/6679b3eeefcccc95693e40cc937a1df2ca5a752c))

## [1.1.0](https://github.com/iyzana/yt-sync/compare/v1.0.4...v1.1.0) (2022-05-07)


### Features

* change text that shows connected users ([39d3703](https://github.com/iyzana/yt-sync/commit/39d3703ac8e242e35251d430951653aca660d7ea))


### Bug Fixes

* take a lock for queue operations ([eb1e895](https://github.com/iyzana/yt-sync/commit/eb1e895e0214b9b5beb169a1386d582cec7601b3))
* update frontend dependencies ([6fc5143](https://github.com/iyzana/yt-sync/commit/6fc5143e4099f5ed083a825973cdc12d500ce1aa))
* update frontend dependencies ([926aede](https://github.com/iyzana/yt-sync/commit/926aede05251eca494e9b21201103f5a5d983be1))
* update frontend dependencies ([8e197bd](https://github.com/iyzana/yt-sync/commit/8e197bd33f419da959f54d50960391bbb65f0bdf))
* use yarn in Dockerfile build ([00acf7f](https://github.com/iyzana/yt-sync/commit/00acf7faabab91704831a0a5bc8d8621f510e066))

### [1.0.4](https://www.github.com/iyzana/yt-sync/compare/v1.0.3...v1.0.4) (2021-11-08)


### Bug Fixes

* update frontend dependencies ([03eb730](https://www.github.com/iyzana/yt-sync/commit/03eb730fa3dc2c8b1740d7b02ed5e84a3036a1c8))

### [1.0.3](https://www.github.com/iyzana/yt-sync/compare/v1.0.2...v1.0.3) (2021-09-29)


### Bug Fixes

* update frontend dependencies ([3763d51](https://www.github.com/iyzana/yt-sync/commit/3763d517f96ea7b206a77056c640ae3768c82db4))
* update frontend dependencies ([3b66067](https://www.github.com/iyzana/yt-sync/commit/3b66067c8196a6a42f8649da052d22d5825b48cd))
* update frontend dependencies ([3a001ca](https://www.github.com/iyzana/yt-sync/commit/3a001ca7fe7a40f9cad4b3f0952b313747d4d588))

### [1.0.2](https://www.github.com/iyzana/yt-sync/compare/v1.0.1...v1.0.2) (2021-06-06)


### Bug Fixes

* update frontend dependencies ([d39ead8](https://www.github.com/iyzana/yt-sync/commit/d39ead81fda3670f6f44c7dd3c028b071e509ee6))
* update frontend dependencies ([4f78b89](https://www.github.com/iyzana/yt-sync/commit/4f78b89d2833ef6c34c49445036c0bc4ba0ba6f1))
* use css box-sizing: border-box for simpler calculations ([64814ab](https://www.github.com/iyzana/yt-sync/commit/64814ab927661759df55efbd8567138485c7656c))

### [1.0.1](https://www.github.com/iyzana/yt-sync/compare/v1.0.0...v1.0.1) (2021-05-08)


### Bug Fixes

* update frontend dependencies ([05b4c63](https://www.github.com/iyzana/yt-sync/commit/05b4c63a7e95e4372861e7519b95d7898b9768de))

## 1.0.0 (2021-05-03)


### ⚠ BREAKING CHANGES

* initial release

### Features

* initial release ([15fbac4](https://www.github.com/iyzana/yt-sync/commit/15fbac4a08fc140de170482a1fb5c9e845438c93))
