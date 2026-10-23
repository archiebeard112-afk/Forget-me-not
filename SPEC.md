# Forget-me-not: spec

## Problem
Requests arrive all day in Microsoft Teams. They're acknowledged (👍) and then forgotten until someone follows up ("how is this coming along?").

## Goals
1. Capture a request in under 10 seconds, from anywhere, including the lock screen.
2. Turn a Teams message or screenshot into a ready task without retyping it.
3. Keep reminding until the task is done or deliberately pushed back.

## Capture paths
| Path | How |
|---|---|
| Hold power button | App is the default digital assistant (`VoiceInteractionService`). The assist session gets the screen's text (`AssistStructure`) and, if allowed, a screenshot, then opens a bottom-sheet pop-up. Default mode lists the screen's lines to tap and save (no AI; sender guessed from the line above). Optional mode sends the screen to Claude for a drafted task. If the app on screen blocks capture, the pop-up opens empty for typing. |
| One-tap save | Shared and selected text (`ACTION_SEND`, `ACTION_PROCESS_TEXT`) is saved immediately with the default due time from Settings, then a pop-up offers due time, priority, edit and undo. Can be switched off to open quick add instead. |
| Quick Settings tile "+ Task" | Opens quick add (asks to unlock first, so work data stays private). |
| Pinned notification | Optional, always-visible "+ Add task". |
| Copy → quick add | Teams on Android has no share for messages; quick add offers the copied text. |
| Screenshot share | Share sheet → Forget-me-not, or "Pick screenshot" in quick add. |
| Share / select text | `ACTION_SEND` text and `ACTION_PROCESS_TEXT`. |

## Reading messages with Claude
- Screen text from the power-button capture is sent along with the screenshot; Claude uses the text for exact spelling of codes and the image for who said what.
- Sends the text and/or screenshot (downscaled to ≤2000px JPEG) to the Messages API with a JSON schema (`output_config.format`).
- Default model `claude-opus-5-5`; `claude-haiku-5-5` selectable in Settings as the cheaper option.
- Each draft has: title, requester, details (codes copied exactly), due (ISO / ASAP / none), high priority, warnings.
- Rules: messages from "You" aren't requests; a quoted request plus a follow-up is one task and becomes high priority; thanked/finished requests are skipped; cut-off text and unsure characters (O vs 0) become warnings.
- The user always reviews drafts before saving.

### Test case: order-chat screenshot
A group chat where (1) the user posted an order and a coworker thanked them, (2) the coworker asked for a new extract order (customer, grams, product code, container), and (3) two days later quoted that request and asked "how is this coming along? customer is picking up…" with the rest cut off.
Expected: one task for request (2), details with the code copied exactly, requester set, ASAP, high priority, and a warning that the pickup time is cut off. Message (1) is skipped.

## Reminders
- Exact alarm at the due time (or immediately for ASAP), then every `nagMinutes` until done.
- Notification actions: Done, +1 hour, Tomorrow (9am). Tapping opens the task to edit.
- Quiet hours (start/end to the minute, may cross midnight) push nags (not explicit due times) to the end of the quiet window. Nags only fire on the chosen days; other days are quiet all day.
- Alarms are rebooked after reboot and app updates.
- Tasks without a due time don't nag; they sit in "No date".

## Roadmap
- Home screen widget (next tasks + add button).
- Daily digest of "No date" tasks.
- Cloud sync and a Claude Desktop connector (MCP server) so Claude on the work computer can add tasks from Teams, plus a scheduled "scan today's Teams for requests" sweep.
- Structured order fields (customer, amount, code, container).
