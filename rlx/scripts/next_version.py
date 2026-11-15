#!/usr/bin/env python3
"""Computes the next RLX Patches version.

Rules (see rlx/README.md):

* Morphe ``X.Y.0-dev.N`` -> RLX ``X.Y.N``. A skipped Morphe dev release skips its number too.
* Morphe ``X.Y.Z`` (stable) -> RLX ``X.(Y+1).0``. Morphe 1.50.0 -> RLX 1.51.0.
* An RLX-only release between two Morphe releases -> the last RLX version of the line + 1.
* The last number only ever grows within a line: when the number a rule gives is already
  taken (or lower than one already used), the next free number is used instead.

Usage:
    next_version.py morphe <morphe-version> [--tags-from-git | --tags v1 v2 ...]
    next_version.py rlx <current-rlx-version> [--tags-from-git | --tags v1 v2 ...]

Prints the version without the leading ``v``.
"""

from __future__ import annotations

import argparse
import re
import subprocess
import sys

MORPHE_VERSION = re.compile(r"^v?(\d+)\.(\d+)\.(\d+)(?:-dev\.(\d+))?$")
RLX_VERSION = re.compile(r"^v?(\d+)\.(\d+)\.(\d+)$")


def parse_rlx(version: str) -> tuple[int, int, int]:
    match = RLX_VERSION.match(version.strip())
    if not match:
        raise ValueError(f"Not an RLX version: {version!r}")
    return int(match[1]), int(match[2]), int(match[3])


def used_patch_numbers(tags: list[str], major: int, minor: int) -> list[int]:
    numbers = []
    for tag in tags:
        match = RLX_VERSION.match(tag.strip())
        if match and int(match[1]) == major and int(match[2]) == minor:
            numbers.append(int(match[3]))
    return numbers


def first_free(major: int, minor: int, wanted: int, tags: list[str]) -> str:
    used = used_patch_numbers(tags, major, minor)
    if used and wanted <= max(used):
        wanted = max(used) + 1
    return f"{major}.{minor}.{wanted}"


def from_morphe(morphe_version: str, tags: list[str]) -> str:
    match = MORPHE_VERSION.match(morphe_version.strip())
    if not match:
        raise ValueError(f"Not a Morphe version: {morphe_version!r}")
    major, minor, patch, dev = int(match[1]), int(match[2]), int(match[3]), match[4]
    if dev is not None:
        return first_free(major, minor, int(dev), tags)
    # Stable Morphe releases open the next RLX minor line.
    return first_free(major, minor + 1, patch, tags)


def rlx_only(current: str, tags: list[str]) -> str:
    major, minor, patch = parse_rlx(current)
    return first_free(major, minor, patch + 1, tags)


def git_tags() -> list[str]:
    output = subprocess.run(
        ["git", "tag", "--list", "v*"], check=True, capture_output=True, text=True
    ).stdout
    return output.split()


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("mode", choices=["morphe", "rlx"])
    parser.add_argument("version")
    group = parser.add_mutually_exclusive_group()
    group.add_argument("--tags-from-git", action="store_true", help="read the RLX tags of this repo")
    group.add_argument("--tags", nargs="*", default=[], help="RLX tags already used")
    args = parser.parse_args(argv)

    tags = git_tags() if args.tags_from_git else args.tags
    if args.mode == "morphe":
        print(from_morphe(args.version, tags))
    else:
        print(rlx_only(args.version, tags))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
