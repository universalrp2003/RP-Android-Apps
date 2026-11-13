## System App Disabler 0.1.0 — first public release

Turn off the pre-installed apps you never use, without root and without uninstalling anything.
Disable-only: every action can be undone from inside the app. Built for the phones that ship with
dozens of apps you cannot remove — including Xiaomi/HyperOS, where Android refuses the normal
disable command.

### What it does

- **Two windows.** *Setup* checks Shizuku and opens it for you with one button. *Apps* is the list.
- **Real, honest safety marks.** 2,394 packages are classified from the Universal Android Debloater
  lists: *Safe*, *Caution* and *Never* (core system packages are blocked inside the app).
- **Nothing is uninstalled.** A turned-off app is hidden and stopped; it comes back with one tap.
- **Suspended rather than disabled on HyperOS.** Xiaomi's PackageManager refuses `pm disable-user`
  for system packages, so this build falls back to `pm suspend`, which does the same job for you
  (the app cannot run) and is fully reversible. The app says *Suspended* rather than pretending it
  disabled the app.
- **English and Tamil on every screen.** Both languages are shown at once, so nothing is hidden
  behind a language switch.
- **"Get more info"** for any package opens Google, an AI answer, Reddit/XDA or App info, so you can
  check what something does before touching it.

### What you need

Shizuku, installed separately from <https://shizuku.rikka.app/download/>. Start it once with
Wireless debugging, allow this app, and the Setup window turns green. No root, no PC after the first
pairing.

### Tested on

One physical Xiaomi phone running HyperOS, screenshots from 13 November 2026: Shizuku permission
granted, six-to-seven system packages turned off, Calendar storage / Cell Broadcast / OK Google
Hotword / Photo Screensavers / Joyose all suspended successfully, list, tabs, About and both
languages checked. This is **not** a Play Protect clearance and no other phone model was tested.

### Notes

- This is the first public release, so there is no older install to update from.
- Shizuku stops when the phone reboots; start it again and everything here keeps working.
- Disabling *Caution* packages can remove a feature. Read the dialog before confirming.
