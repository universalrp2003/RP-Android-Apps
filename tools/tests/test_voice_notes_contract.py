from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
MAIN = (ROOT / "voicenotes/src/main/java/com/universalrp/voicenotes/MainActivity.kt").read_text(encoding="utf-8")
SPEAKER = (ROOT / "voicenotes/src/main/java/com/universalrp/voicenotes/VoiceNoteSpeaker.kt").read_text(encoding="utf-8")


class VoiceNotesContractTest(unittest.TestCase):
    def test_first_hd_playback_requires_clear_network_disclosure(self):
        self.assertIn("if (showVoicePrivacy)", MAIN)
        self.assertIn("Only this note's text will be sent to an online service", MAIN)
        self.assertIn("இந்தக் குறிப்பின் உரை மட்டும் HD குரல்", MAIN)
        self.assertIn('"hd_voice_disclosure_acknowledged"', MAIN)

    def test_hd_voice_remains_the_only_playback_and_never_silently_falls_back(self):
        self.assertIn("speaker.speak(note.text, voiceId, pitch)", MAIN)
        self.assertIn("No robotic voice fallback is used.", MAIN)
        self.assertNotIn("android.speech.tts.TextToSpeech", MAIN)
        self.assertIn("SharedNeuralVoiceEngine.synthesizeRawMp3", SPEAKER)

    def test_owner_requested_search_removed_but_edit_preserves_ids(self):
        self.assertNotIn("searchQuery", MAIN)
        self.assertNotIn('"Search notes"', MAIN)
        self.assertIn("items(notes, key = { it.id })", MAIN)
        self.assertLess(MAIN.index("// ---------------------------------------------------------------- microphone"), MAIN.index("// ---------------------------------------------------------------- the notes"))
        self.assertIn("NoteStore.update(notes, note.id, editText)", MAIN)
        self.assertIn("onEdit =", MAIN)

    def test_failed_note_write_is_reported_and_temp_file_is_used(self):
        self.assertIn("voice-notes.json.tmp", MAIN)
        self.assertIn("StandardCopyOption.ATOMIC_MOVE", MAIN)
        self.assertIn("Could not save the note.", MAIN)

    def test_recognition_provider_is_not_misrepresented_as_local(self):
        self.assertIn("audio handling depends on that provider", MAIN)
        self.assertIn("அதன் தரவு கையாளுதல் வழங்குநரைப் பொறுத்தது", MAIN)


if __name__ == "__main__":
    unittest.main()


class UpdateResultVisibilityTest(unittest.TestCase):
    def test_check_reports_result_in_visible_dialog(self):
        notice = (ROOT / "shared/src/main/java/com/universalrp/updates/StoreFirstUpdateNotice.kt").read_text()
        self.assertIn("showResult = manual", notice)
        self.assertIn("val manual = requested > 0", notice)
        self.assertIn('getBoolean("daily_in_app", false)', notice)
        self.assertIn("UpdateReminderPolicy.shouldCheck", notice)
        self.assertIn("Skip version", notice)
        self.assertIn("if (showResult) AlertDialog(", notice)
        self.assertIn("Update check unavailable; try later", notice)
        self.assertIn("No newer stable version found", notice)
        self.assertIn("Open Store / website", notice)

class PlaybackControlSourceTest(unittest.TestCase):
    def test_active_button_stops_and_other_note_replaces_request(self):
        self.assertIn('if (speaking) Icons.Filled.Stop else Icons.Filled.PlayArrow', MAIN)
        self.assertIn('Stop playback', MAIN)
        self.assertIn('val wasPlaying = speakingId == note.id', MAIN)
        self.assertIn('playbackJob?.cancel()', MAIN)
        self.assertIn('request == playbackRequest', MAIN)
        self.assertNotIn('if (speakingId != null) return', MAIN)
        self.assertIn('runInterruptible(Dispatchers.IO)', SPEAKER)
        self.assertIn('playbackMutex.withLock', SPEAKER)
        self.assertIn('generation.get() != requestGeneration', SPEAKER)

class NewNoteVisibilityTest(unittest.TestCase):
    def test_prepend_reveals_new_note_without_edit_delete_scroll_jumps(self):
        self.assertIn('val notesListState = rememberLazyListState()', MAIN)
        self.assertIn('state = notesListState', MAIN)
        self.assertIn('LaunchedEffect(newlyAddedNoteId)', MAIN)
        self.assertIn('notesListState.scrollToItem(0)', MAIN)
        self.assertIn('notes.firstOrNull()?.id == added', MAIN)
        self.assertEqual(1, MAIN.count('newlyAddedNoteId = note.id'))
        self.assertIn('persist(NoteStore.add(notes, note))\n                newlyAddedNoteId = note.id', MAIN)
