# Firebase App Distribution for milo.

How a successful build reaches your testers. Nothing here contains a secret; secrets are named only.

## Which workflow builds what

| Workflow | App | Package | Firebase app |
|---|---|---|---|
| `native.yml` | milo. Full (native) | `com.ubayanda.milonative` | `FIREBASE_APP_ID` |
| `lite.yml` | milo. Lite | `com.ubayanda.milonative.lite` | `FIREBASE_LITE_APP_ID` (its own Firebase app) |
| `android.yml` | web-wrapper APK (not the native app) | its own | `FIREBASE_WEB_APP_ID` (optional, old workflow kept) |

Full and Lite are different Android apps, so they need different Firebase registrations and different App IDs. Never put the Lite App ID in `FIREBASE_APP_ID` or the other way round.

## Firebase side (once)

1. Firebase Console, project settings, add an Android app for `com.ubayanda.milonative` (and one for `com.ubayanda.milonative.lite` if you want Lite distributed). Each app's **App ID** looks like `1:1234567890:android:abc123`.
2. App Distribution, enable it, then **Testers & Groups**: create groups. The *alias* (shown under the group name, lower case) is what the secrets hold.
3. Service account: Google Cloud IAM, give the service account the role **Firebase App Distribution Admin**, and make sure the *Firebase App Distribution API* is enabled for the project. One service account can serve both apps.
4. Add or remove testers any time in Console, App Distribution, Testers & Groups. Nothing in the repo changes.

## GitHub secrets (names only)

| Secret | Used for |
|---|---|
| `FIREBASE_SERVICE_ACCOUNT` | the whole service-account JSON key text (both apps) |
| `FIREBASE_APP_ID` | Full app's Firebase App ID |
| `FIREBASE_TESTER_GROUPS` | group alias(es) for **internal** testers (comma-separated, no spaces) |
| `FIREBASE_BETA_GROUPS` | optional: **beta** testers |
| `FIREBASE_RC_GROUPS` | optional: **release-candidate** testers |
| `FIREBASE_LITE_APP_ID`, `FIREBASE_LITE_TESTER_GROUPS`, `FIREBASE_LITE_BETA_GROUPS`, `FIREBASE_LITE_RC_GROUPS` | the same four for Lite |
| `FIREBASE_WEB_APP_ID` | only if you still want the web-wrapper APK distributed |
| `MILO_KEYSTORE_B64`, `MILO_KEYSTORE_PASSWORD`, `MILO_KEY_ALIAS`, `MILO_KEY_PASSWORD` | release signing (unchanged, not touched by this work) |
| `GOOGLE_SERVICES_JSON` | push (unchanged) |

A track whose group secret is empty is **skipped with a clear message**; it never falls back to another group, so a beta build cannot land with internal testers by accident, and nothing goes to external testers unless you put those groups in a secret yourself.

## When something is distributed

| Event | Result |
|---|---|
| Pull request | build + tests only |
| Push to any branch except `main` | build + tests only |
| Push to `main` | **internal** track |
| Tag `v1.2.3` | **beta** track |
| Tag `v1.2.3-rc1` (contains `-rc`) | **release-candidate** track |
| Manual run (Actions, Run workflow) | pick `none`, `internal`, `beta` or `rc` |

Distribution runs in its own `distribute` job that only starts when the build job succeeded, **unit tests passed** and the **signed release APK** was built. A failed upload never removes the GitHub artifacts (they are uploaded earlier, in the build job).

Fork pull requests never see secrets and never distribute.

## What is checked before upload

The script `.github/scripts/fb-distribute.sh` refuses to upload unless the APK exists (exactly one, non-empty, valid archive), has package `com.ubayanda.milonative` (or `.lite` for Lite), has a valid signature and is **not signed with the debug key**. Debug APKs are no longer sent to Firebase; they stay available as GitHub artifacts.

Release notes come from `.github/scripts/fb-release-notes.sh`: version name and code (read from the APK), track, branch, short commit, up to 15 commit messages since the previous `v*` tag (or the last commits if there is none), and the unit-test result. It states plainly that device testing is not done by CI.

The upload uses a pinned Firebase CLI (`firebase-tools@13.29.1`), not "latest". The service-account JSON is written to a temporary file readable only by the runner user, checked for shape, and deleted when the script ends, including on failure. It is never printed.

## Version numbers

`versionCode` = the workflow run number. `versionName` = the tag without the `v` (tag `v1.2.0-rc1` gives `1.2.0-rc1`), otherwise `1.0`.

## First beta release (checklist)

1. Secrets above are set (`FIREBASE_SERVICE_ACCOUNT`, `FIREBASE_APP_ID`, `FIREBASE_TESTER_GROUPS`, the keystore secrets). Note the **old exposed signing key should be replaced first** (see `docs/SECURITY-AUDIT.md`), then update the keystore secrets.
2. Run the Full workflow manually with track `internal`. Check the run summary: it shows the Firebase result and, only if Firebase returned them, the console and tester links.
3. On a phone, accept the invite and install; run `docs/DEVICE-TEST-CHECKLIST.md`.
4. When happy: `git tag v1.0.0 && git push origin v1.0.0` for a beta distribution (or `v1.0.0-rc1` for release candidates).

## Troubleshooting

| What the summary says | Meaning |
|---|---|
| Skipped: missing configuration | a secret (app id, service account or the track's group) is empty |
| signed with the DEBUG key | the keystore secrets are missing or wrong in this run |
| package is X but expected Y | wrong APK or `FIREBASE_APP_ID` belongs to the other app |
| Likely cause: permission | service account lacks *Firebase App Distribution Admin* or the API is off |
| Likely cause: app id | `FIREBASE_APP_ID` does not match an Android app in the project |
| Likely cause: group | a group alias does not exist; use the alias, comma-separated, no spaces |
| service-account is not valid JSON | paste the whole key file as the secret |

## Security notes

- Workflows request `contents: read` only. Third-party distribution actions were removed; only GitHub's own `actions/*` and `gradle/actions` are used, referenced by version tag. Pinning those to full commit SHAs is a further hardening step (Dependabot can do it); it was not done here because the SHAs must be looked up from GitHub.
- You can require manual approval for uploads by putting the `distribute` jobs in a GitHub *Environment* with required reviewers.
- Rotate the service-account key if it is ever pasted anywhere it should not be.

## What has and has not been verified

Checked in the sandbox: workflow YAML parses, the shell scripts pass a syntax check, release-notes generation works against a test repository, and the missing-credentials and missing-APK paths behave as described. **Not** done: any real authenticated upload, tester invitations, the Android-tool checks (`aapt2`, `apksigner`) on a real runner, and Lite's new unit-test step. The first real run will be the first proof.
