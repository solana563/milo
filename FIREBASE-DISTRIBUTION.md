# Send the test APK to your phone with Firebase App Distribution

Project: milo-2026563. The GitHub workflow can upload every APK build there automatically.

1. Firebase console → Project settings → Your apps: make sure an **Android app with package `com.ubayanda.milo`** exists. Copy its **App ID** (looks like `1:123…:android:abc…`). Download `google-services.json` into `native/` too (push notifications).
2. App Distribution → Get started → Testers & Groups → create a group named **testers** and add your email.
3. Google Cloud → IAM → Service accounts: create one with role **Firebase App Distribution Admin**, make a JSON key.
4. GitHub repo → Settings → Secrets and variables → Actions → add:
   - `FIREBASE_APP_ID` = the App ID from step 1
   - `FIREBASE_SERVICE_ACCOUNT` = the whole JSON key text
5. Run Actions → "Build Android APK". When it finishes, you get an email / the Firebase Tester app shows the build. Install it.

Without the two secrets the step is skipped and the APK is still available as the `milo-debug-apk` download on the run.
