#!/usr/bin/env python3
"""Publish the only System App Disabler bytes anyone has actually run: the owner-tested 0.1.0 candidate.

Nothing here compiles an app. The APK is taken from the pinned successful candidate run and
artifact, its SHA-256 must equal the pin, and the publisher refuses to touch an existing tag.

The app was tested by the owner on a Xiaomi HyperOS phone (screenshots 13 November 2026): Shizuku
ready, suspend fallback working on system packages, English + Tamil UI. This script pins exactly
those bytes. Running the publisher twice is refused on purpose.
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
BRANCH = "arena/745a8cb3-rp-android-apps"

# Candidate run whose artifact the owner installed and reported working.
RUN_ID = 43370535232
WORKFLOW = ".github/workflows/appdisabler-candidate.yml"
ARTIFACT = "appdisabler-0.1.0-candidate"
ARTIFACT_ID = 14012107398
ARCHIVE_SHA = "e461d09843de64ac2d2daadb8003491605ab29188282589aaabf8c75ce8b768f"
ARCHIVE_BYTES = 17265390

TAG = "system-app-disabler-v0.1.0"
# The workflow uploads the debug variant file name; the public asset drops the "-debug".
INNER = "app-debug.apk"
NAME = "System-App-Disabler-v0.1.0.apk"
SIDECAR = "System-App-Disabler-v0.1.0.sha256"
PACKAGE = "com.universalrp.appdisabler"
BADGING = f"package: name='{PACKAGE}' versionCode='1' versionName='0.1.0'"
# Owner-backed replacement release key used by every new RP Ideas APK.
SIGNER = "7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d"


def run(*args):
    return subprocess.check_output(args, text=True).strip()


def api(path):
    return json.loads(run("gh", "api", f"repos/{REPO}/{path}"))


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def pages(path):
    result = []
    for page in range(1, 21):
        batch = api(f"{path}?per_page=100&page={page}")
        result.extend(batch)
        if len(batch) < 100:
            return result
    raise ValueError("Pagination limit exceeded; refuse to guess")


def snapshot(release):
    return release["id"], sorted(
        (a["id"], a["name"], a["size"], a.get("digest")) for a in release["assets"]
    )


def build_tools():
    root = Path(os.environ["ANDROID_HOME"]) / "build-tools"
    return sorted(root.iterdir(), key=lambda d: tuple(int(n) for n in re.findall(r"\d+", d.name)))[-1]


def main():
    assert os.environ.get("GITHUB_ACTIONS") == "true", "Publication only runs in CI with a token"
    assert os.environ.get("GITHUB_REF") == f"refs/heads/{BRANCH}", os.environ.get("GITHUB_REF")
    assert os.environ.get("GH_REPO") == REPO

    work = Path(os.environ["RUNNER_TEMP"]) / "appdisabler-publish"
    work.mkdir(parents=True, exist_ok=True)

    # 1. The pinned candidate run must be this branch's successful candidate build.
    info = api(f"actions/runs/{RUN_ID}")
    assert (info["status"], info["conclusion"], info["path"]) == (
        "completed", "success", WORKFLOW), info
    source = info["head_sha"]
    assert re.fullmatch("[a-f0-9]{40}", source)
    subprocess.run(["git", "merge-base", "--is-ancestor", source, "HEAD"], check=True)
    # The app must not have changed since the tested commit. The publisher itself is excluded:
    # it is not part of the build and has to be committed to trigger this workflow.
    subprocess.run(
        ["git", "diff", "--exit-code", source, "HEAD", "--", "appdisabler/app/",
         ":(exclude)appdisabler/app/build"], check=True)

    artifacts = [a for a in api(f"actions/runs/{RUN_ID}/artifacts")["artifacts"] if a["id"] == ARTIFACT_ID]
    assert len(artifacts) == 1, "Expected exactly one pinned artifact"
    artifact = artifacts[0]
    assert artifact["name"] == ARTIFACT and not artifact["expired"]
    assert artifact["workflow_run"]["head_sha"] == source, artifact["workflow_run"]
    assert artifact["digest"] == "sha256:" + ARCHIVE_SHA and artifact["size_in_bytes"] == ARCHIVE_BYTES

    # 2. Download the archive through the authenticated API (blob hosts are blocked in some
    #    sandboxes; the API path is the one that works here) and pin its bytes.
    archive = work / "candidate.zip"
    with archive.open("wb") as out:
        subprocess.run(["gh", "api", f"repos/{REPO}/actions/artifacts/{ARTIFACT_ID}/zip"],
                       stdout=out, check=True)
    assert sha(archive) == ARCHIVE_SHA and archive.stat().st_size == ARCHIVE_BYTES

    candidate = work / NAME
    with zipfile.ZipFile(archive) as z:
        assert len(z.namelist()) == len(set(z.namelist())), "Duplicate ZIP entry"
        for item in z.infolist():
            path = Path(item.filename)
            assert not path.is_absolute() and ".." not in path.parts
            assert not stat.S_ISLNK(item.external_attr >> 16)
        apks = [n for n in z.namelist() if n.endswith(".apk")]
        assert len(apks) == 1, apks
        candidate.write_bytes(z.read(apks[0]))

    apk_sha = sha(candidate)
    apk_bytes = candidate.stat().st_size
    assert 5_000_000 < apk_bytes < 60_000_000

    # 3. The bytes really are our package, our version, non-debuggable and release-key signed.
    tools = build_tools()
    badging = run(str(tools / "aapt2"), "dump", "badging", str(candidate))
    assert BADGING in badging, badging[:400]
    assert "application-debuggable" not in badging
    assert re.search(r"(?:sdkVersion|minSdkVersion):\s*'26'", badging), badging[:400]
    cert = run(str(tools / "apksigner"), "verify", "--print-certs", str(candidate))
    assert set(re.findall(r"certificate SHA-256 digest: ([a-fA-F0-9]+)", cert)) == {SIGNER}, cert

    # 4. The tag must not exist yet, and no public asset may be touched or replaced.
    releases = pages("releases")
    assert not [r for r in releases if r["tag_name"] == TAG], "Tag already exists; publish a new version"
    assert not [t for t in pages("tags") if t["name"] == TAG]
    prior = {
        r["tag_name"]: snapshot(r)
        for r in releases if not r["draft"]
    }

    checksum = work / SIDECAR
    checksum.write_text(f"{apk_sha}  {NAME}\n")

    # 5. Create the release, verify every asset digest, then make it public.
    subprocess.run(
        ["gh", "release", "create", TAG, "--target", source,
         "--title", "System App Disabler 0.1.0 — first public release",
         "--notes-file", "releases/system-app-disabler-v0.1.0.md",
         "--draft", f"{candidate}#{NAME}", f"{checksum}#{SIDECAR}"], check=True)

    for attempt in range(12):
        matches = [r for r in pages("releases") if r["tag_name"] == TAG]
        assert len(matches) == 1
        draft = api("releases/" + str(matches[0]["id"]))
        assert draft["draft"] and not draft["prerelease"] and draft["target_commitish"] == source
        assert len(draft["assets"]) == 2
        asset = next(a for a in draft["assets"] if a["name"] == NAME)
        side = next(a for a in draft["assets"] if a["name"] == SIDECAR)
        if (asset.get("digest") == "sha256:" + apk_sha
                and side.get("digest") == "sha256:" + sha(checksum)):
            break
        time.sleep(2)
    else:
        raise AssertionError("Draft assets not verified; nothing made public")

    assert asset["size"] == apk_bytes
    subprocess.run(["gh", "release", "edit", TAG, "--draft=false", "--latest=false"], check=True)

    public = api("releases/tags/" + TAG)
    assert not public["draft"] and not public["prerelease"]
    asset = next(a for a in public["assets"] if a["name"] == NAME)
    assert asset["size"] == apk_bytes and asset["digest"] == "sha256:" + apk_sha
    assert asset["browser_download_url"] == f"https://github.com/{REPO}/releases/download/{TAG}/{NAME}"

    # 6. Every earlier release must be byte-identical to its pre-publish snapshot.
    after = {r["tag_name"]: snapshot(r) for r in pages("releases") if not r["draft"]}
    for tag, snap in prior.items():
        assert after.get(tag) == snap, f"Existing release {tag} changed"

    Path(os.environ["RUNNER_TEMP"]).joinpath("appdisabler-publication-receipt.json").write_text(
        json.dumps({
            "tag": TAG, "name": NAME, "source": source, "run": RUN_ID,
            "artifact_id": ARTIFACT_ID, "archive_sha256": ARCHIVE_SHA,
            "apk_sha256": apk_sha, "bytes": apk_bytes, "signer": SIGNER,
            "package": PACKAGE, "version": "0.1.0", "version_code": 1,
            "rebuilt": False, "prior_public_assets_unchanged": True,
            "owner_device_tested": True,
            "owner_device": "Xiaomi HyperOS phone, 13 November 2026 screenshots",
            "play_protect_tested": False,
        }, indent=2))
    print(f"published {TAG} {NAME} {apk_bytes} bytes sha256 {apk_sha}")


if __name__ == "__main__":
    main()
