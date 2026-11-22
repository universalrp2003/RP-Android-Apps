#!/usr/bin/env python3
"""Research-only context baseline. Never predicts intent or runs in the app.

Input is the unmodified CatMeows v1.0.2 dataset.zip, supplied separately.
Individual cats are kept disjoint between train/test. No model or audio is saved.
"""
import argparse
import hashlib
import io
import json
import re
import zipfile
from collections import Counter

import numpy as np
from scipy.io import wavfile
from scipy.signal import stft
from sklearn.dummy import DummyClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score, balanced_accuracy_score, confusion_matrix, f1_score
from sklearn.model_selection import GroupKFold
from sklearn.pipeline import make_pipeline
from sklearn.preprocessing import StandardScaler

EXPECTED_MD5 = 'b5fa911bcd6514e39dfef6876f747df4'
NAME = re.compile(r'^([BFI])_([A-Za-z0-9]+)_(MC|EU)_(FI|FN|MI|MN)_([A-Za-z0-9]+)_([123])([0-9]{2})\.wav$', re.I)
LABELS = ('B', 'F', 'I')


def features(rate, audio):
    if rate != 8000 or audio.ndim != 1 or audio.size < 256 or audio.size > 8000 * 30:
        raise ValueError('Unexpected audio format or length')
    if audio.dtype.kind not in 'iu':
        raise ValueError('Expected PCM integer audio')
    x = audio.astype(np.float64) / max(abs(np.iinfo(audio.dtype).min), np.iinfo(audio.dtype).max)
    _, _, spectrum = stft(x, fs=rate, nperseg=256, noverlap=128, boundary=None)
    power = np.abs(spectrum) ** 2
    # Broad spectral fractions: simple research baseline, not a validated call model.
    freq = np.linspace(0, rate / 2, power.shape[0]); energy = power.sum(axis=0) + 1e-12
    bands = [np.log(np.maximum(power[(freq >= lo) & (freq < hi)].sum(axis=0) / energy, 1e-12))
             for lo, hi in ((0, 250), (250, 500), (500, 1000), (1000, 2000), (2000, 4001))]
    centroid = (freq[:, None] * power).sum(axis=0) / energy
    return np.array([len(x)/rate, np.sqrt(np.mean(x*x)), np.mean(np.signbit(x[1:]) != np.signbit(x[:-1])),
                     np.mean(centroid), np.std(centroid)] + [v for b in bands for v in (np.mean(b), np.std(b))])


def read_dataset(blob, verify=True):
    if verify and hashlib.md5(blob).hexdigest() != EXPECTED_MD5:
        raise ValueError('Archive differs from published CatMeows checksum')
    if len(blob) > 20_000_000: raise ValueError('Archive exceeds limit')
    rows = []
    with zipfile.ZipFile(io.BytesIO(blob)) as z:
        files = [i for i in z.infolist() if not i.is_dir()]
        if len(files) > 500 or sum(i.file_size for i in files) > 30_000_000:
            raise ValueError('Archive contents exceed limits')
        for info in files:
            name = info.filename.rsplit('/', 1)[-1]
            match = NAME.fullmatch(name)
            if not match: raise ValueError('Unexpected archive entry: ' + name)
            if info.file_size > 500_000: raise ValueError('Unexpectedly large WAV')
            rate, audio = wavfile.read(io.BytesIO(z.read(info)))
            rows.append((features(rate, audio), match[1].upper(), match[2].upper()))
    if verify and (len(rows) != 440 or len({r[2] for r in rows}) != 21):
        raise ValueError('Unexpected sample or cat count')
    return rows


def evaluate(rows):
    X = np.stack([r[0] for r in rows]); y = np.array([r[1] for r in rows]); groups = np.array([r[2] for r in rows])
    predictions = np.empty_like(y); baseline = np.empty_like(y); folds = []
    for train, test in GroupKFold(n_splits=5).split(X, y, groups):
        assert set(groups[train]).isdisjoint(set(groups[test]))
        model = make_pipeline(StandardScaler(), LogisticRegression(max_iter=2000, class_weight='balanced', random_state=0))
        model.fit(X[train], y[train]); predictions[test] = model.predict(X[test])
        dummy = DummyClassifier(strategy='most_frequent').fit(X[train], y[train]); baseline[test] = dummy.predict(X[test])
        folds.append({'train_cats': len(set(groups[train])), 'test_cats': len(set(groups[test])), 'test_clips': len(test)})
    return {'clips': len(y), 'cats': len(set(groups)), 'classes': dict(Counter(y)), 'folds': folds,
            'feature_baseline': {'accuracy': accuracy_score(y, predictions),
                'balanced_accuracy': balanced_accuracy_score(y, predictions), 'macro_f1': f1_score(y, predictions, labels=LABELS, average='macro', zero_division=0),
                'confusion_rows_actual_B_F_I': confusion_matrix(y, predictions, labels=LABELS).tolist()},
            'most_frequent_baseline_accuracy': accuracy_score(y, baseline),
            'caveat': 'Experimental contexts for 21 cats, not emotions or intent; grouped cross-validation is not external or phone validation.'}


def main():
    parser = argparse.ArgumentParser(description=__doc__); parser.add_argument('zipfile'); args = parser.parse_args()
    with open(args.zipfile, 'rb') as f: blob = f.read(20_000_001)
    print(json.dumps(evaluate(read_dataset(blob)), indent=2))

if __name__ == '__main__': main()
