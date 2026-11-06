# Google sign-in for the Android app (one time)

The app already contains the Google button and code. What is missing is Google Cloud + Supabase setup.
Package name: `com.ubayanda.milonative`
Web client ID already in the app: `607031598574-svt5i48ppk9udu7mujo76k2j89j2qk3u.apps.googleusercontent.com`

## 1. Get the SHA-1 fingerprints
Run "Build milo. native APK" on GitHub. Open the run, scroll to the bottom of the run's summary page: the heading
"Signing fingerprints for Google Cloud" lists the SHA-1 of the **debug key** and of your **release key**.

## 2. Google Cloud Console (console.cloud.google.com), project that owns the client ID above
1. APIs & Services > OAuth consent screen: app name `milo.`, support email, developer email. Publish it (Testing mode only lets listed test users in).
2. APIs & Services > Credentials > Create credentials > OAuth client ID > **Android**. Create one for each fingerprint:
   - Name: `milo debug` / Package name: `com.ubayanda.milonative` / SHA-1: the debug SHA-1
   - Name: `milo release` / Package name: `com.ubayanda.milonative` / SHA-1: the release SHA-1
   - (If you publish on Google Play with Play App Signing, add a third one with the SHA-1 shown in Play Console > Setup > App signing.)
3. The existing **Web application** client (the ID above) stays as it is. Its client ID goes in the app; its client secret goes in Supabase. You do not put anything from the Android clients into the app: they only need to exist so Google trusts the app.

## 3. Supabase > Authentication > Providers > Google
- Enable it. Client ID: the Web client ID above. Client Secret: the Web client's secret from Google Cloud.
- Authorized Client IDs: the same Web client ID. Tick "Skip nonce checks".

## 4. Try it
Install a build whose fingerprint you registered, tap Continue with Google. "No credentials available" or error 10 / 16 means the SHA-1 of the installed build isn't registered yet.
