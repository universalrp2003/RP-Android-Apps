#!/usr/bin/env python3
"""Create a name-only review queue. Similar names do NOT prove station identity."""
import collections
import csv
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
REFERENCE = pathlib.Path(__file__).resolve().parent


def key(text):
    return re.sub(r'[^a-z0-9]', '', text.casefold())


def main(path):
    inventory = collections.defaultdict(set)
    for line in (REFERENCE/'screenshot-inventory.txt').read_text(encoding='utf8').splitlines():
        if not re.match(r'^\d\d \|', line):
            continue
        for label in line.split(' | ')[1:]:
            inventory[label].add(int(line[:2]))
    stations = json.loads((ROOT/'radio/src/main/assets/stations_seed.json').read_text(encoding='utf8'))['stations']
    by_name = collections.defaultdict(list)
    for station in stations:
        by_name[key(station['name'])].append(station)
    with open(path, 'w', encoding='utf8', newline='') as f:
        writer = csv.writer(f, lineterminator='\n')
        writer.writerow(['reference_label', 'screenshots', 'review_state', 'seed_name_only_matches'])
        for label, screens in sorted(inventory.items(), key=lambda x: x[0].casefold()):
            matches = by_name[key(label)] if '[' not in label else []
            state = 'name-only match; identity/content still require review' if matches else 'unresolved; search or inspect'
            writer.writerow([label, ','.join(f'{s:02}' for s in sorted(screens)), state,
                             '; '.join(s['name'] + ' [' + s['id'] + ']' for s in matches)])
    print('Reference labels:', len(inventory), 'name-only seed matches:', sum(bool(by_name[key(k)]) for k in inventory if '[' not in k))

if __name__ == '__main__':
    main(sys.argv[1])
