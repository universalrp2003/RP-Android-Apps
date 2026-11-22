#!/usr/bin/env python3
"""Read ZIP metadata and small READMEs with HTTP ranges, never download the archive.

Only run against the official ZF-AIM research release. No model is executed or installed.
The 26.6 GB archive's hash is not verified by a partial inspection.
"""
import io
import json
import re
import sys
import urllib.request
import zipfile

URL = "https://zenodo.org/records/23024019/files/zf_aim_release.zip?download=1"
BUDGET = 40 * 1024 * 1024
MAX_READ = 32 * 1024 * 1024

class RangeFile(io.RawIOBase):
    def __init__(self, url=URL, opener=urllib.request.urlopen):
        self.url, self.opener, self.pos, self.total_read = url, opener, 0, 0
        data, self.size = self._request(0, 0, None)
        if len(data) != 1 or self.size < 1:
            raise ValueError("Invalid ZIP size or range support")

    def _request(self, start, end, known_total):
        length = end - start + 1
        if length <= 0 or length > MAX_READ or self.total_read + length > BUDGET:
            raise ValueError(f"Range or aggregate read budget exceeded: requested {length} bytes after {self.total_read}, limits {MAX_READ} per call / {BUDGET} total")
        req = urllib.request.Request(self.url, headers={"Range": f"bytes={start}-{end}", "Accept-Encoding": "identity"})
        with self.opener(req, timeout=60) as response:
            if response.status != 206:
                raise ValueError("Server did not honour bounded byte range")
            match = re.fullmatch(r"bytes (\d+)-(\d+)/(\d+)", response.headers.get("Content-Range", ""))
            if not match or tuple(map(int, match.group(1, 2))) != (start, end):
                raise ValueError("Incorrect Content-Range")
            total = int(match.group(3))
            if known_total is not None and total != known_total:
                raise ValueError("Archive changed between requests")
            data = response.read(length + 1)
            if len(data) != length:
                raise ValueError("Incomplete or oversized range")
            self.total_read += length
            return data, total

    def readable(self): return True
    def seekable(self): return True
    def tell(self): return self.pos
    def seek(self, offset, whence=0):
        target = offset if whence == 0 else self.pos + offset if whence == 1 else self.size + offset if whence == 2 else -1
        if target < 0: raise ValueError("Negative seek")
        self.pos = target
        return self.pos
    def read(self, count=-1):
        if self.pos >= self.size: return b""
        count = min(self.size - self.pos, count if count >= 0 else self.size - self.pos)
        if count == 0: return b""
        data, _ = self._request(self.pos, self.pos + count - 1, self.size)
        self.pos += count
        return data


def inspect(fileobj):
    with zipfile.ZipFile(fileobj) as archive:
        infos = archive.infolist()
        if len(infos) > 500000: raise ValueError("Too many entries")
        readmes = [x for x in infos if x.filename.lower().endswith("readme.md") and "zebrafinchmodeling-internal/" in x.filename.lower() and x.file_size <= 200_000 and x.compress_size <= 200_000]
        checkpoints = [x for x in infos if "zf_aim_model_checkpoints/" in x.filename.lower() and not x.is_dir()]
        output = {"archive_entries": len(infos), "checkpoint_files": len(checkpoints),
                  "checkpoint_bytes_uncompressed": sum(x.file_size for x in checkpoints),
                  "largest_checkpoint_entries": [{"path": x.filename, "bytes": x.file_size, "compressed_bytes": x.compress_size} for x in sorted(checkpoints, key=lambda a: a.file_size, reverse=True)[:30]],
                  "readmes": []}
        for info in readmes[:2]:
            text = archive.read(info).decode("utf-8", errors="replace")
            output["readmes"].append({"path": info.filename, "content": text[:120000]})
        return output

if __name__ == "__main__":
    try:
        with RangeFile() as f:
            result = inspect(f)
            result.update(archive_url=URL, archive_size=f.size, network_bytes=f.total_read,
                          caveat="Only byte ranges were read; archive digest, checkpoint provenance and execution are not verified.")
        print(json.dumps(result, indent=2, ensure_ascii=False))
    except Exception as exc:
        print(f"Inspection failed: {exc}", file=sys.stderr)
        sys.exit(1)
