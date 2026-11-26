# Releasing milo. (website download and in-app update message)

1. Make sure the repo has the release-signing secrets (`MILO_KEYSTORE_B64`, `MILO_KEYSTORE_PASSWORD`, `MILO_KEY_ALIAS`, `MILO_KEY_PASSWORD`). Keep that key safe: Android only installs an update that is signed with the same key.
2. Tag a version and push it: `git tag v1.1.0 && git push origin v1.1.0`.
3. The **Build milo. native APK** workflow builds, tests, and (on a `v*` tag) its `release` job publishes a GitHub Release with `milo.apk` and `milo.apk.sha256`. Tags with a dash (`v1.1.0-rc1`) become pre-releases and are not offered to users.
4. That is all. Two things read the release:
   - the website's Download button points at `https://github.com/<owner>/<repo>/releases/latest/download/milo.apk` (set `repo` in `milo-website/src/site.ts` once);
   - the app, a few seconds after launch (at most every 12 hours), asks GitHub for the newest release and shows "Update available" with a Download button when its version is higher than the installed one. "Not now" hides that version until a newer one appears.

The F-Droid build never shows this message (F-Droid updates the app). The repository the app checks is the one that built it (`GITHUB_REPOSITORY`); set `MILO_UPDATE_REPO` to override.
