# Testing a System App Disabler 0.3.0 candidate

The version number alone does not identify an APK. Multiple 0.3.0 builds, including debug and
release variants, share version code 3. Only publish the exact release APK verified on the phone;
matching version numbers or a screenshot of a file is not a hash match.

## Install and identify the candidate

1. Download the **release** artifact `appdisabler-0.3.0-release-candidate` from the exact Actions
   run supplied with the candidate. Extract `appdisabler-release.apk`. Do not use
   `appdisabler-0.3.0-candidate` / `appdisabler-debug.apk` for publication.
2. Install the release APK over the current app. If installation or signature checks fail, stop;
   do not uninstall the existing app or clear its data just to force an installation.
3. Open **Setup → Check Shizuku shell**. In candidates that include this diagnostic, the first
   lines show **Installed base APK SHA-256**, **Debuggable**, and **Installed splits**. Long-press
   to select the text or send clear screenshots. Compare the complete hash with the APK digest for
   that exact run; require `Debuggable: false` and `Installed splits: 0` for this single-APK release.
   A missing or unreadable hash is not verification. No shell command is required from the user.

## What to observe

- Check the app opens and Shizuku shows ready. In the Apps list, check the running tile opens a
  list and that a suspended row says it **may** still run. A suspended app can also appear in the
  running list; the two describe different things.
- The existing **turned off** count includes recorded suspensions. It is not proof all of those
  processes stopped, and the locally saved suspension status may become stale.
- Test Enable/Disable only on a package you have already chosen to manage, not an unfamiliar or
  critical system package. Record the exact toast or error and any effect on the phone. Suspension
  is not the same as a full disable. Restore using Enable if desired; restoration can fail.
- Restart testing is optional and carries risk. Only test after the owner agrees and has a recovery
  plan; no single successful boot guarantees other packages, firmware or devices are safe. Confirm
  which packages actually stayed disabled or suspended rather than relying on the count alone.
- Do not uninstall system apps, clear their data, or force-stop HyperComm to run this test.

Send the installed base hash/debug/splits and any observed results. Previous screenshots remain
valid observations, but do not establish the installed APK's hash or prove every feature works.
