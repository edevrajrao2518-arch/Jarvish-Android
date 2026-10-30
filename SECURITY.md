# Security notes

## Data flow

The app asks Android's speech recognition service to transcribe speech only after the user taps the voice orb. Depending on Android's configured recognizer and device settings, audio may be processed by that provider.

When an Anthropic API key is configured, assistant prompts and up to the most recent eight conversation messages are sent to `api.anthropic.com` over HTTPS. The key is stored in Android encrypted preferences. Do not put API keys in source files, Git history, or screenshots.

Common financial vocabulary in several languages and long numeric identifiers are filtered on-device before AI requests. The list is heuristic and incomplete; it cannot guarantee detection in every language or phrasing. Do not enter bank credentials, payment codes, or other sensitive financial details.

The accessibility service is opt-in and declares `canRetrieveWindowContent=false`. It does not read screen text. Calls and WhatsApp messages are opened as user-confirmed drafts. The app does not perform payments or bank transactions.

## Reporting issues

Do not include private keys, personal data, or financial information in a public issue. This starter has not received a formal security audit.
