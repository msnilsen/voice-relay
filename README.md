# Voice Relay

A generic Android voice assistant app that listens for a wake word, transcribes your speech, and POSTs it to any webhook endpoint. Forked from [OpenClaw Assistant](https://github.com/yuga-hashimoto/openclaw-assistant).

![Build](https://github.com/msnilsen/voice-relay/actions/workflows/build-debug.yml/badge.svg)

## What it does

1. Listens for a wake word ("Jarvis", "Computer", "Hey Assistant", or a custom phrase)
2. Transcribes your speech to text
3. POSTs it as JSON to your configured webhook URL
4. Speaks the response back to you

That's it. No vendor lock-in, no mandatory cloud service, no account required. Point it at any HTTP endpoint that accepts a JSON POST and returns text.

## Quick start

1. Download the latest APK from [Actions](https://github.com/msnilsen/voice-relay/actions) (click the latest successful run, then download the `voice-relay-debug` artifact)
2. Sideload on your Android phone (enable "Install from unknown sources")
3. Open the app, go to Settings
4. Set your **Webhook URL** (e.g. `http://192.168.1.100:5678/webhook/assistant`)
5. Tap **Test Connection** to verify
6. Enable **Wake Word** on the home screen
7. Say "Jarvis" and speak your command

## Request format

By default, Voice Relay sends a simple JSON POST:

```json
{
  "query": "what's the weather like today",
  "session_id": "a1b2c3d4-e5f6-..."
}
```

Your backend just needs to return JSON with one of these keys: `response`, `text`, `message`, `content`, `result`, or `answer`. For example:

```json
{"response": "It's 72 degrees and sunny."}
```

OpenAI Chat Completions format is also available as an alternative (configurable in settings), which sends the standard `messages` array and parses `choices[0].message.content`.

## Features

**Voice pipeline**
- Offline wake word detection via [Vosk](https://alphacephei.com/vosk/) (no internet required for wake word)
- Preset wake words: Jarvis (default), Computer, Hey Assistant, or custom
- Android SpeechRecognizer for speech-to-text
- Text-to-speech with adjustable speed, multiple engine support (local, ElevenLabs, OpenAI TTS)
- Continuous conversation mode (auto-resumes listening after response)

**Android integration**
- Registers as system assistant (long-press Home to activate)
- Foreground service for always-on wake word detection
- Auto-starts on boot
- Battery optimization exemption for reliable background listening

**Chat**
- In-app chat interface with markdown rendering
- Local conversation history with session management
- Configurable silence timeout
- Thinking sound while waiting for response

**Settings**
- Webhook URL (sent as-is, no path manipulation)
- Optional auth token (sent as Bearer header)
- Request format: Simple (default) or OpenAI Chat Completions
- Ignore SSL errors (for self-signed certs on local networks)
- Speech language selection
- TTS provider and voice configuration

## Example backends

Voice Relay works with anything that accepts HTTP POST and returns JSON. Some examples:

**n8n webhook** -- Set up a webhook node and respond with `{"response": "..."}`. This is what the project was originally built for.

**Any OpenAI-compatible API** -- Switch to OpenAI format in settings and point at any compatible endpoint (Ollama, LM Studio, vLLM, etc.)

**Custom script** -- A 10-line Flask/Express/FastAPI server that processes the query and returns a response.

## Tech stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Wake word | Vosk 0.3.75 (offline) |
| Speech-to-text | Android SpeechRecognizer |
| Text-to-speech | Android TTS, ElevenLabs, OpenAI TTS |
| HTTP client | OkHttp 4.12 |
| Local storage | Room (SQLite) |
| Settings encryption | EncryptedSharedPreferences (AES256-GCM) |
| Min SDK | Android 12 (API 31) |

## Permissions

| Permission | Why |
|-----------|-----|
| `RECORD_AUDIO` | Wake word detection and speech recognition |
| `INTERNET` | Send requests to your webhook |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MICROPHONE` | Always-on wake word listening |
| `POST_NOTIFICATIONS` | Show "listening for wake word" notification |
| `RECEIVE_BOOT_COMPLETED` | Auto-start wake word service after reboot |
| `WAKE_LOCK` | Keep CPU active during voice sessions |

## Building from source

```bash
git clone https://github.com/msnilsen/voice-relay.git
cd voice-relay
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew assembleStandardDebug
```

The APK will be at `app/build/outputs/apk/standard/debug/`.

## Roadmap

- [ ] Porcupine wake word engine as optional alternative (bring your own API key)
- [ ] Strip remaining gateway/node code from upstream
- [ ] Configurable request body template
- [ ] Per-session webhook URL overrides

## License

MIT License. See [LICENSE](LICENSE) for details.

Forked from [openclaw-assistant](https://github.com/yuga-hashimoto/openclaw-assistant) by yuga-hashimoto.
