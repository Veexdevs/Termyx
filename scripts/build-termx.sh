#!/usr/bin/env bash
#
# build-termx.sh — convenience wrapper around the Gradle builds (local development).
# In CI, GitHub Actions runs the equivalent commands directly (see .github/workflows).
#
set -euo pipefail

GRADLE="${GRADLE:-gradle}"

echo "== Android debug APK =="
"$GRADLE" :android:assembleDebug

echo "== Android release APKs (ABI splits) =="
"$GRADLE" :android:assembleRelease

echo "== Desktop run (Windows/Linux) =="
"$GRADLE" :desktop:run

echo "== Desktop EXE (current OS) =="
"$GRADLE" :desktop:createDistributable

echo "All builds completed."
