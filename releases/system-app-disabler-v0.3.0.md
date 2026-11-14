# System App Disabler 0.3.0

Manage selected Android packages using Shizuku, without uninstalling them. Android 8.0 or newer and a separately installed, running Shizuku are required; the device may refuse a request.

## What's new

- Apps absent from the bundled UAD list are **Unlisted**, not automatically rated safe. Research is optional: Google and Google AI links send the app name and package to Google, and AI answers can be wrong.
- Disabled and suspended states can be shown together. Suspension is not the same as disabling and an app may still run. The app reports unknown when it cannot determine suspension state; local records may become stale.
- Suspension command results are checked rather than treating exit status alone as success. Recovery attempts and errors are reported; success is not guaranteed.
- More compact Unlisted rows and a shorter combined-state label.

## Important safety limits

UAD classifications, including **Safe**, are not guarantees. For example, Android Accessibility Suite provides TalkBack and other accessibility features: disabling it can remove features people rely on. Do not disable essential packages, an active keyboard or an accessibility service you need. The app retains its existing critical-package and active-service protections, but no list can cover every device or dependency. Read the details and decide for yourself.

Enable attempts to restore a package but can fail due to device restrictions or state controlled by another actor. Immediate-reboot persistence is not proven. Shizuku may need restarting after a reboot. Manufacturer behavior differs; no broad OEM compatibility, Play Protect approval, or guarantee is claimed.

## Verification and scope

This release is the **exact** owner-tested candidate from [run 43557688697](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43557688697), not a rebuild. The owner verified the installed APK hash and UI on a Redmi 13 5G, and performed one delayed-restart disable and recovery cycle on an inert user-installed fixture. Emulator fixture cycles passed on stock Android 9, 10, 15 and 16 (some infrastructure retries); Android 9 suspension could not be tested under stock shell permissions. These checks do not prove immediate reboot or system-package behavior. Experimental synchronous-write research remains debug-only and is **not** a shipped persistence fix.

- Package: `com.universalrp.appdisabler`; version code 3
- APK SHA-256: `d09ab49e57e7ac3daed7e4edac4a9d8b7fc9292ccbce6ff2a4b48065977f30a0`
- APK size: 11,668,005 bytes

Update in place if Android accepts the signing identity. If an install or signature check fails, stop; do not uninstall the old app or clear its data merely to force installation. Do not bypass device security warnings.
