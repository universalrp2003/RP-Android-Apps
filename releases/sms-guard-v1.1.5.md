### என்ன புதியது (What's New in v1.1.5)

**🧾 பில் & செலுத்தும் நாள் நினைவூட்டல் (Bill & Payment Due Reminders) — brand-new Bills tab**
- Credit-card, electricity (TNEB), internet/broadband, LPG gas, postpaid mobile, water, insurance and loan-EMI **bill SMS are now captured automatically** into their **own dedicated box** — the new **Bills (பில்கள்)** tab. Bills are **never** counted as income/spend in the Finance ledger.
- **Add your own reminders** any time: biller name, optional amount, due date (date picker) and category.
- **Voice announcements you control:** a gentle "coming up soon" heads-up, **capped at 2 per day** by default so you are never disturbed. When a bill is **due very soon (within ~2 days) or overdue, it ALWAYS speaks aloud AND posts a notification** — that urgent alert is never suppressed by the daily cap.
- **Full control:** master ON/OFF, separate voice + notification switches, adjustable daily max (1–5), and the daily check time (default 9:00 AM). Each reminder also has its own 🔊 voice, 🔔 notification, ✅ mark-paid and 🗑 delete controls.
- **"Read Aloud Now"** button speaks every unpaid reminder on demand.
- Anti-spoof by design: only verified official (non-promotional, non-personal) senders can create a reminder; **payment receipts ("your bill has been paid") are ignored**; a random personal number can never manufacture a fake "electricity due" reminder.

### எப்படி இயக்குகிறது (How it behaves)
- A near-due bill announces + notifies **immediately** when its SMS arrives, and again on the daily check while it is still within the near window.
- Voice respects your existing **Sleep / Quiet Hours**, the master Voice switch and the per-bill 🔊 toggle; the notification always informs you visually.
- Survives reboot / time & timezone changes (the daily check re-arms automatically).

### Security & privacy in v1.1.5
- **OTP silence is unchanged and ironclad:** one-time codes are never part of a bill reminder and are never spoken.
- Bills are stored only in the app's private storage; nothing leaves the device.
- One new permission: `RECEIVE_BOOT_COMPLETED` (so the daily bill check survives a reboot). No new data collection.

### Verification
Built, unit-tested (new bill-parser / planner / bilingual-script tests), linted and signature-verified in CI before this release was created. **Post-publication device confirmation (24 October 2026):** the owner installed `SMS-Guard-v1.1.5.apk` on their phone and reports that it works, with the new **Bills (பில்கள்)** tab visible in the bottom navigation and the About screen showing version `1.1.5`. One phone only, and the screenshots do not show an APK SHA-256, so the installed bytes are **not hash-matched** to the published asset; a real-device check on a few more phones is still recommended before wide distribution.

**Signing certificate (verify the publisher before installing):** the release APK is signed with the
official RP Ideas key — certificate SHA-256 `9e30f4eab8afce5e4ab444ff9578fec354d6d1414d245253590a9703d5985d55`.
You can confirm any downloaded `SMS-Guard-v1.1.5.apk` matches this signer with
`apksigner verify --print-certs SMS-Guard-v1.1.5.apk`. The APK's own SHA-256 is printed in the
release header above by the CI release workflow.
