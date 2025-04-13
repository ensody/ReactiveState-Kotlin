#!/usr/bin/env bash
set -euxo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(dirname "$DIR")"

cd "$ROOT"

source "$DIR/utils.sh"

mkdir -p ~/bin

apt-get update
#apt-get upgrade -y

# Make sure a few common tools are installed
apt-get install -y --no-install-recommends ca-certificates curl gettext git gnupg less procps apt-utils locales bash-completion

apt-get install -y --no-install-recommends ca-certificates python3 python3-pip python3-venv
ln -sf "$(command -v python3)" /usr/bin/python
