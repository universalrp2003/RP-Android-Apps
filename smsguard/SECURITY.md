# SMS Guard — Security Verification & Test Catalogue

**App:** `com.universalrp.smsguard` · **Release:** `1.1.4` (`versionCode = 10`) · **Min Android:** 8.0 (API 26)

SMS Guard reads real bank / money / OTP SMS and speaks them aloud. A mistake here can cost the
owner real money or leak a one-time code, so the money- and secret-handling paths are treated as
security-critical. This document records **every security property, how it is enforced, and how it
is tested** (automated unit tests + manual on-device checks).

---

## 1. Ironclad safety rules (enforced in `engine/SmsAnnouncementPlanner.kt`)

The planner returns a spoken script (or `null` = stay silent). Rules are evaluated **in order**, and
the first matching rule wins:

| # | Rule | Behaviour | Why it matters |
|---|------|-----------|----------------|
| 1 | **OTP never spoken** | `if (otp.isOtp) return null` — runs **before everything** | A spoken one-time code can be overheard → account takeover |
| 2 | Promo (`-P`) silence | Promotional DLT ads stay silent unless they carry a dangerous link | Stops marketing spam being read out |
| 3 | **Unsafe link → danger alert** | Scam/phishing/malware link always warns "do not touch" | Active-threat protection |
| 4 | **Fake payment → warning** | Money claim from a personal `+91` number always warns "no money received" | Blocks the #1 SMS scam |
| 6 | Offline/unverified link | Honestly states verification is pending, "do not touch" | No false "safe" on an uncheckable link |
| 6B | Outgoing NEFT/IMPS | Says money reached the **beneficiary**, never "you received" | Prevents counting your own transfer as income |
| 7 | **Verified bank credit/debit** | Speaks the **exact amount only** (`spokenRupees(finance.amountInr)`) | The core money announcement |
| 7.5 | Routine service-status silence | "Pack Valid till… Remaining SMS… Bal:Rs…" silenced when the filter is ON | Stops the annoying operator dumps (still stored in inbox) |
| 8 | Full readout | Non-bank/non-OTP text read out, **with the available/closing balance stripped** | Balance is private; never spoken |

RULE 7.5 is deliberately placed **after** RULE 7 (real bank amount) and after the danger rules, so
the service-info filter can **never** silence a genuine transaction or a scam warning.

---

## 2. Security test catalogue

### 2.1 OTP privacy (RULE 1)
- **What:** A one-time code is never spoken.
- **How it works:** `OtpDetector.analyze()` flags a message as OTP via explicit keywords
  (`otp`, `one-time password`, `verification/security/login/auth code`, `passcode`…), the Google
  `G-######` form, the Android SMS-Retriever `<#>` marker, **and** the bare `code is 482910` /
  `code: 482910` phrasing (added in 1.1.4 — see §4). The planner then returns `null` for OTP.
  The sleep/quiet-hours queue also only queues non-OTP messages (`spokenScript != null`).
- **Automated tests:**
  - `SmsGuardEngineTest.otpIsNeverSpoken…` (OTP body → planner returns `null`).
  - `bareCodePhrasingIsTreatedAsOtpAndNeverSpoken` — "your code is 482910" → `isOtp == true`,
    code `482910`, announcement `null`; and "area code" text stays non-OTP.
- **Manual check:** Send yourself an OTP → phone stays **silent**; inbox shows it; a "Copy OTP"
  notification action is available but nothing is spoken.

### 2.2 Fake money from a personal number (RULE 4 / GATE 1)
- **What:** A `+91…` personal SIM that types "Rs.5000 credited…" can never be announced as real income.
- **How it works:** `FinanceLedgerEngine` GATE 1 — personal / non-DLT senders with an amount + a
  bank/UPI claim → `SPOOF_PERSONAL_NUMBER_FAKE_MONEY`; the voice **warns** "no money was received".
- **Automated tests:** `whatsappJobSalaryScamViaDltHeaderIsBlockedImmediatelyAndNeverAddsMoney`
  (scam → `NOT_FINANCIAL`), plus the personal-number spoof path.
- **Manual check:** Text yourself "Rs.5000 credited to your account" from another phone → app says
  **"Warning! … NOT a verified bank. No money was received!"**

### 2.3 Promo / loan / gambling ads never counted as money (GATE 2 + trap list)
- **What:** `-P` promotional headers, loan offers, cashback, "congratulations/claim now", fantasy/
  rummy, failed/declined/due notices are never real credits/debits.
- **How it works:** GATE 2 rejects `-P`; `NON_REAL_MONEY_TRAPS` regex rejects the scam/ad phrases.
- **Automated tests:** promo sender → `REJECTED_PROMOTIONAL_OFFER` / `NOT_FINANCIAL`.

### 2.4 Balance is never spoken (RULE 7 amount-only + RULE 8 strip)
- **What:** The received/spent **amount** is announced; the available/closing **balance** never is.
- **How it works:** RULE 7 speaks `finance.amountInr` only. RULE 8's `cleanFullNormalBody` strips
  `Avl Bal` / `Available balance` / `Closing balance` / `Bal:Rs…` before reading. The sleep queue
  rebuilds scripts through the same planner, so the rule holds there too.
- **Automated tests:** `fullReadoutNeverSpeaksAvailableBalanceOnlyTheRelevantAmount`
  ("Rs.250.00 … Available balance Rs.24,800.00" → speaks `250`, never `24,800`/`balance`).
- **Manual check:** Real bank credit with "Avl Bal Rs.24,800" → app announces the credited amount
  **only**.

### 2.5 Sender spoofing hardening (1.1.4)
- **What:** A header that merely *names* a bank in its body cannot be trusted as that bank.
- **How it works:** `SenderIdentityEngine` — a sender is "verified financial" only if it is a
  **known bank/UPI brand** (any category) **or** a **`-T` transactional** header whose
  body/template looks like a bank alert. The body-bank-name hint is limited to `-T` (real money
  alerts are transactional per TRAI), so a `-S`/`-G` header naming "State Bank of India" can no
  longer announce fake money.
- **Automated tests:** `serviceHeaderNamingABankCannotAnnounceFakeMoneyButTransactionalCan`
  (`XY-ZZWFT-T` + "Federal Bank…credited" → `GENUINE_BANK_OR_UPI`; `XY-ZZWFT-S` → `NOT_FINANCIAL`).
- **Also:** `looksLikeBankHeaderOrTemplate` + the trap list add defence-in-depth.

### 2.6 Danger breaks through mute
- **What:** Scam-link / fake-payment alerts fire even when notifications are muted.
- **How it works:** In `IncomingSmsProcessor`, `if (!isBlocked && (hasDanger || shouldNotifyRegular))`
  — only a **fully blocked** sender (explicit owner action) is silenced for danger.
- **Manual check:** Mute a sender, then have it send an unsafe link → the danger alert still appears.

### 2.7 No data leakage to logs
- **What:** No OTP, message body, or amount is ever written to logcat.
- **How verified:** `grep -rn "Log\." smsguard/src/main` → **0 matches**.
- **Clipboard:** OTP is copied only on the user's explicit "Copy OTP" tap (Android auto-clears).

### 2.8 Corrupt block-list fails safe
- **What:** A damaged `muted_senders` preference can't permanently hide messages.
- **How it works:** `parseMutedSenders` uses `runCatching{…}.getOrDefault(emptyList())` → on parse
  failure, no sender stays blocked.

---

## 3. The 6 owner requests → where they live

1. **Inbox-sync / phantom notification** → notify only when the SMS actually persisted
   (`insertedId > 0`), one insert retry for OEM flakiness, cancel notification on open/delete, and a
   per-refresh `syncThreadNotifications` that clears now-read threads (covers read/delete in another app).
2. **Service-SMS filter** → `ServiceInfoDetector` + `muteServiceInfoVoice` (RULE 7.5).
3. **Per-sender quick actions** → conversation ⋮ menu (notifications off / don't announce / block) +
   Controls "Blocked & Muted Senders" restore card.
4. **No "all links verified" dialog on every open** → `verifyUncheckedInboxLinksInBackground(isManual)`
   only surfaces the banner on a manual scan or a real threat.
5. **Compose contact name** → inline recent contacts + live contact-name resolution under the field.
6. **Bank amount, not balance** → RULE 7 amount-only + RULE 8 balance strip.

---

## 4. Changes shipped in 1.1.4 (security)
- **Closed an OTP-detection gap:** bare "your code is 482910" / "code: 482910" is now detected as an
  OTP so RULE 1 keeps it silent. Biased toward detection — a false positive only silences a
  non-secret message; a miss would speak a live code.
- **Restricted the body-bank spoof surface** to `-T` transactional headers (see §2.5).

---

## 5. Honest residual risk (read this)

**No SMS app can cryptographically prove a message truly came from your bank.** SMS Guard relies on
TRAI DLT-header + body heuristics + a scam-phrase trap list. A determined scammer holding a
*fraudulently registered transactional DLT header* could still craft a fake "credited" SMS that
passes the gates and is announced as money. 1.1.4 narrows this but **cannot eliminate it**.

➡️ **SMS Guard is an awareness/convenience layer, not the source of truth.** For real money safety,
always confirm against your actual bank app / the SMS inbox (where the sender header is visible).

---

## 6. Build & release gates (CI does the real verification)

This repo's sandbox has no Android SDK, so the authoritative checks run in
`.github/workflows/smsguard-release.yml`, which — before creating any GitHub release:
1. runs `tools/verify_apps_localization.py` (bilingual UI + About links),
2. runs `:smsguard:testReleaseUnitTest` (the unit tests above),
3. runs `:smsguard:lintRelease` and `:smsguard:assembleRelease`,
4. verifies the APK is **not debuggable** and is signed by the expected certificate
   (SHA-256 `9e30f4eab8afce5e4ab444ff9578fec354d6d1414d245253590a9703d5985d55`).

If any step fails, **no release is published.** A physical-phone test on the target device is still
recommended before wide distribution.
