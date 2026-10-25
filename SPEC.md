# ForgetMeNot: spec

## Problem
Requests arrive all day in Microsoft Teams. They're acknowledged (👍) and then forgotten until someone follows up ("how is this coming along?").

## Goals
1. Capture a request in under 10 seconds, from anywhere, including the lock screen.
2. Turn a Teams message into a ready task without retyping it.
3. Keep reminding until the task is done or deliberately pushed back.

## Capture paths
| Path | How |
|---|---|
| Hold power button | App is the default digital assistant (`VoiceInteractionService`). The assist session gets the screen's text and positions (`AssistStructure`), then opens the pop-up. Default mode is the halo: a transparent overlay with a glowing edge over the live app, with tap targets at each text item's on-screen bounds (smallest item wins on overlap); a tap outlines the message for a moment and opens a pop-up (wording, who asked, due time, priority); nothing is saved until Done, and Back returns to the halo (no AI; sender guessed from the line above). List mode shows the same items as tappable lines. If the app on screen blocks capture, the pop-up opens empty for typing. |
| One-tap save | Shared and selected text (`ACTION_SEND`, `ACTION_PROCESS_TEXT`) is saved immediately with the default due time from Settings, then a pop-up offers due time, priority, edit and undo. Can be switched off to open quick add instead. |
| Quick Settings tile "+ Task" | Unlocked: quick add. Locked: an add-only pop-up over the lock screen (`showWhenLocked`); Save requires `BiometricPrompt` (fingerprint/face, PIN fallback) and the phone stays locked. If the prompt can't show over the lock screen, falls back to the system unlock. Settings can turn this off (then unlock first). |
| Pinned notification | Optional, always-visible "+ Add task". |
| Copy → quick add | Teams on Android has no share for messages; quick add offers the copied text. |
| Share / select text | `ACTION_SEND` text and `ACTION_PROCESS_TEXT`. |

## Reminders
- Exact alarm at the due time (or immediately for ASAP), then every `nagMinutes` until done.
- Notification actions: Done, +1 hour, Tomorrow (6am). Tapping opens the task to edit.
- Alarm vibration (on by default): a short foreground service shows the reminder and vibrates a repeating pattern with alarm usage, so it works on mute, for up to 30 s. Done, snooze, opening or swiping the reminder stops it.
- Quiet hours (start/end to the minute, may cross midnight) push nags (not explicit due times) to the end of the quiet window. Nags only fire on the chosen days; other days are quiet all day.
- Alarms are rebooked after reboot and app updates.
- Tasks without a due time don't nag; they sit in "No date".

## Roadmap
- Home screen widget (next tasks + add button).
- Daily digest of "No date" tasks.
- Structured order fields (customer, amount, code, container).
