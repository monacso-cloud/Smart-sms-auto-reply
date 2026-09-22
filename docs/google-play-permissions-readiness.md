# ReplyDesk — Google Play SMS / Call Log Permissions Readiness

Last reviewed: 2026-09-22

## Why this matters
ReplyDesk Core currently relies on Android telephony permissions for direct-SIM missed-call and SMS automation:
- READ_PHONE_STATE
- READ_CALL_LOG
- SEND_SMS
- RECEIVE_SMS

Google Play treats SMS and Call Log permissions as restricted/high-risk permissions.

## Current Play-policy constraint
For Google Play distribution, apps using SMS or Call Log permission groups generally need to qualify as an approved default handler (SMS, Phone/Dialer, or Assistant) or fit an approved exception. Permission use must be essential to the app's core functionality and must be declared in Play Console.

ReplyDesk's current direct-SIM auto-reply use case is not being represented as already approved. Play approval must be obtained before launch.

## Two release paths

### 1. Direct/SIM Android build
Purpose:
- preserve current direct SIM behaviour,
- missed-call detection,
- direct SMS auto reply,
- incoming SMS chatbot,
- call log reporting.

This path keeps the existing telephony implementation intact for development/testing and any lawful distribution channel that permits it.

### 2. Google Play build
Before public Play release:
- confirm the final compliant architecture,
- if using restricted permissions, satisfy the applicable default-handler/approved-use requirements,
- request the relevant role before requesting restricted permissions,
- complete Play Console Permissions Declaration,
- provide reviewer instructions,
- provide a video demonstrating the core functionality,
- provide a privacy policy and clear store-listing disclosure.

Do not publish the restricted-permission build to Play until this is confirmed.

## Required Play Console materials

### Core functionality description draft
ReplyDesk is a business communication automation app designed to help businesses respond to missed calls and customer messages. Its core functionality includes configurable automatic replies, keyword-based SMS responses, custom numbered menus, schedules, call/reply logs, and user-controlled automation settings.

### Reviewer instructions draft
1. Install ReplyDesk on a compatible Android phone with telephony support.
2. Open ReplyDesk and review the disclosure that automated messages identify themselves as automated.
3. Configure a missed-call reply.
4. Configure SMS auto reply and one or more custom menu/keyword rules.
5. Configure a reply schedule.
6. Trigger the applicable test flow.
7. Verify that automated messages contain the automation disclosure.
8. Review Call Logs and ReplyDesk event history.
9. Verify STOP / START handling.

### Reviewer video checklist
Show:
- app launch,
- permissions/role request flow,
- Auto Reply page,
- missed-call reply,
- incoming SMS chatbot,
- numbered menu 1–10,
- custom keyword matching,
- schedule controls,
- automated-message disclosure,
- STOP / START,
- call/reply logs,
- log retention controls,
- Test Your Bot.

## Store listing disclosure
The store listing must prominently describe telephony/SMS automation if the Play build uses restricted SMS/Call Log permissions. Do not hide those capabilities in secondary text.

## Privacy requirements
The privacy policy must explain:
- what call/SMS information is accessed,
- why it is accessed,
- what is stored locally or on the backend,
- retention periods,
- how users can clear ReplyDesk-owned history,
- that phone numbers are masked where possible in ReplyDesk logs,
- whether data is shared with service providers,
- account deletion and support contact.

## Current decision
Do not remove the existing Direct/SIM functionality.
Do not connect Railway merely to solve Play policy.
Finish ReplyDesk Core first, then finalise the Play-compliant distribution architecture before Play submission.


## User-controlled device automation architecture

ReplyDesk's Direct/SIM architecture is controlled by the user on their own Android device. ReplyDesk does not use a third-party SMS gateway to send these automated replies to recipients.

The user explicitly controls:
- whether automation is enabled,
- which events trigger replies,
- whether missed calls, incoming SMS, or both are enabled,
- which SIM is used,
- the schedule and allowed days/hours,
- repeat intervals,
- numbered menu options,
- keyword rules,
- and the reply content.

Messages are sent from the user's own Android device using the SIM selected by the user.

This architecture statement is separate from the question of whether incoming SMS content or logs are transmitted to the ReplyDesk backend. That backend-data-flow question must be documented independently and verified against the final implementation before submission.

[VERIFY] Confirm the final production implementation matches this description before using it in Google Play reviewer materials.
