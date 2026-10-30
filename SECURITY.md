# Jarvish security and privacy rules

These are product requirements, not claims that software is impossible to compromise.

## Hard rules

1. No payments, purchases, money transfers, cryptocurrency actions, or financial transactions.
2. No collection, storage, or sharing of bank, card, account, password, or one-time-code details.
3. Never run arbitrary shell commands or let a language model invent an executable command.
4. No network permission or cloud AI in the starter. Any future online feature needs a separate, clear opt-in that says what leaves the phone and who receives it.
5. Ask before an action that sends a message, shares a file, searches the web, changes a sensitive setting, or affects another app.
6. Request Android permissions only when the user starts the feature that needs them. Explain the permission first.
7. Keep conversation history in memory only. Do not include conversation text, microphone audio, files, or screen content in crash reports or analytics.
8. If a feature cannot honor these rules, keep it unavailable and explain why.

## Android access boundaries

Android does not let an ordinary app silently control every setting or read and operate every other app. App switching, cross-app interaction, screen capture, device locking, shutdown, calls, messages, and calendars use distinct system APIs and permissions. Jarvish must disclose each capability and obtain the user's choice at the point it is enabled. Some actions remain unavailable to ordinary apps or require confirmation in the destination app.

## Starter behavior

The starter asks for microphone permission only when voice mode is tapped. It asks Android for on-device recognition and refuses network speech recognition. It has no `INTERNET` permission, no background microphone service, no file access, no contacts access, no accessibility service, no screen-capture permission, and no analytics. A web search is handed to the user's browser only after Jarvish displays the query and asks for confirmation. Chat history is kept only in the current app session.

## Future-feature gate

Before adding documents, screenshots, messaging, email, calendar, web search, or third-party integrations, document the data involved, where it is processed, what permission is needed, how it is deleted, and which step the user confirms. Never automatically forward data that looks financial or secret. Do not treat keyword detection as a guarantee that sensitive data has been identified.
