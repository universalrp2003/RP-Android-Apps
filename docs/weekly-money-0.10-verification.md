# TEST 0.10 candidate verification — publication held

Successful final run: https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43838513731
Source: `44569805e87bdf829df9ef5305e24edbf6a39aad`.

Compile, unit tests (including derived settlement lock/correction test), lint and offline identity passed. Native integration regression passed: 1 test, 0 failures/errors/skips, 105.998 seconds. Native coverage retains pinch, screenshot switch, calendar/history, Downloads decryption and volume/archive navigation; not every new UI feature has independent automated acceptance coverage.

Artifact ID `14212143089`, `WeeklyMoney-0.10-calendar-preview-and-isolated-demo`, 19,886,881 bytes.
ZIP SHA-256: `cd8366b62e42177223203b09217f2db8022125a2148afb366185aae617b2995f`
Notebook APK SHA-256: `5f05a9bd070d451b0db51560f3e1304903db2e64cc2c402843f64c6924730494`
DEMO APK SHA-256: `be59da34508cf1a66f2e005b609bb7e4c28c4c00002d2d2e6b4e1db32a3b6613`
Signer SHA-256: `7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d`

Download: https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43838513731/artifacts/14212143089
Install `handover/notebook/WeeklyMoney-TEST0.10.apk`, not DEMO. Export a backup, update in place, never uninstall/clear data. Same package/signer, code10/schema5; do not downgrade after saving.

Prior final-candidate runs lost emulator device during pinch section and were not treated as passes; two attempts of 43828934305 were canceled after stalls. Diagnostics now stream logcat and bound device/Gradle/boot waits. Build-daemon/emulator memory is bounded. Ledger projections are cached across zoom frames, and optional pinch reflows once on release rather than for every event. Final source above passed the full retained native test.

Tamil-date setting defaults OFF; bounded 2026 table only, unsupported dates omitted without affecting Gregorian/financial operation. Archive remains in same encrypted snapshot. Collector/recovery and physical biometric/old-Android SAF gaps remain as documented. No release, Store publication, main merge, or website deployment. Owner testing approval is still required.
