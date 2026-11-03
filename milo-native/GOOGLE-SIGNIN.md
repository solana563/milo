# Google sign-in setup (one time, in your own accounts)

Client ID used by web and native: `607031598574-svt5i48ppk9udu7mujo76k2j89j2qk3u.apps.googleusercontent.com`

1. **Supabase** > Authentication > Providers > Google: switch it on, paste that client ID and the client secret from Google Cloud. Add the same client ID under "Authorized Client IDs" and tick "Skip nonce checks" (the web button and Android's Credential Manager do not send a nonce).
2. **Google Cloud** > APIs & Services > Credentials, same project as the client ID:
   - Web client: add every site that serves milo. (for example your Vercel domain) under "Authorized JavaScript origins".
   - Android: create an "Android" OAuth client with package `com.ubayanda.milonative` and the SHA-1 of the key that signs the APK (debug key for debug builds, your release key for releases). Add one per signing key.
3. The Android web-wrapper APK and the Windows app cannot show the Google button (their web address is not a normal website Google accepts); use the native app, the website, or email sign-in there.
4. The F-Droid build has no Google libraries, so it hides the button.
