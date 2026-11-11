# Android TV Guard — status overlay over other apps

The overlay draws four readings on top of whatever is currently on screen: free processor
capacity, available RAM, available storage, and network quality. It is **off by default** and
must be switched on in Expert mode.

## Turning it on

1. Tools → Expert → enable the master switch.
2. Turn on **Status bar** (it supplies the readings).
3. Turn on **Status pill over other apps**.
4. Grant **Display over other apps** when the app asks. Android does not allow this
   permission to be granted from a normal dialog; it opens a system settings page, and the
   app re-checks the result when you come back.
5. Turning either switch off stops the service and removes the pill immediately.

The overlay is a foreground service with a low-importance notification, which is required
by Android for a long-running overlay. Dismissing the notification does not affect it.

## What each reading means, and what it does not mean

| Reading | Source | Honest limitation |
|---|---|---|
| `CPU n% free` | `/proc/stat` aggregate counters, sampled about every 2.5 s | Percentage of processor **time**, not clock speed. It is the whole device, not one app. |
| `RAM n MB free` | `ActivityManager.MemoryInfo` | What Android reports as available. Some of it is reclaimable cache, so the TV can free more than this under pressure. |
| `Storage n GB free` | `StatFs` on the data filesystem | Free space the filesystem reports. It does not include space reserved by the system or lost to a hidden partition. |
| `Temp n.n °C` | `/sys/class/thermal/thermal_zone*` (hottest readable zone) | **Not every TV exposes a sensor.** Many OEM builds expose none to an unprivileged app, and some report `0`, which means uncalibrated rather than 0 °C. In those cases the pill says `Temp --`. This is the SoC sensor, not room temperature. |
| `Wi-Fi -n dBm` | `WifiManager` RSSI | **Radio signal strength only.** A strong signal says nothing about internet speed. Shows `n/a` when the TV is on Ethernet or mobile data, because those have no RSSI. |
| `Net n ms` | TCP connect to public DNS | **Actual internet round-trip latency**, sampled about every 15 s. This is the number that reflects real connection quality. It is latency, not download speed. |

The two network numbers are deliberately separate. A TV can show a strong Wi-Fi signal and
still have a poor `Net` reading, or the reverse. When a source is unavailable the pill says
so instead of showing a guessed value.

## Colour thresholds

The same bands are used by the in-app status pills. They are documented here so the colours
are never a mystery.

| Metric | Green | Yellow | Red |
|---|---|---|---|
| CPU (usage) | ≤ 60 % | 61–85 % | > 85 % |
| RAM (available) | ≥ 25 % | 12–24 % | < 12 % |
| Storage (free) | ≥ 15 % | 7–14 % | < 7 % |
| Network (`Net` latency) | ≤ 60 ms | 61–150 ms | > 150 ms |
| Wi-Fi signal (RSSI) | ≥ −67 dBm | −68 to −75 dBm | < −75 dBm |
| Temperature | < 70 °C | 70–85 °C | > 85 °C |

Grey means unavailable: the reading could not be obtained, or the value was implausible
(for example free space larger than the total). Grey is never used to mean "fine".

## Transparency and remote behaviour

- The pill background is about **50 % opaque**.
- The window is `FLAG_NOT_FOCUSABLE` and `FLAG_NOT_TOUCHABLE`, so it cannot take focus or
  swallow a key press. D-pad navigation in the app underneath is unaffected.
- It sits in the top-left corner and does not move on its own.

## Android and TV restrictions — stated, not worked around

- **Secure screens hide overlays.** DRM video playback, some system dialogs and the lock
  screen suppress overlay windows. There is no supported way to draw over those, so the pill
  will not appear there.
- **Android 12+ shows the overlay slightly after an app opens**, by design, to stop overlays
  obscuring permission prompts.
- **Some TV manufacturers restrict overlays** altogether, or require the app to be granted
  the permission again after an update.
- **Temperature bands are advisory.** The TV's own thermal governor throttles on its own
  schedule; these colours are for noticing a trend, not for predicting a shutdown.
- The overlay can only be drawn while the permission is held. Revoking it stops the service
  at the next check rather than leaving a stale window.

## Cost when idle

Sampling only happens while the service is running. CPU is sampled every 2.5 s, RAM every
10 s, storage every 30 s, temperature every 20 s and the network probe every 15 s. Stopping the overlay cancels the
sampling job and clears the CPU baseline, so nothing continues in the background.
