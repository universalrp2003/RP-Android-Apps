# System App Disabler 0.5.0 TV candidate (unpublished)

This is candidate work, not a new public release. It adds a Leanback launcher entry and a TV
banner while retaining the phone launcher. On a TV, focus starts on Setup; app cards and summary
tiles show a focus outline, the Shizuku guide has a top Back button, and the remote Back action
returns to Setup. Android TV devices vary: browser availability, Shizuku support, pairing and
manufacturer navigation can differ. The app does not enable debugging itself.

## Launcher protection

Before either the disable command or its suspension fallback, the app queries enabled HOME
activities. If the query fails or returns no HOME package, it refuses the change. For a target HOME
package, it requires a *different* visible HOME package with an enabled, exported HOME activity,
an enabled application and a suspension status known to be off. Multiple HOME activities from one package are not alternatives.
An unknown or suspended alternative does not count. On older systems where suspension cannot be
read, the check fails closed for HOME packages. The rule is repeated immediately before the
fallback; it cannot prevent an external package-state change between a check and a shell command.

This is a recovery precaution, not proof that any launcher will work. An alternative can be broken,
misconfigured, hidden by firmware or lose dependencies. Other important services can also be lost.
The app keeps its existing critical-package, boot-risk, active-keyboard and active-accessibility
checks; no list protects all devices. Recovery is not guaranteed. Do not disable provider-managed
components without understanding the consequences.

The confirmation screen warns if it can identify a device owner among active administrators visible
to the current user. It cannot determine every managed profile or hidden administrator; absence of
a warning does **not** prove the device is unmanaged.

## Verification scope

The TV test workflow `.github/workflows/appdisabler-tv-focus.yml` runs on a disposable Android TV
emulator without granting Shizuku: it checks Setup/Apps navigation, the planning dialog, guide Back,
and opening an app's details without disabling it. Unit tests check the HOME decision for missing,
suspended/unknown and verified alternatives. The candidate workflow runs unit tests, lint and APK
assembly. These tests do not establish that Shizuku works on any particular TV, that all screens are
reachable with every remote, or that an essential app can be safely disabled or recovered. A real TV
and manufacturer-specific testing are still needed.
