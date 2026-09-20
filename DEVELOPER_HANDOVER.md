# Smart SMS Reply — Developer Handover

## Purpose
This handover is for a new developer/agent continuing the existing Smart SMS Reply project. Do not restart the project from scratch unless a specific component is proven unusable.

## Repository
Repository: monacso-cloud/Smart-sms-auto-reply
Primary handover branch: production-clean-v1
Current handover commit before this note: 299e0e33505d3df95691a65c0461d4b781830bc3

## Product Goal
Android app that automatically replies by SMS to:
- missed calls
- incoming SMS keywords/menu choices
- configurable business responses

Planned commercial distribution requires a production-signed APK and licence/subscription enforcement.

## Keep / Existing Work

### Android app source
Located under:
- app/src/main/java/com/smartreply/beta/
- app/src/main/res/
- app/src/main/AndroidManifest.xml

Existing classes include:
- MainActivity.java
- PhoneStateReceiver.java
- MissedCallReplyWorker.java
- SmsReplyReceiver.java
- SmsSender.java
- SmsStatusReceiver.java
- ChatbotRules.java
- ChatbotReplyGate.java

### Existing app functions
Existing code includes:
- missed-call detection/reply workflow
- SMS auto replies
- keyword/menu reply handling
- configurable missed-call reply message
- reply delay setting
- repeat/suppression interval setting
- SIM selection
- status/error information
- chatbot/menu responses
- test coverage for chatbot regression logic

Do not remove these without first testing the current implementation.

## Production Android Identity
Target applicationId:
com.smartsmsreply.app

Current production candidate version:
1.0.0-rc1

The Java source package/namespace is still:
com.smartreply.beta

This is source-package history and can be refactored later if desired. The commercial Android applicationId is the important installed-app identity.

## Current Android Build Status
The source compiles far enough to reach signing/package steps, but the clean production workflow is currently failing before a successful production APK is produced.

Workflow:
.github/workflows/main.yml

Latest diagnostic improvement:
- restores signing keystore from a Base64 GitHub Actions secret
- verifies a deterministic SHA-256 checksum
- verifies the key with keytool before build
- builds release APK
- verifies APK signature
- verifies package name is com.smartsmsreply.app

### Current unresolved build issue
The GitHub Actions secret used for SMART_SMS_RELEASE_KEYSTORE_B64 has repeatedly failed Base64 restoration/verification in CI.

Important:
- Do not keep guessing passwords.
- First prove that the exact keystore bytes in CI match the expected local file.
- Then verify store password and alias with keytool.
- Only then build/sign.

## Signing Material
Production signing material is intentionally NOT committed to this repository.

The owner has a separate private signing bundle.
Do not commit:
- .jks/.keystore files
- Base64 keystore content
- store/key passwords

Expected alias:
smart-sms-reply

GitHub secret names currently used:
- SMART_SMS_RELEASE_KEYSTORE_B64
- SMART_SMS_RELEASE_STORE_PASSWORD

A legacy secret named SMART_SMS_RELEASE_KEY_PASSWORD may still exist, but the current clean workflow is intended to use one password for both store and key.

## Railway / Licensing Backend
Source exists under:
railway/

Files include:
- server.js
- package.json
- package-lock.json
- railway.json
- README.md

The backend source contains licensing API logic and database schema setup.

IMPORTANT:
Presence of this source does NOT prove the live Railway deployment is correctly deployed or connected to the Android app.

Before changing anything:
1. Inspect the existing live Railway project.
2. Do not delete/redeploy a live service until its current state is recorded.
3. Verify environment variables, database, domain, and health endpoint.
4. Verify whether Android currently calls the licensing API.
5. Preserve any working live configuration.

## Stripe
Do not assume Stripe is absent or broken.
The current GitHub Android source does not contain a verified Stripe integration.

Before making changes:
1. Audit the owner's existing Stripe account/products/prices/webhooks.
2. Preserve anything already working.
3. Confirm the intended subscription/licensing flow.
4. Connect Stripe to Railway licensing only after current live state is documented.

Do not create duplicate products/subscriptions unless necessary.

## AAIT Academy Website
Do not modify or redeploy AAIT Academy as part of the Android build repair.

The owner believes product/listing work may already exist on the AAIT Academy website.
Audit it first and preserve it.

Do not:
- remove existing listings
- change DNS/domains
- replace the AAIT site
- change unrelated courses/products

The Smart SMS Android build can be repaired independently.

## Systems That Must Not Be Interrupted
Do not interrupt or reset:
- live Railway services
- Stripe account/products/payments
- AAIT Academy website/domain
- unrelated AAIT products/courses

Audit first. Change only what is required.

## Old Work
Two old pull requests were closed without merge:
- PR #1: Smart SMS Reply Advanced v0.5.2 sale template and licensing foundation
- PR #2: Add 14-day pilot license and installation guide

Old branches/history may still exist for reference.
Use production-clean-v1 as the handover branch.

## Recommended Next Steps
1. Clone production-clean-v1.
2. Build locally without production signing first to confirm source integrity.
3. Inspect Gradle/AGP/JDK versions.
4. Verify the private signing keystore locally with keytool.
5. Build a locally signed release APK.
6. Verify APK with apksigner.
7. Install on a physical Android phone.
8. Test:
   - permissions
   - missed call auto reply
   - repeat interval
   - SMS keyword replies
   - menu 1-6
   - dual SIM behavior if relevant
9. Audit Railway live deployment without changing it.
10. Audit Stripe without creating duplicates.
11. Audit AAIT Academy listing without changing unrelated site content.
12. Only after all above, connect licensing/subscription end-to-end.

## Sale-Ready Definition
Do not call the app sale-ready until all of these are true:
- signed production release APK exists
- signature verified
- package name verified
- physical-device installation passes
- missed-call/SMS functions pass
- licensing enforcement works
- subscription/payment flow is verified
- update path with the same signing key is preserved

## Important Instruction From Owner
Preserve everything already completed and working.
Do not make the owner redo setup that already exists.
Check first, then fix only the missing or broken pieces.
