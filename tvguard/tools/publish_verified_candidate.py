#!/usr/bin/env python3
"""Publish the owner-approved TV Guard 1.5.1 candidate: exact tested bytes, never a rebuild.

The APK is taken from the CI artifact that produced the hash the owner tested. Nothing here
compiles an app. If any pinned value disagrees, the script stops before creating a release.
"""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time
import zipfile

REPO = "universalrp2003/RP-Android-Apps"
BRANCH = "arena/60062a79-rp-android-apps"

# Pinned to the run whose artifact the owner installed and reported working on the TV.
RUN_ID = 43012300628
ARTIFACT = "tvguard-1.5.1-stable-signed-tested-candidate"
APK_SHA = "aea1ae03d2bfe87ee0659cd2cba8104e22365c1c18391587d9ae7b70cae3614b"
APK_BYTES = 11_254_309
TAG = "tvguard-v1.5.1"
INNER = "Android-TV-Guard-1.5.1-signed-candidate.apk"
NAME = "Android-TV-Guard-v1.5.1.apk"
SIDECAR = "Android-TV-Guard-v1.5.1.apk.sha256"
PACKAGE = "com.universalrp.tvguard"
BADGING = f"package: name='{PACKAGE}' versionCode='8' versionName='1.5.1'"


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
    return release["id"], sorted((a["id"], a["name"], a["size"], a.get("digest")) for a in release["assets"])


def build_tools():
    root = Path(os.environ["ANDROID_HOME"]) / "build-tools"
    return sorted(root.iterdir(), key=lambda d: tuple(int(n) for n in re.findall(r"\d+", d.name)))[-1]


def main():
    assert os.environ.get("GITHUB_ACTIONS") == "true", "Publication only runs in CI with a token"
    assert os.environ.get("GITHUB_REF") == f"refs/heads/{BRANCH}", os.environ.get("GITHUB_REF")
    assert os.environ.get("GH_REPO") == REPO

    work = Path(os.environ["RUNNER_TEMP"]) / "tvguard-publish"
    work.mkdir(parents=True, exist_ok=True)

    # 1. The run must be the pinned, successful candidate build from this branch.
    info = api(f"actions/runs/{RUN_ID}")
    assert (info["status"], info["conclusion"], info["path"]) == (
        "completed", "success", ".github/workflows/tvguard-candidate.yml"), info
    source = info["head_sha"]
    subprocess.run(["git", "merge-base", "--is-ancestor", source, "HEAD"], check=True)
    # The app itself must not have changed since the tested commit. The publishing tool is not
    # part of the build, so it is excluded: it has to be committed to trigger this workflow.
    subprocess.run(["git", "diff", "--exit-code", source, "HEAD", "--", "tvguard/app/",
                    "tvguard/tools/", ":(exclude)tvguard/tools/publish_verified_candidate.py"], check=True)

    artifacts = [a for a in api(f"actions/runs/{RUN_ID}/artifacts")["artifacts"] if a["name"] == ARTIFACT]
    assert len(artifacts) == 1, "Expected exactly one pinned artifact"
    artifact = artifacts[0]
    assert not artifact["expired"] and artifact["workflow_run"]["head_sha"] == source

    archive = work / "candidate.zip"
    with archive.open("wb") as stream:
        subprocess.run(["gh", "api", f"repos/{REPO}/actions/artifacts/{artifact['id']}/zip"], stdout=stream, check=True)
    with zipfile.ZipFile(archive) as z:
        for item in z.infolist():
            assert not Path(item.filename).is_absolute() and ".." not in Path(item.filename).parts
            assert not (item.external_attr >> 16) & 0o170000 == 0o120000, "No symlinks in the artifact"
        assert INNER in z.namelist(), z.namelist()
        candidate = work / NAME
        candidate.write_bytes(z.read(INNER))
        receipt = json.loads(z.read("verification.json"))
        assert z.read(INNER + ".sha256").split()[0].decode() == APK_SHA

    # 2. The bytes must be exactly the ones the owner tested.
    assert sha(candidate) == APK_SHA and candidate.stat().st_size == APK_BYTES
    assert receipt["apkSha256"] == APK_SHA and receipt["apkBytes"] == APK_BYTES
    assert (receipt["package"], receipt["versionName"], receipt["versionCode"]) == (PACKAGE, "1.5.1", 8)
    assert receipt["exactCandidateInstalledAndTested"] is True and receipt["published"] is False

    tools = build_tools()
    badging = run(str(tools / "aapt2"), "dump", "badging", str(candidate))
    assert BADGING in badging, badging[:400]
    assert "application-debuggable" not in badging
    sys.path.insert(0, "tools")
    from prepare_release_signing import EXPECTED_SIGNER
    certs = run(str(tools / "apksigner"), "verify", "--print-certs", str(candidate))
    assert set(re.findall(r"certificate SHA-256 digest: ([a-fA-F0-9]+)", certs)) == {EXPECTED_SIGNER}

    # 3. Nothing may already exist under this tag, and no older asset may change.
    before = pages("releases")
    prior = {r["tag_name"]: snapshot(r) for r in before if not r["draft"]}
    assert not [r for r in before if r["tag_name"] == TAG], "Never overwrite or resume a release blindly"
    assert not [t for t in pages("tags") if t["name"] == TAG], "Never replace a tag"

    checksum = work / SIDECAR
    checksum.write_text(f"{APK_SHA}  {NAME}\n")
    subprocess.run([
        "gh", "release", "create", TAG, "--target", source,
        "--title", "Android TV Guard 1.5.1 — copy fixes from the owner's screenshots",
        "--notes-file", f"releases/{TAG}.md", "--draft",
        f"{candidate}#{NAME}", f"{checksum}#{SIDECAR}",
    ], check=True)

    for _ in range(12):
        matches = [r for r in pages("releases") if r["tag_name"] == TAG]
        if matches:
            break
        time.sleep(2)
    else:
        raise AssertionError("Draft release did not appear")
    assert len(matches) == 1
    draft = api(f"releases/{matches[0]['id']}")
    assert draft["draft"] and not draft["prerelease"] and draft["target_commitish"] == source
    assert sorted(a["name"] for a in draft["assets"]) == sorted([NAME, SIDECAR])
    asset = next(a for a in draft["assets"] if a["name"] == NAME)
    for _ in range(12):
        asset = next(a for a in api(f"releases/{draft['id']}")["assets"] if a["name"] == NAME)
        if asset.get("digest") == f"sha256:{APK_SHA}" and asset["size"] == APK_BYTES:
            break
        time.sleep(2)
    else:
        raise AssertionError("Draft asset digest never matched; nothing published")

    # 4. Prove the bytes GitHub stored are the tested bytes, then publish that same asset.
    for phase in ("draft", "public"):
        directory = work / phase
        directory.mkdir(exist_ok=True)
        with (directory / NAME).open("wb") as stream:
            subprocess.run(["gh", "api", f"repos/{REPO}/releases/assets/{asset['id']}",
                            "-H", "Accept: application/octet-stream"], stdout=stream, check=True)
        assert sha(directory / NAME) == APK_SHA
        assert (directory / NAME).stat().st_size == APK_BYTES
        if phase == "draft":
            subprocess.run(["gh", "release", "edit", TAG, "--draft=false", "--latest=false"], check=True)

    for _ in range(12):
        public = api(f"releases/{draft['id']}")
        asset = next(a for a in public["assets"] if a["name"] == NAME)
        if not public["draft"] and asset["browser_download_url"] == f"https://github.com/{REPO}/releases/download/{TAG}/{NAME}":
            break
        time.sleep(2)
    else:
        raise AssertionError("Published state not verified; do not rerun publication blindly")
    assert asset["digest"] == f"sha256:{APK_SHA}" and asset["size"] == APK_BYTES

    after = {r["tag_name"]: snapshot(r) for r in pages("releases") if not r["draft"]}
    for tag, old in prior.items():
        assert after[tag] == old, f"Prior release {tag} changed"

    result = {
        "app": "Android TV Guard", "package": PACKAGE, "tag": TAG, "versionName": "1.5.1", "versionCode": 8,
        "sourceCommit": source, "workflowRun": RUN_ID, "artifact": ARTIFACT,
        "apkSha256": APK_SHA, "apkBytes": APK_BYTES, "signerSha256": EXPECTED_SIGNER,
        "releaseId": public["id"], "assetId": asset["id"], "publishedAt": public["published_at"],
        "url": asset["browser_download_url"], "rebuilt": False, "priorPublicAssetsUnchanged": True,
        "ownerReportedInstalledOnPhysicalTv": True,
        "ownerObservedPill": "78%/996 MB/11.9 GB/46.0 °C/good and 80%/1039 MB/11.9 GB/45.5 °C/good",
        "installedTvHashIndependentlyMatched": False, "playProtectClearance": False,
    }
    (work / "tvguard-1.5.1-publication-receipt.json").write_text(json.dumps(result, indent=2) + "\n")
    print("::notice title=Published Android TV Guard 1.5.1::" + json.dumps(result), flush=True)


if __name__ == "__main__":
    try:
        main()
    except Exception:
        import traceback
        print("::error title=TV Guard 1.5.1 publication stopped::"
              + traceback.format_exc()[-3500:].replace("%", "%25").replace("\n", "%0A"), flush=True)
        raise
