#!/usr/bin/env bash

download() {
  local url="$1"
  local checksum="$2"
  local dst="$3"
  curl -qsSL -o "$dst" "$url"
  echo "$checksum $dst" > "$dst.sum"
  sha256sum -c "$dst.sum"
  rm -rf "$dst.sum"
}

download_tgz() {
  local url="$1"
  local checksum="$2"
  local dst="$3"
  local tmp="$(mktemp)"
  download "$url" "$checksum" "$tmp"
  shift 3
  tar -C "$dst" -xzf "$tmp" "$@"
  rm -rf "$tmp"
}

download_zip() {
  local url="$1"
  local checksum="$2"
  local dst="$3"
  local tmp="$(mktemp)"
  download "$url" "$checksum" "$tmp"
  shift 3
  unzip -d "$dst" "$tmp" "$@"
  rm -rf "$tmp"
}
