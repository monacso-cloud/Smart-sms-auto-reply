# ReplyDesk Core — Release Readiness Checklist

## Core product
- [x] Preserve existing Advanced missed-call flow
- [x] Preserve delay, repeat interval and SIM selection
- [x] Incoming SMS chatbot
- [x] STOP / START
- [x] Business-neutral templates
- [x] Custom keywords
- [x] Guided keyword builder
- [x] Numbered menu up to 10 options
- [x] Empty unused menu options allowed
- [x] Menu replies connected to incoming SMS
- [x] Menu appended to missed-call reply when enabled
- [x] Test Bot supports keyword and numbered-menu replies
- [x] Auto Reply master control
- [x] Missed Call on/off
- [x] Incoming SMS on/off
- [x] Always / Off / Custom Hours
- [x] Morning / Afternoon / Evening / After Hours presets
- [x] Day selection
- [x] Call Logs
- [x] ReplyDesk log retention 7 / 14 / 30 days
- [x] Clear ReplyDesk-owned logs
- [x] Automated-reply disclosure enforced at send time
- [x] Guided text on main setup screens
- [x] ReplyDesk Pro — Coming Soon promotion
- [x] Account / Subscription screen scaffold
- [x] Privacy / Terms / Support screen scaffold
- [x] Backend schema for core settings
- [x] Backend schema/API for numbered menu 1–10

## Backend / deployment
- [x] Railway configuration scaffold exists
- [x] JWT authentication scaffold exists
- [x] Subscription entitlement backend scaffold exists
- [x] Database migrations exist
- [ ] Production Railway project connection
- [ ] Production database
- [ ] Production secrets
- [ ] Production account sign-in flow
- [ ] Production mobile/backend sync

## Billing
- [x] Planned 14-day trial
- [x] Planned A$30 monthly
- [x] Planned A$299 yearly
- [ ] Google Play Billing products
- [ ] Purchase flow
- [ ] Restore / entitlement sync where applicable
- [ ] Manage Subscription deep link
- [ ] Final renewal/cancellation copy

## Google Play compliance
- [x] Current policy reviewed
- [x] Permissions-readiness document
- [x] Store-listing draft
- [x] Permissions-declaration draft
- [x] Privacy Policy draft
- [x] Terms draft
- [x] Support draft
- [ ] Final compliant SMS/Call Log architecture confirmed
- [ ] Default-handler/approved-use implementation if required
- [ ] Permissions declaration submitted
- [ ] Reviewer video
- [ ] Production privacy-policy URL
- [ ] Final Data safety form

## Testing before release
- [ ] Physical Android device test
- [ ] Dual-SIM test
- [ ] Missed-call reply test
- [ ] SMS menu 1–10 test
- [ ] Keyword collision test
- [ ] STOP / START test
- [ ] Overnight schedule test
- [ ] Log retention test
- [ ] Account deletion production test
- [ ] Subscription purchase test
