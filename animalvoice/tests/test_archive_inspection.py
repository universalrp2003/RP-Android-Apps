import importlib.util
import io
import pathlib
import unittest
import zipfile

path = pathlib.Path(__file__).resolve().parents[1] / 'research' / 'inspect_zf_archive.py'
spec = importlib.util.spec_from_file_location('inspect_zf_archive', path)
m = importlib.util.module_from_spec(spec); spec.loader.exec_module(m)

class FakeResponse:
    def __init__(self, status, headers, data): self.status, self.headers, self.data = status, headers, io.BytesIO(data)
    def __enter__(self): return self
    def __exit__(self, *args): pass
    def read(self, n): return self.data.read(n)

def fake_opener(blob, ignore_range=False):
    def open_(request, timeout):
        if ignore_range: return FakeResponse(200, {}, blob)
        start, end = map(int, request.headers['Range'].removeprefix('bytes=').split('-'))
        return FakeResponse(206, {'Content-Range': f'bytes {start}-{end}/{len(blob)}'}, blob[start:end+1])
    return open_

class RangeInspectionTests(unittest.TestCase):
    def test_lists_models_and_reads_small_readme_without_extracting(self):
        b = io.BytesIO()
        with zipfile.ZipFile(b, 'w', compression=zipfile.ZIP_DEFLATED) as z:
            z.writestr('root/ZebraFinchModeling-Internal/README.md', 'checkpoint documentation')
            z.writestr('root/zf_aim_model_checkpoints/model.pt', b'weights')
            z.writestr('root/ZebraFinchModeling-Internal/scripts/for_live/run.pregenerate_calls.summary_model.sh', 'echo research')
            z.writestr('root/StreamPlayback/README.md', 'stream instructions')
        f = m.RangeFile('https://example.invalid/file', fake_opener(b.getvalue()))
        result = m.inspect(f)
        self.assertEqual(result['checkpoint_files'], 1)
        self.assertEqual(result['largest_checkpoint_entries'][0]['bytes'], 7)
        self.assertEqual(result['readmes'][0]['content'], 'checkpoint documentation')
        self.assertEqual([f['content'] for f in result['source_files']], ['echo research', 'stream instructions'])
        self.assertEqual(len(result['live_paths']), 2)
    def test_rejects_server_ignoring_ranges(self):
        with self.assertRaises(ValueError): m.RangeFile('https://example.invalid', fake_opener(b'abcdef', True))
    def test_bounded_reads(self):
        with self.assertRaises(ValueError): m.RangeFile('https://example.invalid', fake_opener(b'abcdef'))._request(0, m.MAX_READ, None)

if __name__ == '__main__': unittest.main()
