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

    def test_notes_can_be_searched_and_edited_without_changing_ids(self):
        self.assertIn("notes.filter { it.text.contains(query, ignoreCase = true) }", MAIN)
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
