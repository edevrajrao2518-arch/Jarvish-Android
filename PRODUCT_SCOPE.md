# Jarvish product scope

The user wants a friendly, multilingual, voice-first assistant for Android, with stronger problem solving and optional control of device features and connected services. Privacy is the first priority. Banking and money actions are permanently out of scope.

## First milestone

Build a native Android foundation with on-device voice recognition where available, spoken replies, a cinematic dark interface, a small allowlisted set of safe actions, and explicit approval before a web search. The app must start in local mode and be usable without an account or API key.

## Later milestones

- Expand multilingual recognition and natural replies. Evaluate an on-device language model first; any hosted model requires explicit opt-in and a clear data-transfer notice.
- Add user-selected document import using Android's document picker. Process a selected file only after a clear preview and confirmation; never upload it silently.
- Add screen understanding only through Android's screen-capture consent flow. Avoid banking apps and payment screens.
- Add messages, calls, calendar, and supported integrations as user-requested drafts. Require review before sending or committing an action.
- Add optional app navigation and settings control only through narrowly scoped Android APIs. Do not enable a broad Accessibility service by default.
- Keep money transfers, payments, purchases, banking credentials, and sharing financial information disabled.

## Current limits

The first milestone is not a general-purpose AI model. It does not yet provide open-ended reasoning, all-language conversation, document analysis, current screen analysis, messaging, calendar features, third-party integrations, shutdown, or full phone control. Android's platform security also prevents apps from silently controlling many system and cross-app actions.
