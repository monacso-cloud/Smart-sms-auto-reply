# ReplyDesk 0.6.2: simple chatbot setup

The 0.6.1 guard stopped raw keyword configuration from being sent, but users still had to copy a long rule list between similar text fields. This update replaces the raw keyword editor with separate answer cards.

- Open Tasks > FAQ chatbot > Set up answers, or Open chatbot setup in automatic reply settings.
- Add an answer with customer words/phrases and a separate reply. Edit, disable, delete or move cards up. First enabled matching card wins.
- Topic starters open an editor; business-specific details must be entered before saving.
- A prominent Organise my saved answers action previews rules mistakenly pasted into the general/plain reply fields. Confirmation moves them into cards, retains existing cards, removes exact duplicates and restores a general reply in the source field. Original source strings are backed up in the same business preferences. No auto-enabling or cross-business migration.
- Parsing completes before migration writes. Invalid or ambiguous imports are retained for review rather than partly overwritten.
- New cards persist as JSON in the existing chatbot_rules preference. Legacy newline and supported footer-separated rules remain readable; multiline answers persist without truncation.
- Inline preview and incoming SMS share BotReplies selection. Preview sends no SMS and does not change STOP/START state. Sending still obeys permissions, per-business switches, recipient rules and reply hours.
- Old automatic-reply settings hide misplaced rule dumps and provide a direct setup button. Diagnostics reports version 0.6.2.

The APK keeps com.smartreply.beta and the existing release signing certificate; versionCode is 8. Install over 0.6.1. The feature tests exercise migration, malformed data preservation, per-business isolation, UI save/confirm flows and preview/receiver parity. Physical radio delivery and the earlier intermittent Android send error still require device testing; this UI release does not claim to fix carrier delivery.
