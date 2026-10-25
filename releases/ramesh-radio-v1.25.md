# Ramesh Radio 1.25 — More than one saved place

**Version:** 1.25 · **version code 25** · **App:** Ramesh Radio

Rain and cyclone voice alerts could only ever point at one place. You now keep your own list of
places and switch between them with one tap.

No new permissions. No change to how alerts are worded, when they fire, or how the radio ducks and
restores its volume.

## What changed

- A row of chips under the weather banner, one per saved place. The current one is highlighted.
- Tapping a chip makes that place active immediately: the alert re-checks and speaks for the new
  town. No searching, no re-typing the PIN.
- The ✕ on a chip forgets that place. If you remove the active place, the next one in your list
  takes over, so alerts never keep pointing at a place you deleted.
- Every place you pick from the search box is remembered automatically, so the list fills itself as
  you use it.
- The chips only appear once you have more than one place, so a first-time owner sees nothing extra.

Up to 12 places. Re-picking a place you already have moves it to the front instead of adding a
duplicate, and two picks that resolve to the same coordinates count as one place.

## How it is built

The shared weather runtime keeps exactly one active place, and it is shared with Live Guard and Oor
Arivippu, so it is deliberately unchanged: this list lives in Ramesh Radio's own storage and only
hands a place to the shared runtime when you pick one. The other two apps see the new place exactly
as they always did.

Storage is one line per place with control-character separators — no JSON dependency, and a
half-written or hand-edited line costs you one place rather than the whole list. The
encode/decode round-trip, the dedupe key and the cap are unit-tested.

## Not verified

The exact bytes in this release were not installed on a phone before publication: no device test
was run for 1.25. CI unit, lint, regression, signature and release-asset checks pass. The chips
rendering on a narrow screen, and switching places on a real phone with a real alert pending, still
need a physical-device check.
