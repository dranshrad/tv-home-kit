# tv-home-kit

A calm, private home screen for a Google TV or Android TV, and a method for a coding agent to set it up.

- **No adverts, no recommendation rows.** Your own photographs full-screen, a two-row shelf of apps, one small
  preview card, a clock. Tiles for HDMI inputs (set-top box, console) sit beside the apps.
- **Voice search and the watchlist keep working.** Google TV's own home stays enabled behind a "Watchlist" tile.
- **A measured privacy pass.** Is the microphone open for "Hey Google" right now? Is there a camera? Which ports
  are open? The playbook shows how to read each from the TV and where the switch is.
- **Nothing uninstalled.** Vendor packages are disabled, never removed; `restore.sh` puts the TV back.
- **No network access in the apps.** The launcher and the tiles hold no internet permission.

## Use it with Claude Code

```bash
git clone https://github.com/dranshrad/tv-home-kit && cd tv-home-kit
claude
```

Then say: *"Read PLAYBOOK.md and set up my TV. Its address is …"*. The agent connects, backs the TV up, walks the
privacy audit, proposes a package list for your model, installs the home screen and asks you to press a few
buttons on the real remote to confirm. [`PLAYBOOK.md`](PLAYBOOK.md) is the whole method; it also works as a manual
if you prefer to do it by hand.

## Use it by hand

```bash
python3 -m venv tools/venv && tools/venv/bin/pip install -r tools/requirements.txt
export TV_HOST=<your tv's ip>                 # USB debugging on at the TV (PLAYBOOK.md, section 4)
tools/venv/bin/python -I tools/tv.py --probe
# put your photographs in theme/wallpapers/, your apps in theme/config.json, your HDMI ports in tiles/
./install.sh
```

Undo: `TV_HOST=<your tv's ip> ./restore.sh`.

Needs macOS or Linux, Python 3, and for the build a JDK 17 plus the Android SDK (platform 34, build-tools 34).

## What is tested

One television end to end: a OnePlus TV 65 Q2 Pro (Google TV on Android 11, MediaTek). Its package lists and
notes are in [`devices/oneplus-tv-65-q2-pro/`](devices/oneplus-tv-65-q2-pro/). The launcher's 324 unit tests pass.
Other Google TV and Android TV sets should work with their own package list and HDMI input ids; the playbook
explains how to find both. Treat anything not listed here as untested.

## Safety

- Read [`PLAYBOOK.md`](PLAYBOOK.md) section 1 before changing a TV. Two mistakes cost the most: disabling the
  stock Google TV home (voice search dies) and disabling the setup wizard (on some sets it owns the Wi-Fi network).
- USB debugging opens a network port on the TV. Switch it off when you finish.
- Never commit your backups, screenshots or `tools/adbkey/`: they hold serial numbers, account names and a private
  key. The `.gitignore` here excludes them.

## Credits and licence

Built on [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv) (MIT): `launcher/` is its launcher with
the additions listed in `NOTICE.md`, `tools/` is its helper set, unmodified. Outfit font under the SIL Open Font
Licence; icons under the Fluent Emoji licence (files in `launcher/`).

Made by Dr. Divyansh Gupta with Claude Code. MIT licence. Not affiliated with Google, OnePlus or any TV maker;
product names are used only to say what the software works with.
