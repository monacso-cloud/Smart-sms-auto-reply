# ReplyDesk platform foundation

## Existing Android beta already present

The current Android beta already contains:
- missed-call detection
- automatic SMS sending
- SIM selection
- configurable delay
- editable reply message
- basic local status
- Android permissions for phone state, call log and SMS sending

These features should be preserved and migrated carefully rather than rebuilt blindly.

## Platform foundation being added

ReplyDesk should remain separate from MANEE Anatomy and AAIT.

Architecture:

Android / future iOS client
→ ReplyDesk API
→ Railway-hosted backend
→ PostgreSQL
→ subscription entitlement services
→ automation rules / event log / team handover

## Backend responsibilities

1. Account and business identity
2. Subscription entitlement state: trial / active / expired / cancelled
3. Business hours and scheduled auto-reply rules
4. Reply mode: missed calls / inbound messages / both
5. Message-event logging without storing unnecessary sensitive content
6. Department and staff routing
7. Human handover ownership and accepted status
8. Multi-device status synchronisation
9. Subscription receipt/event verification
10. Audit and operational logs

## Railway

This repository now includes a Railway deployment scaffold and health endpoint.

Required Railway variables:
- NODE_ENV=production
- PORT (normally provided by Railway)
- DATABASE_URL
- APP_ORIGIN
- JWT_SECRET
- SUBSCRIPTION_PROVIDER_MODE=test

Store credentials must be added only when the corresponding store integration is implemented. Never commit secrets to GitHub.

## Android work still required

- move source out of the legacy ZIP-build pattern into a normal Android project structure
- target the current Google Play required Android API
- add explicit automation mode controls
- add business-hours / custom-hours scheduling
- add inbound SMS support only if Play policy eligibility is confirmed
- add HUMAN handover
- add department routing
- add backend sync and authenticated account
- add subscription screen
- add privacy / terms / delete-account screens
- produce Play Store release build (AAB)
- complete Play Console permission declarations and testing

## iOS note

iOS cannot simply copy Android's direct call-log/SMS behaviour. The iOS client will need a different architecture using supported telephony/messaging integrations for features that Apple does not expose to ordinary apps.

## Subscription

Subscription architecture should be isolated behind server-side entitlement state so Android and iOS can map their store purchases to the same ReplyDesk account.

## Security

- no secrets in the mobile app or repository
- minimise stored phone/message data
- mask phone numbers in operational dashboards where possible
- authenticate all account/business APIs
- verify store purchase events server side
- use role-based access for owners, managers and staff
