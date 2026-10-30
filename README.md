# Jarvis for Android

This is an early, installable debug build of the voice assistant. It supports speech and typed commands, text-to-speech, launching apps, drafting calls and WhatsApp messages for the user to confirm, timers, web search, volume, brightness, and selected Android navigation actions.

## Build

The GitHub Actions workflow builds `app-debug.apk` with JDK 17, Gradle 8.7, and Android SDK 34. The artifact is debug-signed for testing and is not a Play Store release.

## Privacy and security

- The microphone is requested only when the user taps the orb. Speech recognition is provided by Android's configured recognition service; that service may process audio outside the phone depending on the device and settings.
- If the user adds an Anthropic API key in the app, assistant prompts and up to the recent conversation context are sent to Anthropic over HTTPS. The key is stored using Android encrypted preferences and is not included in this repository or build.
- Common financial terms in several languages and long numeric identifiers are blocked locally before the AI request. This filter cannot recognize every wording or language; do not enter bank details, passwords, or other sensitive information.
- The accessibility service is opt-in and configured without access to screen or window content. It only requests Android global navigation actions.
- Calls and WhatsApp messages are opened as drafts for the user to confirm. The app does not initiate payments or bank transactions.

This starter has not had a formal security audit. A debug APK is for personal testing, not a guarantee that the app is unhackable or appropriate for sensitive financial use.
