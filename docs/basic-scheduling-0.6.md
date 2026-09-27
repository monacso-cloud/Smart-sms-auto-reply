# ReplyDesk Basic 0.6.0 candidate

## Changes
- Task cards with visible switches for missed-call reply, plain SMS and FAQ chatbot; Recipients, Templates and Settings navigation.
- Separate saved recipients and ignored-number rules per business. Local/international numbers normalized with the SIM country. No Contacts permission or address-book upload.
- Editable message templates per business, usable for replies or scheduled SMS.
- One-time scheduled SMS to one or several chosen recipients, pinned to that business SIM. Edit/cancel pending messages; status and per-recipient outcomes.
- Persisted exact alarms with user-granted SCHEDULE_EXACT_ALARM access; restore after reboot, app upgrade and permission regrant. No unattended repeated sending, automatic retry or default-SIM fallback. More than 15 minutes late is failed, not sent unexpectedly. A send interrupted after claiming is unconfirmed and never automatically retried.
- SMS multipart callbacks counted as one logical message. Sent means Android reported sending every part, not delivery to the recipient. New confirmed-send counters start in this version.
- Diagnostics now records actual SMS broadcasts, phone-state events, missed-call scans, skip reasons and Android send outcomes; includes a user-triggered real-SMS test and copyable report without incoming message bodies.
- SMS routing can use a unique active slot mapping only when subscription IDs are absent. Conflicting identifiers still fail closed. Overnight schedules use the starting day's selection.
- Removed legacy duplicate master/channel controls from Advanced settings; that page now only edits missed-call delay/repeat protection.

## Upgrade
Install over signed 0.5.0. Same package and signing certificate, versionCode 6. Existing business settings remain; no re-import required. Alarms & reminders access is requested only when creating a scheduled message. Scheduled sends are independent of automatic-reply ON/OFF and reply-hours settings; cancel them from Scheduled SMS.

## Device status
The user's report of no automatic replies has NOT been reproduced on her physical phone. Do not claim this release fixes that incident. Automated tests cover storage, routing, scheduling races, failed parts, revocation/reboot, filter isolation and UI creation; they cannot prove incoming carrier broadcasts or sending on a Samsung radio.

Phone acceptance: send one test SMS from each SIM; trigger one ordinary SMS and one missed call to each line; copy Diagnostics if either fails. Schedule a message two minutes ahead, verify the actual originating number and received content; cancel another, edit another, test screen lock, inactive SIM and reboot. Do not use critical messages until those device tests pass. Force-stopping the app or turning off its alarm access stops alarms; reopen/regrant before expecting scheduled sends. RCS is not SMS and is not handled by SMS_RECEIVED. Network and OS restrictions can delay or prevent sending.

Pro deferred: recurring schedules, backup/restore, per-contact custom reply text, call-ended rules, cloud inbox and staff handover.
