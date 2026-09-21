# ReplyDesk: Business SMS Bot — Developer Handover

## Locked product name
**ReplyDesk: Business SMS Bot**

The owner has explicitly locked this name. Do not change or substitute the product name without explicit owner approval.

## Repository
Repository: `monacso-cloud/Smart-sms-auto-reply`  
Primary branch: `production-clean-v1`

## Android identity
Customer-facing app name: **ReplyDesk: Business SMS Bot**  
Application ID: `com.smartsmsreply.app`  
Version: `1.0.0-rc2`  
Target SDK: API 36

The application ID is intentionally preserved as a technical identifier. Do not change it merely to match the branding.

## Existing app functions
- missed-call detection/reply
- incoming SMS auto replies
- keyword/menu handling
- configurable response text
- reply delay
- repeat/suppression interval
- SIM selection
- status/error information
- STOP/START controls
- user-controlled permission disclosure
- chatbot regression tests

## Current build status
GitHub Actions builds production-candidate APK/AAB artifacts from `production-clean-v1`.

Release artifact names must use the locked ReplyDesk branding:
- `ReplyDesk-Business-SMS-Bot-v1.0.0-RC2-Unsigned.apk`
- `ReplyDesk-Business-SMS-Bot-v1.0.0-RC2-Unsigned.aab`
- final signed AAB should use `ReplyDesk-Business-SMS-Bot` in its filename

## Signing
Do not commit private keystores or passwords.

There are separate signing materials for testing/production and Google Play upload. Preserve existing keys and verify the exact key/certificate before signing.

## Google Play
See:
`docs/GOOGLE_PLAY_SMS_CALL_LOG_DECLARATION.md`

Restricted SMS/Call Log permissions must remain aligned with the shipped user-controlled automation behavior and the Play Store description.

## Railway / licensing
Source exists under `railway/`. Presence of source does not prove the live deployment is connected. Audit the existing Railway service before changing or redeploying anything.

## Stripe
Audit existing Stripe products, prices, subscriptions, and webhooks before creating anything new. Do not create duplicates.

## AAIT Academy
Do not change AAIT Academy, DNS, domains, unrelated courses, or existing product listings as part of Android release work unless specifically requested.

## Sale-ready definition
Do not call the product sale-ready until:
- final signed release exists
- signature is verified
- package identity is verified
- physical-device testing passes
- missed-call and SMS automation passes
- licensing enforcement works
- payment/subscription flow is verified
- Google Play policy requirements are satisfied
- update path preserves the required signing identity
