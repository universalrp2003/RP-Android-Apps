> Historical record of an earlier **superseded** candidate, not the published APK. The corrected candidate and publication are documented in [system-app-disabler-v0.5.0-publication.json](system-app-disabler-v0.5.0-publication.json).

# System App Disabler 0.5.0 candidate verification

**Unpublished candidate; not a physical-TV test or a guarantee that disabling is safe.**

- Source commit: `478b3d50996588a606b8b804a083c7f8da9c39d7`; package `com.universalrp.appdisabler`, version 0.5.0 / code 6.
- [Signed TV candidate workflow 45530168923](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/45530168923): success. Artifact `appdisabler-0.5.0-stable-signed-tested-candidate`, ID `14984961011` (retained for 30 days by CI). The artifact contains the APK, SHA-256 sidecar and verification receipt.
- APK SHA-256: `f9e72e4342dad9cdf806731c9355d11fb9640125b47ec2bcb95dea79623ed0c4`.
- CI verified the expected signer and non-debuggable release build, ran 13 release unit tests and upgraded the public 0.4.1 APK in place on a disposable Android TV emulator. Its UID, synthetic suspension preferences, synthetic inventory and a private marker survived. The installed APK hash matched the offered candidate.
- Four D-pad tests exercised setup/apps, planning, guide Back, search and filters, and opening app details. The same four passed on the exact signed candidate, and separately in [debug TV workflow 45530168796](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/45530168796).
- No package was disabled during these emulator tests; they do not verify Shizuku on a real TV, provider-managed devices, every screen or remote, or recovery after disabling a real app. No physical TV was tested. See [TV safety notes](appdisabler-tv-safety.md).
- Candidate artifacts are not public release assets; production links and published pins still point to 0.4.1.
