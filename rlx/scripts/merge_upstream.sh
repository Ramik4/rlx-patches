#!/usr/bin/env bash
# Merges a Morphe release into the current branch (never rebases).
#
#   rlx/scripts/merge_upstream.sh <morphe-ref>      e.g. refs/upstream-tags/v2.3.0-dev.8
#
# Conflicts in files listed in rlx/sync/removed.txt (kept deleted) and rlx/sync/ours.txt
# (RLX side kept) are resolved automatically. Morphe workflows are removed so they never run
# here. Any other conflict stops the merge with exit code 1 and lists the files; the merge is
# left in progress for a person to resolve. On success the RLX version in rlx/rlx.properties
# is set from the Morphe version and the merge is committed.
set -euo pipefail

ref="${1:?usage: merge_upstream.sh <morphe-ref>}"
root="$(git rev-parse --show-toplevel)"
cd "$root"

list() { grep -v '^\s*#' "$1" | sed '/^\s*$/d'; }

morphe_version="$(git show "$ref:gradle.properties" | sed -n 's/^version *= *//p' | tr -d '[:space:]')"
if [[ -z "$morphe_version" ]]; then
  echo "Cannot read the Morphe version of $ref" >&2
  exit 2
fi

if git merge-base --is-ancestor "$ref" HEAD; then
  echo "Already based on $ref"
  exit 0
fi

# A conflicting merge exits non-zero; the conflicts are handled below.
git merge --no-ff --no-commit "$ref" || true

mapfile -t removed < <(list rlx/sync/removed.txt)
mapfile -t ours < <(list rlx/sync/ours.txt)
unresolved=()
while IFS= read -r path; do
  [[ -z "$path" ]] && continue
  if printf '%s\n' "${removed[@]}" | grep -qxF "$path"; then
    git rm -q --cached --ignore-unmatch -- "$path"
    rm -f -- "$path"
  elif printf '%s\n' "${ours[@]}" | grep -qxF "$path"; then
    git checkout -q --ours -- "$path"
    git add -- "$path"
  else
    unresolved+=("$path")
  fi
done < <(git diff --name-only --diff-filter=U)

# Workflows added by Morphe must not run in this repository.
for workflow in .github/workflows/*; do
  [[ -e "$workflow" ]] || continue
  case "$(basename "$workflow")" in
    rlx-*) ;;
    *)
      git rm -q -f -- "$workflow"
      grep -qxF "$workflow" rlx/sync/removed.txt || echo "$workflow" >> rlx/sync/removed.txt
      git add rlx/sync/removed.txt
      ;;
  esac
done

if (( ${#unresolved[@]} )); then
  echo "Merge conflicts that need a person:" >&2
  printf '  %s\n' "${unresolved[@]}" >&2
  exit 1
fi

rlx_version="$(python3 rlx/scripts/next_version.py morphe "$morphe_version" --tags-from-git)"
sed -i "s/^version=.*/version=$rlx_version/" rlx/rlx.properties
git add rlx/rlx.properties

git commit -q --no-edit -m "Merge Morphe v$morphe_version (RLX $rlx_version)" \
  -m "Based on Morphe $morphe_version"
echo "rlx_version=$rlx_version"
echo "morphe_version=$morphe_version"
