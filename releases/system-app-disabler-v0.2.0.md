## System App Disabler 0.2.0 — colour-coded safety, bootloop protection, live counts

Still disable-only: no root, no uninstalling anything, and every action can be undone inside the app.
0.2.0 is about making the safety information impossible to miss — and about refusing the handful of
packages that can leave a phone unable to start.

### New in 0.2.0

- **Counts at the top of the list.** 📦 installed, ⚙ pre-installed, 👤 you added, ⛔ turned off and
  ▶ running. Tap any tile to filter the list to exactly that set. *Running* is read from the
  process table over Shizuku, so without Shizuku it shows `—` rather than a false zero.
- **Green, amber, red — on the whole row.** Safe rows are green, Caution rows amber, Never rows red:
  the app name, the package, the rating and the description all take the colour, and the card is
  tinted to match, so the list reads at a glance.
- **Bootloop protection.** The database's own warnings are now enforced, not just displayed.
  Twenty packages whose database text says removal can stop the phone booting are **blocked** with a
  disabled button — including `com.android.htmlviewer`, which is only rated *Caution* but whose text
  names MIUI 12.5.4+ phones (that includes HyperOS).
- **The keyboard and accessibility guard.** The app refuses to turn off the keyboard currently in
  use, or a package with an accessibility service switched on. Both are recoverable in principle,
  but not from a phone you cannot type on.
- **Less clutter.** Search is a magnifier that expands when tapped. The Apps window is English-only;
  About keeps its Tamil section.

### Why these exist

0.1.0 let a real phone turn off a vendor keyboard and the print spooler with no warning. Printing
stopped working and a Settings menu began showing "Interconnectivity isn't available… managed by
Shell". Both are reversible from this app — but the same mistake with an unlisted keyboard can leave
no way to pair Shizuku again.

### Tested on

One physical Xiaomi phone running HyperOS, screenshots from 13 November 2026 of the exact candidate
this release was published from: counts strip, green/amber/red rows, running filter, blocked rows
with disabled buttons, and the Apps window in English. The 0.1.0 install was updated in place with
the same release key. This is **not** a Play Protect clearance and no other phone model was tested.

### Notes

- Updates in place over 0.1.0; no uninstall needed, and no data is cleared.
- Shizuku stops when the phone reboots; start it again and everything keeps working.
- Disabling a *Caution* package can remove a feature. Read the confirm dialog.
