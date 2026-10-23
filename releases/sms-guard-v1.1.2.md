## SMS Guard (குறுஞ்செய்தி காவலன்) v1.1.2 — Reply Keyboard Fix, NEFT Beneficiary Fix, Clean Voice Mode & SMS Readout Controls

**Package:** `com.universalrp.smsguard` · **Version:** `1.1.2` (`versionCode = 8`) · **Minimum Android:** Android 8.0+ (API 26)

### What's New in v1.1.2
- **Keyboard Never Hides the SMS Reply Bar:** Fixed edge-to-edge IME inset handling (`imePadding()` + `navigationBarsPadding()`) and added automatic message list scrolling when the keyboard opens.
- **NEFT Outgoing Beneficiary Credit Fix + Clear Voice Announcement:** Messages stating `amount credited to the beneficiary account` are never miscounted as your income in the Finance Ledger, and clearly announce that your sent transfer reached the recipient's account.
- **Top `தமிழ் | ENG` Toggle + Automatic HD Male/Female Voices:** Controls both UI language and voice announcement language together, rotating crisp HD Male ↔ Female voices online with automatic Male/Female Android System TTS fallback offline.
- **Normal-SMS Voice Options (`Sender Name Only` & `Read Full SMS`):** Choose between sender-name-only alerts or full normal-SMS voice readout (while keeping OTPs 100% silent).
- **Updated Bilingual Guide:** Full Tamil & English in-app guide updated for all v1.1.2 features.
