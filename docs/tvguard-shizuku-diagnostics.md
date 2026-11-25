# TV Guard Shizuku diagnostics — 1.5.3 candidate

This is **work in progress, not a published version**. Public TV Guard remains 1.5.2.

## Use and consent

Tools & Settings → Shizuku diagnostics has its own saved on/off control, off by default. When on, the screen automatically connects if the separately installed Shizuku service is already running and TV Guard is authorized. If it is running but permission is missing, the on switch asks through Shizuku; the user can approve or deny. If Shizuku is not running, the screen offers Open Shizuku and instructions; installation alone cannot silently start its non-root service. Wireless pairing or root and a restart after reboot may be needed. Turning diagnostics off stops its queries and the extra pill cell; it does **not** stop or change the separate Shizuku service or revoke its global permissions.

The screen is read-only: a fixed `ps` command through Shizuku, normal package metadata, Android available/total RAM, and `/data` filesystem available/total. The pill appends a package count only while diagnostics is on and authorized; it is the number of visible installed package names matched to visible process names, sampled about every 30 seconds. The detail screen refreshes about every 15 seconds while open. No shell arguments come from app names, no processes are stopped, and snapshots are not logged or stored. Unavailable, refused, timed-out or unparseable results are reported as unavailable, not zero.

Process RSS includes shared pages: per-process RSS must not be added into an exact per-app total. Names can omit or misattribute processes because of shared processes, isolated services, Android visibility, users and vendor behavior. The count is not an exact number of actively running applications. `/data` is a filesystem report, not proof of the physical disk's size. Android and Shizuku versions and OEMs vary; access to app data, exact storage attribution and system-wide completeness are not guaranteed. Shizuku's older `newProcess` API used by the current 13.x dependency is deprecated and may disappear in a future Shizuku API. Failures are displayed as unavailable.

The status strip CPU value is now **non-idle processor load between samples**, not free CPU capacity. It shows unavailable until a valid second sample. The existing recently-used fallback no longer divides device RAM among apps and claims those amounts belong to each app; it shows `-- MB` when a process measurement is unavailable. Recently used is not the same as currently running.

This candidate does not fix the separately documented [TV Guard 1.5.2 security-audit partial-read limitations](tvguard-security-audit.md). The shield is not proof of safety.

## Evidence and outstanding checks

- Candidate build at `6844681` passed release unit tests and the signed disposable-TV synthetic upgrade and seven D-pad tests in run 45555088877, but this earlier build did not include the final auto-request behavior.
- An actual official Shizuku 13.6.0 server started as shell, showed and accepted its permission dialog, and returned a process snapshot including TV Guard, RAM and storage on a disposable TV emulator in run 45556782967 at `11124cf`. This is **not** physical-TV evidence and did not use the final pill assertion.
- Final source, on/off pill shape, D-pad behavior, stable signed update and exact installed candidate bytes require the final CI runs to pass before an APK is offered. No publication is authorized by this verification note.
