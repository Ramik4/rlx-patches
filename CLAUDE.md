# Notes for Claude

## Workflow

- Commits are authored by the owner: set `git config user.name "Ramik ☆"` and
  `git config user.email "60112744+Ramik4@users.noreply.github.com"` in every clone before the first commit (also
  in a fork such as `Ramik4/morphe-patches`), so GitHub shows the owner as the author, and end the message with a
  `Co-Authored-By:` line for Claude. A commit made by mistake with another author is amended only while it is not
  on `main` yet.

- Every change that needs testing goes into a pull request to `main`. The Build workflow builds a test bundle
  (`rlx-patches-<version>-build<run>.mpp`) for every push and pull request, so no merge is needed for a test build.
- Do not merge right away: merge a pull request only after the owner has tested it on a device and says so.
- Pull request descriptions: clear and short, for the owner to read (what changes for the user, where the
  settings are, how it was tested). No "Generated with Claude Code" footer and no session link; the GitHub tool
  adds a footer to new pull requests, so remove it from the description right after creating one.
- Before merging, merge the latest `main` (Morphe releases are merged into it) into the branch, build and
  validate it again.
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
