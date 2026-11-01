#!/usr/bin/env python3
"""Compare upstream short/long synthesis without any private note or runtime fallback."""
import asyncio
import json
import time
import edge_tts

async def main():
    for voice, sentence in [('ta-MY-SuryaNeural', 'வணக்கம். இது செயற்கை நீண்ட குறிப்பு சோதனை. '),
                            ('en-IN-PrabhatNeural', 'Hello. This is a synthetic long note playback test. ')]:
        for length in (len(sentence), 900):
            text = (sentence * 30)[:length]
            received = 0
            started = time.monotonic()
            async def receive():
                nonlocal received
                async for part in edge_tts.Communicate(text, voice).stream():
                    if part['type'] == 'audio': received += len(part['data'])
                    assert received < 2_000_000
            result = dict(voice=voice, chars=len(text))
            try:
                await asyncio.wait_for(receive(), 35)
                result['ok'] = received > 1500
            except Exception as e:
                result.update(ok=False, errorClass=type(e).__name__)
                if isinstance(getattr(e, 'status', None), int): result['httpStatus'] = e.status
            result.update(bytes=received, elapsedMs=round((time.monotonic()-started)*1000))
            print('::notice title=Independent note HD reference::'+json.dumps(result), flush=True)

asyncio.run(main())
