#!/usr/bin/env bash
set -euxo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(dirname "$DIR")"

cd "$ROOT"

mkdir -p ~/.ssh
cp -r ~/.ssh-localhost/* ~/.ssh
chmod 700 ~/.ssh
chmod 600 ~/.ssh/*
