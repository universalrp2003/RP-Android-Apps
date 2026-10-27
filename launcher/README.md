# Elderly Launcher (முதியவர் திரை)

A home screen with eight very large buttons and nothing else. No small text, no menus, no settings.

Built as part of the RP Ideas suite. Package `com.universalrp.launcher`, version 1.0.

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

## Why there is no settings screen

The screen is for somebody who does not read small text and does not navigate menus. A screen for configuring a launcher is a screen somebody has to learn to use, and this launcher exists precisely so nobody has to learn anything. So the eight buttons are in the same places every time, and the four most-used ones are nearest the thumb.

Every label is at least 18sp, every button is at least 96dp tall, and the clock is 44sp. The torch and the volume buttons are on the home screen because reaching the real volume slider on a modern Android phone is a gesture, and a gesture is exactly what this screen exists to avoid.

## Failure behaviour

Every button either starts an app or acts on the phone directly, and each one fails quietly: if the phone has no app that answers a button, the screen says so rather than doing nothing. A launcher that crashes when the camera app is missing is worse than no launcher at all, because the phone becomes unusable.

## Permissions

| Permission | Why |
|---|---|
| `QUERY_ALL_PACKAGES` | to list the installed apps. A launcher is the one category Google Play explicitly allows this for. |
| `CALL_PHONE` | to place the emergency call directly. Without it the dialler opens with the number filled in instead. |
| `FLASHLIGHT`, `VIBRATE` | the torch button |
| `POST_NOTIFICATIONS` | Android 13+ |

No location, contacts, storage, camera use of its own, phone state, advertising identifier or accounts.

## Build

```
./gradlew :launcher:testDebugUnitTest :launcher:assembleRelease
```

ARM only, `minSdk` 26, `targetSdk` 36.

## Status

Version 1.0 has **not been installed on a phone**, so it has had no device test. The home screen's shape and order are covered by unit tests. Whether it actually becomes the home screen when the owner presses Home, whether the torch toggles, and whether the emergency call goes through are not - and a launcher is the one kind of app that cannot be judged from anything but a phone, because being the home screen is the whole feature.
