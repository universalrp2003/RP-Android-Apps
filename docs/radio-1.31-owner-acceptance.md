# Radio 1.31 owner acceptance / exact-artifact publication

Publication authorized: yes

On 31 October 2026 the owner supplied a phone screenshot of the new direct-drag tab UI and said: “Its super now you can publish now”. Earlier the owner reported playing a few stations successfully. This authorizes Radio only; it does not authorize SMS Guard/Oor publication or establish a complete listening audit, absence of provider promotions, an independently matched installed APK hash, or a separately traced restart/persistence test.

## Immutable accepted candidate

- Source: 691d176cc00adb59702e53bbae3e99bfc2663ec5
- Run: 41205378161; successful job: 124549323422
- Artifact: 13044417700, Ramesh-Radio-1.31-signed-candidate
- Archive SHA-256: 347be4eadcc9e24635b470bb8660ba8f0c130edca0c0fc03c93175df79a5098d
- File: Ramesh-Radio-v1.31.apk
- APK bytes: 15179972
- APK SHA-256: fceac7c50758990eaa33228a3ec375f38fb9a307bca8452566eb84d037a5dc22
- APK Git blob: 843d173c181af7efd69dad8132a934be7a06e957
- Package: com.universalrp.tamilnadufm; version1.31/code31; signer7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d
- Both JVM variants, release lint, signed release identity and all12 workflows at that source passed, including full Build APK41205378185. Independent actual APK bytes/hash/binary manifest/unchanged532-entry packaged seed checks passed. No new platform permission versus1.30.

Publish these exact bytes without a rebuild. New automatic CI outputs are validation only, never substitutes. Preserve1.29 and both earlier test artifacts. Recheck established signer, nondebuggable manifest, actual permission delta against public1.29 and re-download the promoted release before advertising it.

## Release scope isolation

Held SMS1.1.14/Oor1.1.13 work remains reachable at immutable commit47717f9b0072c8e50a6b4a9e32d537603840b950 (and source1fb1ba413e4d00cdf78f48e5e1ab6b4ae55a8714 for their selected APKs). Their app files and related source contracts are restored to main/public versions for this Radio-only merge. Resume them selectively when the owner reports natural-use results; do not reset the branch or replace their selected APKs. Radio source is identical to the accepted source; shared SisterApps changes only restore the two deferred apps' directory metadata and subsequently advertise the published Radio version. No shared runtime logic changes are permitted by the promotion guard.

Catalogue remains532 entries:504 originals plus28 deduplicated directory additions.13 shared-provider streams are held;42 endpoints remain unresolved/unavailable. Audio-metadata reachability is not complete listening/content/identity/rights verification. Tamil80’s normal programme is not restored or bypassed.
