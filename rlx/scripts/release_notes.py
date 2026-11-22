#!/usr/bin/env python3
"""
Release notes of an RLX Patches release, written to stdout. Run from the root of the repository, on the
commit that is released (with its patches-list.json).

Environment: VERSION (RLX version), MORPHE (the Morphe base version), PREVIOUS_TAG (the last RLX release,
empty for the first one), EXTRA_NOTES (optional Markdown, after the new features).

With --manager: the notes for Morphe Manager (the "description" of patches-bundle.json), in the layout of
Morphe's own bundle: "## [version](release) (date)", the sections as "###", without the install section
(the source is already added there).

Layout (see rlx/README.md, "Release notes"):

    Based on **Morphe Patches X.Y.Z**
    ## ✨ New features         the Morphe features since the base of the last RLX release, so also of the
                              Morphe versions RLX did not release; YouTube, YouTube Music, then every app
    ## 🔧 Other improvements   the heading alone: fixes are not listed, neither Morphe's nor RLX's
    ## 🌟 RLX extras          what RLX adds to Morphe Patches (rlx/extras.md), in every release
    ## 📥 Install
    ## 📱 Supported versions   from patches-list.json
"""
import json
import os
import re
import subprocess
import sys
from datetime import datetime, timezone

ROOT = os.getcwd()


def git(*args):
    return subprocess.run(["git", *args], check=True, capture_output=True, text=True, cwd=ROOT).stdout


def key(v):
    # 2.4.0-dev.5 < 2.4.0; app versions such as 21.46.522 have no pre-release part.
    main, _, pre = v.partition("-")
    numbers = [int(x) if x.isdigit() else 0 for x in main.split(".")]
    numbers += [0] * (4 - len(numbers))
    return numbers + ([0, int(re.sub(r"\D", "", pre) or 0)] if pre else [1, 0])


def clean(entry):
    entry = re.sub(r",? closes (\[#\d+\]\([^)]*\)(, )?)+", "", entry)
    entry = re.sub(r" \(\[[0-9a-f]{7}\]\([^)]*\)\)", "", entry)
    entry = re.sub(r" \(\[#\d+\]\([^)]*\)\)", "", entry)
    return re.sub(r"\s{2,}", " ", entry).strip()


def order(entry):
    scope = re.match(r"\*\*([^:*]+):\*\*", entry)
    app = scope.group(1) if scope else ""
    if app.startswith("YouTube Music"):
        return 1
    if app.startswith("YouTube"):
        return 0
    return 2


def features(morphe, previous):
    """New features of the Morphe releases after `previous` (the first release: of every release of this
    Morphe version, X.Y.0-dev.N) up to `morphe`, from Morphe's CHANGELOG.md. Reddit is not in RLX."""
    base = morphe.split("-")[0]
    found = []
    for section in re.split(r"(?m)^## ", open(os.path.join(ROOT, "CHANGELOG.md"), encoding="utf-8").read())[1:]:
        release = re.match(r"\[?([0-9][^\]\s]*)", section).group(1)
        if key(release) > key(morphe):
            continue
        if previous and key(release) <= key(previous) or not previous and not release.startswith(base):
            continue
        for part in re.split(r"(?m)^### ", section)[1:]:
            title, _, items = part.partition("\n")
            if "Features" in title:
                for line in items.splitlines():
                    if line.startswith("* ") and not line.startswith("* **Reddit"):
                        entry = clean(line[2:])
                        if entry not in found:  # A change listed again in a later version
                            found.append(entry)
    return sorted(found, key=order)


def supported_versions():
    """The newest recommended version of each app, and the newest one at all (experimental)."""
    supported = []
    patches = json.load(open(os.path.join(ROOT, "patches-list.json"), encoding="utf-8"))["patches"]
    for app in ("YouTube", "YouTube Music"):
        stable, every = set(), set()
        for patch in patches:
            for package in patch.get("compatiblePackages") or []:
                if package.get("name") != app:
                    continue
                for target in package.get("targets") or []:
                    every.add(target["version"])
                    if not target.get("isExperimental"):
                        stable.add(target["version"])
        if not every:
            continue
        text = f"* **{app}** {max(stable, key=key)}" if stable else f"* **{app}**"
        newest = max(every, key=key)
        if not stable or key(newest) > key(max(stable, key=key)):
            text += f" (up to {newest}, experimental)"
        supported.append(text)
    return supported


def main():
    manager = "--manager" in sys.argv[1:]
    morphe = os.environ["MORPHE"]
    start = os.environ.get("PREVIOUS_TAG")
    previous = None
    if start:
        previous = re.search(r"(?m)^version\s*=\s*(\S+)", git("show", f"{start}:gradle.properties")).group(1)

    lines = [f"Based on **Morphe Patches {morphe}**", ""]

    new = features(morphe, previous)
    if new:
        lines += ["## ✨ New features", "", *[f"* {e}" for e in new], ""]

    extra = os.environ.get("EXTRA_NOTES", "").strip()
    if extra:
        lines += [extra, ""]

    # Fixes are not listed one by one, neither Morphe's nor RLX's.
    lines += ["## 🔧 Other improvements", ""]

    extras = open(os.path.join(ROOT, "rlx", "extras.md"), encoding="utf-8").read()
    extras = re.sub(r"(?s)<!--.*?-->", "", extras).strip()
    lines += ["## 🌟 RLX extras", "", extras, ""]

    if not manager:
        lines += [
            "## 📥 Install",
            "",
            "[Add RLX Patches to Morphe Manager](https://morphe.software/add-source?github=Ramik4/rlx-patches&name=RLX%20Patches),"
            " or add `https://github.com/Ramik4/rlx-patches` under *Sources → + → Remote*, then patch YouTube or"
            " YouTube Music with the RLX Patches source.",
            "",
        ]

    supported = supported_versions()
    if supported:
        lines += ["## 📱 Supported versions", "", *supported, ""]

    if manager:
        version = os.environ["VERSION"]
        date = os.environ.get("RELEASE_DATE") or datetime.now(timezone.utc).strftime("%Y-%m-%d")
        url = f"https://github.com/Ramik4/rlx-patches/releases/tag/v{version}"
        lines = [f"## [{version}]({url}) ({date})", ""] + [
            "#" + line if line.startswith("## ") else line for line in lines
        ]

    print("\n".join(lines).strip())


if __name__ == "__main__":
    main()
