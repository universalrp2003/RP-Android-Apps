# TV Guard Shizuku diagnostics — 1.5.3 candidate

This is **work in progress, not a published version**. Public TV Guard remains 1.5.2.

## Use and consent

Tools & Settings → Shizuku diagnostics has its own saved on/off control, off by default. When on, the screen automatically connects if the separately installed Shizuku service is already running and TV Guard is authorized. If it is running but permission is missing, the on switch asks through Shizuku; the user can approve or deny. If Shizuku is not running, the screen offers Open Shizuku and instructions; installation alone cannot silently start its non-root service. Wireless pairing or root and a restart after reboot may be needed. Turning diagnostics off stops its queries and the extra pill cell; it does **not** stop or change the separate Shizuku service or revoke its global permissions.

The screen is read-only: a fixed `ps` command through Shizuku, normal package metadata, Android available/total RAM, and `/data` filesystem available/total. The pill appends a package count only while diagnostics is on and authorized; it is the number of visible installed package names matched to visible process names, sampled about every 30 seconds. The detail screen refreshes about every 15 seconds while open. No shell arguments come from app names, no processes are stopped, and snapshots are not logged or stored. Unavailable, refused, timed-out or unparseable results are reported as unavailable, not zero.

Process RSS includes shared pages: per-process RSS must not be added into an exact per-app total. Names can omit or misattribute processes because of shared processes, isolated services, Android visibility, users and vendor behavior. The count is not an exact number of actively running applications. `/data` is a filesystem report, not proof of the physical disk's size. Android and Shizuku versions and OEMs vary; access to app data, exact storage attribution and system-wide completeness are not guaranteed. Shizuku's older `newProcess` API used by the current 13.x dependency is deprecated and may disappear in a future Shizuku API. Failures are displayed as unavailable.

The status strip CPU value is now **non-idle processor load between samples**, not free CPU capacity. It shows unavailable until a valid second sample. The existing recently-used fallback no longer divides device RAM among apps and claims those amounts belong to each app; it shows `-- MB` when a process measurement is unavailable. Recently used is not the same as currently running.

This candidate does not fix the separately documented [TV Guard 1.5.2 security-audit partial-read limitations](tvguard-security-audit.md). The shield is not proof of safety.

## Verification and release status

- Candidate source `96c7885fcfaf220e28d40418e6bd973ba46cc1ca`. [Signed candidate run 45558877424](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/45558877424) passed 87 release unit tests and seven remote tests on the exact installed APK, rejected a different signer and verified both synthetic and exact public 1.5.2 same-key updates with selected synthetic state and a private file preserved. The public 1.5.2 update retained the test installation's UID. APK SHA-256 `1d7452ce651551c1e1fd5c4a4dcc73764a6f317ac58a1d69cb71bd9e719ac3c1`; artifact `14997038159` (artifact ZIP, not APK, size 11,327,210 bytes).
- [Real Shizuku run 45558466007](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/45558466007) passed with official Shizuku 13.6.0 started as shell on a disposable TV emulator. It exercised the actual permission dialog, matched TV Guard to a visible process, checked RAM and storage, and verified that turning diagnostics on/off adds/removes the process pill cell. This used a debug build of the same source, not the signed APK.
- [TV focus run 45558880909](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/45558880909) passed seven D-pad tests. Local source checks: 573 Python tests and resource validation passed.
- **Not physically tested and not published.** User approval is required before a public release. The candidate can be retrieved from the signed workflow artifact by an authenticated repository user. Never uninstall or clear data to force an update; update only if Android accepts the existing signer. The earlier public 1.5.2 security-audit limitation remains.
