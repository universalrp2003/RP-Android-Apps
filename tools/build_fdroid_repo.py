#!/usr/bin/env python3
"""Generate and sign a standards-compliant F-Droid v1 repository (index-v1.jar).

Allows F-Droid, Droid-ify, Neo Store, and Obtainium clients to install and auto-update
official RP Ideas apps via Android's Store Session Installer (PACKAGE_SOURCE_STORE)
with full SHA-256 + RP Ideas X.509 certificate verification and Google Play Protect ON.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import shutil
import subprocess
import time
from pathlib import Path

EXPECTED_SIGNER = "7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d"


def parse_badging(aapt_bin: str, apk_path: Path) -> dict:
    out = subprocess.check_output([aapt_bin, "dump", "badging", str(apk_path)], text=True)
    pkg_m = re.search(
        r"package:\s+name='([^']+)'\s+versionCode='(\d+)'\s+versionName='([^']+)'",
        out,
    )
    if not pkg_m:
        raise RuntimeError(f"Could not parse package badging from {apk_path}")
    sdk_m = re.search(r"sdkVersion:'(\d+)'", out)
    target_m = re.search(r"targetSdkVersion:'(\d+)'", out)
    label_m = re.search(r"application-label:'([^']+)'", out)
    perms = [[m.group(1), None] for m in re.finditer(r"uses-permission:\s+name='([^']+)'", out)]
    return {
        "packageName": pkg_m.group(1),
        "versionCode": int(pkg_m.group(2)),
        "versionName": pkg_m.group(3),
        "minSdkVersion": int(sdk_m.group(1)) if sdk_m else 26,
        "targetSdkVersion": int(target_m.group(1)) if target_m else 35,
        "label": label_m.group(1) if label_m else pkg_m.group(1),
        "uses-permission": perms,
    }


def main() -> None:
    parser = argparse.ArgumentParser(description="Build signed RP Ideas F-Droid index-v1.jar")
    parser.add_argument("--apk", action="append", required=True, help="Path to verified signed APK(s)")
    parser.add_argument("--out-dir", required=True, help="Output directory for fdroid/repo")
    parser.add_argument("--repo-url", default="https://universalrp2003.github.io/RP-Android-Apps/fdroid/repo")
    parser.add_argument("--keystore", default="ci-keystore/release.p12")
    parser.add_argument("--alias", default="cleansweep")
    parser.add_argument("--aapt", required=True)
    args = parser.parse_args()

    out_dir = Path(args.out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    now_ms = int(time.time() * 1000)

    apps = []
    packages: dict[str, list[dict]] = {}

    for raw_apk in args.apk:
        src = Path(raw_apk)
        dst = out_dir / src.name
        if src.resolve() != dst.resolve():
            shutil.copy2(src, dst)
        meta = parse_badging(args.aapt, dst)
        sha256 = hashlib.sha256(dst.read_bytes()).hexdigest()
        pkg = meta["packageName"]

        summary_en = (
            "Privacy-first Default SMS app with link shield, voice alerts & spoof-proof finance ledger"
            if pkg == "com.universalrp.smsguard"
            else f"{meta['label']} — Verified RP Ideas Android App"
        )
        summary_ta = (
            "போலி இணைப்பு கவசம், குரல் அறிவிப்பு மற்றும் பாதுகாப்பான வரவு-செலவு கணக்கு கொண்ட குறுஞ்செய்தி செயலி"
            if pkg == "com.universalrp.smsguard"
            else f"{meta['label']} — RP Ideas அதிகாரப்பூர்வ செயலி"
        )

        apps.append(
            {
                "added": now_ms,
                "authorEmail": "universalrp2003@gmail.com",
                "authorName": "Ramesh prathap .R (RP Ideas)",
                "categories": ["Security", "Phone & SMS", "System"],
                "lastUpdated": now_ms,
                "license": "Apache-2.0",
                "localized": {
                    "en-US": {
                        "name": meta["label"],
                        "summary": summary_en,
                        "description": summary_en,
                    },
                    "ta-IN": {
                        "name": "குறுஞ்செய்தி காவலன் (SMS Guard)" if pkg == "com.universalrp.smsguard" else meta["label"],
                        "summary": summary_ta,
                        "description": summary_ta,
                    },
                },
                "name": meta["label"],
                "packageName": pkg,
                "sourceCode": "https://github.com/universalrp2003/RP-Android-Apps",
                "suggestedVersionCode": str(meta["versionCode"]),
                "suggestedVersionName": meta["versionName"],
                "summary": summary_en,
                "webSite": "https://universalrp2003.github.io/RP-Android-Apps/",
            }
        )

        packages.setdefault(pkg, []).append(
            {
                "added": now_ms,
                "apkName": dst.name,
                "hash": sha256,
                "hashType": "sha256",
                "minSdkVersion": meta["minSdkVersion"],
                "packageName": pkg,
                "signer": EXPECTED_SIGNER,
                "size": dst.stat().st_size,
                "targetSdkVersion": meta["targetSdkVersion"],
                "uses-permission": meta["uses-permission"],
                "versionCode": meta["versionCode"],
                "versionName": meta["versionName"],
            }
        )

    index_payload = {
        "repo": {
            "address": args.repo_url,
            "description": "Official cryptographically verified F-Droid repository for RP Ideas Android apps (Live Guard, SMS Guard, Ramesh Radio, Oor Arivippu, PulseEQ, Vocal Remover, Secret Message).",
            "icon": "icon.png",
            "name": "RP Ideas Official Repository",
            "timestamp": now_ms,
            "version": 20002,
        },
        "requests": {"install": [], "uninstall": []},
        "apps": apps,
        "packages": packages,
    }

    index_json = out_dir / "index-v1.json"
    index_jar = out_dir / "index-v1.jar"
    index_json.write_text(json.dumps(index_payload, ensure_ascii=False, indent=2), encoding="utf-8")

    subprocess.check_call(["jar", "cf", str(index_jar), "-C", str(out_dir), "index-v1.json"])
    if not os.environ.get("RP_RELEASE_KEYSTORE_PASSWORD"):
        raise RuntimeError("RP_RELEASE_KEYSTORE_PASSWORD is required to sign the F-Droid repository index")
    subprocess.check_call(
        [
            "jarsigner",
            "-keystore",
            args.keystore,
            "-storepass:env",
            "RP_RELEASE_KEYSTORE_PASSWORD",
            "-sigalg",
            "SHA256withRSA",
            "-digestalg",
            "SHA-256",
            str(index_jar),
            args.alias,
        ]
    )
    subprocess.check_call(["jarsigner", "-verify", "-certs", str(index_jar)])
    index_json.unlink(missing_ok=True)
    print(f"PASS: Signed F-Droid repository index generated and verified at {index_jar}")


if __name__ == "__main__":
    main()
