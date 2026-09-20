# Smart SMS Reply Advanced — v0.5.3 menu fix test

Fixes the sender-wide 30-second cooldown that discarded a new menu selection after any reply. Known choices now respond without that cooldown; identical SMS broadcasts are deduplicated using a hash of the received PDUs. Fallback greetings retain a 30-second interval, with a separate 20-replies-per-minute limit per sender to contain loops. Send errors and skipped messages appear in the app status.

The generic editable template offers:

1. Prices and services
2. Availability and cancellations (newly available slots)
3. Booking
4. Human assistance
5. Cancel an appointment
6. Reschedule an appointment

Cancellation and rescheduling replies refer to the business website or booking confirmation email, the business's own notice period, and a possible fee outside that period. The app does not cancel or reschedule a booking itself.

## Updating and testing

1. Back up your current reply text. Install the new Advanced APK over the existing Advanced app. If Android rejects the update, keep the existing app and report the exact error; do not uninstall and lose settings.
2. Existing saved replies remain until you choose to load a template. To use all six generic options, tap **Load suggested 1–6 chatbot menu**, confirm the replacement, fill every `[enter ...]` field, then **Save settings**. Put your own notice periods in the Cancel and Reschedule replies.
3. Confirm Send SMS and Receive SMS permissions, selected SIM and the SMS chatbot switch.
4. From a second phone send `1`, `2`, `1`, `3`, `4`, `5`, `6` in quick succession. Each new message should receive the matching reply. Also test `cancel my appointment` and `reschedule my appointment`.
5. Test STOP, verify replies stop, then START to resume. Carrier SMS costs still apply.
6. If a reply is missing, reopen the app and read Chatbot status, SMS status and Error. A build passing is not proof of network delivery on a physical phone.

## Build and verification

GitHub Actions runs `testDebugUnitTest assembleDebug` and saves the APK with the Gradle test reports. Regression tests exercise the actual template resources and production routing/rate-limit code. They cover rapid choices, repeated option 1, duplicate broadcasts, text/number matching, cancel versus availability, reschedule versus booking, and absence of the owner's business details.

This is the Advanced test line. The separate `codex/pilot-license-v0.2` prototype is not merged into this version; its workflow replaced the Advanced manifest with one lacking the SMS receiver. Production licensing and Stripe delivery remain separate work.

Android reference: https://developer.android.com/reference/android/provider/Telephony.Sms.Intents#getMessagesFromIntent(android.content.Intent)
