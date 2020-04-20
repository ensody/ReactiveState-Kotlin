#!/bin/bash
set -euxo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

./gradlew dokka

cp README.md docs/index.md
cp CHANGELOG.md docs/changelog.md
