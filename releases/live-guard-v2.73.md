# Live Guard 2.73

## What changed

- **The speaker symbol on the home screen is now a real switch.** It already showed whether voice was
  on through its shape and colour, but tapping it only opened the volume dialog — it reported the
  state without changing it, and the actual master switch sat on the Voice screen three navigations
  away. One tap now turns every spoken announcement on or off: the choice is saved, anything already
  being read out stops immediately, and the health-watch schedule is re-synced so turning voice off
  genuinely silences the app rather than just recolouring an icon. A long press still opens the
  volume slider, because that is the master level and burying it would trade one problem for another.
- **The same master switch is now a visible row inside the volume panel.** It used to exist only
  behind that gesture, so it now also appears in Settings and on the weather alert screen.
- **A one-time explainer for "HD voice only".** The first time the owner opens the voice screen they
  see a short, dismissible note saying why there is no offline robot-voice fallback: when the HD
  voice is unreachable Live Guard waits and then announces, rather than quietly switching to a
  different voice or dropping the announcement. It is shown once, and the dismissal is written
  through to disk immediately so it survives the process being killed.
- `0` new permissions over `v2.72` — the manifest is unchanged.

## Why it is built this way

The explainer is deliberately dismissible and deliberately permanent. It answers a question the
owner has actually asked — why is there no second, quieter voice? — and an explanation that cannot be
dismissed is an explanation the owner learns to stop reading. The dismissal is stored with `commit()`
rather than `apply()` because a read-back of an unsaved preference is the failure that shows up as
"it came back", and the write happens on the tap rather than at some later save point.

The home-screen switch is the owner's own words: one voice symbol, one tap. Both halves of it are
covered by tests — the explainer's shown-once contract, its survival across a new preferences
instance, and the fact that a read never writes.

## Device test

Not phone-installed for this release. The owner has 2.72 installed and running, and the voice on/off
button described above was built and compile-verified after that install, so its exact bytes have
not been on a phone. The one-time explainer has never been seen on a screen at all. Both are small,
self-contained changes to the voice path the owner already exercises daily, but neither has been
looked at.

## தமிழ்

இந்தப் பதிவுக்கு இரண்டு மாற்றங்கள் உள்ளன. முகப்புத் திரையில் உள்ள குரல் சின்னம் இப்போது நிச்சயமான ஒரு நிலைமாற்றி. அதைத் தொட்டால் எல்லா பேசும் அறிவிப்புகளும் அமைதியாகும், மீண்டும் தொட்டால் மீண்டும் பேசும். தேர்ந்தெடுத்தால் அது சேமிக்கப்படும், ஏற்கணவே பேசப்பட்டுக் கொண்டிருக்கும் குரல் உடனே நிற்கும். நீண்ட நேரம் தொட்டால் ஒலி அளவு பெட்டி திறக்கும். இரண்டாவது, HD குரல் மட்டுமே என்பதை ஒரு முறை விளக்கும் குறுகிய குறிப்பு தோன்றும். குரல் கிடைக்காதப்போது வேறு ஒரு குரளுக்கு மாறுவதில்லை; காத்திருந்து பேசும். ஒரு முறை காட்டப்பட்ட பிறகு மீண்டும் காட்டப்படாது, மூடியது உடனே சேமிக்கப்படும். புதிய அனுமதிகள் ஏதுவும் இல்லை.
