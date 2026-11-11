#!/usr/bin/env bash
# Validates the release APK and uploads it to Firebase App Distribution.
# Inputs (env): APK_DIR EXPECT_PKG LABEL TRACK FB_APP FB_GROUPS FB_SA NOTES_FILE
# Never prints FB_SA. Exits 0 with an explanation when credentials are missing, non-zero on every real failure.
set -u
set +x
SUM="${GITHUB_STEP_SUMMARY:-/dev/null}"
say() { echo "$*" | tee -a "$SUM"; }
fail() { say ""; say "**Distribution FAILED:** $*"; exit 1; }

say "## Firebase App Distribution - ${LABEL}"

# 1. credentials present? (a missing secret is a clear skip, not a failure)
MISSING=""
[ -n "${FB_APP:-}" ]    || MISSING="$MISSING app-id"
[ -n "${FB_SA:-}" ]     || MISSING="$MISSING service-account"
[ -n "${FB_GROUPS:-}" ] || MISSING="$MISSING tester-group(s) for the '${TRACK}' track"
if [ -n "$MISSING" ]; then
  say "Skipped: missing configuration:${MISSING}. The build and its artifacts are unaffected. See docs/FIREBASE_APP_DISTRIBUTION.md."
  exit 0
fi

# 2. the APK: exists, non-empty, a valid zip, right package, valid signature, not the debug key
shopt -s nullglob
APKS=("$APK_DIR"/*.apk)
[ "${#APKS[@]}" -eq 1 ] || fail "expected exactly one release APK in $APK_DIR, found ${#APKS[@]}."
APK="${APKS[0]}"
[ -s "$APK" ] || fail "the APK is empty: $APK"
unzip -tq "$APK" >/dev/null 2>&1 || fail "the APK is not a valid archive: $APK"
BT=$(ls -d "$ANDROID_HOME"/build-tools/* 2>/dev/null | sort -V | tail -1)
[ -n "$BT" ] || fail "Android build-tools not found on this runner."
BADGE=$("$BT/aapt2" dump badging "$APK" 2>/dev/null | sed -n "s/^package: name='\([^']*\)' versionCode='\([^']*\)' versionName='\([^']*\)'.*/\1|\2|\3/p")
IFS='|' read -r PKG VCODE VNAME <<< "$BADGE"
[ -n "${PKG:-}" ] || fail "could not read the APK's package information."
[ "$PKG" = "$EXPECT_PKG" ] || fail "package is '$PKG' but this distribution is for '$EXPECT_PKG'. Wrong APK or wrong Firebase app."
"$BT/apksigner" verify "$APK" >/dev/null 2>&1 || fail "the APK signature does not verify (unsigned or damaged)."
CERT=$("$BT/apksigner" verify --print-certs "$APK" 2>&1)
if echo "$CERT" | grep -qi "Android Debug"; then
  fail "the APK is signed with the DEBUG key. Add the release keystore secrets (MILO_KEYSTORE_B64 and friends); debug-signed builds are not distributed."
fi
SIZE=$(du -h "$APK" | cut -f1)
say "- App: \`$PKG\`  version \`$VNAME\` (code $VCODE)  size $SIZE  file \`$(basename "$APK")\`"
say "- Track: **${TRACK}**  groups: \`${FB_GROUPS}\`  commit: \`${GITHUB_SHA:0:7}\`  branch: \`${GITHUB_REF_NAME:-?}\`"

# 3. credentials: temp file, owner-only, removed on exit (also on failure)
umask 077
CRED=$(mktemp "${RUNNER_TEMP:-/tmp}/fbsa.XXXXXX")
trap 'rm -f "$CRED"' EXIT
printf '%s' "$FB_SA" > "$CRED"
python3 - "$CRED" <<'PY' || fail "FIREBASE_SERVICE_ACCOUNT is not a valid service-account JSON key (paste the whole file as the secret)."
import json, sys
d = json.load(open(sys.argv[1]))
assert d.get("type") == "service_account" and d.get("client_email") and d.get("private_key")
PY
export GOOGLE_APPLICATION_CREDENTIALS="$CRED"

# 4. upload with a pinned CLI; stop immediately if it fails
LOG=$(mktemp "${RUNNER_TEMP:-/tmp}/fb.XXXXXX")
set +e
npx --yes firebase-tools@13.29.1 appdistribution:distribute "$APK" \
  --app "$FB_APP" --groups "$FB_GROUPS" --release-notes-file "$NOTES_FILE" 2>&1 | tee "$LOG"
RC=${PIPESTATUS[0]}
set -e
set +e

if [ "$RC" -ne 0 ]; then
  say ""
  say "**Distribution FAILED** (exit code $RC). The APK is still in this run's artifacts."
  if   grep -qiE "permission|403|PERMISSION_DENIED" "$LOG"; then say "- Likely cause: the service account lacks the *Firebase App Distribution Admin* role, or the Firebase App Distribution API is not enabled."
  elif grep -qiE "404|not found|could not find|invalid app" "$LOG"; then say "- Likely cause: FIREBASE_APP_ID does not match an Android app in this project (it must be the App ID of \`$EXPECT_PKG\`, like 1:123456789:android:abcdef)."
  elif grep -qiE "group" "$LOG"; then say "- Likely cause: a tester group alias in the groups secret does not exist in Firebase (use the group alias, comma-separated, no spaces)."
  elif grep -qiE "credential|auth|token|401" "$LOG"; then say "- Likely cause: the service-account key is invalid or revoked."
  else say "- See the log of this step; Firebase may be temporarily unavailable."; fi
  say '```'; tail -n 12 "$LOG" | sed -E 's/("private_key"[^,}]*)/"private_key":"[redacted]"/' | tee -a "$SUM"; say '```'
  exit 1
fi

# 5. report only what Firebase actually returned
say ""
say "- Upload: **succeeded** at $(date -u +%Y-%m-%dT%H:%M:%SZ)"
CONSOLE=$(grep -oE 'https://console\.firebase\.google\.com[^[:space:]]*' "$LOG" | head -1)
SHARE=$(grep -oE 'https://appdistribution\.firebase\.google\.com/testerapps[^[:space:]]*' "$LOG" | head -1)
[ -n "$CONSOLE" ] && say "- Firebase console release: $CONSOLE"
[ -n "$SHARE" ] && say "- Tester link: $SHARE"
say ""
say "<details><summary>Release notes sent</summary>"; say ""; say '```'; cat "$NOTES_FILE" | tee -a "$SUM"; say '```'; say "</details>"
rm -f "$LOG"
exit 0
