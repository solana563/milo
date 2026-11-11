#!/usr/bin/env bash
# Writes the Firebase App Distribution release notes to the file given as $1.
# Reads: LABEL TRACK VERSION_NAME VERSION_CODE BRANCH SHA TESTS_OUTCOME TEST_TASK
# Only states what CI actually knows: nothing here claims device testing.
set -u
OUT="${1:?usage: fb-release-notes.sh <output file>}"
SHORT="${SHA:0:7}"
PREV=$(git describe --tags --abbrev=0 --match 'v*' "HEAD^" 2>/dev/null || true)
if [ -n "$PREV" ]; then RANGE="$PREV..HEAD"; SINCE="since $PREV"; else RANGE="HEAD"; SINCE="latest commits"; fi
CHANGES=$(git log --no-merges --pretty='- %s' -n 15 $RANGE 2>/dev/null | grep -viE '^- (merge|wip)' || true)
[ -n "$CHANGES" ] || CHANGES="- (no commit messages found)"
case "${TESTS_OUTCOME:-}" in
  success) TESTS="- Automated unit tests passed (${TEST_TASK:-unit tests})." ;;
  failure) TESTS="- Automated unit tests FAILED (${TEST_TASK:-unit tests})." ;;
  *)       TESTS="- Automated unit tests did not run in this workflow." ;;
esac
{
  echo "milo. Android - Version ${VERSION_NAME} (${VERSION_CODE})"
  echo
  echo "Build: ${LABEL} (${TRACK} testers)"
  echo "Branch: ${BRANCH}"
  echo "Commit: ${SHORT}"
  echo
  echo "What's new (${SINCE}):"
  echo "$CHANGES"
  echo
  echo "Testing:"
  echo "$TESTS"
  echo "- Not tested by CI: playback on real devices, Bluetooth, Android Auto, home-screen widgets."
  echo
  echo "Distribution: Firebase App Distribution"
  echo "This is a testing release. Please report crashes, playback problems and anything unexpected."
} > "$OUT"
echo "Release notes written to $OUT"
