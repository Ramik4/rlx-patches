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
- `patches/src/main/resources/rlx/`: RLX resources copied into the apps.
- `extensions/*/src/main/java/app/rlx/`: RLX extension code (`app.rlx.extension`).

Hooks in Morphe files, each marked with an `RLX` comment:

| File | Hook |
| --- | --- |
| `build.gradle.kts` | applies `rlx/rlx.gradle.kts` (version) |
| `patches/build.gradle.kts` | applies `rlx/patches.gradle.kts`; bundle name and description |
| `settings.gradle.kts` | root project name |
| `extensions/proguard-rules.pro` | keeps `app.rlx.**` |
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
  *Morphe* (GPLv3 section 7c and 7e). The user can still rename it with Morphe's settings name.
- **Attribution**: the "About" entry of the settings reads
  *RLX Patches 2.3.7 · Based on Morphe Patches 2.3.0-dev.7* (GPLv3 section 7b) and opens the
  RLX about dialog.
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
- **Theme**: the dark theme defaults to the custom color `#0F0F0F`, which Morphe also gives the
  menus and sheets (with *App default* they keep YouTube's grey). A user who selected another
  color, such as pure black, keeps it.

## RLX patches

| Patch | Code | Notes |
| --- | --- | --- |
| Voice over translation (Yandex), YouTube | `patches/.../app/rlx/patches/youtube/video/yandexvot/`, `extensions/youtube/.../app/rlx/extension/youtube/yandexvot/` | See below |

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

## Building

Morphe's Gradle plugin and libraries are on GitHub Packages, which needs a GitHub token with
`read:packages`, as `gpr.user`/`gpr.key` in `~/.gradle/gradle.properties` or as
`GITHUB_ACTOR`/`GITHUB_TOKEN`. In GitHub Actions the workflow token is used.

```sh
./gradlew :patches:buildAndroid    # patches/build/libs/rlx-patches-<version>.mpp
```
