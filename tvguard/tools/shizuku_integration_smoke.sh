#!/usr/bin/env bash
set -Eeuo pipefail
log="$RUNNER_TEMP/shizuku-test.log"
exec > >(tee "$log") 2>&1
trap 'code=$?; if (( code != 0 )); then echo "::error title=Real Shizuku integration failed::Line $LINENO exited $code. See the saved shizuku-test.log artifact."; fi' EXIT
set -x
case "$(adb get-serialno)" in emulator-*) ;; *) exit 1 ;; esac
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
adb install "$RUNNER_TEMP/shizuku/shizuku-v13.6.0.r1086.2650830c-release.apk"
adb install tvguard/app/build/outputs/apk/debug/tvguard-debug.apk
adb install tvguard/app/build/outputs/apk/androidTest/debug/tvguard-debug-androidTest.apk
apk=$(adb shell pm path moe.shizuku.privileged.api | tr -d '\r' | sed -n 's/^package://p' | head -1)
test -n "$apk"
starter=$(adb shell "find $(dirname "$apk")/lib -name libshizuku.so" | tr -d '\r' | head -1)
test -n "$starter"
adb shell "$starter"
adb shell am instrument -w -e shizukuIntegration true -e class com.universalrp.tvguard.ShizukuLiveDiagnosticsTest com.universalrp.tvguard.test/androidx.test.runner.AndroidJUnitRunner | tee "$RUNNER_TEMP/shizuku-instrumentation.log"
grep -Fq 'OK (1 test)' "$RUNNER_TEMP/shizuku-instrumentation.log"
