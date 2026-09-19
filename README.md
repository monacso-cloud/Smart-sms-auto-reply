# Smart SMS Reply Advanced

Android test build for missed-call SMS auto reply and a keyword-based SMS chatbot.

## Current verification target: v0.5.0 menu test

This version must pass real-device testing before sales or Railway licensing work resumes.

1. Install `Smart-SMS-Reply-Advanced-v0.5.2-Sale-Template-Test.apk` from the latest successful GitHub Actions run.
2. Open **Grant or check permissions** and confirm Phone, Call log, Send SMS and Receive SMS are all allowed.
3. On Samsung sideloaded builds, open App info, use the three-dot menu, choose **Allow restricted settings**, then grant SMS permissions.
4. Enter a second phone number and use **Send test SMS**. Confirm the in-app status changes to `Sent successfully`.
5. Enable Automatic reply, save settings, call from the second phone and do not answer.
6. Enable SMS chatbot, save rules, and send a keyword such as `availability` from the second phone.
7. Tap **Load suggested 1–4 chatbot menu**, save, then send `1`, `2`, `3`, and `4` as separate messages. Each number must select only its matching reply.

The app now records carrier/SIM errors instead of silently ignoring them. Do not report the feature as working until steps 4–6 have passed on the target Samsung phone.

## Repository layout

- `app/`: Android source
- `.github/workflows/main.yml`: reproducible APK build
- `railway/`: licensing API intended for a separate Railway service
Old Beta ZIP files are intentionally not used by the build workflow.
Railway licensing must be connected to the Android app and tested before it is used for customer sales.
