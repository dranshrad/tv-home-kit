# OnePlus TV 65 Q2 Pro

Measured on one set. Model `E00`, codename `Edinburgh`, Google TV on Android 11 (API 30), firmware
`RTT2.220808.001`, security patch 2023-09-05, MediaTek board `mt5889`, `armeabi-v7a`, 2.9 GB RAM, UI at 1920×1080.

## Facts that shape the work

| Fact | Value |
|---|---|
| Debugging | USB debugging alone opens TCP 5555 on the network; `ro.adb.secure=1` (a computer must be accepted on the TV) |
| Home claimants | `com.google.android.apps.tv.launcherx` (priority 2), `com.google.android.tungsten.setupwraith` (priority 1), Settings fallback |
| Saved Wi-Fi owner | `com.google.android.tungsten.setupwraith` — **never disable it** on this set |
| HDMI input ids | HDMI 1 = `com.mediatek.tvinput/.hdmi.HDMIInputService/HW5`, HDMI 2 = `…/HW6`, HDMI 3 = `…/HW7` |
| Input viewer app | `com.heytap.tv.livetv` (add it as the tiles' `app` meta-data and to `<queries>`) |
| HDMI capability | 4K 120 Hz and VRR on HDMI 2 and 3; eARC on HDMI 1 (maker's product page) |
| Cameras | none (`dumpsys media.camera`: 0 devices) |
| Microphones | far-field array; "Hands-free mic" was on by default and the Assistant held the built-in microphone open continuously until it was switched off (PLAYBOOK.md 5.1) |
| Standby | the network and the phone-remote service stay reachable for at least 90 s after standby; wake lands on the new home |
| Updates | none found after September 2023 |

## Package lists

`debloat-1-telemetry.txt` (5 packages) and `debloat-2-oneplus-extras.txt` (8 packages). Apply in that order.

| Disabled package | Feature that goes with it |
|---|---|
| `com.oneplus.tv.launcher` | OxygenPlay page; the OnePlus button on the remote |
| `com.heytap.tv.assistant.oneplus` | TV Manager: memory cleaner, bandwidth alerts |
| `com.heytap.tv.msgboard` | Message board |
| `com.coloros.relax.tv` | "Relax" ambient sounds |
| `com.oneplus.tv.userguide` | Built-in user guide |
| `fusion.android.tv.demo` | Shop demo mode |
| `oneplus.smarthome` | OnePlus smart-home panel |
| `android.autoinstalls.config.OnePlus` | Partner app auto-install list |
| `com.google.android.feedback` | "Send feedback" to Google |
| `com.heytap.mcs` | OnePlus push notifications |
| `com.heytap.tv.remotediagnosing`, `com.heytap.tv.remotecontrolinspect` | OnePlus service diagnostics |
| `com.heytap.rom.service` | Log upload; also account and far-field-plug receivers |

**Persistent processes (PLAYBOOK.md 5.7):** `com.heytap.mcs`, `com.heytap.rom.service` and `oneplus.smarthome` are
flagged `PERSISTENT`. Disabled, they still load at boot (no services; four code-registered receivers in
`com.heytap.mcs`; no outside connection seen in a 24-second sample). `am force-stop` leaves them running and
`pm hide` is refused to the shell.

## Traps found on this set

- `cmd package set-home-activity` prints Success and changes nothing while `launcherx` is enabled.
- Picture and sound `settings` keys are stale mirrors; only the TV's menu is the truth.
- `tv_input_hdmi_edid_version` mirrors the last-selected HDMI port (rewritten by `com.mediatek.tv.service` at every
  switch): never "restore" it with `settings put`.
- The Inputs menu's "Connected" mark and `tv_input` states do not prove which port a sleeping box is on.
- A file pushed to `/sdcard/Pictures` stays invisible to other apps until
  `content call --uri content://media --method scan_volume --arg external_primary`.
- A permission granted with `pm grant` reaches a running app only after `am force-stop`.
- `uiautomator dump` returns the screen behind the main Settings list, but reads its sub-pages.
- OnePlus's own page, Settings → System → Personalise, holds "Visible to other bluetooth devices" and "Quick
  discover" (the OnePlus Connect link).
- Privacy switches found on at first read: Hands-free mic, Usage & diagnostics, an advertising ID, "Scanning always
  available", Bluetooth visibility.
