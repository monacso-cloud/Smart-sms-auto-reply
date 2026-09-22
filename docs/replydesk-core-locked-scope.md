# ReplyDesk Core — Locked First-Release Scope

This document records the agreed first-release direction so future development improves the existing Advanced app instead of replacing or removing working functionality.

## Preserve what already works
- Do not remove or replace the existing Advanced missed-call reply flow.
- Keep SIM selection, reply delay, repeat interval, incoming SMS chatbot, STOP/START handling and fallback replies.
- Improve around the existing implementation rather than rebuilding from scratch.

## Core customisation
- Master Auto Reply on/off.
- Reply to missed calls on/off.
- Reply to incoming SMS on/off.
- Businesses can use missed calls only, SMS only or both.
- Custom schedule with selected days and custom start/end time.
- Quick presets: Morning, Afternoon, Evening and After Hours.
- Customisable numbered menu (1–4) with business-owned labels and replies.
- Custom keywords with multiple phrases mapping to one reply.
- Guided keyword rule builder plus advanced editor.
- Test Your Bot must test both keyword matching and numbered menu replies.
- Business-neutral templates only. Never ship a customer's personal business information in starter templates.
- Use editable placeholders such as [BUSINESS NAME], [PRICE], [WEBSITE], [BUSINESS ADDRESS], [BUSINESS HOURS].

## Automated-message disclosure
- Every automated outbound SMS must identify itself as automated.
- The app should enforce a disclosure prefix such as `Automated reply:` at send time so businesses do not have to remember to add it manually.
- This applies to numbered-menu replies, keyword replies, fallback replies, STOP/START confirmations and missed-call auto replies.

## User guidance
Every major settings screen should explain:
1. what the section does,
2. what the business owner should enter or choose,
3. an example when useful.

## Call logs
- Show recent Android call history where permission allows.
- Show ReplyDesk auto-reply event history separately.
- Filters: 1 week, 2 weeks, 1 month.
- Mask phone numbers where possible.
- ReplyDesk event retention: 7, 14 or 30 days.
- Auto-delete old ReplyDesk logs to reduce storage.
- Clear Logs clears ReplyDesk-owned history only; it must not delete the phone's system call history.

## ReplyDesk Pro — future paid upgrade
Do not block the Core launch on large-organisation features.

Advertise ReplyDesk Pro as Coming Soon for larger businesses and organisations. Future Pro scope may include:
- multiple staff,
- departments,
- advanced routing,
- external number/team forwarding,
- shared handover/takeover status,
- organisation-level permissions and workflows.

Pro features must not be represented as currently available until implemented.

## Release order
1. Finish and validate ReplyDesk Core structure.
2. Keep Android build green.
3. Prepare/store-compliance work.
4. Connect backend deployment/Railway only after Core structure is stable.
