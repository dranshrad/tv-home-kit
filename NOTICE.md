# Notice — what comes from where

## From Gathin23/cleanyourtv (MIT, © 2026 Gathin)

Source: https://github.com/Gathin23/cleanyourtv at commit `294d3d59dc2514e69fed557f46c1ecdfb87a95ec`.

| Path here | Upstream path | State |
|---|---|---|
| `launcher/` | `launcher/` | Modified: see below |
| `tools/` | `tools/` | Unmodified. Left out: `race_calendar.py`, `philips-api.py`, the README generators, Windows scripts |

Fonts and icons inside `launcher/` keep their own licences: `launcher/FONT-LICENSE-OFL.txt` (Outfit, SIL OFL 1.1),
`launcher/ICONS-LICENSE-FLUENT-EMOJI.txt`.

## Changes to the launcher (© 2026 Dr. Divyansh Gupta, MIT)

Every change is marked "home-tv addition" in the source.

| File | Change |
|---|---|
| `Lean.java` (new), `AppAdapter.java`, `MainActivity.java` | 3D lean on the focused tile and its highlight |
| `WallpaperPlayer.java` | Backdrop drift; each photograph measured once for the stage |
| `Stage.java` (new), `StageTest.java` (new) | Shade behind text per photograph, the preview card and where it goes, the dock wave, the arrange hint |
| `MainActivity.java` | The two-row shelf that pages, the idle glide, the arrange hint hooks, `live_tv_input_id: "none"` |
| `StockHomeActivity.java` (new), `HomeKeyService.java`, `AndroidManifest.xml` | The Watchlist pass; every Home-press announcement opens this home; that announcement is accepted from the system only (signature permission); the stock-home redirect checks the window's package. A Home press starts this home twice on purpose (at the window change and 250 ms after the announcement): the first start can lose a race with the stock home |
| `res/values/ids.xml` (new) | One view tag |

## New in this repository (© 2026 Dr. Divyansh Gupta, MIT)

`tiles/`, `theme/`, `devices/`, `install.sh`, `restore.sh`, `PLAYBOOK.md`, `README.md`, `CLAUDE.md`. The wallpapers
in `theme/wallpapers/` are generated gradients with no third-party content.
