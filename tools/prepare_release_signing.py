#!/usr/bin/env python3
"""Materialize and verify the private fresh-install signer from Actions secrets.

The keystore is never committed. CI receives it only for the duration of a job.
"""
from __future__ import annotations

import base64
import binascii
import os
import re
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
KEYSTORE = ROOT / "ci-keystore" / "release.p12"
EXPECTED_SIGNER = "7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d"


def prepare(env: dict[str, str] | None = None) -> str:
    env = os.environ if env is None else env
    encoded = env.get("RP_RELEASE_KEYSTORE_BASE64", "")
    password = env.get("RP_RELEASE_KEYSTORE_PASSWORD", "")
    if not encoded or not password:
        raise ValueError("Both RP_RELEASE_KEYSTORE_BASE64 and RP_RELEASE_KEYSTORE_PASSWORD Actions secrets are required")

    compact = re.sub(r"\s+", "", encoded)
    try:
        blob = base64.b64decode(compact, validate=True)
    except (binascii.Error, ValueError) as error:
        raise ValueError("RP_RELEASE_KEYSTORE_BASE64 is not valid base64") from error
    if not 1024 <= len(blob) <= 2_000_000:
        raise ValueError("Signing keystore has an unexpected size")

    KEYSTORE.parent.mkdir(parents=True, exist_ok=True)
    KEYSTORE.write_bytes(blob)
    KEYSTORE.chmod(0o600)
    try:
        cert = subprocess.run(
            ["openssl", "pkcs12", "-in", str(KEYSTORE), "-clcerts", "-nokeys", "-passin", "env:RP_RELEASE_KEYSTORE_PASSWORD"],
            check=True,
            capture_output=True,
            env={**env, "RP_RELEASE_KEYSTORE_PASSWORD": password},
        ).stdout
        fingerprint_out = subprocess.run(
            ["openssl", "x509", "-noout", "-fingerprint", "-sha256"],
            input=cert,
            check=True,
            capture_output=True,
        ).stdout.decode("ascii", errors="strict")
        match = re.search(r"=([A-Fa-f0-9:]+)", fingerprint_out)
        fingerprint = match.group(1).replace(":", "").lower() if match else ""
        if fingerprint != EXPECTED_SIGNER:
            raise ValueError("Signing certificate does not match the pinned fresh-install identity")
    except (subprocess.CalledProcessError, OSError) as error:
        KEYSTORE.unlink(missing_ok=True)
        raise ValueError("Could not open or verify the private release keystore") from error
    except ValueError:
        KEYSTORE.unlink(missing_ok=True)
        raise

    return fingerprint


def main() -> None:
    try:
        fingerprint = prepare()
    except ValueError as error:
        print(f"::error title=Release signer setup::{error}")
        raise SystemExit(1)
    print(f"PASS: loaded the private fresh-install signer; certificate SHA-256 {fingerprint}")


if __name__ == "__main__":
    main()
