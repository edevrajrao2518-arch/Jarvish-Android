# Jarvis Android

A privacy-focused Android voice assistant prototype with a Jarvis-inspired interface and launcher icon.

## Build with GitHub Actions

Push to `main` or run **Build Jarvis APK** from the repository's Actions tab. Download the `Jarvis-APK` artifact from a successful run.

## Privacy and platform limits

- No cloud API key is required or bundled.
- Voice recognition uses Android's on-device speech recognizer when the device supports it. If unavailable, use typed commands; the app does not fall back to network recognition.
- No Accessibility screen-reading or overlay permission is requested. Some device actions use Android's Accessibility service and require the user to enable it explicitly in Settings.
- Android and manufacturer restrictions control which system actions an app can perform. The app cannot silently control every phone function.
- This is a debug APK for personal testing. It is not a Play Store release and is not signed with a private release key.
