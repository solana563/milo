# Publishing milo. native on F-Droid

## What's already done
- **Two flavours.** `full` (GitHub/Play builds, includes Google Cast) and `fdroid` (no proprietary Google libraries: Cast is replaced by a small stub in `app/src/fdroid/`). Build it with `gradle assembleFdroidRelease`.
- **No signing info or dependency blob** in the APK (`dependenciesInfo` off) and the `fdroid` variant is left unsigned, because F-Droid signs it.
- **Fastlane metadata** (title, descriptions, icon, screenshots, changelog) in `fastlane/metadata/android/en-US/`. Add one `changelogs/<versionCode>.txt` per release and bump `versionCode`/`versionName` in `app/build.gradle.kts`.
- CI checks that the `fdroid` flavour compiles on every push.

## What you must do
1. **Add a free-software licence** file (`LICENSE`) at the repo root, for example GPL-3.0-or-later, Apache-2.0 or MIT. F-Droid will not accept the app without one, and the choice is yours.
2. Make the repo public and **tag a release** that matches the version, e.g. `v1.0` with `versionCode = 1`.
3. Fork <https://gitlab.com/fdroid/fdroiddata>, add `metadata/com.ubayanda.milonative.yml` (below), and open a merge request.

## Recipe (`metadata/com.ubayanda.milonative.yml`)
```yaml
Categories:
  - Multimedia
License: GPL-3.0-or-later        # replace with the licence you chose
SourceCode: https://github.com/<you>/<repo>
IssueTracker: https://github.com/<you>/<repo>/issues
AuthorName: UBayanda

AntiFeatures:
  NonFreeNet:
    en-US: Uses YouTube search and a hosted Supabase account service (both optional).

AutoName: milo.

RepoType: git
Repo: https://github.com/<you>/<repo>.git

Builds:
  - versionName: '1.0'
    versionCode: 1
    commit: v1.0
    subdir: milo-native/app
    gradle:
      - fdroid

AutoUpdateMode: Version
UpdateCheckMode: Tags
CurrentVersion: '1.0'
CurrentVersionCode: 1
```

## Things reviewers may ask about
- **Network services:** the optional account uses Supabase and YouTube search uses the YouTube Data API; both are why `NonFreeNet` is declared. The app works without them.
- **Gradle wrapper:** F-Droid builds with its own Gradle. If a reviewer wants a wrapper, run `gradle wrapper --gradle-version 8.9` inside `milo-native/` and commit `gradlew` and `gradle/wrapper/`.
- **Different signature:** F-Droid signs with its own key, so it cannot update over a GitHub-built APK (same package name, different signature). Uninstall first, or use `applicationIdSuffix` for one of them.
- **Fonts:** Figtree and Fraunces are SIL Open Font Licence; mention that in the licence/credits if asked.
