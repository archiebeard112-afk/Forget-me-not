# Forget-me-not 🌼

An Android app for the requests you get at work and would otherwise forget.
Capture a task in seconds, or hand it a Teams screenshot, and it nags you until it's done.

## Features

- **Quick capture**: a "+ Task" Quick Settings tile, an optional pinned "+ Add task" notification, and an in-app Add button.
- **Hold the power button**: with Forget-me-not set as your digital assistant app, holding the power button reads what's on screen (e.g. a Teams chat) and pops up a drafted task over it. No screenshot needed.
- **Copied messages**: copy a Teams message, open quick add, tap **Use this**.
- **Screenshots**: share a screenshot to Forget-me-not (or pick one in quick add). Claude reads the chat and drafts the task: what, who asked, details (codes copied exactly), when it's due, and warnings for anything cut off.
- **Text from anywhere**: works from the share sheet and the text-selection menu.
- **Nagging reminders**: once a task is due it re-notifies every 15m–4h until you tap **Done**, **+1 hour** or **Tomorrow**. Quiet hours (default 7pm–8am) pause nags.
- **Edit anything**: title, details, who asked, due time, priority, nag interval, notes.
- Everything is stored on the phone.

See [SPEC.md](SPEC.md) for the design and roadmap.

## Install on your phone

Every push builds an APK with GitHub Actions.

1. On your phone, open this repo's **Releases** page and pick the newest `Forget-me-not build N`.
2. Tap `forget-me-not.apk`, then open the download. Android asks you to allow installs from your browser the first time.
3. Open the app and allow notifications.
4. In **Settings**: paste an Anthropic API key (for screenshot reading), and tap **Allow running in background** so Samsung/Xiaomi battery savers don't kill reminders.
5. Optional, for the power button: **Settings → Choose digital assistant** → Forget-me-not, and turn on its screen-text and screenshot options. On Samsung also set **Settings → Advanced features → Side button → Press and hold → Digital assistant**. This replaces Gemini/Google Assistant on that button.
6. Add the tile: swipe down twice, tap the pencil, drag **+ Task** into your tiles.

## Build locally

Requires JDK 17+ and the Android SDK (API 35).

```sh
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

## Privacy

Screenshots and messages you choose to have read are sent directly from the phone to Anthropic's API using your own key. Nothing else leaves the device. Check your company's policy before sending work chats.
