# The playbook — a calm, private home screen for a Google TV or Android TV

This file is written for a coding agent (Claude Code or a similar tool) working for the owner of one television.
Read all of it before the first command. It holds the whole method: the rules, the order of work, the commands,
the design of the home screen, the privacy and security audit, the traps found on a real set, and how to undo
everything.

The owner is assumed to be non-technical. The agent does the work, reads every result back from the TV, and asks
the owner only to press a button on the TV or to choose between plain options.

---

## 0. What the owner gets

1. A home screen with no adverts and no recommendation rows: the owner's own photographs full-screen, a two-row
   shelf of apps at the bottom, one small preview card, a clock. Tiles for HDMI inputs (set-top box, console) sit
   beside the apps, so one remote and one screen reach everything.
2. Google TV's own home kept alive behind a "Watchlist" tile, because voice search and the watchlist live there.
3. A measured privacy and security pass: microphone, camera, diagnostics, advertising ID, open network ports,
   Bluetooth visibility, remote-access apps, vendor telemetry.
4. Nothing uninstalled. Every change has a one-command undo (`restore.sh`).
5. The TV controllable from a phone (Android or iPhone) with Google's own app.

## 1. Rules — each one cost something to learn

1. **Back up before the first change.** Package lists, `getprop`, the three `settings` tables, the Home claimants,
   the owner of the saved Wi-Fi network, TV inputs, HDMI-CEC state. Keep the backup **outside git**: it holds
   serial numbers, account names and network names.
2. **Disable, never uninstall,** anything that came with the TV. `pm disable-user --user 0 <package>` is undone by
   `pm enable <package>`.
3. **Never disable the stock Google TV home** (`com.google.android.apps.tv.launcherx`). The Assistant refuses voice
   search without it, and the watchlist lives in it.
4. **Find who owns the saved Wi-Fi network before disabling any setup app.** On Google TV the setup wizard
   (`com.google.android.tungsten.setupwraith`) can own it; disabling it deletes the network and cuts your own
   connection. Read `dumpsys wifi | grep -i -E "cname|creator"` first.
5. **Never launch or disable factory and service-menu apps** (names with `factory`, `factorymenu`, `engineer`).
6. **Change in small batches and read the result back from the TV** (foreground activity, `pm list packages -d`,
   a screenshot). A result is not real until it is read back.
7. **A key press you inject is not proof of what the physical remote does.** `input keyevent` never passes through
   an accessibility key filter, and `sendevent` is denied to the shell. For remote behaviour, ask the owner to
   press the real button and say what happened.
8. **Before every injected OK in the TV's Settings, read which row is highlighted** (a screenshot, or
   `uiautomator dump`). Settings pages remember their last row; "Reset" and the Wi-Fi switch are one OK away.
9. **`settings` keys for picture and sound can be stale mirrors.** Change picture settings only through the TV's
   own menu, and read the menu, not the key.
10. **Never fill a gap with a guess.** Which HDMI port a box is on, whether a channel list matches, whether the
    microphone button works: if it was not measured, say "not confirmed" and name the test that settles it.
11. **Close the door when the work is finished:** USB debugging off, then prove the port is closed
    (`tools/adb-closed.py <tv ip>`).
12. **No private data in git, ever:** photographs, backups, screenshots of Settings (they show account names,
    device names, Bluetooth addresses), IP and MAC addresses, serial numbers, the ADB key under `tools/adbkey/`.
    Save every screenshot under `devices/<your-tv>/shots/` and every backup under `devices/<your-tv>/backup-<date>/`
    or outside the repository: the `.gitignore` here excludes those folders, the owner's wallpapers and key files.
    Keep it that way, and read `git status` before any commit.

## 2. What is in this repository

| Path | What it is |
|---|---|
| `launcher/` | The home-screen app (`com.gathin.tvhome`, Java). It is the launcher of [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv) at commit `294d3d5` (MIT) plus the additions in section 7. No internet permission |
| `tiles/` | A 100 KB companion app (`tv.home.tiles`): one tile per HDMI input and a Watchlist tile. No permissions |
| `theme/` | `config.json` (which apps, which order, how it looks) and `wallpapers/` (four neutral placeholders; put the owner's photographs here) |
| `tools/` | The Mac/Linux-side helpers from cleanyourtv, unmodified: `tv.py` (a pure-Python ADB client, no `adb` binary needed), `disable.sh`, `restore.sh`, `check.py`, `theme.sh`, `adb-closed.py`, and more |
| `devices/` | Per-TV package lists and notes. One real example: `oneplus-tv-65-q2-pro/` |
| `install.sh`, `restore.sh` | Install everything; put everything back |
| `PLAYBOOK.md` | This file |

## 3. The order of work

| Phase | Outcome | Proof it is done |
|---|---|---|
| A. Connect | The computer talks to the TV | `tv.py --probe` prints the model |
| B. Know the TV | A facts table and a backup | backup folder outside git |
| C. Privacy and security audit | Section 5 walked, findings listed, fixes applied through the TV's own menus | each switch read back; microphone test passes |
| D. Debloat | Telemetry and promo packages disabled in batches | `pm list packages -d`; nothing the owner uses is gone; a restart survives |
| E. Home screen | Launcher, tiles, helper, theme installed | Home from an app lands on the new home; restart lands on it |
| F. Inputs | HDMI tiles switch ports | routing path read back; the owner confirms the picture |
| G. Verify with the owner | The physical-remote checklist (section 9) | the owner's answers, written down |
| H. Phone as remote | Google TV app paired | the owner moves the highlight from the phone |
| I. Close | Debugging off, records written | port 5555 closed |

Do C before D: the audit tells you what the vendor packages are doing, and its fixes are the ones the owner cares
about most.

## 4. Phases A and B — connect, learn the TV, back up

Setup on the computer (once):

```bash
python3 -m venv tools/venv && tools/venv/bin/pip install -r tools/requirements.txt
```

On the TV: Settings → System → About → press OK on "Android TV OS build" seven times → back → Developer options →
USB debugging on. On most sets this opens ADB on TCP port 5555 on the home network. The first connection shows a
prompt on the TV: the owner chooses "Always allow from this computer".

```bash
export TV_HOST=<tv ip>          # Settings → Network & internet shows it; it can change (DHCP)
tools/venv/bin/python -I tools/tv.py --probe
```

Shorthand below: `T '<command>'` means `tools/venv/bin/python -I tools/tv.py '<command>'`. Run one `pm` command per
call; chained `pm` commands can stop silently after the first.

Facts to collect (and where):

| Fact | Command |
|---|---|
| Model, Android version, patch level, chip, ABI | `T 'getprop'` (`ro.product.*`, `ro.build.version.*`, `ro.product.cpu.abi`) |
| Memory, storage | `T 'cat /proc/meminfo'`, `T 'df /data'` |
| Who claims Home, and at what priority | `T 'cmd package query-activities -a android.intent.action.MAIN -c android.intent.category.HOME'` |
| Who owns the saved Wi-Fi network | `T 'dumpsys wifi'` → `cname=` / creator |
| HDMI inputs and their ids | `T 'dumpsys tv_input'` → `HW<n>` entries |
| HDMI-CEC state and devices | `T 'dumpsys hdmi_control'` |
| Every package, system and user | `T 'pm list packages -s'`, `-3`, `-d` |
| All settings | `T 'settings list global'`, `secure`, `system` |
| What is in front right now | `T 'dumpsys activity activities \| grep mResumedActivity'` |

Write these into a backup folder next to the repository (not inside it), then into a short facts table in
`devices/<your-tv>/README.md` **without** serial numbers, addresses or account names.

## 5. Phase C — the privacy and security audit

Read-only first. For each row: read the state, decide, change it **in the TV's own Settings menu**, read it back.

### 5.1 Microphone — is the TV always listening?

A TV with far-field microphones can keep them open for the "Hey Google" wake word, including in standby.

```bash
T 'dumpsys media.audio_flinger | grep -E "^Input thread|Input device|Tracks of which|Standby:"'
T 'dumpsys audio | grep -E "rec (update|start)|src:HOTWORD"'
T 'appops get com.google.android.katniss RECORD_AUDIO'
```

- **Listening:** an `Input thread` with `Standby: no` on `AUDIO_DEVICE_IN_BUILTIN_MIC`, recording activity with
  `src:HOTWORD pack:com.google.android.katniss`, and `RECORD_AUDIO … (running)`.
- **Not listening:** no `Input thread` lines at all.

The switch: **Settings → Privacy → Google Assistant → Hands-free mic** (on newer builds: Accounts & profiles →
the account → Voice assistant → Hands-free mic). Google's own text on that page says the Assistant "will wait in
standby mode to detect 'Hey Google', even if your screen is off". Turn it **off**, then run the three commands
again: the input threads must be gone. Repeat the check once with the TV in standby
(`T 'input keyevent KEYCODE_SLEEP'`, wait a minute, read, then `KEYCODE_WAKEUP`).

The microphone button on the remote keeps working: it is press-to-talk through the remote, not the wake word. If
the TV has a physical microphone switch, ask the owner to slide it off as well: hardware beats software.

Things that do **not** work, so do not try them: `am start -a com.google.android.assistant.HOTWORD_DISABLE` (the
app answers only the Settings app), revoking `RECORD_AUDIO` from the Assistant (that also kills the remote's
microphone button), disabling the Assistant package (voice search goes).

### 5.2 Camera

```bash
T 'dumpsys media.camera | head -12'      # "Number of camera devices: 0" = no camera present
```

A TV with zero camera devices cannot take a picture, whatever permissions its apps hold. If a USB camera is ever
plugged in, the count changes; unplug it after use.

### 5.3 Which apps hold sensitive permissions

```bash
T 'for p in $(pm list packages -3 | cut -d: -f2); do d=$(dumpsys package $p | grep -E "android.permission.(RECORD_AUDIO|CAMERA|ACCESS_FINE_LOCATION|READ_CONTACTS|SYSTEM_ALERT_WINDOW): granted=true" | sed "s/android.permission.//; s/: granted.*//" | tr -d " " | tr "\n" ","); [ -n "$d" ] && echo "$p $d"; done'
T 'settings get secure enabled_accessibility_services'      # expect only the Home helper
T 'settings get secure enabled_notification_listeners'      # expect only the stock home
T 'dumpsys device_policy | head -20'                        # expect no device admins
T 'appops query-op REQUEST_INSTALL_PACKAGES allow'          # apps allowed to install other apps: expect none
```

An installed (third-party) app with the microphone, camera or an accessibility service is a finding to show the
owner. System apps sharing the `android` user all show every permission; that is normal.

### 5.4 The Settings switches

| Where on the TV | Recommended | Why | Cost |
|---|---|---|---|
| Privacy → Google Assistant → **Hands-free mic** | Off | 5.1 | "Hey Google" by voice stops; the mic button stays |
| Privacy → **Usage & diagnostics** | Off | usage and crash data to Google | none |
| Privacy → **Ads** → Delete advertising ID | Delete | apps lose the cross-app ad identifier | adverts become generic |
| Privacy → Location → **Scanning always available** | Off | Wi-Fi scanning for location even with Wi-Fi off | none on a TV |
| Privacy → Google Assistant → **Personal results** | Owner's choice | anyone at the TV can ask for the owner's personal results | personal answers stop |
| Privacy → Security & restrictions → **Unknown sources** | every app off | sideloading path for malware | none |
| System → Cast → **Let others control your cast media** | Owner's choice ("Never" is tightest) | any Android device on the Wi-Fi can control a cast | other phones lose the notification |
| Vendor page, if present → **Visible to other Bluetooth devices** | Off | 5.6 | pairing a new device needs the pairing page open |
| Vendor "user experience / improvement programme" agreements | Decline | vendor telemetry | none |

How to drive Settings without the owner: open it with `T 'am start -a android.settings.SETTINGS'`, move with
`input keyevent KEYCODE_DPAD_DOWN` and friends, take a screenshot after every move (`tv.py --screenshot`), and press
OK only after reading the highlighted row (rule 8). `uiautomator dump` reads sub-pages but often not the main list.
Menu labels and hidden pages can be read from the TV's settings APK: `T 'pm path com.android.tv.settings'`, pull
it, then `aapt2 dump strings` and `aapt2 dump xmltree --file res/xml/<page>.xml`.

### 5.5 Open network ports

```bash
T 'netstat -ltn'                               # what listens
T 'cat /proc/net/tcp /proc/net/tcp6'           # column 8 is the owning uid
T 'pm list packages -U'                        # uid → package
```

| Port | Owner | Verdict |
|---|---|---|
| 5555 | ADB (while USB debugging is on) | The one real risk. Botnets that reach home networks look for it. Off when the work ends (rule 11) |
| 6466, 6467 | Android TV Remote Service | Needed for the phone remote; pairing needs a code shown on the TV |
| 8008, 8009, 8443 | Chromecast | By design; anyone on the Wi-Fi can cast. Never forward these on the router |
| 9000, 9080, 1900/udp, and a DIAL server's own ports | Netflix / DIAL discovery | By design |
| 53317 | LocalSend, while it runs | Close the app after a transfer (`am force-stop org.localsend.localsend_app`); keep its Quick Save off |
| vendor ports under the system uid | the vendor's phone-link service | Owner's choice: a vendor "quick discover" or "connect" switch closes them |

Also read what the TV advertises to the whole network: `dns-sd -B _adb._tcp` (macOS) shows the TV's serial number
for as long as debugging is on.

### 5.6 Old firmware

Read `ro.build.version.security_patch`. A TV whose maker has stopped updates keeps every later hole. The ones a
setting can soften:

- **Bluetooth keystroke injection (CVE-2023-45866, fixed in the December 2023 patch level):** a device in radio
  range can pair as a keyboard without consent. The remote needs Bluetooth, so it stays on; switch the TV's
  Bluetooth **visibility** off, keep Unknown sources off and debugging off so injected keys have little to reach.
- **ADB on the network:** rule 11.
- **Remote-access apps** (AnyDesk and similar): without their control plug-in they can only show the screen after
  a prompt on the TV; unattended access must stay off. If the owner does not use the app, suggest removing it.

### 5.7 Vendor telemetry packages, and the persistent-process trap

List the vendor's packages (`pm list packages -e | grep -i -E "<vendor names>"`) and read what each does before
proposing a list. After disabling, check that they are really gone:

```bash
T 'for p in <each disabled package>; do echo "$p pid=$(pidof $p)"; done'
T 'dumpsys package <package> | grep pkgFlags'          # look for PERSISTENT
```

**Trap:** a system package flagged `PERSISTENT` is started by Android at every boot **even when disabled**. Its
services and receivers declared in the manifest are dead, but its process loads and can register receivers in
code. `am force-stop` does not kill it, and `pm hide` is refused to the shell. The only shell-level stop is
`pm uninstall --user 0 <package>` (undone with `cmd package install-existing <package>`): it removes nothing from
the system image, but it is an uninstall for that user, so it is the **owner's decision**, not the agent's.
Report the packages, what was measured (threads, receivers, outside connections sampled from `/proc/net/tcp`),
and ask.

## 6. Phase D — debloat in batches

1. Write lists under `devices/<your-tv>/debloat-<n>-<name>.txt`: one package per line with a comment saying what it
   is. Batch 1: telemetry, diagnostics, push marketing. Batch 2: the vendor's content and promo apps.
2. Never list: the stock home, the setup wizard (rule 4), the TV input and Live TV apps, the remote's own service,
   the Assistant, Chromecast, the updater, anything `factory`.
3. `tools/disable.sh devices/<your-tv>/debloat-1-….txt`, read `pm list packages -d`, try the TV, then batch 2.
4. Restart once (`T 'reboot'`) and read: home in front, Wi-Fi up, lists intact, no missing feature.
5. Keep a table "disabled package → feature that went with it" in the device README, so a later complaint maps to
   one `pm enable`.

## 7. Phase E — the home screen

### 7.1 How it works

Android keeps the stock launcher as "Home" on Google TV (`cmd package set-home-activity` prints Success and changes
nothing while `launcherx` is enabled). So:

- The launcher's accessibility service `HomeKeyService` (shown on the TV as "Home button") watches window changes.
  When the stock home comes to the front, it starts our `MainActivity`. A Home press is: stock home for an instant
  → ours.
- It also opens our home on Android's own Home-press announcement (`ACTION_CLOSE_SYSTEM_DIALOGS`, reason
  `homekey`), which covers the case where the stock home was already in front and no window change happens. The
  receiver accepts that broadcast **from the system only** (a signature permission), and the window check matches
  the stock launcher's **package**, not just a class name: without those two, any installed app could pull the
  home screen over whatever is playing.
- One Home press therefore starts our home twice, at the window change and 250 ms after the announcement. Keep
  both: merging them into one start was tried and lost a race with the stock home when the idle clock was in front.
- `key_filter` stays **false** in the theme: a service that filters keys can stop held-Volume repeat and the
  remote's app buttons on some firmware.
- The **Watchlist** tile starts `StockHomeActivity`, which sets a pass flag and opens the stock home. While the
  flag is set there is no redirect, also across a visit to another app (Back from a film returns to the
  watchlist). The next Home press clears it.
- **HDMI tiles** send `ACTION_VIEW` on the TV-input passthrough URI of the port (`TvContract.
  buildChannelUriForPassthroughInput`). Input ids and, where needed, the viewer app go in `tiles/…/AndroidManifest.xml`.

### 7.2 The design (the additions over upstream, all marked "home-tv addition" in the source)

The brief was: minimalist, the photograph is the hero, a little depth, nothing that hides the picture.

| Element | Rule | Where |
|---|---|---|
| The shelf | Two rows of eight tiles inside one dark glass band at the bottom. Up and Down bring the next row in on the row line while the leaving row fades; nothing scrolls over the photograph. A dot per row on the band's edge | `MainActivity.snapShelf`, theme `dock_row false`, `rows_below_dock 1`, `columns 8` |
| Preview card | The focused app's artwork on a small tilted card (16% of the screen wide) that turns over when focus moves. Each photograph is measured once; the card goes where the picture has least detail, the middle counted against a place; a picture that is busy everywhere gets a smaller card parked in the corner above the shelf | `Stage.java` (`SPOTS`, `CENTRE_WEIGHT`, `BUSY`, `PARKED`) |
| Text over photographs | Solid white with a soft dark edge. Shade only behind the text, only as strong as that photograph needs (0 on dark pictures, capped) — never a wash over the whole picture | `Stage.legible`, `Stage.shadeFor` (`SMALL_TEXT`, `LARGE_TEXT`, `MAX_SHADE`) |
| No names | No large app name over the photograph and none under the tiles: the tile and the card already say it | theme `dock_title false`, `cinematic_home false`; `Stage.tileNamesHidden` |
| 3D lean | The focused tile leans a few degrees with a moving highlight | `Lean.java` |
| Wave | The two neighbours in the same row rise to 105.5% | `Stage.wave` |
| Backdrop drift | The photograph shifts slightly with the focused column | `WallpaperPlayer.drift` |
| Idle glide | After 25 idle seconds focus walks one app every 6 seconds; any key stops it; a clock comes at 5 minutes | `MainActivity` (`GLIDE_START_MS`, `GLIDE_STEP_MS`), theme `home_idle_clock_minutes` |
| Arrange | A two-arrows button in the top bar; a hint line says what the keys do while the mode is on | theme `reorder_button`, `Stage.hint` |
| Live TV row | `"live_tv_input_id": "none"` leaves it out of the Inputs menu on a TV with no aerial | `MainActivity` |

Every other theme key is upstream's and documented in its
[theme reference](https://github.com/Gathin23/cleanyourtv/blob/294d3d59dc2514e69fed557f46c1ecdfb87a95ec/themes/README.md).
Motion runs only with "Advanced effects" on and reduced motion off.

Design rules worth keeping when the owner asks for changes:

- Ask what the owner sees, in their words, and fix that. A complaint that a picture "is not clearly visible" can
  mean: remove the colour wash over it, not: add contrast.
- Judge an animation from a screen recording cut into frames (`screenrecord` on the TV, `ffmpeg` on the computer),
  not from a description.
- Read every wallpaper on the TV after a text or shade change; one bright photograph breaks what six dark ones hid.
- A name that is too long for its box is a bug: relabel it in the theme (`app_labels`).

### 7.3 Install

1. Put the owner's photographs (JPEG, 1920×1080 or larger, landscape) in `theme/wallpapers/`, with an optional
   `captions.txt` (`file<TAB>caption`). Strip location data first (`exiftool -all= *.jpg`, or re-encode).
2. Edit `theme/config.json`: `dock_apps` in the owner's order (package names from `pm list packages -3`),
   `hidden_apps` for what should stay in the App library only.
3. Edit `tiles/app/src/main/AndroidManifest.xml` and `res/values/strings.xml` for the HDMI ports in use; make
   banners with `tiles/art/banner.html`.
4. `TV_HOST=<tv ip> ./install.sh`. The launcher step prints "Home resolves to …launcherx" as an error: that is
   upstream's script reporting the Google TV behaviour described in 7.1; `install.sh` checks the install itself.
5. Read back: `T 'settings get secure enabled_accessibility_services'` holds `com.gathin.tvhome/.HomeKeyService`;
   Home from an app lands on `com.gathin.tvhome/.MainActivity`; a restart lands there too.

Build needs a JDK 17 and the Android SDK (platform 34, build-tools 34). `launcher/gradlew` fetches Gradle 8.9 and
checks its SHA-256. `tools/docker-build.sh` builds without a local toolchain.

## 8. Phase F — inputs

- Ids: `T 'dumpsys tv_input' | grep HW`. Which physical port is which id: switch ports from the TV's own Inputs
  menu and read `T 'dumpsys hdmi_control | grep mActiveRoutingPath'` (`0x1000` = port 1, `0x2000` = port 2 …).
- Which device sits on which port: **only a picture proves it.** "Connected" marks, cable-status keys and
  `tv_input` states read the same for a sleeping box and an empty port. Ask the owner to switch the device on and
  press the tile.
- Switching the TV to a port does not wake a console. The supported direction is the other one: the console wakes,
  HDMI-CEC switches the TV.
- A set-top box that does not answer HDMI-CEC cannot be driven by the TV's remote. Look for the provider's app on
  the TV instead, or teach the box's remote the TV's volume and power.
- Do not "fix" a provider's set-top box with exploits: it is usually the provider's property.

## 9. Phase G — what only the owner can verify

Ask for each, one at a time, and write the answer down:

1. Press the real Home button from an app, and from the Watchlist screen.
2. Press the microphone button and ask for a film.
3. Hold Volume Up: it must ramp, not step once.
4. Press the remote's app buttons (Netflix, YouTube …).
5. Sound through the soundbar or receiver.
6. Each HDMI tile with its device switched on.
7. The Arrange button with the real remote.
8. Whether the motion (lean, glide) and the picture mode suit them.

Symptom → cause → fix, for the common ones:

| Symptom | Likely cause | Fix |
|---|---|---|
| Home stays on the stock home | the helper was unbound (Android unbinds it when the launcher is reinstalled) | re-run `install.sh`'s "Home helper" step |
| Stock home flashes on every Home press | by design (7.1) | theme `key_filter: true` + `home_long_press_home: true`, then re-test held Volume |
| Mic button does nothing | the stock home or the Assistant got disabled | `pm enable` it |
| Held Volume moves one step | a key-filtering service is on | `key_filter` false; remove other accessibility apps |
| A tile shows "No signal" | device asleep, or the wrong port id | section 8 |
| Text hard to read on one photograph | its shade is too weak | lower `SMALL_TEXT` / `LARGE_TEXT` or raise `MAX_SHADE` in `Stage.java`; last resort `wallpaper_dim` |
| Card sits on the subject of a photograph | no calm place in that picture | edit `SPOTS`, or drop the picture |
| A vendor feature is gone | its package is in a debloat list | `pm enable <package>` |
| Everything should go back | — | `TV_HOST=<tv ip> ./restore.sh` |

## 10. Phase H — the phone as the remote (Android or iPhone)

The TV side is Google's Android TV Remote Service (`com.google.android.tv.remote.service`, ports 6466 and 6467,
advertised as `_androidtvremote2._tcp`). Check it before sending the owner to the phone:

```bash
T 'pm list packages -e | grep tv.remote.service'
T 'netstat -ltn | grep -E ":646[67] "'
```

On the phone: install **Google TV** (App Store or Play Store) → open it → tap the remote / "TVs nearby" button →
choose the TV → type the code the TV shows. iPhone: allow "Local Network" when asked; same Wi-Fi; no VPN on the
phone. The **Google Home** app offers the same remote once the TV is in the owner's Home.

The remote's Home button goes through the same path as the physical one, so it lands on the new home.

Power: if the TV's standby keeps the network up, the phone can switch it on as well as off. Test it: standby,
wait a minute, check that port 6466 still answers, then press power in the app.

AirPlay is a different thing (sending a picture, not control), and a TV without it needs a receiver app; third-party
"TV remote" apps from the stores are not needed and often carry adverts or subscriptions.

## 11. Phase I — close

1. USB debugging off (Settings → System → Developer options). `tools/adb-closed.py <tv ip>` must report closed.
2. Write, outside git: what changed, what was verified and how, what was not verified, what the owner still has
   to decide.
3. Keep `restore.sh` and the device lists: they are the undo.

## 12. A bug hunt to run before calling it done

- Home: from an app; from the Watchlist; twice in a row; held; after a theme push; after a restart; after standby.
- A forged Home announcement must do nothing while another app is in front:
  `T 'am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS --es reason homekey'` → the foreground app stays.
- Watchlist → open any app → Back: the watchlist is still there. Home: the new home.
- Shelf: walk to the last row and back; Right on the last tile; Up into the top bar (neighbours settle); Home from
  a lower row (the first rows show).
- Arrange: pick up, move across rows, put down, Back; the order survives a restart.
- Every wallpaper: greeting, clock, date and caption readable.
- Idle: glide at 25 s, clock at 5 min, screensaver, sleep; any key returns.
- Unit tests: `cd launcher && ./gradlew testReleaseUnitTest`.

## 13. Known limits

- Content previews from inside apps (posters, "continue watching") are not available to an add-on launcher on
  Google TV: the data permission is signature-or-privileged.
- The top shade fades toward the date line; on a very bright photograph the date can be the weakest text.
- The arrange hint is English only.
- With a library footer row in the shelf (not this theme), the footer can sit off the band.
- The companion tiles assume the launcher's default package name.
- On Android 8–10 another app with storage permission could rewrite the theme file; on Android 11 and later it
  cannot.

## 14. What this builds on

| Project | What it is | What was taken |
|---|---|---|
| [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv) (MIT) | Debloat lists, ADB helpers and a launcher for many TVs | `launcher/` (modified) and `tools/` (unmodified) at commit `294d3d5`; the theme system; the idea of a prompt file for an agent |
| [man15h/tellyscope](https://github.com/man15h/tellyscope), [seun-novodev/android-tv-debloat-toolkit](https://github.com/seun-novodev/android-tv-debloat-toolkit), [Ansub/tv-detox](https://github.com/Ansub/tv-detox), [humblemuzzu/dumbtv](https://github.com/humblemuzzu/dumbtv) | Other debloat and privacy projects | Read as references; no code copied |
| [spocky/miproja1](https://github.com/spocky/miproja1) (Projectivy), [yuliskov/SmartTube](https://github.com/yuliskov/SmartTube) | A closed-source launcher; an alternative YouTube client | Considered and not used. If SmartTube is ever installed, take it only from its developer's release page |
| [theothernt/AerialViews](https://github.com/theothernt/AerialViews) | A screensaver | Recommended as the TV's screensaver; installed from the Play Store, not bundled |

What this repository adds on top: the order of work with proofs, the measured privacy and security audit (5), the
persistent-process finding (5.7), the Google-TV-safe Home design with its two hardening checks (7.1), the shelf,
preview card and per-photograph shade (7.2), the HDMI and Watchlist tiles, and the owner-facing verification and
undo.

Made by Dr. Divyansh Gupta with Claude Code. MIT licence; see `LICENSE` and `NOTICE.md`.
