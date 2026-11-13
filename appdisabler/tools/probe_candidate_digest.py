#!/usr/bin/env python3
"""Report the SHA-256 of an already-built candidate APK, and publish nothing.

The authoring sandbox cannot reach the Actions artifact blob host or the run-log host, so the APK
digest of an owner-tested candidate can only be learned inside a runner. This script downloads the
pinned artifact through the API, verifies it against the pinned archive digest, hashes the single APK
inside, writes the result to docs/ and pushes that commit. It creates no release, tag or asset.

Run it from a workflow on the branch (the pins below must name that branch's candidate).
"""
import hashlib
import json
import os
from pathlib import Path
import re
import stat
import subprocess
import zipfile

REPO = "universalrp2003/RP-Android-Apps"

# The owner-tested candidate. Do not change without a fresh owner test.
RUN_ID = 43410268620
ARTIFACT_ID = 14028948231
ARTIFACT = "appdisabler-0.2.0-release-candidate"
ARCHIVE_SHA = "cfb9ac11e8dd2b087944aef7de2cec3200dae6ac024385ac52a551654382135d"
ARCHIVE_BYTES = 11157191
OUT = Path("docs/appdisabler-0.2.0-candidate-digest.md")


def run(*args):
    return subprocess.check_output(args, text=True).strip()


def api(path):
    return json.loads(run("gh", "api", f"repos/{REPO}/{path}"))


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def build_tools():
    root = Path(os.environ["ANDROID_HOME"]) / "build-tools"
    return sorted(root.iterdir(), key=lambda d: tuple(int(n) for n in re.findall(r"\d+", d.name)))[-1]


def main():
    work = Path(os.environ["RUNNER_TEMP"]) / "apk-probe"
    work.mkdir(parents=True, exist_ok=True)

    info = api(f"actions/runs/{RUN_ID}")
    assert (info["status"], info["conclusion"]) == ("completed", "success"), info
    source = info["head_sha"]

    artifact = next(a for a in api(f"actions/runs/{RUN_ID}/artifacts")["artifacts"]
                    if a["id"] == ARTIFACT_ID)
    assert artifact["name"] == ARTIFACT and not artifact["expired"], artifact
    assert artifact["digest"] == "sha256:" + ARCHIVE_SHA, artifact
    assert artifact["size_in_bytes"] == ARCHIVE_BYTES, artifact

    archive = work / "candidate.zip"
    with archive.open("wb") as out:
        subprocess.run(["gh", "api", f"repos/{REPO}/actions/artifacts/{ARTIFACT_ID}/zip"],
                       stdout=out, check=True)
    assert sha(archive) == ARCHIVE_SHA, "archive digest moved"

    apk = work / "candidate.apk"
    with zipfile.ZipFile(archive) as z:
        apks = [n for n in z.namelist() if n.endswith(".apk")]
        assert len(apks) == 1, apks
        for item in z.infolist():
            p = Path(item.filename)
            assert not p.is_absolute() and ".." not in p.parts, item.filename
            assert not stat.S_ISLNK(item.external_attr >> 16), item.filename
        apk.write_bytes(z.read(apks[0]))

    apk_sha = sha(apk)
    apk_bytes = apk.stat().st_size
    assert 5_000_000 < apk_bytes < 60_000_000, apk_bytes

    tools = build_tools()
    badging = run(str(tools / "aapt2"), "dump", "badging", str(apk))
    cert = run(str(tools / "apksigner"), "verify", "--print-certs", str(apk))
    signers = sorted(set(re.findall(r"certificate SHA-256 digest: ([a-fA-F0-9]+)", cert)))
    version = re.search(r"versionName='([^']+)'", badging)
    code = re.search(r"versionCode='([^']+)'", badging)
    assert version and code, badging[:400]

    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(f"""# System App Disabler 0.2.0 — owner-tested candidate digest

Written by `appdisabler/tools/probe_candidate_digest.py` inside Actions; do not edit by hand.
Nothing was published to produce this file.

| Field | Value |
| --- | --- |
| run | {RUN_ID} |
| source commit | `{source}` |
| artifact | `{ARTIFACT}` (id {ARTIFACT_ID}) |
| archive sha256 | `{ARCHIVE_SHA}` |
| archive bytes | {ARCHIVE_BYTES} |
| apk | {apks[0].split('/')[-1]} |
| apk sha256 | `{apk_sha}` |
| apk bytes | {apk_bytes} |
| package | `com.universalrp.appdisabler` |
| versionName | `{version.group(1)}` |
| versionCode | `{code.group(1)}` |
| debuggable | {'yes' if 'application-debuggable' in badging else 'no'} |
| signer certificate sha256 | {', '.join('`' + s + '`' for s in signers)} |

A publisher must pin `apk sha256` and `apk bytes` above. The *archive* digest is the ZIP GitHub
stores for the artifact, which is a different value from the APK's own digest.
""")
    print(OUT.read_text())


if __name__ == "__main__":
    main()
