# ForgetMeNot 🌼

An Android app for the requests you get at work and would otherwise forget.
Capture a task in seconds, or hand it a Teams screenshot, and it nags you until it's done.

## Features

- **Quick capture**: a "+ Task" Quick Settings tile, an optional pinned "+ Add task" notification, and an in-app Add button.
- **Hold the power button**: with ForgetMeNot set as your digital assistant app, holding the power button puts a glowing halo around the screen with the app (e.g. a Teams chat) still visible. Tap the message itself and a pop-up lets you adjust the wording, who asked, due time and priority; **Done** saves it. No AI needed. A **List** view of the screen's text is the fallback, and Claude can draft the task instead if you prefer.
- **One-tap save**: highlight text in any app (or share it) and pick **Add to ForgetMeNot**. It's saved immediately, with a pop-up to change the due time, mark it high priority, edit or undo.
- **From the lock screen**: the "+ Task" tile or pinned notification opens a pop-up over the lock screen without unlocking. It can only add tasks, and **Save** needs your fingerprint, face or PIN; the phone stays locked. A second app icon, **Quick add**, opens the same pop-up and can be chosen as a Samsung lock screen shortcut.
- **Copied messages**: copy a Teams message, open quick add, tap **Use this**.
- **Screenshots**: share a screenshot to ForgetMeNot (or pick one in quick add). Claude reads the chat and drafts the task: what, who asked, details (codes copied exactly), when it's due, and warnings for anything cut off.
- **Text from anywhere**: works from the share sheet and the text-selection menu.
- **Nagging reminders**: once a task is due it re-notifies every 15m–4h until you tap **Done**, **+1 hour** or **Tomorrow**. Quiet hours (to the minute, default 7pm–8am) and days off pause nags.
- **Edit anything**: title, details, who asked, due time, priority, nag interval, notes.
- Everything is stored on the phone.

See [SPEC.md](SPEC.md) for the design and roadmap.

## Install on your phone

Every push builds an APK with GitHub Actions.

1. On your phone, open this repo's **Releases** page and pick the newest `ForgetMeNot build N`.
2. Tap `forget-me-not.apk`, then open the download. Android asks you to allow installs from your browser the first time.
3. Open the app and allow notifications.
4. In **Settings**: paste an Anthropic API key (for screenshot reading), and tap **Allow running in background** so Samsung/Xiaomi battery savers don't kill reminders.
5. Optional, for the power button: **Settings → Choose digital assistant** → ForgetMeNot, and turn on its screen-text and screenshot options. On Samsung also set **Settings → Advanced features → Side button → Press and hold → Digital assistant**. This replaces Gemini/Google Assistant on that button.
6. Add the tile: swipe down twice, tap the pencil, drag **+ Task** into your tiles.

## Build locally

Requires JDK 17+ and the Android SDK (API 35).

```sh
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

## Privacy

Screenshots and messages you choose to have read are sent directly from the phone to Anthropic's API using your own key. Nothing else leaves the device. Check your company's policy before sending work chats.
