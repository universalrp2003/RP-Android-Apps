# Ramesh Radio 1.26

## What changed

- The saved-places row is now labelled and shown from the very first place you save. Until now the
  row only appeared once you had two or more places, so with one place the list was invisible and
  there was no way to tell a growing list from a single place that kept changing.
- Saved places are now written to storage synchronously. The write was asynchronous, so Android
  could kill the process before it reached disk and every place except the most recent one was
  silently lost on the next launch.
- The list behaviour itself (a new place goes in front, re-picking a saved place moves it instead of
  duplicating it, the list is capped, removing a place leaves the rest) is now covered by unit
  tests. Previously only the on-disk encoding was tested.

## Device test

Not phone-installed for this release. The changes were reviewed and the new list tests were written,
but 1.26 has not been installed on a phone, so the labelled saved-places row and the synchronous
write are not verified on a device.

## Why this release exists

A defect report said that adding another place "just replaces" the saved place instead of adding to
the list. The list logic was audited and is correct: `add` reads the saved list, removes any entry
with the same rounded coordinates, puts the new place at the front, caps the list at twelve and
writes it back, and the key is the rounded latitude/longitude so the same town picked twice is one
chip however it is spelled. The two gaps were that the list was not visible until the second place
and that the write could be lost, so this release closes both and pins the list algebra with tests.

If the list still does not grow after installing 1.26, the labelled row will show a single chip
whose name changes, and a screenshot of that row is what is needed to diagnose it further.

## தமிழ்

சேமித்த ஊர்கள் வரிசை இப்போது முதல் ஊரிலிருந்தே தலைப்புடன் காட்டப்படும். சேமித்த ஊர்கள் ஒரே சேமிப்பாக இருந்ததால் வரிசை தெரியாததால், புதிய ஊர் சேமித்தால் பழையது மாற்றப்பட்டது வென்று தோன்றியிருக்கலாம். சேமிப்பு இப்போது உடனடியாக எழுதப்படுகிறது; முன்பு தாமதமாக எழுதப்பட்டதால் சில ஊர்கள் அழியக்கூடியிருந்தன. புதிய ஊர் முன்னால் வரும், அதே ஊரை மீண்டும் தேர்ந்தெடுத்தால் நகல் ஆகாமல் நகரும், வரிசை 12 ஊர்களுக்கு மட்டுமே, நீக்கும் போது மற்ற ஊர்கள் அப்படியே இருப்பதால் சோதனை சேர்க்கப்பட்டது.
