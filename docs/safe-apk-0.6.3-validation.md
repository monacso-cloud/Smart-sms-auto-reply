# ReplyDesk 0.6.3 signed repair candidate

This candidate is based on PR #5 (`3b616765dc76112d8470d4404e86a10a161f151b`), retaining the Advanced Android source, the existing interface, business profiles, and approved customer wording. Package remains `com.smartreply.beta`; versionCode is 9. No backend deployment, merge, or public release is part of this change.

## Repairs

- Incoming broadcasts assemble one complete message before responding; malformed or mixed-sender parts fail closed.
- Durable per-business receipt identity uses canonical sender, the earliest valid source timestamp, and the full exact body. Duplicate broadcasts cannot resend after the cooldown or receiver recreation. A new identical-content SMS with a different source timestamp is a new event.
- Receipt and STOP/START/HUMAN state are committed together before acknowledgement. START explicitly supersedes old raw-number opt-out hashes; owner Resume still respects STOP, and business takeover still overrides START.
- Normalized 30-second SMS reservations and configured missed-call cooldown reservations are committed before dispatch. Any failed reservation suppresses dispatch. No automatic resend follows failed/uncertain Android delivery callbacks.
- A fallback remains off by default and, if enabled, is reserved once per normalized sender until Resume/START. Approved reply text is unchanged.
- Missed-call row/date claims now check durable commit success. Duplicate jobs cannot submit twice. Missed-call opt-out and cooldown use the same canonical sender as incoming SMS.
- Fractional or conflicting SIM/slot metadata fails closed. The sending SIM is still the verified active receiving subscription, never the default SIM.
- Returning from chatbot setup refreshes a general reply changed there, so the older settings screen cannot overwrite it. Local unsaved drafts survive when setup has not changed that saved field. Settings Save reports success only after a successful synchronous commit; answer-card Save already follows that contract.

## Ledger boundary

Incoming event hashes are retained for 30 days based on source time. Older/undated SMS and source timestamps more than 24 hours in the future are skipped, so a delayed duplicate cannot become new after cleanup. No customer message body is stored in this ledger. Existing reply logs and settings are unchanged.

## Verification

The signed-candidate workflow runs `gradle -p android-advanced testDebugUnitTest assembleRelease`, checks the existing expected SHA-256 signer certificate `7be4051ec5fbeddaa9808897706a1b793f423ed8f4dde50204d7da98208f6bcb`, confirms package identity and non-debuggable status, then uploads only the APK. Its permissions, signing secret names, alias selection, and cleanup are unchanged. The additional push trigger names only this repair branch.

Regression coverage includes answer creation/edit/cancel/recreation, failed-save behavior, stale settings return, per-SIM isolation, normalized duplicate and cooldown handling, duplicate STOP/START ordering, fallback once, old opt-out migration, multipart broadcasts, malformed messages, missing/conflicting/inactive SIMs, failed receipt/reservation commits, duplicate missed-call jobs, no retry after delivery failure, and preventing rule-configuration text from being transmitted. Robolectric simulates telephony and does not send real SMS. The exact build run and results belong in the pull request, not a claim that physical-device acceptance has passed.

## Phone acceptance still required

Use only owner-controlled numbers. Before customer use, verify Save/Save Changes/Cancel with the keyboard open, force-stop/reopen and reboot persistence, an in-place update without data loss, actual SIM1/SIM2 routing, multipart incoming/outgoing messages, repeated phone-state/SMS events, STOP/START/HUMAN, and failed/no-service sends. The installed phone's version and signing certificate have not been inspected. Install over the existing app only if Android accepts the matching package/signature and increasing version; do not uninstall or clear storage to work around an incompatibility because backup is disabled.

Human takeover is manual and applies to incoming SMS automation. This build does not detect replies sent from another SMS app and does not extend takeover to missed-call automation. No production SMS or live-radio test was performed by the automated suite.
