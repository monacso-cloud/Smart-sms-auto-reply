# Smart SMS Reply — Licensed Android Pilot

This pilot sends an automatic SMS after a missed call from the selected Android SIM.

## Pilot license

- A license must be activated before SMS settings can be enabled.
- The free test license starts when first activated and lasts 30 days.
- Activation is bound to the Android installation/device identifier.
- The background receiver checks the license again before sending every SMS.
- This pilot uses offline validation. Production sales will require a server-backed license service and Stripe webhook so a key cannot be reused across different devices.

## Included

- App icon and on/off switch
- Editable SMS reply
- 0, 15, 30 or 60 second delay
- SIM selection
- One reply per caller within 24 hours
- Last reply status with masked phone number
- License activation status and days remaining

## Installation — English

1. Download the APK only from the official Smart SMS Reply link.
2. On Samsung, open **Settings → Security and privacy → Auto Blocker**. Temporarily switch it off only if it blocks installation.
3. Allow **Install unknown apps** for the app used to open the APK, normally Chrome or My Files.
4. If Google Play Protect blocks this private pilot APK, temporarily pause Play Protect scanning for the supervised installation.
5. Open the APK and install Smart SMS Reply.
6. Open the app and enter the supplied license key.
7. Allow **Phone**, **Call logs**, and **SMS** permissions.
8. Select the SIM, edit the reply, switch Automatic reply on, and tap Save settings.
9. Open **Settings → Apps → Smart SMS Reply → Battery** and choose **Unrestricted**.
10. Test with a different phone: call the Android phone, leave it unanswered, and confirm that one SMS is sent.
11. Re-enable Auto Blocker and Play Protect after installation.

Menu names can vary by Samsung model and Android version. Carrier SMS fees and limits may apply.

## วิธีติดตั้ง — ภาษาไทย

1. ดาวน์โหลด APK จากลิงก์ทางการของ Smart SMS Reply เท่านั้น
2. โทรศัพท์ Samsung ไปที่ **Settings → Security and privacy → Auto Blocker** แล้วปิดชั่วคราวเฉพาะกรณีที่ระบบขัดขวางการติดตั้ง
3. อนุญาต **Install unknown apps** ให้กับ Chrome หรือ My Files ซึ่งเป็นแอปที่ใช้เปิดไฟล์ APK
4. หาก Google Play Protect บล็อก APK รุ่นทดลอง ให้หยุดการสแกนชั่วคราวเฉพาะระหว่างการติดตั้งที่มีผู้ดูแล
5. เปิดไฟล์ APK และติดตั้ง Smart SMS Reply
6. เปิดแอปแล้วกรอก License Key ที่ได้รับ
7. อนุญาตสิทธิ์ **Phone**, **Call logs** และ **SMS**
8. เลือก SIM แก้ไขข้อความ เปิด Automatic reply แล้วกด Save settings
9. ไปที่ **Settings → Apps → Smart SMS Reply → Battery** แล้วเลือก **Unrestricted**
10. ใช้โทรศัพท์อีกเครื่องโทรเข้ามาโดยไม่รับสาย แล้วตรวจว่ามี SMS ตอบกลับหนึ่งครั้ง
11. หลังติดตั้งเสร็จ ให้เปิด Auto Blocker และ Play Protect กลับคืน

ชื่อเมนูอาจแตกต่างกันตามรุ่น Samsung และ Android ค่าบริการและข้อจำกัดของ SMS เป็นไปตามผู้ให้บริการเครือข่าย

## Build with GitHub Actions

The workflow assembles a debug pilot APK and uploads it as the **Smart-SMS-Reply-Beta** artifact. A signed release build is required before commercial distribution.
