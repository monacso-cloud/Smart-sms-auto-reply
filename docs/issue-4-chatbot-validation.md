# ReplyDesk Issue 4 test candidate

Based on the maintained Advanced source on PR #3, not the legacy root Beta files. Existing answer persistence uses synchronous SharedPreferences commit, per subscription ID. This patch retains the approved answer and fallback strings.

Changes: Save for creation, Save Changes for editing, Cancel discards edits; bounded scrollable editor with action buttons outside fields and keyboard resize. Fallback defaults off (including upgrades without an explicit setting), can be enabled per business, and is reserved durably once per normalized sender before dispatch. Failed dispatch remains reserved to avoid repeated unsolicited replies. Owner Resume or inbound START resets the reservation. Owner can pause all incoming SMS automation for a business or pause/resume one customer. Inbound HUMAN pauses that customer without sending an unapproved acknowledgement. Owner Resume does not override STOP. These controls do not change missed-call automation.

Manual takeover is required before replying in another SMS app. Automatic detection of owner messages is not implemented; this app does not have a verified outgoing-message observation path.

## Automated verification

Run `gradle -p android-advanced testDebugUnitTest assembleDebug` in an Android SDK environment. Regression tests cover create/edit/cancel and activity recreation, per-SIM isolation, fallback disabled by default and repeat suppression after cooldown, HUMAN and owner takeover. Robolectric uses simulated SMS; it does not send to real recipients. Activity recreation is not proof of physical device process restart.

## Physical phone acceptance (pending)

Use only owner-controlled test numbers and review every configured message before enabling.

1. Add answer, Save; edit, Save Changes; edit again, Cancel. Confirm only saved changes appear. Open keyboard on a small screen and confirm both actions remain reachable.
2. Force-stop and reopen app, then reboot phone. Confirm answers and takeover state remain. Test with both SIM profiles; business B must not inherit A's answers or state.
3. Fallback off: unmatched SMS sends nothing. Enable it: first unmatched SMS sends approved general reply, further unmatched SMS after 30 seconds and after restart sends nothing.
4. Pause customer: matched and unmatched SMS send nothing. Resume: configured automation works again and one-time fallback resets. STOP must remain respected after owner Resume.
5. HUMAN pauses that sender. START resumes unless business takeover is still enabled. Business takeover pauses every incoming SMS reply on that SIM.
6. On a dual-SIM phone send distinct keywords to both lines; confirm replies originate from the receiving SIM. Repeat while changing default sending SIM and disabling an eSIM. No routing guesses or fallback to another SIM.

No physical device, test SIM, or approved test recipient was available in this workspace. Installed APK identity and real radio behavior remain unverified. This candidate is not a production readiness claim.
