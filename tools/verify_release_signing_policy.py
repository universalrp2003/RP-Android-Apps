#!/usr/bin/env python3
"""Publication hold for a known-exposed signing identity; never blocks build-only tests.

Deleting a tracked keystore does not revoke copies/history or repair old signatures.
This is an explicit legacy-publication gate, not proof that any other key is secure.
"""
import argparse
import re

LEGACY_MAIN = '9e30f4eab8afce5e4ab444ff9578fec354d6d1414d245253590a9703d5985d55'
KNOWN_EXPOSED = {LEGACY_MAIN}


def verify_signer(value):
    canonical = value.lower().replace(':', '').strip()
    if not re.fullmatch(r'[a-f0-9]{64}', canonical):
        raise ValueError('Invalid signing-certificate fingerprint; publication refused')
    if canonical in KNOWN_EXPOSED:
        raise ValueError('Publication paused: this signing private-key container and its unlock reference are public. '
                         'Removing the file does not fix exposure. Preserve installed data; reviewed signing-continuity '
                         'and platform-specific migration plan required. Do not bypass device warnings.')
    return canonical


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument('--legacy-main', action='store_true', help='Gate current main/Store legacy signing identity')
    group.add_argument('--signer', help='Future verified actual APK signer; use only after reviewed migration')
    args = parser.parse_args()
    try:
        verify_signer(LEGACY_MAIN if args.legacy_main else args.signer)
    except ValueError as error:
        print('::error title=Release signing hold::' + str(error))
        raise SystemExit(1)
    print('Signer is not on the known-exposed list. This is not a key-custody or security certification.')


if __name__ == '__main__':
    main()
