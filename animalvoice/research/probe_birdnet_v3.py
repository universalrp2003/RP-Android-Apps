#!/usr/bin/env python3
"""Research-only host probe of BirdNET+ 3.0 FP16 pruned; no app integration.

Uses deterministic synthetic audio only; never records a microphone. A successful
run is compatibility evidence, not species-identification accuracy.
"""
import argparse
import csv
import hashlib
import json
import time
from pathlib import Path

import numpy as np
from ai_edge_litert.interpreter import Interpreter

MODEL_MD5 = '3efd27ebf17a14996035564c4f4b944e'
LABELS_MD5 = 'bb25847af8a581e1382081382f01e762'


def digest(path, name):
    h = hashlib.new(name)
    with Path(path).open('rb') as f:
        for chunk in iter(lambda: f.read(1 << 20), b''): h.update(chunk)
    return h.hexdigest()


def run(model, labels):
    if digest(model, 'md5') != MODEL_MD5 or digest(labels, 'md5') != LABELS_MD5:
        raise ValueError('Files differ from the published v3.0 checksums')
    with Path(labels).open(encoding='utf-8-sig', newline='') as f: rows = list(csv.DictReader(f))
    if len(rows) != 11725 or not all('common_name' in r and 'scientific_name' in r for r in rows):
        raise ValueError('Unexpected labels schema or count')
    interpreter = Interpreter(model_path=str(model), num_threads=2)
    ins = interpreter.get_input_details()
    if len(ins) != 1: raise ValueError('Unexpected input count')
    interpreter.resize_tensor_input(ins[0]['index'], [1, 96000]); interpreter.allocate_tensors()
    ins = interpreter.get_input_details(); outs = interpreter.get_output_details()
    if ins[0]['dtype'] != np.float32: raise ValueError('Unexpected input dtype')
    by_size = {int(np.prod(o['shape'])): o for o in outs}
    if len(outs) != 2 or set(by_size) != {11725, 1280}: raise ValueError('Unexpected output tensors')
    t = np.arange(96000, dtype=np.float32) / 32000
    samples = {'tone_1000hz': (0.2 * np.sin(2 * np.pi * 1000 * t)).astype(np.float32),
               'noise': np.random.default_rng(2026).normal(0, 0.05, len(t)).astype(np.float32)}
    outputs = {}
    for name, audio in samples.items():
        interpreter.set_tensor(ins[0]['index'], audio.reshape(1, -1))
        before = time.monotonic(); interpreter.invoke(); seconds = time.monotonic() - before
        scores = interpreter.get_tensor(by_size[11725]['index']).reshape(-1)
        embeddings = interpreter.get_tensor(by_size[1280]['index']).reshape(-1)
        if not np.isfinite(scores).all() or not np.isfinite(embeddings).all(): raise ValueError('Non-finite output')
        if np.min(scores) < 0 or np.max(scores) > 1: raise ValueError('Scores outside expected range')
        outputs[name] = {'elapsed_seconds': round(seconds, 3), 'score_max': float(scores.max()),
                         'finite_scores': int(scores.size), 'finite_embeddings': int(embeddings.size)}
    needles = ('corvus splendens', 'bos taurus', 'canis lupus familiaris', 'felis catus', 'hemidactylus frenatus', 'rattus rattus')
    coverage = {s: [r['common_name'] for r in rows if r['scientific_name'].strip().lower() == s] for s in needles}
    return {'model_sha256': digest(model, 'sha256'), 'labels_sha256': digest(labels, 'sha256'),
            'label_count': len(rows), 'inputs': [1, 96000], 'output_sizes': sorted(by_size),
            'synthetic': outputs, 'exact_scientific_name_matches': coverage,
            'warning': 'Desktop synthetic execution only. No accuracy, Android compatibility, semantics or reply established.'}


if __name__ == '__main__':
    p = argparse.ArgumentParser(); p.add_argument('model'); p.add_argument('labels'); args = p.parse_args()
    print(json.dumps(run(Path(args.model), Path(args.labels)), indent=2))
