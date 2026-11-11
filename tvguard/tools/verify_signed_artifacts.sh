#!/usr/bin/env bash
# Fail closed before offering any APK: verify package, version, signer and build type.
set -Eeuo pipefail
trap 'echo "::error title=TV artifact verification::A check failed at line $LINENO; inspect the candidate diagnostics."' ERR
: "${RUNNER_TEMP:?RUNNER_TEMP must name the CI scratch directory}"
work="$RUNNER_TEMP/tvguard-signed-candidate"
old="$RUNNER_TEMP/tvguard-old-debug/tvguard-debug.apk"
tools="$(find "$ANDROID_HOME/build-tools" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -1)"
expected="$(PYTHONPATH=tools python3 -c 'from prepare_release_signing import EXPECTED_SIGNER; print(EXPECTED_SIGNER)')"
python3 tools/verify_release_signing_policy.py --fresh-install

check_signer() {
  local path="$1" label="$2" want="$3" digest
  "$tools/apksigner" verify --print-certs "$path" >"$work/$label-signer.txt"
  mapfile -t digests < <(sed -n 's/^.*certificate SHA-256 digest: //p' "$work/$label-signer.txt")
  test "${#digests[@]}" -gt 0
  for digest in "${digests[@]}"; do test "$digest" = "$want"; done
}

candidate="$work/Android-TV-Guard-1.2-signed-candidate.apk"
fixture="$work/synthetic-older-same-key.apk"
test_apk="$work/tvguard-release-androidTest.apk"
for path in "$old" "$candidate" "$fixture" "$test_apk"; do test -s "$path"; done
check_signer "$candidate" candidate "$expected"
check_signer "$fixture" fixture "$expected"
check_signer "$test_apk" androidTest "$expected"

"$tools/apksigner" verify --print-certs "$old" >"$work/old-debug-signer.txt"
old_signer="$(sed -n 's/^.*certificate SHA-256 digest: //p' "$work/old-debug-signer.txt" | sed -n '1p')"
test -n "$old_signer"
test "$old_signer" != "$expected"

for label in candidate fixture androidTest old-debug; do
  case "$label" in
    candidate) path="$candidate" ;;
    fixture) path="$fixture" ;;
    androidTest) path="$test_apk" ;;
    old-debug) path="$old" ;;
  esac
  "$tools/aapt" dump badging "$path" >"$work/$label-badging.txt"
done
grep -Fq "package: name='com.universalrp.tvguard' versionCode='4' versionName='1.2'" "$work/candidate-badging.txt"
grep -Fq "package: name='com.universalrp.tvguard' versionCode='1' versionName='1.0'" "$work/fixture-badging.txt"
grep -Fq "package: name='com.universalrp.tvguard' versionCode='1' versionName='1.0'" "$work/old-debug-badging.txt"
grep -Fq "package: name='com.universalrp.tvguard.test'" "$work/androidTest-badging.txt"
# aapt badging does not list instrumentation on every build-tools version.
"$tools/aapt" dump xmltree "$test_apk" AndroidManifest.xml >"$work/androidTest-manifest.txt"
grep -Eq 'android:targetPackage[^=]*="com\.universalrp\.tvguard"' "$work/androidTest-manifest.txt"
grep -Eq 'android:name[^=]*="androidx\.test\.runner\.AndroidJUnitRunner"' "$work/androidTest-manifest.txt"
grep -Fq 'application-debuggable' "$work/old-debug-badging.txt"
if grep -Fq 'application-debuggable' "$work/candidate-badging.txt"; then
  echo 'Refusing to offer a debuggable release candidate.' >&2
  exit 1
fi
(cd "$work" && sha256sum Android-TV-Guard-1.2-signed-candidate.apk >Android-TV-Guard-1.2-signed-candidate.sha256)
echo "PASS: candidate and tests use the pinned signer; package and versions are correct; candidate is non-debuggable."
