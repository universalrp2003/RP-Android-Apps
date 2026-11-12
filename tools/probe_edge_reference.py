#!/usr/bin/env python3
"""Monitoring-only upstream comparison using synthetic text; never a production fallback.

Run in the same CI runner as the Kotlin probe. Records bytes/error class, not raw
speech, user SMS, credentials, URLs or a conclusion about owner-device behavior.
"""
import asyncio
import hashlib
import importlib.metadata
import json
import time
from pathlib import Path

PROFILES = [
    ('ta-MY-SuryaNeural', 'வணக்கம்! குறுஞ்செய்தி காவலன் தயாராக உள்ளது.'),
    ('ta-IN-PallaviNeural', 'வணக்கம்! குறுஞ்செய்தி காவலன் தயாராக உள்ளது.'),
    ('en-IN-PrabhatNeural', 'SMS Guard online voice protection active.'),
    ('en-IN-NeerjaNeural', 'SMS Guard online voice protection active.'),
]


async def sample(voice, text):
    import edge_tts
    started = time.monotonic()
    audio = bytearray()

    async def receive():
        async for item in edge_tts.Communicate(text=text, voice=voice).stream():
            if item['type'] == 'audio':
                audio.extend(item['data'])
                if len(audio) > 2_000_000:
                    raise ValueError('Synthetic reference response exceeds safety bound')

    result = {'voice': voice, 'bytes': 0, 'status': 'FAIL'}
    try:
        await asyncio.wait_for(receive(), timeout=30)
        if len(audio) <= 1500:
            raise ValueError('No complete reference audio larger than 1500 bytes')
        result.update(status='PASS', sha256=hashlib.sha256(audio).hexdigest())
    except Exception as error:
        # Avoid embedding URLs, request headers, cookie/GEC values or traces in annotations.
        result['errorClass'] = type(error).__name__
        status = getattr(error, 'status', None)
        if isinstance(status, int):
            result['httpStatus'] = status
    finally:
        result.update(bytes=len(audio), elapsedMs=round((time.monotonic()-started)*1000))
    return result


async def main():
    report = {'referencePackage': 'edge-tts', 'version': importlib.metadata.version('edge-tts'),
              'scope': 'CI synthetic monitoring only; not owner runtime or service recovery proof',
              'profiles': []}
    # Advertised voice metadata is not audio availability. Preserve the distinction.
    import edge_tts
    try:
        advertised = await asyncio.wait_for(edge_tts.list_voices(), timeout=20)
        report['advertisedTamilIndia'] = [{'voice': v['ShortName'], 'gender': v['Gender']}
                                          for v in advertised if v.get('Locale') == 'ta-IN']
        print('::notice title=Reference advertised voices::' + json.dumps(report['advertisedTamilIndia']))
    except Exception as error:
        report['voiceMetadataErrorClass'] = type(error).__name__
    for voice, text in PROFILES:
        result = await sample(voice, text)
        report['profiles'].append(result)
        print('::notice title=Independent reference probe::' + json.dumps(result))
    Path('edge-reference-report.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n')
    # A failed reference is evidence of failure, never a silently skipped audio success.
    if any(item['status'] != 'PASS' for item in report['profiles']):
        raise SystemExit(1)


if __name__ == '__main__':
    asyncio.run(main())
