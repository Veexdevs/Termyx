#!/bin/sh

#
# TERMYX Gradle wrapper (standard).
# NOTE: the gradle-wrapper.jar is not committed. To enable `./gradlew` locally, run:
#     gradle wrapper --gradle-version 8.9
# CI uses the Gradle provided by gradle/actions/setup-gradle instead.
#

# Resolve the project root.
cd "$(dirname "$0")" || exit 1

exec gradle "$@"
