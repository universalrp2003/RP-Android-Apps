"""Layout source contracts; native geometry tests validate the actual Compose component."""
from pathlib import Path
import unittest
ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT/'oorarivippu/src/main/java/com/oorarivippu/voiceannouncer'
class StableOorLayout(unittest.TestCase):
    def test_value_slots_do_not_autosize_or_expand_with_content(self):
        s=(SRC/'StableTripLayout.kt').read_text()
        for text in ('Modifier.weight(.44f)', 'Modifier.weight(.56f)', 'minLines = 2, maxLines = 2',
                     'density.fontScale', 'SelectionContainer', 'verticalScroll', 'View full value', 'modifier.height(slotHeight)', 'rememberTextMeasurer'):
            self.assertIn(text,s)
        self.assertNotIn('TextAlign.End',s)
        self.assertNotIn('autoSize',s)
    def test_dynamic_rows_and_reset_keep_their_place(self):
        s=(SRC/'MainActivity.kt').read_text()
        self.assertIn('StableTripField(ui.currentDistrictLabel, state.currentDistrict.ifBlank',s)
        self.assertNotIn('if (state.currentDistrict.isNotBlank())',s)
        self.assertNotIn('if (state.sessionTraveledMeters > 0f)',s)
        self.assertIn('enabled = state.sessionTraveledMeters > 0f',s)
        self.assertIn('StableTripField(ui.destinationFieldLabel, state.destinationName',s)
    def test_age_and_estimate_slots_survive_invalid_or_stale_fixes(self):
        s=(SRC/'TripProgress.kt').read_text()
        self.assertNotIn('if(meters != null) Text',s)
        for text in ('StableTripField(words("Fix age"','StableTripField(words("Time at current speed"',
                     'Last known:', 'takeIf { fresh }','not road/traffic ETA'):
            self.assertIn(text,s)
