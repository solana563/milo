# "Your account is verified" page

1. Host `verified.html` next to the other web files (same place `index.html` is hosted).
2. Supabase dashboard > Authentication > URL Configuration:
   - Site URL: the hosted address of verified.html, e.g. https://YOUR-SITE/verified.html
   - Redirect URLs: add the same address.
3. Optional: put the same address in `CloudApi.VERIFY_URL` (milo-native/.../data/CloudApi.kt) so every sign-up asks for it explicitly.
4. The page opens the app with `milo://verified` (registered in the Android manifest). If the phone blocks that, it shows an Open milo. button and a note to close the page and open milo.
