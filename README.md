# Jarvis Android

A small Android voice shortcut assistant with a local session log and a Jarvis inspired interface.

## Privacy and capabilities
- No internet permission, cloud AI API, API key, Accessibility service, or stored conversation history.
- Voice recognition uses Android's on-device recognizer only when the device provides one. Otherwise, use typed commands.
- Android asks for microphone access only when you tap the microphone.
- The session log exists in memory and disappears when the app closes.
- Opening a browser search, WhatsApp draft, dialer, or another installed app hands control to that app. Review any message or call there before sending or placing it.
- Jarvis cannot read your screen or control Home, Back, Recents, shutdown, or protected settings.

Commands include app launch, time/date, volume, display settings, dialer, WhatsApp draft, web search, and timer.

Builds from GitHub Actions are debug APKs for sideloading, not Play Store releases.
