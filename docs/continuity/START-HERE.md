# Your apps can continue without one particular AI service

Prepared 4 November 2026. This is a recovery and handover guide, not a new app release.

## What is already safe—and what still needs your action

Your source and published APKs live on GitHub, independently of the Arena conversation. Another developer or agent can work from that repository. This does **not** guarantee that GitHub, cloud providers or AI tools will always remain available/free.

This pack contains instructions, a public-release inventory and a ready-to-paste handover prompt. **It is not a complete backup of the source, APK files, phone data or signing keys.** No passwords, keystores, account tokens, private screenshots or private messages were copied into it. No app was rebuilt or released for this task.

### Your three priorities

1. **Download this continuity pack** to your own device and keep another copy in storage you control, outside Arena.
2. **Confirm you can access the private signing-key recovery backup.** Repository records say the replacement main signer has an encrypted backup in your private Google Drive. That backup's present availability/password has not been checked here. Read [Private backup checklist](PRIVATE-BACKUP-CHECKLIST.md); never send a key/password to an AI chat. Secret Message has separate signing custody.
3. **Keep an independent source and APK backup.** Read the same checklist. GitHub's “Download ZIP” copies source, not releases, account secrets or app data. Do not rely on expiring Actions artifacts as your only APK copy.

The source repository is https://github.com/universalrp2003/RP-Android-Apps . Download source from its **Code → Download ZIP** menu on the branch/commit you intend to preserve. For the public Oor1.1.16 integration, the recorded main commit is `ce15e055a54b094e4e32c86175e352ffa187dac6`. A separate copy of this new documentation is also needed until it reaches main.

## If Arena becomes unavailable

There is no need to uninstall apps, clear their data or generate a replacement signing key in a hurry.

- Give the next helper the repository URL, these documents and `agent-start-prompt.txt`.
- Ask for a **read-only review first**, then a small non-release task.
- Review the proposed changes and tests before allowing any build/signing/publication.
- Keep release authorization separate: old approvals and old publisher workflows are not approval for a new APK.
- Cloud services still require their own account access, quotas and maintenance. A code backup does not back up a running service or private user data.

## What the next helper should read

1. [Technical handover](TECHNICAL-HANDOVER.md).
2. [Private backup checklist](PRIVATE-BACKUP-CHECKLIST.md).
3. [Public release inventory](release-inventory.json)—14 apps, versions, URLs, sizes and hashes checked against GitHub metadata on 4 November. This is not an APK download or safety audit.
4. `AGENTS.md` in the repository—owner requirements and historical decisions. Newer verified status supersedes old “pending” notes; historical branch names are not instructions to switch branches.

## Current important facts

- Oor Arivippu **1.1.16 / code21** is public, using the exact owner-accepted configured APK without a rebuild. Its website integration merged in PR88 and main Pages run41751753970 passed, including live metadata and eight APK/ZIP pairs. The repository's older top “website pending” note was stale and has been superseded after checking the successful run.
- Keep previous releases and saved app data. If using the approved1.1.16 candidate, the public APK is identical; no reinstall is needed.
- Live Guard's home-screen gap and oversized “Scan & clean junk” label remain **deferred until the next requested Live Guard update**. Do not build it merely to complete this backup task.
- Owner reports that something works are useful, but not independent installed-phone hash, audible-speech, all-device reliability, security audit or Play Protect clearance.

**தமிழ் சுருக்கம்:** ஒரு AI சேவை நின்றாலும் GitHub-இல் உள்ள நிரலும் வெளியிட்ட செயலிகளும் அதனால் அழியாது. இந்த வழிகாட்டியை உங்கள் சாதனத்தில் சேமிக்கவும். நிரல்/செயலிக் கோப்புகள் மற்றும் தனிப்பட்ட கையொப்பச் சாவிக்கு தனித்தனி பாதுகாப்பான காப்பு தேவை. சாவி அல்லது கடவுச்சொல்லை உரையாடலில் அனுப்ப வேண்டாம். செயலியை நீக்கவோ தரவை அழிக்கவோ அவசரப்பட வேண்டாம்.
