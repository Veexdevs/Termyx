@echo off
REM TERMYX Gradle wrapper (standard).
REM NOTE: the gradle-wrapper.jar is not committed. To enable gradlew locally, run:
REM     gradle wrapper --gradle-version 8.9
REM CI uses the Gradle provided by gradle/actions/setup-gradle instead.

cd /d "%~dp0"
gradle %*
