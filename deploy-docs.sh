#!/bin/bash
set -euxo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

source .venv/bin/activate

./prepare-docs.sh

if [ -n "${GITHUB_REPOSITORY:-}" ] && ! git config --get user.name; then
  git config --global user.name "${GITHUB_ACTOR}"
  git config --global user.email "${GITHUB_ACTOR}@users.noreply.github.com"
  git remote rm origin
  git remote add origin "https://x-access-token:${PERSONAL_TOKEN}@github.com/${GITHUB_REPOSITORY}.git"
fi

mkdocs gh-deploy --config-file "$ROOT/mkdocs.yml" --force
