# Security and privacy

Jarvis Android is intentionally a local shortcut app. The manifest requests microphone access at runtime and has no INTERNET or WRITE_SETTINGS permission. It contains no Accessibility service, API key field, cloud endpoint, analytics, or persistence for the session log.

Speech input uses `SpeechRecognizer.createOnDeviceSpeechRecognizer` where Android reports that feature available. The app does not fall back to a network recognizer. Typed commands work without speech recognition.

Commands that open external apps explicitly pass their requested content to the selected app (for example, a WhatsApp draft or browser search). The user must review and confirm sending/calling in that app. Financial and authentication related terms are declined by a basic local safety filter; this is not a guarantee against every possible wording.

This app cannot guarantee that a device or operating system is unhackable. Keep Android updated and install only builds from a source you trust.
