# System App Disabler candidate digests

Written by `.github/workflows/appdisabler-candidate.yml`; do not edit by hand.

| Field | Value |
| --- | --- |
| run | 43417680960 |
| commit | 333940235c81e9fe8b6589badd87b22a6fa9dbad |
| artifact | appdisabler-0.2.0-release-candidate |
| apk | appdisabler-release.apk |
| bytes | 11651613 |
| apk_sha256 | 4fa0510b06ae948f43fa5b34f4a4dafdfc77c06ce497e9c6cc8876ba1fce4b8f |
| archive_sha256 | (read from the Actions artifact API; not the APK) |

A publisher must pin `apk_sha256` above. `archive_sha256` is the ZIP GitHub
stores for the artifact and is a different value.
