# RLX maintenance

Notes for maintaining RLX Patches on top of Morphe Patches.

## Branches

| Branch | Content |
| --- | --- |
| `upstream` | Mirror of Morphe's `dev` branch. Only ever fast-forwarded, never committed to. |
| `main` | Morphe + RLX. Morphe releases are merged in, never rebased; no force pushes. |

Morphe is the `upstream` remote. Its tags are fetched into `refs/upstream-tags/*`, so they
never mix with the RLX tags (`vX.Y.Z`) of this repository:

```sh
git remote add upstream https://github.com/MorpheApp/morphe-patches.git
git config remote.upstream.tagOpt --no-tags
git config --add remote.upstream.fetch '+refs/tags/*:refs/upstream-tags/*'
git fetch upstream
```

## Syncing with Morphe

```sh
rlx/scripts/merge_upstream.sh refs/upstream-tags/v2.3.0-dev.8
```

The script merges the Morphe release, sets the RLX version and commits. It resolves the
conflicts that are expected on every release by itself:

- [`sync/removed.txt`](sync/removed.txt): Morphe files that RLX deletes (Morphe's workflows,
  issue templates, bundle icon). Workflows Morphe adds later are deleted too, so they never
  run here.
- [`sync/ours.txt`](sync/ours.txt): files RLX owns entirely (`README.md`,
  `patches-bundle.json`, `patches-list.json`).

Any other conflict stops the script with the list of files and leaves the merge in progress.

## Where RLX code lives

RLX changes as few Morphe files as possible, so merges stay clean.

- `rlx/`: build hooks, version, default settings, sync scripts and lists.
- `patches/src/main/kotlin/app/rlx/`: RLX patches (`app.rlx.patches`).
- `patches/src/main/resources/addresources/values*/rlx/`: RLX strings.
- `patches/src/main/resources/addresources/values*/rlx-shared/`, `values/rlx-youtube/`,
  `values/rlx-music/`: Morphe strings and arrays that RLX replaces (settings names, app names).
- `patches/src/main/resources/rlx/`: RLX resources copied into the apps.
- `extensions/*/src/main/java/app/rlx/`: RLX extension code (`app.rlx.extension`).

Hooks in Morphe files, each marked with an `RLX` comment:

| File | Hook |
| --- | --- |
| `build.gradle.kts` | applies `rlx/rlx.gradle.kts` (version) |
| `patches/build.gradle.kts` | applies `rlx/patches.gradle.kts`; bundle name and description |
| `settings.gradle.kts` | root project name |
| `extensions/proguard-rules.pro` | keeps `app.rlx.**` |
| `patches/.../shared/layout/branding/BaseCustomBrandingPatch.kt` | default app icon RLX red (`RLX_DEFAULT_ICON_STYLE`) |
| `patches/.../shared/layout/branding/BaseCustomBrandingPatch.kt` | patch option "App icon" with the RLX icons in place of the Morphe icons (`RlxPatchOptions.kt`) |
| `patches/.../shared/layout/branding/BaseCustomBrandingPatch.kt` | default app name "YouTube RLX" / "YT Music RLX" (`RLX_DEFAULT_APP_NAME_INDEX`) |
| `patches/.../youtube/layout/branding/CustomBrandingPatch.kt`, `patches/.../music/layout/branding/CustomBrandingPatch.kt` | number of app names (`RLX_APP_NAME_COUNT`) |
| `extensions/shared-youtube/.../shared/patches/CustomBrandingPatch.java` | default app name (`RlxBranding.DEFAULT_APP_NAME_INDEX`); no placeholder name "Custom" without a custom name (`RlxBranding.checkAppName`) |
| `extensions/youtube/.../patches/ChangeHeaderPatch.java` | RLX header logos in `HeaderLogo` |
| `extensions/music/.../patches/ChangeHeaderPatch.java` | RLX header logos in `HeaderLogo` |
| `patches/.../youtube/misc/settings/SettingsPatch.kt` | depends on the RLX core patch of YouTube; title of the settings entry (`rlx_settings_title`) |
| `patches/.../music/misc/settings/SettingsPatch.kt` | depends on the RLX core patch of YouTube Music; title of the settings entry (`rlx_settings_title`) |

Patches of other apps (Reddit) stay in the tree untouched and are left out of the bundle by
`rlx/patches.gradle.kts`, so deleting them never causes merge conflicts.

## RLX core

`app/rlx/patches/shared/RlxCorePatch.kt` is applied with the Morphe settings of each app
(it cannot be left out, so the attribution is always there):

- **RLX Extended**: a settings screen of its own, right below "About" (the settings root is
  sorted by key, so its key starts with `morphe_settings_screen_00_`); VoT, the downloader and
  all other RLX settings go there.
- **Settings name**: the entry in the app settings and the toolbar read *RLX* instead of
  *Morphe* (GPLv3 section 7c and 7e). The setting to rename it reads *RLX settings name* and offers
  *Default*, *RLX Settings* (the RLX default), *Morphe Settings*, *Extended*, *RLX* and *Custom*; the
  Morphe names are hidden (arrays in `addresources/values/rlx-shared`, names 5 to 8 in `values/rlx`).
- **Attribution**: the "About" entry of the settings reads *RLX Patches 2.3.7* and opens the RLX
  about dialog, which names the Morphe Patches base version (GPLv3 section 7b).
- **Default settings**: every change RLX makes to Morphe's default settings is in
  [`defaults.json`](defaults.json), one section per app, with the keys of the Morphe settings
  export. Each default is applied once, on the first start of a build that has it, and only if
  the setting still has Morphe's default value; whatever the user changes is kept, also across
  updates. Settings already applied by the earlier YT Morphe builds are not applied again.
  *RLX Extended → Restore RLX default settings* applies all of them again.
  When RLX changes a default, the earlier value goes to the `replaced` section of
  `defaults.json`: a setting that still has an earlier RLX default gets the new one once.
- **Channel whitelists**: importing settings keeps the ad, playback speed and SponsorBlock
  channel whitelists when the imported text does not contain them (preset files), instead of
  clearing them (`RlxWhitelistKeeper`, an import/export callback of Morphe's `Setting`).
- **Branding** (GPLv3 section 7e): `RlxBranding`, a hook of Morphe's `CustomDialogListPreference`,
  rebuilds the branding lists of the settings in the RLX order and with the RLX names. Options that are
  not in a list (the Morphe icons, header logo and animations, some Morphe names) are hidden; a hidden
  option that is still selected stays at the end of the list.
  - App icons: *YouTube (Original)*, the original icons of YouTube 21.41+ (*Original Analog*, *Cake*,
    *Metal*), *RLX Red* (default), *RLX Green*, *RLX Layered* and, with a custom icon, *Custom*. For
    YouTube Music: *YouTube Music (Original)*, *RLX Red Music* (default), *RLX Green Music*, *RLX
    Layered Music*. Square for YouTube and round for YouTube Music, on a white background, with
    monochrome (themed) and notification icons. They take the place of the Morphe icon styles `play`,
    `play_black` and `light`: when all patches have run, the RLX core writes them over the Morphe files
    of these styles (`RlxBranding.kt`, only if *Custom branding* is applied). So the launch aliases,
    setting values and patch options of Morphe stay as they are, and an icon selected with an earlier
    YT Morphe build keeps working. The patch option *App icon* offers *Automatic*, *Original*, the RLX
    icons and *Custom*.
  - Notification icons: *Match app icon* (default), *Original (YouTube)*, *RLX* (`PLAY`) and *RLX
    Layered* (`LIGHT`); for YouTube Music *YouTube Music (Original)* and *RLX Music*.
  - App names (launch aliases, `RLX_APP_NAME_COUNT` = 9, the last is the custom name):
    YouTube: *YouTube* (original, 1), *YouTube Premium* (5), *YouTube RLX* (2, default), *YouTube X*
    (6), *YouTube Morphe* (7), *YT Morphe* (8), *YT RLX* (3); Morphe's *YT* (4) is hidden.
    YouTube Music: *YouTube Music* (5), *YT Music* (original, 1), *YT Music RLX* (2, default), *YT Music
    X* (6), *YT Music Morphe* (7), *RLX Music* (3); Morphe's *Music* (4) and *Music Morphe* (8) are
    hidden. The names are the strings `morphe_custom_branding_name_entry_N` in
    `addresources/values/rlx-youtube` and `rlx-music`, added by the RLX core after the strings of the
    app, so they replace the Morphe strings. Names can be added, but no alias removed. Without a custom
    name given while patching, the custom name (Morphe's placeholder *Custom*) is never selected: a
    selection left from an earlier install or an imported settings file goes back to the default name.
  - Header logos: *Default*, *YouTube (Original)*, *Premium (Original)*, *RLX Red YouTube* (default),
    *RLX Green YouTube*, *RLX Red Premium*, *RLX Green Premium*, *Custom*; for YouTube Music *Default
    (Original)*, *RLX Red Music* (default), *RLX Green Music*. The RLX logos (the RLX icon with
    "YouTube", "Premium" or "Music", light and dark theme) are added by the RLX core when *Change
    header* is applied, as values `RLX_*` of the header logo setting.
  - Startup animation: the splash screen style *RLX* (Morphe's `MORPHE_COLOR`, the RLX default) plays
    the RLX red icon popping in, square or round. The RLX core writes it over both Morphe animations.
  - The drawables are generated by [`branding/generate.py`](branding/generate.py), which also
    holds the proportions, and the animation by [`branding/animation.py`](branding/animation.py). The
    letters are Roboto Condensed Bold (SIL Open Font License 1.1), written as paths, so the app does
    not need the font.
- **Theme**: the dark theme defaults to the custom color `#0F0F0F`, which Morphe also gives the
  menus and sheets (with *App default* they keep YouTube's grey). A user who selected another
  color, such as pure black, keeps it.

## RLX patches

| Patch | Code | Notes |
| --- | --- | --- |
| Voice over translation (Yandex), YouTube | `patches/.../app/rlx/patches/youtube/video/yandexvot/`, `extensions/youtube/.../app/rlx/extension/youtube/yandexvot/` | See below |
| Save to device, YouTube | `patches/.../app/rlx/patches/youtube/interaction/savetodevice/`, `extensions/youtube/.../app/rlx/extension/youtube/savetodevice/` | See below |

### Voice over translation (Yandex)

Based on [YaVoT](https://github.com/sashade8-ship-it/morphe-patches-yavot) 1.1.4 by sashade8, with
work of Jav1x (original patch and Morphe port), anddea (revanced-patches port), MarcaDian (original
YaVoT add-on) and Ramik4 (RLX). Timing and coordination with Morphe's own voice over translation
come from the Dual VoT maintainers; the API client follows vot.js (FOSWLY) and Voice Over
Translation (ilyhalight). Maintained here as RLX code, not as a subtree.

- Built into the bundle instead of loaded as an add-on bundle: it subscribes to the player hooks
  of Morphe's `AddOnApi` (a public contract of Morphe Patches) from the main activity, and only
  hooks `AudioTrack.setVolume` itself, to lower the original audio.
- Settings keys are unchanged (`morphe_yandex_vot_*`, `yandex_vot_*` in the settings export), so
  the settings of YaVoT and YT Morphe carry over. They are on *RLX Extended → Voice over
  translation (Yandex)*.
- It does not conflict with Morphe's own Google-based voice over translation (`vot_*` keys):
  starting one stops the other.
- Changes to YaVoT: compiled against Morphe's current `Event` API (fixes the crash on long press
  of the button, YT Morphe 1.49.0-dev.9); API client updated to vot.js 3.2.2 (component version,
  user agent, browser client hints, session request signature, empty-audio file id); network and
  API failures stop the translation with a short notice and never crash the app.
- YouTube Music: not supported. Music has none of the player button and video hooks the
  translation needs, and its content is mostly music, which Yandex usually does not translate.

When updating from YaVoT or vot.js, compare `YandexVotApiClient` with
`packages/shared/src/data/config.ts`, `secure.ts` and `packages/core/src/providers/yandex.ts`
of vot.js.

### Save to device

Based on the downloader of [lavinhoque33](https://github.com/lavinhoque33)
([anddea/revanced-patches#1883](https://github.com/anddea/revanced-patches/pull/1883)), as adapted
for Morphe in YT Morphe (2.1.0 to 2.3.0-dev.5). Every file keeps the copyright notice of
lavinhoque33. Maintained here as RLX code.

- Download buttons of the app (below the video, feed menus, Shorts menu of Morphe) open a quality
  picker and save videos, audio, thumbnails and subtitles to `Download/YouTube`. Off by default
  (`morphe_save_to_device`); when off, Morphe's download mode is used.
- Library of the saved videos and an offline player with subtitles and offline SponsorBlock,
  shown in the activity of the settings. It can replace the Downloads page of the app.
- The offline player uses the stock `BackgroundPlayerService`, the downloads keep the process
  alive with the stock `OfflineKeepAliveService` (both hooked, each hook optional).
- The player response is read with `save_to_device.proto` (only the fields that are used).
- Settings keys are unchanged (`morphe_save_to_device`, `morphe_offline_*`), so the settings of
  YT Morphe carry over. They are on *RLX Extended → Save to device*.


## Versioning

`gradle.properties` keeps Morphe's own version, which is the Morphe base version. The RLX
version is in [`rlx.properties`](rlx.properties) and is computed by
[`scripts/next_version.py`](scripts/next_version.py):

| Morphe | RLX |
| --- | --- |
| `X.Y.0-dev.N` | `X.Y.N` (a skipped dev release skips its number too) |
| `X.Y.0` | `X.(Y+1).0` |
| RLX-only release | last RLX version + 1 |

When a number is already taken, the next free one is used, so the last number only grows.
Examples: Morphe 1.50.0 → RLX 1.51.0, Morphe 1.51.0-dev.2 → RLX 1.51.2.

```sh
./gradlew -q printVersions
python3 -m unittest discover -s rlx/scripts
```

## GitHub Actions

| Workflow | When | What |
| --- | --- | --- |
| `rlx-build.yml` | every push and pull request | builds the bundle and checks its RLX parts |
| `rlx-sync.yml` | every 6 hours, or by hand | mirrors Morphe's `dev` to `upstream`, merges a new Morphe release into `main` with `merge_upstream.sh`, builds it and pushes it; started by hand it can also start a draft release |
| `rlx-release.yml` | by hand | releases the version of `rlx.properties` as `vX.Y.Z` with the `.mpp` file, then updates `patches-bundle.json` and `patches-list.json` on `main` |

- **Sync**: nothing is pushed to `main` when the merge has a conflict the script does not resolve
  or the build fails; an issue lists the conflicts instead. A Morphe tag can be given by hand.
  Morphe's commits change Morphe's workflows, which GitHub does not let the workflow token push,
  so the sync needs a fine-grained token with the *Contents* and *Workflows* (read and write)
  permissions of this repository as the `RLX_SYNC_TOKEN` secret.
- **Release**: a draft by default (visible to maintainers only, no tag, `patches-bundle.json`
  unchanged); *publish* makes it public and points Morphe Manager to it. When the version of `rlx.properties` is already released, it is an RLX-only
  release and the version is raised by one first. The release notes name the Morphe base version
  and list the RLX commits since the last release (first parents of `main`, so without Morphe's
  history). The tag and the `.mpp` file are published before `patches-bundle.json` points to
  them, so Morphe Manager never gets a link to a missing file.

## Building

Morphe's Gradle plugin and libraries are on GitHub Packages, which needs a GitHub token with
`read:packages`, as `gpr.user`/`gpr.key` in `~/.gradle/gradle.properties` or as
`GITHUB_ACTOR`/`GITHUB_TOKEN`. In GitHub Actions the workflow token is used.

```sh
./gradlew :patches:buildAndroid    # patches/build/libs/rlx-patches-<version>.mpp
```
