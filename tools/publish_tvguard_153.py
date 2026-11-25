#!/usr/bin/env python3
"""One-time exact-byte promotion of the owner-approved TV Guard 1.5.3 candidate."""
import json
import os
from pathlib import Path
import publish_tv_safety_candidates as common


def main():
    assert os.environ.get('GITHUB_ACTIONS') == 'true'
    assert os.environ.get('GITHUB_REF') == 'refs/heads/' + common.BRANCH
    assert os.environ.get('GH_REPO') == common.REPO
    data = json.loads(Path('docs/tvguard-1.5.3-approval.json').read_text())
    assert data['owner_authorized'] is True
    p = data['candidate']
    assert p['tag'] == 'tvguard-v1.5.3' and p['run_id'] == 45558877424
    assert p['sha256'] == '1d7452ce651551c1e1fd5c4a4dcc73764a6f317ac58a1d69cb71bd9e719ac3c1'
    common.WORK.mkdir(parents=True, exist_ok=True)
    prior = common.releases()
    before = common.snapshots(prior)
    apk, checksum = common.preflight(p, prior, common.tools_dir())
    common.publish(p, apk, checksum, before)
    print('PASS: exact approved candidate published without rebuilding', flush=True)


if __name__ == '__main__':
    try:
        main()
    except Exception:
        import traceback
        print('::error title=TV Guard publication stopped::' + traceback.format_exc()[-6000:].replace('%', '%25').replace('\n', '%0A'), flush=True)
        raise
