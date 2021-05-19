#!/bin/bash
set -euxo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

./gradlew dokkaGfm

cp README.md docs/index.md
cp CHANGELOG.md docs/changelog.md

./prepare-docs.py
