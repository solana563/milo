# milo. Lite

A separate, lighter app: package `com.ubayanda.milonative.lite`, Android 8.0 (API 26) and up, installs next to the normal milo.
Built from the same code with the `lite` flavor (`gradle assembleLiteDebug` / `assembleLiteRelease`, or the "Build milo. Lite APK" workflow).

Kept: local library, YouTube streaming, downloads, lyrics, Smart play, playlists, likes, sign-in + sync, friends and chat, EQ.
Swapped: "Listen together" -> "Share my EQ" (sends your EQ curve in chat; a friend taps "Try it").
Left out: Google Cast, home-screen widgets, visualiser, liquid glass / blur effects, x86 builds.

Google sign-in: add an Android OAuth client in Google Cloud for package `com.ubayanda.milonative.lite` with the signing SHA-1
(the release SHA-1 is printed by the fingerprint workflow; the package name is the only change).
