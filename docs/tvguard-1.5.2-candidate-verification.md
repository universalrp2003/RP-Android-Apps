# Android TV Guard 1.5.2 candidate verification

**Unpublished candidate; not a physical-device acceptance or a security guarantee.**

- Source commit: `c515af8418167cef5787c516ac04f35b74507f73`; package `com.universalrp.tvguard`, version 1.5.2 / code 9.
- [Signed candidate workflow 45430906478](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/45430906478): success. Artifact `tvguard-1.5.2-stable-signed-tested-candidate`, ID `14939417657` (retained for 30 days by CI). The artifact contains the APK, SHA-256 sidecar and verification receipt.
- APK SHA-256: `4408af2b1fc32e95fd87da710f5d1093cf0350203f8917b0512d3af96ff61672`.
- The signed workflow checked the package, version, non-debuggable build and expected signer; 85 release unit tests; same-key update of a synthetic older install preserving launcher settings and a private file; rejection of a different signer; and six D-pad tests against the exact installed candidate bytes on an Android TV emulator.
- [Debug TV navigation workflow 45430906103](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/45430906103): success, six D-pad tests. Android TV hardware, manufacturer settings screens and the security posture of a real device were not verified.
- The current branch has no changes to `tvguard/app/` or `tvguard/tools/` since that source commit. Candidate artifacts are not public release assets; production links and published pins still point to 1.5.1.
