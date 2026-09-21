# ReplyDesk — Google Play Permissions Declaration Draft

This is a preparation draft only. It must not be submitted until the final compliant Android role/distribution architecture is confirmed.

## Restricted permissions currently used by Direct/SIM build
- READ_CALL_LOG
- SEND_SMS
- RECEIVE_SMS
- READ_PHONE_STATE

## Core functionality statement
ReplyDesk's primary purpose is automated business communication following missed calls and customer SMS messages. The app uses telephony information to detect eligible missed calls, send configured automatic SMS replies, receive customer SMS responses, match custom menus and keywords, and show recent call/reply history.

## Why each permission is requested

### READ_CALL_LOG
Used in the Direct/SIM build to identify recent missed calls and display call history inside ReplyDesk.

### SEND_SMS
Used in the Direct/SIM build to send automatic replies configured by the business.

### RECEIVE_SMS
Used in the Direct/SIM build to receive customer SMS replies for menu and keyword chatbot processing.

### READ_PHONE_STATE
Used to support telephony state and SIM-related functionality.

## User-facing controls
Users can:
- turn automation off,
- independently enable missed-call replies and SMS replies,
- choose a schedule,
- choose a SIM,
- configure repeat intervals,
- change all reply text,
- configure numbered menus,
- configure keywords,
- test replies before enabling them,
- clear ReplyDesk-owned logs,
- choose log retention.

## Recipient transparency
All automated outbound messages are prefixed with an automation disclosure such as:
Automated reply:

## STOP / START
Incoming STOP disables chatbot replies for that sender in the local Direct/SIM implementation. START re-enables them.

## Reviewer video
Demonstrate:
1. app setup,
2. role/permission flow used by the final Play build,
3. missed-call reply,
4. incoming SMS response,
5. menu 1–10,
6. keyword reply,
7. automated disclosure,
8. schedule,
9. logs,
10. STOP / START,
11. Test Your Bot.

## Important
Do not claim or imply that Google Play approval has already been obtained.
Do not submit this declaration until the final Play-compliant default-handler or other approved architecture is implemented and tested.
