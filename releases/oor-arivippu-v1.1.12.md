# Oor Arivippu 1.1.12 — destination data validation (unpublished candidate)

- Destination search, like journey reverse-geocoding, no longer promotes the district field into a town name. Preserve the person's query when no actual town/feature is provided.
- Show the explicit district role even when town/locality/district share a name, instead of losing it during label deduplication.
- Reject non-finite and out-of-range coordinates before distance/arrival calculations.
- Retain legitimate route returns, movement controls, journey district speech/history, saved destinations and history. Compatible owner-key **update** intended; no uninstall or clear-data step. Candidate/code17 is separate from1.1.11 and public1.1.10, with no new permissions.
- Deterministic tests/CI do not prove real geocoder quality or a physical district crossing. No publication or phone acceptance claimed.

These exact new bytes are **not installed on a phone**. Uninstalling may erase local data; preserve the existing installation and backups.

## தமிழ்

இலக்குத் தேடலில் மாவட்டத்தை ஊர் பெயராகப் பயன்படுத்தாது. ஊரும் மாவட்டமும் ஒரே பெயரில் இருந்தாலும் மாவட்டம் என்ற விவரம் விடுபடாது. தவறான அட்சரேகை/தீர்க்கரேகையைத் தூரம் கணக்கிடும் முன் நிராகரிக்கும். பயண அமைப்புகள், இலக்குகள் மற்றும் வரலாறு தொடரும். இது வெளியிடப்படாத சோதனைப் பதிப்பு; செயலியை நீக்கவோ தரவை அழிக்கவோ வேண்டாம். உண்மையான மாவட்ட எல்லை மாற்றம் கைபேசியில் இன்னும் சோதிக்கப்பட வேண்டும்.
