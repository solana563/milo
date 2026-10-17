# milo. push notifications (new messages + friend requests while the app is closed)

Web Push covers the installed PWA (Chrome/Edge/Android, Safari 16.4+ when added to the Home Screen).
Firebase Cloud Messaging (FCM) covers the Android APK. Both are sent by one Supabase Edge Function.

## 1. Supabase
1. SQL editor: run `supabase-setup.sql` again (adds `push_subs`, `push_save`, `push_drop`).
2. Edge Functions > Deploy a new function > name it `milo-push`, paste `supabase/functions/milo-push/index.ts`, Deploy.
3. Edge Functions > Secrets, add:
   - `VAPID_PUBLIC`  = the public key already inside index.html (`VAPID_PUB`)
   - `VAPID_PRIVATE` = your private key (never commit it)
   - `VAPID_SUBJECT` = `mailto:you@example.com`
   - `FCM_SERVICE_ACCOUNT` = whole JSON of a Firebase service account key (step 2 below; skip if you only want the PWA)
4. Database > Webhooks > Create (type: Supabase Edge Functions, function: milo-push), three times:
   - table `messages`, event Insert
   - table `friendships`, event Insert
   - table `friendships`, event Update

## 2. Firebase (Android APK only)
1. console.firebase.google.com > Add project > Add Android app, package name `com.ubayanda.milo`.
2. Download `google-services.json` and put it in `native/google-services.json` (the build script copies it into the Android project).
3. Project settings > Service accounts > Generate new private key. Paste that JSON into the `FCM_SERVICE_ACCOUNT` secret.
4. Rebuild the APK (`npm i` in `native/` pulls `@capacitor/push-notifications`).

## Changing the VAPID keys
`npx web-push generate-vapid-keys`, replace `VAPID_PUB` in index.html and both secrets, then users re-open the app once.
