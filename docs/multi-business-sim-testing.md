# Multi-business / SIM release candidate 0.5.0

Android only. Each Android subscription ID has separate settings, schedules, menus, keyword rules, opt-outs, cooldowns and application logs. The UI selection is never used by receivers. No fallback to the default SIM. Saved profiles are retained when an eSIM is disabled. Newly issued subscription IDs get a new paused profile; never identify a business by carrier name or slot position alone.

## Upgrade and migration
Install over signed 0.4.0 (same application ID and pinned signing certificate). Do not uninstall. Old global preferences are kept untouched. On first launch allow Phone permission, name the detected active SIM profiles, then choose **Import my previous settings** on the correct business once. This copies settings into that profile but leaves replies paused. Configure other profiles separately and enable each after reviewing it. Previously enabled automation is intentionally paused during this migration to avoid sending another business's text.

## Phone acceptance gate (not replaced by automated tests)
Use a second phone to call/text both business numbers. Confirm the actual sending number on that phone, not just the app log. Results below are pending until a physical phone is used.

| Test | Expected | Result |
|---|---|---|
| Same keyword sent to both lines | Each business's distinct answer from its own number | Pending |
| Plain SMS mode on A, chatbot on B | Each follows its own mode | Pending |
| Missed calls to A then B | Correct business text, reply from receiving SIM | Pending |
| Two close missed calls on different lines | Both handled once; no mixed business | Pending |
| Default Android SMS SIM changed to B | Incoming A still replies through A | Pending |
| STOP on A, then SMS to B from same caller | A opted out; B unaffected; START on A restores A | Pending |
| Different schedules and repeat intervals | Each profile obeys its own settings | Pending |
| Disable receiving eSIM before delayed reply | No fallback to another line; skipped log | Pending |
| Re-enable same eSIM | Existing profile and messages retained | Pending |
| Remove/reissue eSIM with new subscription ID | New paused profile; no automatic reassignment | Pending |
| Permission denied / unknown receiving SIM | No cross-SIM send; diagnostics explain skip | Pending |
| Screen locked / battery saver | Observe Android scheduling delays and actual sending | Pending |
| Multi-part reply / no credit or no service | All parts use same SIM; callbacks show failure when reported | Pending |
| Upgrade from signed 0.4.0 | Data retained; import only into chosen profile | Pending |

## Platform boundaries
Only currently active voice/SMS-capable subscriptions can receive and send. Storing many eSIMs is not equivalent to activating them all simultaneously. Data-only eSIMs cannot send SMS. This APK does not implement iPhone automation.

Missed-call routing uses CallLog PHONE_ACCOUNT_COMPONENT_NAME + PHONE_ACCOUNT_ID with TelephonyManager.getSubscriptionId(PhoneAccountHandle), available on Android 11+. Older Android or OEMs without a usable mapping skip the reply rather than guess. SMS broadcasts without a valid subscription ID are also skipped. Jobs expire after five minutes and are not persisted across reboot; Android may delay them in idle/battery saver. SMS submitted/sent is distinct from delivery; delivery receipts are not requested.

Automation tests cover per-profile storage and editor isolation, one-time migration, inbound SMS ID validation, exact call-account mapping, same-SIM sending despite another default, and no fallback after removal. Live radio behavior and OEM call-log metadata still require the phone tests above.
