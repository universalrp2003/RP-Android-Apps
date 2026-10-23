### என்ன புதியது (What's New in v1.1.4)

**Inbox sync & phantom-notification fix**
- A "new SMS" notification is now shown **only when the message actually lands in the inbox**. After a bulk delete (or a read/delete in the Google SMS app), the stale notification is cleared automatically — no more alert with an empty inbox.
- The incoming-SMS write now retries once on OEM flakiness (ColorOS/MIUI/One UI) so genuine messages are not dropped.

**Service-SMS filter (data / validity / balance dumps)**
- Routine operator texts like *"Pack Valid till 10 Dec 2026. Remaining SMS:569 Bal:Rs.0.00."* are now **silent** (no voice, no notification) but still kept in the inbox.
- Toggle: **Controls → 📊 Silence Data / Balance / Validity Service SMS** (default ON). **Real bank credit/debit always still announces.**

**Per-sender quick actions**
- Every conversation has a **⋮ menu**: *🔕 Turn notifications OFF*, *🔇 Don't announce aloud*, *🚫 Block this sender entirely*.
- Muted senders show 🔕/🔇 badges; restore any sender from **Controls → 🚫 Blocked & Muted Senders**.

**No more "all links verified" dialog on every open**
- The link-verified banner only appears on a **manual** scan (refresh / Shield → Scan all) or when a real threat is found — never just from opening the app.

**Compose: contact name now shows**
- The New SMS screen lists **recent contacts inline** (one tap to fill) and shows the **resolved contact name** under the recipient field — no extra contact picker needed.

**Bank voice: amount only, never the balance**
- Received/spent **amount** is announced; the **available/closing balance is never spoken**.

### Security hardening in v1.1.4
- **OTP gap closed:** bare *"your code is 482910"* / *"code: 482910"* phrasings are now recognised as OTP and kept **silent** (one-time codes are never spoken aloud).
- **Spoof surface reduced:** only genuine `-T` transactional bank headers (or known bank/UPI brands) can announce money; a service/government header merely *naming* a bank can no longer announce money that never arrived.
- Full catalogue, threat model and honest residual-risk notes: [`smsguard/SECURITY.md`](../blob/arena/a7c60edc-rp-android-apps/smsguard/SECURITY.md).

### Verification
Built, unit-tested, linted, and signature-verified in CI before this release was created. A physical-phone test on the target device is still recommended before wide distribution. `0` new permissions over `v1.1.3`.
