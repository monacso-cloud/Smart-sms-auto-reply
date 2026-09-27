# ReplyDesk 0.6.2: simple chatbot setup

The 0.6.1 guard stopped raw keyword configuration from being sent, but users still had to copy a long rule list between similar text fields. This update replaces the raw keyword editor with separate answer cards.

- Missed-call reply and plain SMS now have separate message screens. Missed-call delay choices are visible on that screen: 0, 5, 10, 15, 30 or 60 seconds.
- Open Tasks > FAQ chatbot > Set up answers, or Open chatbot setup in automatic reply settings.
- Add an answer with customer words/phrases and a separate reply. Edit, disable, delete or move cards up. First enabled matching card wins.
- Topic starters open an editor; business-specific details must be entered before saving.
- A prominent Organise my saved answers action previews rules mistakenly pasted into the general/plain reply fields. Confirmation moves them into cards, retains existing cards, removes exact duplicates and restores a general reply in the source field. Original source strings are backed up in the same business preferences. No auto-enabling or cross-business migration.
- Parsing completes before migration writes. Invalid or ambiguous imports are retained for review rather than partly overwritten.
- New cards persist as JSON in the existing chatbot_rules preference. Legacy newline and supported footer-separated rules remain readable; multiline answers persist without truncation.
- Inline preview and incoming SMS share BotReplies selection. Preview sends no SMS and does not change STOP/START state. Sending still obeys permissions, per-business switches, recipient rules and reply hours.
- Old automatic-reply settings hide misplaced rule dumps and provide a direct setup button. Diagnostics reports version 0.6.2.

The APK keeps com.smartreply.beta and the existing release signing certificate; versionCode is 8. Install over 0.6.1. The feature tests exercise migration, malformed data preservation, per-business isolation, UI save/confirm flows and preview/receiver parity. Physical radio delivery and the earlier intermittent Android send error still require device testing; this UI release does not claim to fix carrier delivery.

## Physical phone acceptance — pending, not performed by the agent
1. Update on the Samsung without uninstalling. Open Chatbot setup and confirm Organise my saved answers if shown. Review the availability reply and link, then enable the chatbot.
2. From a second phone, send a real SMS: "Are you available today?" Expect only the availability answer and link, with automated disclosure. Repeat after at least 30 seconds with "Any cancellation today?".
3. After 30 seconds, send a question with no configured keyword, e.g. "Do you have parking?" Expect only the general reply.
4. Call from the second phone and leave the call unanswered. Expect the missed-call message, after the selected delay and subject to repeat protection. Record actual receive time and sending line.
5. Record PASS/FAIL for each item. If it fails, copy the diagnostic report and note which test, time, selected SIM and what the receiving phone actually showed. Never infer receipt from a Submitted or Sent status alone.
