# Elderly Launcher (முதியவர் திரை)

A home screen with eight very large buttons and nothing else. No small text, no menus, no settings.

Built as part of the RP Ideas suite. Package `com.universalrp.launcher`, version 1.1.

## First launch

On first launch the app offers to become the phone's home screen. Pressing the button opens Android's own home-app chooser with "முதியவர் திரை" in it. There is nothing to configure and nothing to explain in a settings screen, because the system picker is the explanation. It asks once and never again.

Version 1.0 did not do this. It registered itself as a home-screen option and said nothing, so the only way to use it was Settings > Default Home screen by hand - which the owner had to do, and which nobody who needs this app should have to do.

## The home screen

Eight buttons in a fixed order, then a red emergency button on its own, then the list of every installed app:

| Button | What it does |
|---|---|
| தொலைபேசி | opens the dialler |
| செய்திகள் | opens the messaging app |
| கேமரா | opens the camera |
| தொடர்புகள் | opens contacts |
| விளக்கு | toggles the camera flash |
| ஒலி அதிகரிப்பு | raises the media volume |
| ஒலி குறைப்பு | lowers the media volume |
| அமைப்பு | opens Android settings |

Then **அவசரம்** (emergency), in red and alone, which asks first and then dials 108; and **எல்லா செயலிகள்**, which lists every installed app.

## One-tap calling for the people who matter

Between the button grid and the emergency button sits a favourites card. Pressing a name dials that person directly - no contact list, no confirmation, no hunting.

- **Up to eight**, laid out to wrap so a long name does not push a button off the edge.
- **Adding one opens the phone's own contact picker**, filtered to phone numbers, so a contact with no number cannot be chosen.
- **Contacts are read from the phone's own provider** and stored on the device. There is no second copy of the address book and nothing is sent anywhere.
- **Numbers are compared without their spaces, dashes and brackets**, so "98400 12345" and "9840012345" are one person and a duplicate cannot be added.
- **Removing matches on the number, not the name**, so renaming a contact on the phone does not orphan its button.

## Why there is no settings screen

The screen is for somebody who does not read small text and does not navigate menus. A screen for configuring a launcher is a screen somebody has to learn to use, and this launcher exists precisely so nobody has to learn anything. So the eight buttons are in the same places every time, and the four most-used ones are nearest the thumb.

Every label is at least 17sp and every button is at least 96dp tall, and the clock is 36sp. Version 1.0 used 20sp labels and a 44sp clock; the owner reported the letters were a little bit big, and 1.1 brings them down while keeping them large by normal standards. The torch and the volume buttons are on the home screen because reaching the real volume slider on a modern Android phone is a gesture, and a gesture is exactly what this screen exists to avoid.

## Failure behaviour

Every button either starts an app or acts on the phone directly, and each one fails quietly: if the phone has no app that answers a button, the screen says so rather than doing nothing. A launcher that crashes when the camera app is missing is worse than no launcher at all, because the phone becomes unusable.

## Permissions

| Permission | Why |
|---|---|
| `QUERY_ALL_PACKAGES` | to list the installed apps. A launcher is the one category Google Play explicitly allows this for. |
| `CALL_PHONE` | to place the emergency and favourite calls directly. Without it the dialler opens with the number filled in instead. |
| `READ_CONTACTS` | only for the favourite contacts the owner picks |
| `FLASHLIGHT`, `VIBRATE` | the torch button |
| `POST_NOTIFICATIONS` | Android 13+ |

No location, storage, camera use of its own, phone state, advertising identifier or accounts.

## Build

```
./gradlew :launcher:testDebugUnitTest :launcher:assembleRelease
```

ARM only, `minSdk` 26, `targetSdk` 36.

## Status

Version 1.1 has **not been installed on a phone**, so it has had no device test. The home screen's shape, the button order and the favourites rules are covered by unit tests. Whether the home-app chooser actually appears on first launch, whether a favourite dials, and whether the smaller text still reads at arm's length are not - and a launcher is the one kind of app that cannot be judged from anything but a phone, because being the home screen is the whole feature.
