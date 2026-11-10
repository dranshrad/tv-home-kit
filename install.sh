#!/usr/bin/env bash
# Put the home screen on a Google TV / Android TV: the companion tiles, the launcher, the Home helper and the theme.
# Google TV's own home (launcherx) stays enabled on purpose: voice search and the watchlist need it.
#
# Usage: TV_HOST=<tv ip> ./install.sh               builds both apps, then installs
#        TV_HOST=<tv ip> SKIP_BUILD=1 ./install.sh   installs the APKs already built
# Needs: tools/venv (python3 -m venv tools/venv && tools/venv/bin/pip install -r tools/requirements.txt),
#        a JDK 17 and the Android SDK (platform 34, build-tools 34) for the build, USB debugging on at the TV.
set -euo pipefail
cd "$(dirname "$0")"
: "${TV_HOST:?set TV_HOST to the IP address of the TV}"
PY=tools/venv/bin/python
[ -x "$PY" ] || { echo "run: python3 -m venv tools/venv && tools/venv/bin/pip install -r tools/requirements.txt" >&2; exit 1; }
if [ "${SKIP_BUILD:-}" != 1 ]; then
  [ -n "${JAVA_HOME:-}" ] || { [ -d /opt/homebrew/opt/openjdk@17 ] && export JAVA_HOME=/opt/homebrew/opt/openjdk@17; }
  [ -n "${ANDROID_HOME:-}" ] || { [ -d "$HOME/Library/Android/sdk" ] && export ANDROID_HOME="$HOME/Library/Android/sdk"; }
  [ -n "${JAVA_HOME:-}" ] && export PATH="$JAVA_HOME/bin:$PATH"
  # A Gradle 8.9 unpacked under .toolchain/ is used when present; otherwise the launcher's own wrapper (which
  # downloads Gradle 8.9 from services.gradle.org and checks its SHA-256).
  GRADLE="$PWD/.toolchain/gradle-8.9/bin/gradle"
  [ -x "$GRADLE" ] || GRADLE="$PWD/launcher/gradlew"
  (cd tiles && "$GRADLE" assembleRelease -q --console=plain)
  (cd launcher && "$GRADLE" assembleRelease -q --console=plain)
fi

echo "== companion tiles"
"$PY" -I tools/b64push.py tiles/app/build/outputs/apk/release/app-release.apk /data/local/tmp/hometiles.apk
"$PY" -I tools/tv.py 'pm install -r /data/local/tmp/hometiles.apk; rm -f /data/local/tmp/hometiles.apk'

echo "== launcher (the stock launcher is left alone)"
# The upstream script ends with an error here by design: on Google TV, Android keeps launcherx as Home, and the
# script reports that. Everything before that line (install, start, location grant) has run by then.
# So a failed install cannot pass for a good one, the package's update time must change across the call.
stamp() { "$PY" -I tools/tv.py 'dumpsys package com.gathin.tvhome | grep -m1 lastUpdateTime' 2>/dev/null || true; }
before=$(stamp)
(cd launcher && SKIP_BUILD=1 STOCK_LAUNCHER=none ./build-and-install.sh) || true
after=$(stamp)
[ -n "$after" ] && [ "$after" != "$before" ] || { echo "ERROR: the launcher was not installed (update time unchanged: '$after')" >&2; exit 1; }

echo "== Home helper"
# Android unbinds the helper when its app is reinstalled. Take only our entry out of the list, wait, and put it
# back: every other accessibility service (a screen reader, for one) stays on throughout.
SVC=com.gathin.tvhome/.HomeKeyService
"$PY" -I tools/tv.py "s=\$(settings get secure enabled_accessibility_services | tr ':' '\\n' | grep -v -e '^com\\.gathin\\.tvhome/' -e '^null\$' -e '^\$' | tr '\\n' ':' | sed 's/:\$//'); settings put secure enabled_accessibility_services \"\$s\"; sleep 1; if [ -n \"\$s\" ]; then settings put secure enabled_accessibility_services \"\$s:$SVC\"; else settings put secure enabled_accessibility_services $SVC; fi; settings put secure accessibility_enabled 1; settings get secure enabled_accessibility_services"

echo "== theme"
tools/theme.sh theme
