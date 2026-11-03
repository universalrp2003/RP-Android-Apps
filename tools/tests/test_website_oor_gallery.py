"""Privacy-reviewed screenshot derivatives only; no private originals required in CI."""
import hashlib
import json
from pathlib import Path
import struct
import unittest
import zipfile
import zlib

ROOT=Path(__file__).resolve().parents[2]
PREFIX='images/oor-arivippu-v1.1.15/'

def png_info(data):
    if not data.startswith(b'\x89PNG\r\n\x1a\n'):raise ValueError('Expected flattened PNG')
    offset=8;types=[];size=None
    while offset<len(data):
        if offset+12>len(data):raise ValueError('Truncated chunk')
        length=struct.unpack('>I',data[offset:offset+4])[0]
        kind=data[offset+4:offset+8];payload=data[offset+8:offset+8+length]
        if offset+12+length>len(data):raise ValueError('Oversized chunk')
        crc=struct.unpack('>I',data[offset+8+length:offset+12+length])[0]
        if zlib.crc32(kind+payload)!=crc:raise ValueError('Wrong PNG CRC')
        if kind not in (b'IHDR',b'IDAT',b'IEND'):raise ValueError('Unreviewed PNG metadata/chunk')
        types.append(kind)
        if kind==b'IHDR':
            w,h,depth,color,compression,filtering,interlace=struct.unpack('>IIBBBBB',payload)
            if (depth,color,compression,filtering,interlace)!=(8,2,0,0,0):raise ValueError('Expected flat RGB, not alpha/layers')
            size=(w,h)
        offset+=length+12
    if not types or types[0]!=b'IHDR' or types[-1]!=b'IEND' or types.count(b'IHDR')!=1 or types.count(b'IEND')!=1 or b'IDAT' not in types:raise ValueError('Invalid image structure')
    return size

class RedactedOorGallery(unittest.TestCase):
    def setUp(self):
        self.review=json.loads((ROOT/'docs/oor-family-1.1.15-gallery-review.json').read_text())
    def test_only_owner_authorized_reviewed_derivatives(self):
        r=self.review
        for field in ('owner_authorized','visual_review_completed','opaque_masks_verified'):self.assertIs(r[field],True)
        for field in ('apk_rebuilt','store_apk_changed','app_store_submission'):self.assertIs(r[field],False)
        self.assertEqual(len(r['images']),4)
        self.assertEqual([len(im['opaque_masks']) for im in r['images']],[3,2,0,2])
        expected={im['website'] for im in r['images']}|{r['artwork_archive']['website']}
        actual={str(p.relative_to(ROOT/'site')) for p in (ROOT/'site'/PREFIX).iterdir()}
        self.assertEqual(actual,expected,'no original JPEGs or scratch images')
    def test_exact_rgb_images_no_exif_text_gps_or_transparency(self):
        for im in self.review['images']:
            with self.subTest(image=im['website']):
                self.assertTrue(im['website'].startswith(PREFIX))
                data=(ROOT/'site'/im['website']).read_bytes()
                self.assertEqual((len(data),hashlib.sha256(data).hexdigest()),(im['bytes'],im['sha256']))
                self.assertEqual(png_info(data),(im['width'],im['height']))
                for a,b,c,d in im['opaque_masks']:
                    self.assertTrue(0<=a<c<im['width'] and 0<=b<d<im['height'])
    def test_artwork_zip_contains_only_same_redacted_pngs_and_readme(self):
        archive=self.review['artwork_archive'];p=ROOT/'site'/archive['website'];data=p.read_bytes()
        self.assertEqual((len(data),hashlib.sha256(data).hexdigest()),(archive['bytes'],archive['sha256']))
        with zipfile.ZipFile(p) as z:
            self.assertEqual(z.namelist(),[Path(im['website']).name for im in self.review['images']]+['README.txt'])
            for im in self.review['images']:
                self.assertEqual(z.read(Path(im['website']).name),(ROOT/'site'/im['website']).read_bytes())
            self.assertIn('Originals are not included',z.read('README.txt').decode())
    def test_guide_and_listing_use_only_reviewed_new_image_paths(self):
        guide=(ROOT/'site/oor-arivippu-guide.html').read_text();site=(ROOT/'site/index.html').read_text()
        for im in self.review['images']:
            self.assertIn(im['website'],guide);self.assertIn(im['website'],site)
        self.assertIn(self.review['artwork_archive']['website'],guide)
        self.assertIn('partially hidden with solid masks',guide)
        self.assertIn('oor-family-1.1.15-gallery-review.json',(ROOT/'tools/verify_live_three_releases.py').read_text())
    def test_metadata_or_non_png_rejected(self):
        data=(ROOT/'site'/self.review['images'][0]['website']).read_bytes()
        payload=b'private metadata';kind=b'tEXt'
        chunk=struct.pack('>I',len(payload))+kind+payload+struct.pack('>I',zlib.crc32(kind+payload))
        for bad in (b'jpeg',data[:-5],data[:8]+chunk+data[8:]):
            with self.assertRaises(ValueError):png_info(bad)
