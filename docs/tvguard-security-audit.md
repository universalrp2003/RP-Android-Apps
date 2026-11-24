# Android TV Guard — the Security audit (1.5.2)

A read-only screen under **Tools & Settings → Expert settings → Security audit**. It answers one
question for someone who bought a set-top box that a provider script or a stranger has touched:
*what on this box can install apps, connect to it, control it, or replace the launcher — and where
do I go to turn that off?*

It changes nothing itself. Every line links to the Android screen that owns the setting; only
there can anything be changed.

## Why this exists

The video that prompted it (MRVIDH, 21 Nov 2026) shows a Jio set-top box unlocked from a PC on
the same network: ADB switched on, apps installed from outside the store, a replacement launcher
set as HOME, the box still managed by the provider. None of that is visible anywhere in the stock
TV interface. The audit makes each item visible with a severity and a way out.

## The five findings

| Key | Finding | What is read | Link |
|---|---|---|---|
| `adb` | ADB debugging (USB / network) | `Settings.Global.adb_enabled`; `adb_wifi_enabled` on Android 11+ | Developer options |
| `unknown_sources` | Apps allowed to install from unknown sources | Packages holding `REQUEST_INSTALL_PACKAGES` whose AppOps mode is `MODE_ALLOWED`; this app is left out of its own audit | One link per app: its "Install unknown apps" page |
| `device_policy` | Device management and admin apps | `getActiveAdmins()` and `isDeviceOwnerApp()` for visible admins | Device admin apps (falls back to Security settings) |
| `accessibility` | Enabled accessibility services | `Settings.Secure.enabled_accessibility_services` | Accessibility settings |
| `home` | Default HOME app | `resolveActivity(MAIN + HOME)`; `"android"` (the system picker) means none is set | Home app settings |

The HOME finding compares the current default launcher with the one recorded at the previous TV
Guard launch. Opening the audit acknowledges the change: the row keeps showing it for that visit,
and the shield reflects the remaining findings at its next sample.

## Severity table

The rules live in `SecurityAuditRules` and are unit-tested row by row (`SecurityAuditRuleTest`).

| Finding | OK | Review | Alert |
|---|---|---|---|
| ADB debugging | both off | USB on or network status unavailable | **network on** |
| Install unknown apps | none allowed | 1–2 allowed | 3 or more allowed |
| Device policy | no owner, no admin | an admin app | **managed device** |
| Accessibility | none enabled | 1 enabled | 2 or more enabled |
| HOME app | unchanged | changed since the last launch | — |

The reasoning behind the red items:

- **Network ADB** exposes a debugging endpoint over the network; Android normally requires pairing or authorization, but an already-authorized device may connect without physical access. USB ADB also requires authorization.
- **A managed device** can be controlled remotely by whoever provisioned it. An admin app only has
  the policies it was granted.
- **Several accessibility services** can read the screen and inject input. One may be deliberate
  (a remote-control helper); several is worth a hard look.

On Android 10 and older, or where the network ADB setting is absent, the row says the network status is unavailable and asks for review instead of assuming it is off. The status setting is an Android implementation detail and may not reflect every vendor mechanism.

## What it stores

Three things, in `tvguard_security_audit` (separate from launcher data and from the Expert choices):

- `last_home_package` — one package name, written at every launch so the comparison is possible.
- `home_changed` — set at launch when the package differs; cleared when the audit is opened.
- `overlay_shield` — the opt-in shield below.

## The status-strip shield (opt-in, off by default)

A switch at the top of the audit adds a **sixth cell** to the floating status pill:
`secure` / `review` / `alert`, from the worst of the five findings, or `unavailable` if a read fails. It needs the status pill on.
The readings behind it are package-manager queries, so the service samples it on a 60 s cadence
instead of every CPU tick, and only when the cell exists. The pill is otherwise unchanged; the
default build still has exactly five cells (`OverlayRenderTest` measures both).

## D-pad

Focus lands on the back button. Down walks the summary line, the shield switch, the first finding,
its link, then the next finding. The HOME finding is the last lazy row and is reached by scrolling;
its link is the end of the chain. Links open system screens and are not pressed by the focus test.

## Limitations, stated plainly

- Device-owner detection checks only visible active admins for the current user; it does not prove that other users or profiles are unmanaged. An error reading the audit is shown as unavailable, not clear.
- It reports what Android exposes to an app. A rooted or provider-signed modification can hide
  from these settings; a clean result is not proof of a clean box.
- App names come from the package manager; an app with no label shows its package name.
- Firmware without a settings screen gets the "not available on this TV" line rather than a crash.
