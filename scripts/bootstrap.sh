#!/usr/bin/env bash
#
# bootstrap.sh — install a Linux distribution inside TERMYX via PRoot (no root required).
#
# This is the real mechanism the Systems module calls through com.termx.core.linux.LinuxSystemManager.
# It requires the `proot-distro` tooling to be present in the environment (bundled with the
# Termux base). If it is missing, the manager reports the requirement instead of faking success.
#
# Usage:
#   ./scripts/bootstrap.sh <distro> [arch]
#   ./scripts/bootstrap.sh ubuntu aarch64
#
set -euo pipefail

DISTRO="${1:-ubuntu}"
ARCH="${2:-aarch64}"

if ! command -v proot-distro >/dev/null 2>&1; then
  echo "error: proot-distro not found in PATH. Bundle the Termux proot-distro tooling first." >&2
  exit 1
fi

echo "Installing $DISTRO ($ARCH) via PRoot…"
proot-distro install "$DISTRO" -a "$ARCH"

echo "Verifying…"
proot-distro login "$DISTRO" -- uname -a

echo "Done. $DISTRO is available to TERMYX SYSTEMS."
