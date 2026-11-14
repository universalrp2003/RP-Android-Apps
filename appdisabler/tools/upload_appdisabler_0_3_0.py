#!/usr/bin/env python3
"""Upload the owner-tested APK into the pre-created 0.3.0 draft release, then publish it.

Why this is split in two: the authoring sandbox can reach api.github.com (so the draft release is
created from there) but not uploads.github.com (so the assets cannot be attached from there). A CI
runner can reach both. This script therefore does only the part that needs a runner — attach the
pinned bytes and flip the draft public — and it re-verifies every pin before doing so.

It refuses to run if the draft does not already exist, if the tag is already public, if the assets
are already attached, or if the downloaded bytes do not match their pinned digests.
"""
import hashlib
import json
import os
from pathlib import Path
import re
import stat
import subprocess
import sys
import time
import zipfile

REPO = "universalrp2003/RP-Android-Apps"

# Pins: identical to publish_appdisabler_0_2_0.py, which is the authoritative record.
RUN_ID = 43557688697
ARTIFACT_ID = 14098824005
ARCHIVE_SHA = "880465bea1d0ac9cfcae903f73445a1ec91fd549ef7db8d4b524054e6e662d22"
ARCHIVE_BYTES = 11166671
APK_SHA = "d09ab49e57e7ac3daed7e4edac4a9d8b7fc9292ccbce6ff2a4b48065977f30a0"
APK_BYTES = 11668005

TAG = "system-app-disabler-v0.3.0"
NAME = "System-App-Disabler-v0.3.0.apk"
SIDECAR = "System-App-Disabler-v0.3.0.sha256"
PACKAGE = "com.universalrp.appdisabler"
BADGING = f"package: name='{PACKAGE}' versionCode='3' versionName='0.3.0'"
SIGNER = "7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d"


def run(*args):
    return subprocess.check_output(args, text=True).strip()


def api(path):
    return json.loads(run("gh", "api", f"repos/{REPO}/{path}"))


def release_by_tag(tag):
    """GET /releases/tags/<tag> answers 404 for drafts, even with a write token — measured.

    A draft only appears in the list endpoint, so look it up there.
    """
    matches = []
    for page in range(1, 21):
        batch = api(f"releases?per_page=100&page={page}")
        matches.extend(r for r in batch if r["tag_name"] == tag)
        if len(batch) < 100:
            break
    else:
        raise RuntimeError("Release pagination limit exceeded")
    assert len(matches) <= 1, f"{len(matches)} releases claim {tag}"
    return matches[0] if matches else None


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def build_tools():
    root = Path(os.environ["ANDROID_HOME"]) / "build-tools"
    return sorted(root.iterdir(), key=lambda d: tuple(int(n) for n in re.findall(r"\d+", d.name)))[-1]


def main():
    assert os.environ.get("GITHUB_ACTIONS") == "true", "asset upload only runs in CI"

    # 1. The draft must exist, be a draft, and have no assets yet.
    draft = release_by_tag(TAG)
    assert draft is not None, f"no draft release for {TAG}; create it first"
    assert draft["draft"] and not draft["prerelease"], "the release is not a draft"
    assert draft["tag_name"] == TAG, draft["tag_name"]
    assert draft["assets"] == [], f"assets already attached: {[a['name'] for a in draft['assets']]}"
    assert draft["target_commitish"] == "3c512b9f5ed1f893ad86fe5d6b90fe87d6d4fb9c", draft["target_commitish"]
    release_id = draft["id"]

    work = Path(os.environ["RUNNER_TEMP"]) / "appdisabler-upload"
    work.mkdir(parents=True, exist_ok=True)

    # 2. The tested bytes, from the pinned successful candidate run and artifact only.
    info = api(f"actions/runs/{RUN_ID}")
    assert (info["status"], info["conclusion"], info["head_sha"], info["path"]) == (
        "completed", "success", "3c512b9f5ed1f893ad86fe5d6b90fe87d6d4fb9c",
        ".github/workflows/appdisabler-candidate.yml"), info
    artifact = next(a for a in api(f"actions/runs/{RUN_ID}/artifacts")["artifacts"]
                    if a["id"] == ARTIFACT_ID)
    assert artifact["name"] == "appdisabler-0.3.0-release-candidate" and not artifact["expired"]
    assert artifact["digest"] == "sha256:" + ARCHIVE_SHA, artifact["digest"]
    assert artifact["size_in_bytes"] == ARCHIVE_BYTES, artifact["size_in_bytes"]

    archive = work / "candidate.zip"
    with archive.open("wb") as out:
        subprocess.run(["gh", "api", f"repos/{REPO}/actions/artifacts/{ARTIFACT_ID}/zip"],
                       stdout=out, check=True)
    assert sha(archive) == ARCHIVE_SHA, "archive digest moved"

    apk = work / NAME
    with zipfile.ZipFile(archive) as z:
        apks = [n for n in z.namelist() if n.endswith(".apk")]
        assert len(apks) == 1, apks
        for item in z.infolist():
            p = Path(item.filename)
            assert not p.is_absolute() and ".." not in p.parts, item.filename
            assert not stat.S_ISLNK(item.external_attr >> 16), item.filename
        apk.write_bytes(z.read(apks[0]))

    apk_sha, apk_bytes = sha(apk), apk.stat().st_size
    assert apk_sha == APK_SHA, f"APK digest {apk_sha} != pinned {APK_SHA}"
    assert apk_bytes == APK_BYTES, f"APK bytes {apk_bytes} != pinned {APK_BYTES}"

    tools = build_tools()
    badging = run(str(tools / "aapt2"), "dump", "badging", str(apk))
    assert BADGING in badging, badging[:400]
    assert "application-debuggable" not in badging, "the release APK must not be debuggable"
    assert re.search(r"(?:sdkVersion|minSdkVersion):\s*'26'", badging), badging[:400]
    cert = run(str(tools / "apksigner"), "verify", "--print-certs", str(apk))
    assert set(re.findall(r"certificate SHA-256 digest: ([a-fA-F0-9]+)", cert)) == {SIGNER}, cert

    checksum = work / SIDECAR
    checksum.write_text(f"{apk_sha}  {NAME}\n")

    # 3. Attach both assets to the existing draft.
    token = os.environ["GH_TOKEN"]

    def rest(method, url, data_file=None, content_type="application/octet-stream", raw_json=None):
        cmd = ["curl", "-sS", "-X", method, "-H", f"Authorization: Bearer {token}",
               "-H", f"Content-Type: {content_type}", "-w", "\n%{http_code}"]
        if raw_json is not None:
            cmd += ["--data-binary", raw_json]
        elif data_file is not None:
            cmd += ["--data-binary", f"@{data_file}"]
        cmd.append(url)
        proc = subprocess.run(cmd, capture_output=True, text=True)
        body, _, code = proc.stdout.rpartition("\n")
        return code.strip(), body

    upload_url = draft["upload_url"].split("{")[0]
    for path, asset_name, mime in ((apk, NAME, "application/vnd.android.package-archive"),
                                   (checksum, SIDECAR, "text/plain")):
        code, body = rest("POST", f"{upload_url}?name={asset_name}", path, mime)
        if code != "201":
            raise SystemExit(f"asset upload for {asset_name} returned HTTP {code}: {body[:400]}")
        print(f"attached {asset_name}")

    # 4. Verify the digests GitHub stored, then publish.
    for attempt in range(12):
        current = api(f"releases/{release_id}")
        by_name = {a["name"]: a for a in current["assets"]}
        if NAME in by_name and SIDECAR in by_name:
            if (by_name[NAME].get("digest") == "sha256:" + apk_sha
                    and by_name[SIDECAR].get("digest") == "sha256:" + sha(checksum)):
                break
        time.sleep(2)
    else:
        raise SystemExit("attached assets did not verify; the release stays a draft")

    assert by_name[NAME]["size"] == apk_bytes

    code, body = rest("PATCH", f"https://api.github.com/repos/{REPO}/releases/{release_id}",
                      raw_json=json.dumps({"draft": False, "prerelease": False}),
                      content_type="application/json")
    if code != "200":
        raise SystemExit(f"publishing the draft returned HTTP {code}: {body[:400]}")

    public = release_by_tag(TAG)
    assert not public["draft"] and not public["prerelease"]
    asset = next(a for a in public["assets"] if a["name"] == NAME)
    assert asset["size"] == apk_bytes and asset["digest"] == "sha256:" + apk_sha
    assert asset["browser_download_url"] == f"https://github.com/{REPO}/releases/download/{TAG}/{NAME}"

    receipt = Path(os.environ["RUNNER_TEMP"]) / "appdisabler-0.3.0-publication-receipt.json"
    receipt.write_text(json.dumps({
        "tag": TAG, "name": NAME, "run": RUN_ID, "artifact_id": ARTIFACT_ID,
        "archive_sha256": ARCHIVE_SHA, "apk_sha256": apk_sha, "bytes": apk_bytes,
        "signer": SIGNER, "package": PACKAGE, "version": "0.3.0", "version_code": 3,
        "rebuilt": False, "owner_device_tested": True, "owner_test_scope": "UI and one delayed-restart user fixture disable/restore cycle",
        "release_id": release_id, "published_at": public["published_at"],
    }, indent=2))
    print(f"published {TAG}: {NAME}, {apk_bytes} bytes, sha256 {apk_sha}, release {release_id}")


if __name__ == "__main__":
    main()
