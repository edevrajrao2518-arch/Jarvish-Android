# Jarvish for Android

Jarvish is a local-first Android voice assistant prototype. The first build focuses on privacy and safe, explicit actions.

## Privacy and safety commitments

- The app requests microphone access only when you tap **Activate voice**.
- It uses Android's on-device speech recognizer only. If your phone has no on-device recognizer, voice mode stays off and you can type instead.
- The app has no Internet permission. Chat text and recognized speech are not uploaded or saved by Jarvish.
- Web searches open only after a confirmation screen. The search text is then sent to the browser/search provider you use.
- Payments, transfers, banking tasks, and arbitrary shell commands are refused. Jarvish does not collect banking information.
- It does not read other apps, capture the screen, send messages, place calls, or silently change device settings.
- Android permissions and services such as Accessibility, screen capture, device administration, and contacts are intentionally not enabled in this starter project.

No software can promise that it will never be hacked. This starter reduces exposure by avoiding network access, persistent conversation storage, and broad device permissions. Device speech recognition availability and its provider are controlled by Android and the phone manufacturer.

## Available in this starter

- Voice input when on-device Android speech recognition is available
- Spoken replies using Android text-to-speech
- Time and date responses
- Open a named installed app
- Raise or lower media volume
- Web search after an on-screen disclosure and confirmation
- Basic greeting in Hindi, English, Spanish, or French

The broader features from the product brief—open-ended reasoning, more languages, document and screen understanding, messaging, calendar, integrations, and hands-free cross-app navigation—are not implemented in this starter. Each will need its own privacy design, platform permission, and user-confirmation flow. Adding an online AI service would send selected data off the phone, so it is excluded until a separate, explicit privacy choice is designed.

## Build

The app supports Android 12 (API 31) and later. In Android Studio, open this folder with Android SDK 36 and JDK 17 installed, sync the Gradle project, then choose **Build > Build App Bundle(s) / APK(s) > Build APK(s)**.

The GitHub Actions workflow in `.github/workflows/build-apk.yml` builds a debug APK and publishes it as the `jarvish-debug-apk` artifact for 14 days. This workspace did not have Java, Gradle, or Android SDK tools when the starter was created, so the APK must be built on GitHub Actions or a computer with Android Studio installed.
