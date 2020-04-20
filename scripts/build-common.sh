#!/usr/bin/env bash
set -euxo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(dirname "$DIR")"

cd "$ROOT"

source "$DIR/utils.sh"

mkdir -p ~/bin

apt-get update
#apt-get upgrade -y

apt-get install -y --no-install-recommends ca-certificates curl gettext git python3 python3-pip python3-venv
ln -sf "$(command -v python3)" /usr/bin/python

# Install poetry
POETRY_VERSION="1.0.5"
download "https://raw.githubusercontent.com/sdispater/poetry/${POETRY_VERSION}/get-poetry.py" \
  40cf7b39a926acae9d1d0293983a508c58a6788554c6120a84e65d18ce044e51 /tmp/get-poetry.py
python3 /tmp/get-poetry.py -y --version "$POETRY_VERSION"
rm -rf /tmp/get-poetry.py
