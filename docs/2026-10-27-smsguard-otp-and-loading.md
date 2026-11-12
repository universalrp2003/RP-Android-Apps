# SMS Guard OTP sender-only and inbox loading follow-up

The friend's device currently has no internet pack, so their on-device test is postponed until they can reconnect. No SMS data or screenshots are needed; the owner will report whether the inbox remains visible after the update.

For a future SMS Guard update, this branch adds an optional setting (off by default) to announce only the sender of an OTP message. The announcement planner gets sender identity and an OTP classification, never the OTP code; it returns a fixed sender-only sentence and does not read the message body, amount, link or code. Existing incoming OTP speech remains completely silent until the owner explicitly enables the new option. The existing OTP replay action stays silent. This is not an authenticity guarantee: the spoken sender label is only what Android/provider and sender recognition report.

The inbox screen now shows an indeterminate progress bar, elapsed seconds and count already loaded while the two-stage read runs. It deliberately does not invent a percentage or ETA because the Android SMS provider gives no reliable total-count estimate. The existing quick first batch and bounded 1,800-message initial cap remain; older items use the existing load-more path. A real successful empty query still clears deleted messages, while failed hydration preserves the current list and reports an error.

No stable release or version bump was made. The friend has not tested this follow-up yet. Android runtime behavior and MIUI-specific access still need device validation; support for every OEM cannot be guaranteed from source checks alone.


## Follow-up after the friend's screenshot

The owner reports the same refresh-error toast again. The old inbox remains visible, so snapshot preservation is working; the full refresh still fails. A subsequent defensive patch reduces individual cursor reads from 400 to 100 rows to lower per-window memory pressure on OEM providers and shows a coarse phase code (settings/provider/contact/enrichment/finance) without recording SMS content. This narrows support diagnostics; it does not prove that MIUI was failing from CursorWindow pressure. The friend has no active data pack, but reading stored SMS is an on-device provider operation and does not require mobile data. Re-test can wait for the friend's availability; do not clear app data.
