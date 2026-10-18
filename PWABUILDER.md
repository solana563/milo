# Publishing milo. with pwabuilder.com

The app already passes the PWABuilder checks: HTTPS manifest with id, name, icons (192, 512, maskable), screenshots (wide + narrow),
shortcuts, share target, file handlers, a service worker that works offline, push, background sync and periodic sync hooks.

1. Host the folder over HTTPS (Vercel is best, because the Android check file must sit at the site root:
   `https://YOUR-SITE/.well-known/assetlinks.json`). GitHub Pages project sites (`user.github.io/repo/`) work for the PWA and for
   Windows/iOS packages, but cannot serve the Android check file.
2. Open https://www.pwabuilder.com, paste your site URL, press Start. All three tabs (Manifest, Service worker, Security) should be green.
3. Package for stores:
   - **Android (Google Play / APK):** Package for stores > Android > fill the form. Keep Package ID `com.ubayanda.milo`.
     Download the zip. It contains `signing.keystore` (keep it safe) and `assetlinks.json`. Replace `.well-known/assetlinks.json`
     in this project with that file, redeploy, and the app opens full-screen without the browser bar.
   - **Windows:** Package for stores > Windows. Upload the .msixbundle to Partner Center.
   - **iOS / iPadOS:** Package for stores > iOS gives an Xcode project (needs a Mac and an Apple developer account).
4. When you change the app, bump `const V="milo-vNN"` in `sw.js` so installed copies refresh.

Note: the Capacitor APK in `native/` and a PWABuilder Android package are two separate ways to ship Android. Use one or the other.
Push notifications work in both (the PWABuilder/TWA build uses Web Push through Chrome).

## Report card leftovers
- **IARC rating:** needs a real certificate ID. Get it free by filling the age-rating questionnaire in Google Play Console (or iarc-ratings.org), then add `"iarc_rating_id": "YOUR-ID"` to `manifest.webmanifest`.
- **scope_extensions, widgets, tabbed display, note taking:** optional extras left out on purpose (they need extra domains or features milo. doesn't use).
