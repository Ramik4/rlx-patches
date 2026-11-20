# Notes for Claude

## Workflow

- Every change that needs testing goes into a pull request to `main`. The Build workflow builds a test bundle
  (`rlx-patches-<version>-build<run>.mpp`) for every push and pull request, so no merge is needed for a test build.
- Do not merge right away: merge a pull request only after the owner has tested it on a device and says so.
- RLX code goes into `app.rlx` packages and RLX strings into `addresources/*/rlx/strings.xml`, so merges from
  Morphe (`rlx/README.md`) do not conflict. Change Morphe files only when there is no other way.

## Testing

- The stock YouTube APK is attached to the draft release `source` of `Ramik4/yt-morphe`.
- `./gradlew :patches:validateRlxApk -PrlxApk=/absolute/path/youtube.apk` patches it with the patches Morphe
  Manager selects by default, with fingerprint validation and SDK DEX verification. Only some patches:
  `-PrlxExtraPatches="Glass menus" -PrlxOnly=true`.
- For every change to test, start the test APK build yourself, right after the push: workflow `release.yml` of
  `Ramik4/yt-morphe` on `main`, inputs `rlx` = the branch name and **`publish` = false** (its default is true,
  which would publish the build for everyone). The owner downloads the APK from the Actions artifacts of
  yt-morphe and installs it over the app on the phone.
