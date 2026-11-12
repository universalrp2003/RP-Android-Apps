#!/usr/bin/env python3
"""Delete superseded GitHub Actions artifacts, keeping everything that still has proof value.

The repository had accumulated thousands of build artifacts (each ``Build APK`` run uploads five
APK/bundle packages). They are free on a public repository, but they make the Actions API and UI
slow and bury the runs anyone actually needs. Issue #67 recorded that the Arena integration cannot
delete artifacts (403), so the repository's own workflow token does it here instead.

Keep rules, in order:

1. anything younger than ``KEEP_DAYS`` (default 7) - recent failures are still being debugged;
2. the newest artifact of every (name, branch) pair - one sample of each pipeline survives;
3. a ``github-pages`` package younger than ``PAGES_KEEP_DAYS`` (default 7) - never disturb a
   deployment that may still be in flight;
4. any artifact id quoted in ``docs/`` or ``releases/`` - publication receipts must keep resolving.

Everything else is deleted oldest-first, at most ``MAX_DELETES`` per run because a workflow token
is limited to 1,000 requests per hour per repository.

    DRY_RUN=1 GITHUB_TOKEN=... GITHUB_REPOSITORY=owner/repo python3 tools/cleanup_actions_artifacts.py

This never touches a release, a tag, a run, a log or the website - only stored artifact packages.
"""

from __future__ import annotations

import json
import os
import re
import sys
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timedelta, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
API = 'https://api.github.com'
KEEP_DAYS = int(os.environ.get('KEEP_DAYS', '7'))
PAGES_KEEP_DAYS = int(os.environ.get('PAGES_KEEP_DAYS', '7'))
MAX_DELETES = int(os.environ.get('MAX_DELETES', '300'))
DRY_RUN = os.environ.get('DRY_RUN', '').lower() in {'1', 'true', 'yes'}
TOKEN = os.environ.get('GITHUB_TOKEN', '')
REPO = os.environ.get('GITHUB_REPOSITORY', '')


def get(url: str) -> dict:
    request = urllib.request.Request(url, headers={
        'Authorization': f'Bearer {TOKEN}',
        'Accept': 'application/vnd.github+json',
        'X-GitHub-Api-Version': '2022-11-28',
    })
    with urllib.request.urlopen(request, timeout=60) as response:
        return json.loads(response.read().decode('utf-8'))


def delete(url: str) -> None:
    request = urllib.request.Request(url, method='DELETE', headers={
        'Authorization': f'Bearer {TOKEN}',
        'Accept': 'application/vnd.github+json',
        'X-GitHub-Api-Version': '2022-11-28',
    })
    with urllib.request.urlopen(request, timeout=60):
        pass


def all_artifacts() -> list[dict]:
    artifacts: list[dict] = []
    page = 1
    while True:
        payload = get(f'{API}/repos/{REPO}/actions/artifacts?per_page=100&page={page}')
        batch = payload.get('artifacts', [])
        artifacts.extend(batch)
        if len(batch) < 100 or len(artifacts) >= payload.get('total_count', 0):
            return artifacts
        page += 1


def protected_ids() -> set[str]:
    """Artifact ids written into a receipt, pin or release note must stay downloadable."""
    text = ''
    for folder, suffixes in ((ROOT / 'docs', {'.md', '.json'}), (ROOT / 'releases', {'.md'})):
        for path in folder.rglob('*'):
            if path.is_file() and path.suffix in suffixes:
                try:
                    text += path.read_text(encoding='utf-8', errors='ignore')
                except OSError:
                    continue
    return set(re.findall(r'\b(\d{8,})\b', text))


def parse(art: dict) -> datetime:
    return datetime.fromisoformat(art['created_at'].replace('Z', '+00:00'))


def main() -> int:
    if not TOKEN or '/' not in REPO:
        print('GITHUB_TOKEN and GITHUB_REPOSITORY are required', file=sys.stderr)
        return 2
    now = datetime.now(timezone.utc)
    artifacts = all_artifacts()
    protected = protected_ids()
    by_id = {str(art['id']): art for art in artifacts}
    newest: dict[tuple[str, str], str] = {}
    for art in artifacts:
        branch = (art.get('workflow_run') or {}).get('head_branch') or '-'
        key = (art['name'], branch)
        current = newest.get(key)
        if current is None or parse(art) > parse(by_id[current]):
            newest[key] = str(art['id'])

    keep_recent = timedelta(days=KEEP_DAYS)
    keep_pages = timedelta(days=PAGES_KEEP_DAYS)
    victims: list[dict] = []
    reasons = {'young': 0, 'newest-per-name': 0, 'pages-recent': 0, 'referenced': 0}
    for art in artifacts:
        age = now - parse(art)
        if age <= keep_recent:
            reasons['young'] += 1
        elif newest.get((art['name'], (art.get('workflow_run') or {}).get('head_branch') or '-')) == str(art['id']):
            reasons['newest-per-name'] += 1
        elif art['name'] == 'github-pages' and age <= keep_pages:
            reasons['pages-recent'] += 1
        elif str(art['id']) in protected:
            reasons['referenced'] += 1
        else:
            victims.append(art)

    victims.sort(key=parse)  # oldest first: the biggest wins come first
    batch = victims[:MAX_DELETES]
    freed = sum(int(art['size_in_bytes']) for art in batch)
    print(f'scanned {len(artifacts)} artifacts ({sum(int(a["size_in_bytes"]) for a in artifacts) / 1e9:.2f} GB)')
    print('kept: ' + ', '.join(f'{k}={v}' for k, v in reasons.items()))
    print(f'pending deletion: {len(victims)} artifacts ({sum(int(a["size_in_bytes"]) for a in victims) / 1e9:.2f} GB)')
    print(f'this run: {"DRY RUN, nothing deleted" if DRY_RUN else f"deleting {len(batch)}"} '
          f'({freed / 1e9:.2f} GB, oldest {parse(batch[0]).date() if batch else "-"})')

    deleted = 0
    for art in batch:
        if DRY_RUN:
            continue
        try:
            delete(f'{API}/repos/{REPO}/actions/artifacts/{art["id"]}')
            deleted += 1
        except urllib.error.HTTPError as error:
            print(f'::warning::could not delete {art["id"]} ({art["name"]}): {error.code}')

    verb = 'would delete' if DRY_RUN else 'deleted'
    print(f'::notice title=Actions artifact cleanup::{verb} {len(batch) if DRY_RUN else deleted} '
          f'of {len(victims)} pending packages, {freed / 1e9:.2f} GB; kept {len(artifacts) - len(victims)} '
          f'(recent, newest per pipeline, recent Pages packages and every id quoted in docs/)')
    if not DRY_RUN and batch and deleted == 0:
        # A run that deletes nothing is a failure, not a success: the token cannot write to Actions
        # storage (Settings -> Actions -> General -> Workflow permissions), or every call was refused.
        print('::error title=Actions artifact cleanup::no package could be deleted; check the workflow '
              'token permission (Settings -> Actions -> General -> Workflow permissions)', file=sys.stderr)
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main())
