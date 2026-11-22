#!/usr/bin/env python3
"""Non-commercial research only: cat detection on CatMeows positive clips.

No audio, individual predictions or fitted models are saved. This is not an
intent test, specificity test, Android test or independent evaluation suite.
"""
import argparse
import csv
import io
import json
import zipfile
from collections import Counter
from pathlib import Path

import numpy as np
from ai_edge_litert.interpreter import Interpreter
from scipy.io import wavfile
from scipy.signal import resample_poly

from catmeows_baseline import NAME, EXPECTED_MD5
from probe_birdnet_v3 import MODEL_MD5, LABELS_MD5, digest


def run(model, labels, dataset):
    for p, expected in [(model, MODEL_MD5), (labels, LABELS_MD5), (dataset, EXPECTED_MD5)]:
        if digest(p, 'md5') != expected: raise ValueError('Dataset or model checksum differs from official record')
    with Path(labels).open(encoding='utf-8-sig', newline='') as f: rows = list(csv.DictReader(f))
    matches = [i for i, r in enumerate(rows) if r['scientific_name'].strip().lower() == 'felis catus']
    if len(rows) != 11725 or len(matches) != 1: raise ValueError('Unexpected labels')
    cat_index = matches[0]
    interpreter = Interpreter(model_path=str(model), num_threads=2)
    interpreter.resize_tensor_input(interpreter.get_input_details()[0]['index'], [1, 96000]); interpreter.allocate_tensors()
    input_idx = interpreter.get_input_details()[0]['index']
    score_out = [o for o in interpreter.get_output_details() if np.prod(o['shape']) == len(rows)]
    if len(score_out) != 1: raise ValueError('Unexpected model outputs')
    counts = Counter(); durations = []
    with zipfile.ZipFile(dataset) as z:
        entries = [i for i in z.infolist() if not i.is_dir()]
        if len(entries) != 440 or sum(i.file_size for i in entries) > 30_000_000: raise ValueError('Unexpected dataset size')
        for info in entries:
            if not NAME.fullmatch(info.filename.rsplit('/', 1)[-1]) or info.file_size > 500_000:
                raise ValueError('Unexpected data entry')
            rate, wave = wavfile.read(io.BytesIO(z.read(info)))
            if rate != 8000 or wave.ndim != 1 or wave.dtype != np.int16 or not 256 <= wave.size <= 8000*30:
                raise ValueError('Unexpected audio format')
            durations.append(len(wave)/rate)
            audio = resample_poly(wave.astype(np.float32) / 32768, 4, 1)
            # First 3 s only; right-pad short samples with digital silence.
            sample = np.zeros(96000, np.float32); n = min(len(sample), len(audio)); sample[:n] = audio[:n]
            if np.var(sample) == 0: raise ValueError('Constant input')
            interpreter.set_tensor(input_idx, sample[None, :]); interpreter.invoke()
            scores = interpreter.get_tensor(score_out[0]['index']).reshape(-1)
            if not np.isfinite(scores).all(): raise ValueError('Non-finite scores')
            score = float(scores[cat_index]); counts['clips'] += 1
            counts['cat_top_1'] += int(int(np.argmax(scores)) == cat_index)
            counts['cat_top_5'] += int(int(np.sum(scores > score)) < 5)
            # This is an illustrative fixed threshold, not a calibrated confidence.
            counts['cat_score_at_least_0_35'] += int(score >= .35)
    return {'counts': dict(counts), 'duration_min_seconds': round(min(durations), 3), 'duration_max_seconds': round(max(durations), 3),
            'procedure': '8kHz mono CatMeows, resampled to 32kHz, first 3 seconds, zero padded when short',
            'warning': 'Positive meows only. Not specificity, balanced accuracy, intent, reply, Android, field or calibrated threshold validation.'}


if __name__ == '__main__':
    p = argparse.ArgumentParser(); p.add_argument('model'); p.add_argument('labels'); p.add_argument('dataset'); a = p.parse_args()
    print(json.dumps(run(a.model, a.labels, a.dataset), indent=2))
