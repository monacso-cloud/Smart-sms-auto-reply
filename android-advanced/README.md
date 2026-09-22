# Smart SMS Reply — Android Beta

This first beta focuses on one job: after a missed call, send an automatic SMS from the selected Android SIM.

## Included

- App icon and on/off switch
- Editable SMS reply
- 0, 15, 30 or 60 second delay
- SIM selection
- One reply per caller within 24 hours
- Last reply status with masked phone number

## Build the APK with GitHub

1. Create a private GitHub repository named `smart-sms-auto-reply`.
2. Extract this ZIP file on your computer.
3. Upload all extracted files and folders to the repository, including `.github`.
4. Commit to the `main` branch.
5. Open **Actions** → **Build Android APK**.
6. When the run is green, open it and download **Smart-SMS-Reply-Beta**.
7. Extract the downloaded artifact to get `app-debug.apk`.

## Install on Samsung

1. Send `app-debug.apk` to the Samsung phone or download it there.
2. Open it and allow installation from the browser or Files app when Android asks.
3. Open **Smart SMS Reply**.
4. Allow Phone, Call logs and SMS permissions.
5. Select the SIM, edit the reply, turn the service on and save.
6. In Samsung battery settings, set the app to **Unrestricted**.
7. Test with a different phone by making a call and leaving it unanswered.

## Important beta notes

- This is a test build, not yet a production release.
- SMS charges and carrier limits may apply.
- Some Samsung battery settings can stop background operation until the app is set to Unrestricted.
- The first test must be supervised. Confirm the recipient and message before using it for customers.
