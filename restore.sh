#!/usr/bin/env bash
# Put the TV back the way it was before this project touched it:
#   1. switch the Home helper off (it was not there before),
#   2. re-enable every package the device's lists disabled,
#   3. remove the two apps this project added (the launcher and the companion tiles). Nothing that came with
#      the TV is ever uninstalled. Apps you installed from the Play Store stay; remove them on the TV if unwanted.
# Google TV's own home was never disabled, so it is simply Home again afterwards.
#
# Usage: TV_HOST=<tv ip> [DEVICE_DIR=devices/<your tv>] ./restore.sh   (USB debugging must be on, as during the install)
set -euo pipefail
cd "$(dirname "$0")"
: "${TV_HOST:?set TV_HOST to the IP address of the TV}"
PY=tools/venv/bin/python
if [ -z "${DEVICE_DIR:-}" ]; then
  DEVICE_DIR=$(ls -d devices/*/ 2>/dev/null | head -n 1 || true)
fi
"$PY" -I tools/tv.py "s=\$(settings get secure enabled_accessibility_services | tr ':' '\\n' | grep -v -e '^com\\.gathin\\.tvhome/' -e '^null\$' -e '^\$' | tr '\\n' ':' | sed 's/:\$//'); settings put secure enabled_accessibility_services \"\$s\"; echo helper-off"
# Later lists first, so the order of the undo mirrors the order of the changes.
for list in $(ls -r "${DEVICE_DIR%/}"/debloat-*.txt 2>/dev/null); do
  tools/restore.sh "$list"
done
"$PY" -I tools/tv.py 'pm uninstall com.gathin.tvhome; pm uninstall tv.home.tiles; input keyevent KEYCODE_HOME'
"$PY" -I tools/tv.py 'pm list packages -d'
echo "Above: the packages still disabled. Expected: only those that were already disabled before this project."
