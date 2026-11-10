# Firebase for the native Android app (push notifications)

What it gives you: new messages and friend requests arrive instantly, even when milo. is closed, instead of being checked every 15 minutes.
Your server already has the sending side (`supabase/functions/milo-push`, described in `PUSH.md`). This file covers the native app.

Only the **full** app supports push. The F-Droid and Lite apps contain no Google libraries and keep the 15-minute check.
Without a `google-services.json` the app builds and runs exactly as before, with push off.

## One-time setup
1. **Firebase console** (project `milo-2026563`): Project settings > Your apps > Add app > Android.
   Package name `com.ubayanda.milonative`. Download `google-services.json`.
2. **Give it to the build** (pick one):
   - GitHub: Settings > Secrets and variables > Actions > New secret named `GOOGLE_SERVICES_JSON`, paste the whole file. The native workflow writes it into the build for you.
   - or put the file at `milo-native/app/google-services.json` (it is not a password, but it holds your project's public API key; keep the repository private or use the secret).
3. **Server**: follow `PUSH.md` step 1 (run `supabase-setup.sql`, deploy `milo-push`, set the `FCM_SERVICE_ACCOUNT` secret, add the three database webhooks). Nothing new is needed on the server for the native app.
4. Build and install. Sign in. Profile shows an "Instant message notifications" switch (only in builds that have the Firebase file).

## How to check it works
- Phone A signed in with push on. Close milo. completely. From another account send A a message: the notification should arrive within seconds.
- If nothing arrives: check Supabase > Edge Functions > milo-push > Logs; check that `push_subs` has a row of kind `fcm` for the account; check Android notification permission for milo.; make sure the three webhooks exist.

## Notes
- Signing out or switching the setting off removes this phone's address from the server.
- The notification text is whatever `milo-push` sends (a short preview of a text message). If you don't want previews on the lock screen, change the body in `milo-push/index.ts` to a fixed text.
- Firebase App Distribution (`FIREBASE-DISTRIBUTION.md`) uses `FIREBASE_APP_ID`. For the native app that must be the App ID of the Android app named `com.ubayanda.milonative`, not the older `com.ubayanda.milo` one.
- Analytics and Crashlytics are deliberately not added: they would send data about people's use to a third party, which needs its own consent screen. The existing error report (Profile) stays under the person's control.
- Raising the app's target Android version past 34 later needs a check of the foreground-service rules for downloads and playback.
