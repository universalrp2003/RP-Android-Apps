#!/usr/bin/env python3
"""Publish only the exact Store 0.13 candidate that was upgrade-tested, without a rebuild.

The candidate is the artifact of run 43385879437 on this branch: a release-key-signed,
non-debuggable 15-app Tamilan Store that was `adb install -r`-updated over the public
Store 0.12 on an Android 15 emulator with preferences and a private file preserved.

Everything here fails closed. The pin must match the artifact byte-for-byte, the APK inside
must carry package/version/signer/minSdk exactly as recorded, every earlier release must be
byte-identical after publication, and the tag must not already exist. Run it twice and it
stops before touching anything.
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

sys.path.insert(0, str(Path(__file__).resolve().parent))
from verify_release_permission_delta import parse_badging  # noqa: E402

REPO = "universalrp2003/RP-Android-Apps"
BRANCH = "arena/745a8cb3-rp-android-apps"

TAG = "tamilan-store-v0.13"
NAME = "Tamilan-Store-v0.13.apk"
OLD_TAG = "tamilan-store-v0.12"
OLD_NAME = "Tamilan-Store-v0.12.apk"
OLD_SHA = "29ad4b2a8b3dc575ef2e6707bc81e27dcfc0541a69bfef350f1a83ddd4298cce"

PACKAGE = "com.universalrp.tamilanstore"
VERSION = "0.13"
VERSION_CODE = 15
# Owner-backed replacement release key used by every new RP Ideas APK.
SIGNER = "7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d"

CANDIDATE_RUN = 43385879437
CANDIDATE_WORKFLOW = ".github/workflows/store-013-candidate.yml"
ARTIFACT = "Tamilan-Store-0.13-release-key-upgrade-candidate"
ARTIFACT_ID = 14018727997
ARCHIVE_SHA = "e21a82e22af7f01e5cd310f9c072b4ec37206092ae948e416604bf044f0ec0ee"
ARCHIVE_BYTES = 18422937
APK_SHA = "8efc49af3ab281720dd4a4cfedc9afcc319f8f578211796a16cc5aa5c0fa155b"
APK_BYTES = 19906055
# Recorded by the upgrade probe from the same bytes (see the run's job annotation).
UPGRADE_GATES = (
    "debug_over_release_rejected", "same_uid", "private_file_preserved",
    "language_preserved", "motion_preference_preserved", "restart_passed",
    "manual_catalog_refresh_rendered",
)


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
    assert "APPROVED_STORE_0_13_PUBLICATION" in os.environ, "Owner approval marker missing"

    work = Path(os.environ["RUNNER_TEMP"]) / "store013-publish"
    work.mkdir(parents=True, exist_ok=True)

    # 1. The pinned candidate run must be this branch's successful, fully gated candidate.
    info = api(f"actions/runs/{CANDIDATE_RUN}")
    assert (info["status"], info["conclusion"], info["path"]) == (
        "completed", "success", CANDIDATE_WORKFLOW), info
    source = info["head_sha"]
    assert re.fullmatch("[a-f0-9]{40}", source)
    subprocess.run(["git", "merge-base", "--is-ancestor", source, "HEAD"], check=True)
    job = api(f"actions/runs/{CANDIDATE_RUN}/jobs")["jobs"]
    assert len(job) == 1 and job[0]["conclusion"] == "success"
    steps = {s["name"]: s["conclusion"] for s in job[0]["steps"]}
    for required in ("Source and localization gates", "Test, lint and build release-key candidate",
                     "Pin identity, signer and exact upgrade fixture",
                     "Exact signed Store0.12 upgrade and data preservation on Android15",
                     "Exact artifact receipt after upgrade testing"):
        assert steps.get(required) == "success", required
    # The Store sources and catalogs must not have moved since the tested commit.
    subprocess.run(["git", "diff", "--exit-code", source, "HEAD", "--", "tamilanstore/",
                    "shared/", "tools/build_store_catalog.py"], check=True)

    artifacts = [a for a in api(f"actions/runs/{CANDIDATE_RUN}/artifacts")["artifacts"]
                 if a["id"] == ARTIFACT_ID]
    assert len(artifacts) == 1, "Expected exactly one pinned artifact"
    artifact = artifacts[0]
    assert artifact["name"] == ARTIFACT and not artifact["expired"]
    assert artifact["workflow_run"]["head_sha"] == source
    assert artifact["digest"] == "sha256:" + ARCHIVE_SHA
    assert artifact["size_in_bytes"] == ARCHIVE_BYTES

    # 2. Download through the authenticated API and pin the archive.
    archive = work / "candidate.zip"
    with archive.open("wb") as out:
        subprocess.run(["gh", "api", f"repos/{REPO}/actions/artifacts/{ARTIFACT_ID}/zip"],
                       stdout=out, check=True)
    assert sha(archive) == ARCHIVE_SHA and archive.stat().st_size == ARCHIVE_BYTES

    candidate = work / NAME
    with zipfile.ZipFile(archive) as z:
        assert len(z.namelist()) == len(set(z.namelist())), "Duplicate ZIP entry"
        assert sum(i.file_size for i in z.infolist()) < 100_000_000
        for item in z.infolist():
            path = Path(item.filename)
            assert not path.is_absolute() and ".." not in path.parts
            assert not stat.S_ISLNK(item.external_attr >> 16)
        assert [n for n in z.namelist() if n.endswith(".apk")] == [NAME], z.namelist()
        verification = json.loads(z.read("verification.json"))
        candidate.write_bytes(z.read(NAME))

    # 3. The recorded upgrade evidence must describe exactly these bytes.
    assert (verification["to_version"], verification["version_code"], verification["from_version"]) == (
        VERSION, VERSION_CODE, "0.12")
    assert verification["package"] == PACKAGE and verification["signer"] == SIGNER
    assert verification["source"] == source
    assert verification["bytes"] == APK_BYTES and verification["apk_sha256"] == APK_SHA
    for gate in UPGRADE_GATES:
        assert verification[gate] is True, gate
    assert verification["physical_phone_tested"] is False
    assert verification["play_protect_tested"] is False
    assert verification["permission_comparison"]["added"] == []
    assert verification["permission_comparison"]["removed"] == []

    apk_sha = sha(candidate)
    apk_bytes = candidate.stat().st_size
    assert (apk_sha, apk_bytes) == (APK_SHA, APK_BYTES)

    # 4. Inspect the bytes themselves: identity, debuggability, minSdk and signer.
    tools = build_tools()
    badging = run(str(tools / "aapt2"), "dump", "badging", str(candidate))
    assert f"package: name='{PACKAGE}' versionCode='{VERSION_CODE}' versionName='{VERSION}'" in badging
    assert "application-debuggable" not in badging
    assert re.search(r"(?:sdkVersion|minSdkVersion):\s*'26'", badging), badging[:400]
    cert = run(str(tools / "apksigner"), "verify", "--print-certs", str(candidate))
    assert set(re.findall(r"certificate SHA-256 digest: ([a-fA-F0-9]+)", cert)) == {SIGNER}, cert

    # 5. No new permissions over the public 0.12 build.
    baseline = work / "baseline"
    baseline.mkdir()
    subprocess.run(["gh", "release", "download", OLD_TAG, "--pattern", OLD_NAME,
                    "--dir", str(baseline)], check=True)
    assert sha(baseline / OLD_NAME) == OLD_SHA
    previous = run(str(tools / "aapt2"), "dump", "badging", str(baseline / OLD_NAME))
    assert parse_badging(badging) == parse_badging(previous), "Merged APK permission set changed"

    # 6. The tag must not exist, and every public release is snapshotted for a byte-for-byte check.
    releases = pages("releases")
    assert not [r for r in releases if r["tag_name"] == TAG], "Tag already exists; publish a new version"
    assert not [t for t in pages("tags") if t["name"] == TAG]
    prior = {r["tag_name"]: snapshot(r) for r in releases if not r["draft"]}
    old = api("releases/tags/" + OLD_TAG)

    checksum = work / "Tamilan-Store-v0.13.sha256"
    checksum.write_text(f"{apk_sha}  {NAME}\n")

    # 7. Create the release, verify both asset digests, then make it public.
    subprocess.run(
        ["gh", "release", "create", TAG, "--target", source,
         "--title", "Tamilan Store 0.13 — Android TV Guard and System App Disabler",
         "--notes-file", "releases/tamilan-store-v0.13.md",
         "--draft", f"{candidate}#{NAME}", f"{checksum}#{checksum.name}"], check=True)

    for attempt in range(12):
        matches = [r for r in pages("releases") if r["tag_name"] == TAG]
        assert len(matches) == 1
        draft = api("releases/" + str(matches[0]["id"]))
        assert draft["draft"] and not draft["prerelease"] and draft["target_commitish"] == source
        assert len(draft["assets"]) == 2
        asset = next(a for a in draft["assets"] if a["name"] == NAME)
        side = next(a for a in draft["assets"] if a["name"] == checksum.name)
        if (asset.get("digest") == "sha256:" + APK_SHA
                and side.get("digest") == "sha256:" + sha(checksum)):
            break
        time.sleep(2)
    else:
        raise AssertionError("Draft assets not verified; nothing made public")

    assert asset["size"] == APK_BYTES
    subprocess.run(["gh", "release", "edit", TAG, "--draft=false", "--latest=false"], check=True)

    public = api("releases/tags/" + TAG)
    assert not public["draft"] and not public["prerelease"]
    asset = next(a for a in public["assets"] if a["name"] == NAME)
    assert asset["size"] == APK_BYTES and asset["digest"] == "sha256:" + APK_SHA
    assert asset["browser_download_url"] == f"https://github.com/{REPO}/releases/download/{TAG}/{NAME}"

    # 8. The previously public Store release must be untouched, and so must every other release.
    after = api("releases/tags/" + OLD_TAG)
    assert old["id"] == after["id"]
    assert snapshot(old) == snapshot(after), "The public 0.12 release changed"
    current = {r["tag_name"]: snapshot(r) for r in pages("releases") if not r["draft"]}
    for tag, snap in prior.items():
        assert current[tag] == snap, f"Existing release {tag} changed"

    receipt = dict(
        rebuilt=False, prior_public_assets_unchanged=True, tag=TAG, source=source,
        candidate_run=CANDIDATE_RUN, artifact_id=ARTIFACT_ID,
        release_id=public["id"], asset_id=asset["id"], published_at=public["published_at"],
        url=asset["browser_download_url"], bytes=apk_bytes, sha256=apk_sha, signer=SIGNER,
        package=PACKAGE, version=VERSION, version_code=VERSION_CODE,
        upgraded_from="0.12", upgrade_gates=list(UPGRADE_GATES),
        physical_phone_tested=False, play_protect_tested=False,
    )
    Path(os.environ["RUNNER_TEMP"], "store013-publication-receipt.json").write_text(
        json.dumps(receipt, indent=2) + "\n")
    print("::notice title=Published exact Store0.13::" + json.dumps(receipt), flush=True)


if __name__ == "__main__":
    try:
        main()
    except Exception:
        import traceback
        print("::error title=Store0.13 promotion stopped::"
              + traceback.format_exc()[-3500:].replace("%", "%25").replace("\n", "%0A"),
              flush=True)
        raise
