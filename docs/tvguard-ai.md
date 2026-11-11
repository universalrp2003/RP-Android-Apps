# Android TV Guard — optional AI assistant

TV Guard can answer general questions using a provider you choose. This is real
question-and-answer, not the local rule-based helper that explains storage and RAM readings.

**It is off by default. Nothing is sent anywhere until you switch it on and ask a question.**

## How it differs from the local helper

| | Local helper | AI assistant |
|---|---|---|
| Answers about storage, RAM, Android, CPU readings | Yes | Yes, and general questions too |
| Needs the internet | No | Yes, except a custom server on your own network |
| Needs a key | No | Yes, except the free option |
| Off by default | Always available | Yes |
| Can answer "why is my TV slow when…" | No | Yes |

The local helper has never been, and is not now, presented as a general AI.

## Providers

Any OpenAI-compatible chat-completions service works. Choose one in Tools → AI assistant.

| Provider | Key needed | Where the key comes from |
|---|---|---|
| Free (no key) | no | — |
| Google Gemini | yes | Google AI Studio |
| Groq | yes | console.groq.com |
| NVIDIA NIM | yes | build.nvidia.com |
| OpenRouter | yes | openrouter.ai |
| OpenAI | yes | platform.openai.com |
| Custom | usually | any OpenAI-compatible server, including Ollama or LM Studio on your own network |

### The free option, honestly

It is real — it needs no signup and no card — but it is not a guarantee:

- It uses **shared public community endpoints**, tried in order until one answers.
- They are rate limited, sometimes busy, and can disappear without notice.
- When this build's endpoint list was checked, `text.pollinations.ai` responded and
  advertised an anonymous tier. A second endpoint returned an empty document and was not
  confirmed working, so it is not described as working.
- The app shows which endpoint actually answered and remembers it for next time.

Listing a provider in the settings screen never means your key can use it, and never means
the provider is free. Use **Load models** to ask the provider itself which model IDs your key
can actually use, then select one.

## Your API key

- TV Guard ships **no key of its own** and runs **no proxy**. Your key goes from your TV
  straight to the provider you picked.
- The key is encrypted at rest with an AES-256-GCM key held in the Android Keystore.
- If Keystore is unavailable, the app **refuses to save the key** rather than storing it in
  the clear, and tells you so.
- The key is never written to logs, crash reports or diagnostic strings; the config's
  `toString()` prints `<redacted>`.
- **Forget key** removes it.
- Never put a key in a chat message, an issue, or a commit. Enter it on the TV only.

## What is sent, and when

Sending happens on exactly one action: you ask a question while the feature is on. That
sends:

- your typed or spoken question,
- a short system instruction (answer briefly, in a TV-friendly way, in your chosen
  language), and
- your provider key, as the authorisation header, to that provider only.

No hardware readings, app lists, file names or usage history are attached. If the feature is
off, a question goes to the local helper and nothing leaves the TV.

**Provider charges may apply.** Free tiers, quotas and prices are set by the provider and can
change. TV Guard cannot know what your account is billed.

## Asking by voice

- **Typed:** open the AI screen and type. A remote or paired keyboard can both type.
- **Spoken:** press **Speak** on the AI screen, or use the microphone shortcut below. The
  recognised words become the question.
- Answers are spoken aloud when **Speak answers aloud** is on (default). Tamil text is
  spoken with a Tamil voice only if the TV has one installed; otherwise it is not spoken
  with an English voice.

## Microphone shortcut, and its real scope

Measured, not promised:

- **Home and Back cannot be used.** The system or the TV firmware consumes them, and an
  ordinary app only sees them while it is the foreground app. TV Guard does not claim them.
- **While TV Guard is on screen**, these keys open the microphone:
  - **hold Menu** (a short Menu press is still the per-tile options shortcut),
  - **Info / i**,
  - **Red colour key**,
  - the remote's **voice**, **Search** or **Assist** keys, if your remote sends them.
- **TV Guard cannot capture the remote system-wide.** Android does not allow it. There is no
  background hotkey that works while another app is on screen, and the app does not pretend
  otherwise.

Whether a particular key reaches the app depends on the manufacturer. The AI settings screen
has a **"press a remote key to test"** readout so you can see what your own remote actually
sends. Only that test, on your TV, proves a key works.

## What was not verified here

The build server runs a disposable TV emulator with no microphone and no provider account.
So the following need a real TV, remote and live key, and are stated as untested:

- which of the shortcut keys your particular remote delivers,
- speech recognition quality with your remote's microphone,
- a real answer from a paid provider using your key,
- how the free endpoints behave on your network on a given day.
