# Testing System App Disabler 0.3.0

You need: the phone you tested 0.2.0 on, Shizuku running, about ten minutes.

This build is signed with the **same release key** as 0.2.0, so it installs straight over it. Nothing
is uninstalled and nothing is cleared.

> **If Android says "App not installed" or a signature mismatch**, the phone's copy came from a
> different key. Do not force anything — tell me and I will check the build before you go further.

---

## 0. Before you start — install the build

1. Download `appdisabler-release.apk` from the 0.2.0 candidate artifact of build **43438847542**.
2. Tap it and install over the existing app.
3. Open it. The header still shows **323 / 271 / 52 / 15** and the ⛔ tile should still say **15**
   if all your packages are still off.

If the ⛔ number changed, that is useful — write down what it says now.

---

## TEST 1 — The bug you reported: does a turned-off app stay off?

*This is the one that matters. It is the whole reason for the build.*

1. Tap the **⛔ 15 turned off** tile. The list now shows only the apps you turned off.
2. Find **HyperComm**. Read its status line and write down which one you see:
   - `● off — tap Enable to restore`
   - `● stopped — no process, starts on next launch`
   - `● running now`
3. Now force-close it: **Settings → Apps → Manage apps → HyperComm → Force stop**.
4. Come back to the Disabler and tap **⛔ 15** again.
5. Open HyperComm from your app drawer or recents, then check the list again.

**What should happen:** it should **not** come back on its own. If it does come back, that is the
suspension being weak on HyperOS — the app cannot fix that, but it must at least *say* so instead of
lying about it.

**Write down:** the status line before force-stop, and after.

---

## TEST 2 — Does Enable/Disable now match reality?

1. On **HyperComm**, tap **Enable**.
2. Wait, then tap **Disable** again.
3. Watch for any red error text. A red message is a **good** sign here — it means the app refused to
   claim success when the phone did not actually disable it.

**Write down:** whether you got a toast saying it worked, or a red error, and the exact red text if
there is one.

---

## TEST 3 — The diagnostic, which is the evidence I need

1. Go to the **Setup** tab.
2. Tap **Check Shizuku shell**.
3. Copy the text it shows and send it to me.

Look for the lines beginning `com.xiaomi.hypercomm:`. They say whether HyperComm is in the enabled
list, whether it is in the disabled list, and what the system reports for `enabled=`, `stopped=`,
`suspended=` and `flags=`.

**That single block tells me whether HyperOS is suspending it or genuinely disabling it** — which
decides what the app should say on the row.

---

## TEST 4 — No row should ever say "running" and "off" together

1. Tap **Off** in the filter row, then scroll the whole list.
2. Every row should read either `● off — tap Enable to restore` or `● stopped — …`,
   and none should show a **Disable** button while saying "off".

**Write down:** any row that looks like a contradiction, with the app name.

---

## TEST 5 — Restart, because that is the safety claim

1. After all the above, restart the phone.
2. It should boot normally, as it did after your last restart.
3. Open the Disabler and confirm the ⛔ count is the same as before the restart.

**If the phone does not boot, or anything breaks, stop and tell me immediately** — that outranks
every other result here.

---

## What to send me

Copy this and fill it in:

```
Build: 43438847542
⛔ count before: ____   after restart: ____

Test 1  HyperComm before force-stop: ______________________
        after force-stop:            ______________________
        came back by itself?         YES / NO

Test 2  Enable then Disable result:  worked / red error
        red text (if any):           ______________________

Test 3  the com.xiaomi.hypercomm: lines from the shell check
        ______________________________________________

Test 4  any contradictory row:       none / (app name) ____

Test 5  phone booted after restart?  YES / NO
```

Anything that looks wrong is worth sending, even if you are not sure it is a bug. Screenshots are
fine too.
