# Android TV Guard — the status strip over other apps

The strip draws five readings on top of whatever is on screen:

**processor load · free RAM · free storage · temperature · internet quality**

It is off by default. It sits at the **top** of the screen, on one line, with no panel behind
it. It can be moved.

## Turning it on

1. **Tools & Settings → Expert settings** → turn on the master switch.
2. Turn on **Status pill over other apps** (it is first in the list).
3. Grant **Display over other apps** when asked. Android does not allow this from a normal
   dialog: it opens a system page, and the app re-checks when you come back.

The **Status bar** switch above it is separate and only controls the in-app header (the app
name and clock). Turning it off does **not** turn the strip off.

## Moving the strip

A remote has no pointer, so the strip is positioned with buttons, not by dragging:

**Tools & Settings → the `Pill: …` row → ◀ ▶ ▲ ▼**

Each press moves it one step and it is saved immediately, including across restarts and
app updates. In 1.3 and earlier a position saved by an older version could keep the strip
mid-screen after an update; that saved position is now discarded once so the strip returns
to the top.

## What each reading means, and what it does not mean

| Reading | Example | Source | Honest limitation |
|---|---|---|---|
| Processor load | `98%` | `/proc/stat` counters, about every 2.5 s | Percentage of processor **busy time**, for the whole device. It is not clock speed and not one app. |
| Free RAM | `846 MB` | `ActivityManager.MemoryInfo` | What Android reports as available. Some is reclaimable cache, so the TV can free more under pressure. |
| Free storage | `26.0 GB` | `StorageManager` volumes, largest real one | **Changed in 1.4.** This used to read the data partition alone, which is a small slice of the drive: a 16 GB TV showed "26.0 GB free", which cannot be true. The figure now comes from the roomiest volume that exists, and a value larger than its own capacity is rejected rather than shown. |
| Temperature | `43.2 °C` | `/sys/class/thermal/thermal_zone*`, hottest readable zone | **Not every TV exposes a sensor.** Many OEM builds expose none to an unprivileged app, and some report `0`, which means uncalibrated rather than 0 °C. Then the strip shows `-- °C`. This is the SoC sensor, not room temperature. |
| Internet quality | `good` / `medium` / `bad` | Real TCP latency to public DNS, about every 15 s | Actual round-trip latency, not download speed. `offline` means no usable connection. Shows `--` when nothing can be measured. |

Internet quality is a single colour-coded word on purpose: it carries the meaning without
turning the strip into a sentence.

## Colour thresholds

| Reading | Green | Yellow | Red |
|---|---|---|---|
| Processor load | under 50% | 50–80% | over 80% |
| Free RAM | over 25% of total | 12–25% | under 12% |
| Free storage | over 20% of total | 8–20% | under 8% |
| Temperature | under 55 °C | 55–70 °C | over 70 °C |
| Internet quality | under 60 ms | 60–150 ms | over 150 ms |

A reading that cannot be measured is shown in grey with `--`, never as a guessed green value.

## Language

The app is fully available in **English and Tamil**. Android 13 and later can switch the app
language from **Settings → System → Languages & input → App languages → Android TV Guard**.
On older versions the language follows the TV's system language. Every label, dialog,
button and message has a Tamil version, and the memory reminder below is deliberate.

## Two things that changed in 1.4 because they were wrong or confusing

- **The AI/voice feature is gone.** The microphone, the assistant switch and its permission
  were all removed, so the app no longer asks for the microphone at all.
- **Free storage was wrong.** Explained in the table above.
