# Jarvis for Windows

Jarvis is a Windows 10/11 desktop voice assistant with a Jarvis-inspired interface. Voice recognition uses an included Vosk speech model and speech output uses the Windows speech engine. No cloud AI key or account is needed.

## Build

The `Build Jarvis Windows installer` GitHub Actions workflow builds the app and packages `JarvisSetup.exe` with Inno Setup. Download `Jarvis-Windows-Setup` from a successful run's Artifacts section.

## Privacy and limitations

- The bundled voice model works offline and is English (India) focused. It does not understand every language.
- Chat and command logs stay in memory and disappear when the app closes.
- Jarvis does not automatically search the web for unknown commands. Say `search ...` or `Wikipedia ...` to open an explicit lookup in your browser; that query is sent to the selected website.
- Messages open as drafts for you to review and send. Jarvis does not submit messages by itself.
- Banking, payments, and money-related commands are refused. Shutdown and restart require a separate spoken or typed confirmation and include a 10-second cancellation window.
- Windows permissions and installed applications limit which controls work. This is not unrestricted control of every PC feature.
