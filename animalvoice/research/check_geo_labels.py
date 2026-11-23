#!/usr/bin/env python3
"""Compare published label sets by exact scientific name; never filters user audio."""
import collections
import csv
import json
import sys


def compare(acoustic_file, geo_file):
    acoustic = list(csv.DictReader(acoustic_file))
    if not acoustic or 'scientific_name' not in acoustic[0]:
        raise ValueError('Unexpected acoustic labels')
    names = [row['scientific_name'].strip() for row in acoustic]
    geo = []
    for line in geo_file:
        parts = line.rstrip('\r\n').split('\t')
        if len(parts) < 2 or not parts[1].strip():
            raise ValueError('Unexpected geomodel labels')
        geo.append(parts[1].strip())
    counts = collections.Counter(geo)
    if not geo:
        raise ValueError('Empty geomodel labels')
    # Exact match matches the pinned reference; blank or absent acoustic taxa remain unfiltered.
    return {
        'acoustic_rows': len(names), 'geo_rows': len(geo),
        'acoustic_matched_rows': sum(name in counts for name in names if name),
        'acoustic_unmatched_rows': sum(not name or name not in counts for name in names),
        'geo_duplicate_scientific_names': sum(1 for count in counts.values() if count > 1),
        'house_crow_acoustic': names.count('Corvus splendens'),
        'house_crow_geo': counts['Corvus splendens'],
        'domestic_cat_acoustic': names.count('Felis catus'),
        'domestic_cat_geo': counts['Felis catus'],
    }


if __name__ == '__main__':
    if len(sys.argv) != 3:
        sys.exit('Usage: check_geo_labels.py acoustic.csv geo.txt')
    with open(sys.argv[1], encoding='utf-8-sig', newline='') as a, open(sys.argv[2], encoding='utf-8-sig') as g:
        print(json.dumps(compare(a, g), indent=2))
