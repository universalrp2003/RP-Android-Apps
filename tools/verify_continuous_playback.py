#!/usr/bin/env python3
"""Source wiring regression gate, not a substitute for JVM, APK or phone tests."""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
PATHS = {
    'podcast': 'podcast/src/main/java/com/universalrp/podcast/PodcastPlaybackService.kt',
    'podcast_ui': 'podcast/src/main/java/com/universalrp/podcast/MainActivity.kt',
    'downloads': 'podcast/src/main/java/com/universalrp/podcast/OfflineDownloadsScreen.kt',
    'pulse': 'equalizer/src/main/java/com/universalrp/pulseeq/audio/PlayerEngine.kt',
    'pulse_ui': 'equalizer/src/main/java/com/universalrp/pulseeq/ui/PlayerScreen.kt',
    'radio': 'radio/src/main/java/com/universalrp/tamilnadufm/MainViewModel.kt',
    'radio_service': 'radio/src/main/java/com/universalrp/tamilnadufm/player/PlaybackService.kt',
    'links': 'shared/src/main/java/com/universalrp/updates/SisterApps.kt',
}

def sources():
    return {key: (ROOT / path).read_text() for key, path in PATHS.items()}

def verify(data):
    if set(data) != set(PATHS) or any(not value.strip() for value in data.values()):
        raise ValueError('Missing playback sources; refusing to verify nothing')
    def need(key, text):
        if text not in data[key]:
            raise ValueError(f'{key}: required wiring missing: {text}')
    need('podcast', 'if (!store.continuous || !stepQueue(1)) finishPlayback(save = false)')
    need('podcast', 'play(entry.url, entry.title, 0, store.queueOfflineOnly)')
    need('podcast', 'override fun onSkipToNext()')
    need('podcast_ui', 'store.queueOfflineOnly')
    need('podcast_ui', 'queue = show.episodes.map')
    need('downloads', 'rows.filter { it.status == PodcastDownloads.Status.READY }')
    need('pulse', 'runPipeline(uri)')
    need('pulse', 'if (stopRequested || state != State.FINISHED) break')
    need('pulse', 'PlaybackOrder.next(queue.size, queueIndex, repeatMode, randomMode, played)')
    if re.search(r'state = State.FINISHED\s+onStateChanged', data['pulse']):
        raise ValueError('Pulse completion callback runs before decoder cleanup')
    need('pulse', 'if (!continuous) { state = State.FINISHED; onStateChanged?.invoke(state); break }')
    need('radio_service', 'player?.pauseAtEndOfMediaItems = local && !prefs.getBoolean("continuous", true)')
    need('pulse_ui', 'ActivityResultContracts.OpenMultipleDocuments()')
    need('radio', 'c.shuffleModeEnabled = _state.value.localShuffle')
    need('radio', 'c.repeatMode = _state.value.localRepeat')
    need('radio', 'c.shuffleModeEnabled = false')
    need('radio_service', 'player?.shuffleModeEnabled = local &&')
    need('links', '${app.tagPrefix}-v${app.publishedVersion}')
    print('PASS: nonempty podcast/offline, PulseEQ, radio and published-link wiring contracts (source only)')

if __name__ == '__main__':
    verify(sources())
