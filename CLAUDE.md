# tv-home-kit — instructions for a coding agent

Read `PLAYBOOK.md` in full before the first command. It holds the rules, the order of work, the privacy audit,
the design of the home screen and the undo.

The rules that cost the most if broken:
- Back up first, and keep the backup, screenshots and `tools/adbkey/` out of git.
- Disable, never uninstall, anything that came with the TV.
- Never disable `com.google.android.apps.tv.launcherx` (voice search, watchlist). Find the owner of the saved
  Wi-Fi network before disabling any setup app.
- Read the TV back after every change. An injected key press is not proof of what the physical remote does.
- Read the highlighted row before every injected OK in the TV's Settings.
- Never state an unmeasured fact (which port, which channel list); say "not confirmed" and name the test.
- `tools/` is upstream's helper set: do not edit it. Our code is `launcher/` (additions marked "home-tv addition"),
  `tiles/`, `theme/`, `install.sh`, `restore.sh`.
- Switch USB debugging off when the work ends and prove the port is closed.
