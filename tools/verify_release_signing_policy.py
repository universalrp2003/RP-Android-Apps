#!/usr/bin/env python3
"""Publication guard for the exposed legacy signer and pinned fresh-install signer."""
import argparse
import re

LEGACY_MAIN = '9e30f4eab8afce5e4ab444ff9578fec354d6d1414d245253590a9703d5985d55'
FRESH_INSTALL_SIGNER = '7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d'
KNOWN_EXPOSED = {LEGACY_MAIN}


def normalize(value):
    canonical = value.lower().replace(':', '').strip()
    if not re.fullmatch(r'[a-f0-9]{64}', canonical):
        raise ValueError('Invalid signing-certificate fingerprint; publication refused')
    return canonical


def verify_signer(value):
    canonical = normalize(value)
    if canonical in KNOWN_EXPOSED:
        raise ValueError('Publication paused: the legacy signing private-key container and unlock reference are public. '
                         'Do not use it for new releases.')
    return canonical


def verify_fresh_install_signer(value):
    canonical = verify_signer(value)
    if canonical != FRESH_INSTALL_SIGNER:
        raise ValueError('Fresh-install signer differs from the owner-backed, pinned replacement identity')
    return canonical


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument('--legacy-main', action='store_true', help='Gate the exposed legacy identity (must fail)')
    group.add_argument('--signer', help='Reject known-exposed fingerprints')
    group.add_argument('--fresh-install', action='store_true', help='Verify the pinned fresh-install replacement signer')
    args = parser.parse_args()
    try:
        if args.legacy_main:
            verify_signer(LEGACY_MAIN)
        elif args.fresh_install:
            verify_fresh_install_signer(FRESH_INSTALL_SIGNER)
        else:
            verify_signer(args.signer)
    except ValueError as error:
        print('::error title=Release signing hold::' + str(error))
        raise SystemExit(1)
    print('Signer matches the pinned fresh-install identity. Existing installs require uninstall/reinstall; this is not a Play Protect or device-safety certification.')


if __name__ == '__main__':
    main()
