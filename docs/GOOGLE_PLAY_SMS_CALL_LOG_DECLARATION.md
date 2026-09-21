# Google Play restricted-permission declaration — ReplyDesk: Automated SMS Bot

## Core functionality
ReplyDesk: Automated SMS Bot is a user-configured device automation tool for business SMS workflows. The device owner creates and controls the automation. The app provides the on-device infrastructure that executes the owner's rules.

The owner independently controls:
- whether automation is enabled or disabled;
- the text of automated responses;
- incoming-message keyword and response rules;
- response delay and repeat settings; and
- the SIM used on supported devices.

The developer does not choose the recipient-specific message content or enable automation on the owner's behalf.

## Requested restricted permissions and necessity
- SEND_SMS: sends the response configured by the device owner when the owner's enabled rule is triggered.
- RECEIVE_SMS: detects an incoming SMS trigger so the owner's configured SMS rule can run.
- READ_CALL_LOG: determines whether an incoming call resulted in a missed-call event so the owner's configured missed-call response can run.
- READ_PHONE_STATE: supports phone-state/SIM operation needed by the on-device workflow.

These permissions support the advertised core functionality. They are not requested for advertising, profiling, authentication, contact harvesting, or sale of SMS/call-log data.

## Google Play declaration path
Requested exception category: Device automation.

Google describes this category as apps that automate repetitive actions across multiple areas of the OS based on one or more conditions (triggers) set by the user. Google lists SEND_SMS, RECEIVE_SMS and READ_CALL_LOG among permissions eligible for this exception, subject to Google Play review and approval.

## Review demonstration
The review video should show, without cuts:
1. Fresh install / permissions not granted.
2. ReplyDesk: Automated SMS Bot explaining why the restricted permissions are needed before the Android permission prompt.
3. The device owner continuing and granting permissions.
4. The owner editing their own auto-response text.
5. The owner configuring/enabling the automation.
6. A missed-call trigger causing the configured response.
7. An incoming SMS trigger causing the owner's configured rule response.
8. The owner disabling automation.

## Store-listing alignment
The Play Store description must prominently describe ReplyDesk: Automated SMS Bot as a user-configured business SMS automation tool. Do not describe restricted permissions as incidental features.

## Submission note
Eligibility is subject to Google Play review. This document is the factual basis for the Permissions Declaration Form and must stay consistent with the shipped app behavior.
