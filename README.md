# Smart SMS Reply Advanced — v0.5.3 menu fix test

Fixes the sender-wide 30-second cooldown that discarded a new menu selection after any reply. Known choices now respond without that cooldown; identical SMS broadcasts are deduplicated using a hash of the received PDUs. Fallback greetings retain a 30-second interval, with a separate 20-replies-per-minute limit per sender to contain loops. Send errors and skipped messages appear in the app status.

The generic editable template offers:

The message and welcome editors are now 220dp tall, and the keyword/reply editor is 400dp tall, with readable 16sp text, extra line spacing and vertical scrollbars.

1. Prices and services
2. Availability and cancellations (newly available slots)
3. Booking
4. Human assistance
5. Cancel an appointment
6. Reschedule an appointment

Cancellation and rescheduling replies refer to the business website or booking confirmation email, the business's own notice period, and a possible fee outside that period. The app does not cancel or reschedule a booking itself.

## Installing the separate test app

The v0.5.2 APK has a different signing certificate from the cached CI test key. This build deliberately uses the separate package `com.smartreply.beta.menutest`, labelled **Smart SMS Reply Test**. It does not replace the old app or copy its saved settings. Do not uninstall the old app. Both auto-reply switches start off in a fresh test installation.

1. Install the APK as a separate test app. Before enabling it, turn off missed-call and SMS auto-reply in the old app to prevent duplicate responses. You can turn the old app back on after turning the test app off.
2. In the test app tap **Load suggested 1–6 chatbot menu**, confirm, fill every `[enter ...]` field, then **Save settings**. Put your own notice periods in the Cancel and Reschedule replies. If keeping your old wording, copy it into the test app manually.
3. Confirm Send SMS and Receive SMS permissions, selected SIM and the SMS chatbot switch.
4. From a second phone send `1`, `2`, `1`, `3`, `4`, `5`, `6` in quick succession. Each new message should receive the matching reply. Also test `cancel my appointment` and `reschedule my appointment`.
5. Test STOP, verify replies stop, then START to resume. Carrier SMS costs still apply.
6. If a reply is missing, reopen the app and read Chatbot status, SMS status and Error. A build passing is not proof of network delivery on a physical phone.

## Build and verification

GitHub Actions runs `testDebugUnitTest assembleDebug` and saves the APK with the Gradle test reports. Regression tests exercise the actual template resources and production routing/rate-limit code. They cover rapid choices, repeated option 1, duplicate broadcasts, text/number matching, cancel versus availability, reschedule versus booking, and absence of the owner's business details.

CI also verifies the separate test package ID and the pinned cached signing certificate. An in-place update to the existing v0.5.2 app still requires its original signing key; this APK is not that update.

This is the Advanced test line. The separate `codex/pilot-license-v0.2` prototype is not merged into this version; its workflow replaced the Advanced manifest with one lacking the SMS receiver. Production licensing and Stripe delivery remain separate work.

Android reference: https://developer.android.com/reference/android/provider/Telephony.Sms.Intents#getMessagesFromIntent(android.content.Intent)
