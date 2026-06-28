#!/usr/bin/env bash
set -euo pipefail

REPO="${1:?repo path required}"
BASE="${2:?base ref required}"
HEAD="${3:-HEAD}"
FAIL_ON="${4:-high}"
SKIP_AI="${5:-true}"
REPORT="${6:?report path required}"
COMMENT="${7:?comment path required}"
JAR="${8:?jar path required}"

ARGS=("$REPO" --base "$BASE" --head "$HEAD" --fail-on "$FAIL_ON" -o "$REPORT" --pr-comment "$COMMENT")
if [ "$SKIP_AI" = "true" ]; then
  ARGS+=(--skip-ai)
fi

set +e
OUTPUT="$(java -jar "$JAR" "${ARGS[@]}" 2>&1)"
EXIT=$?
set -e

echo "$OUTPUT"

HEALTH="$(echo "$OUTPUT" | grep -E '^Architecture health:' | head -1 | sed -E 's/.*: ([0-9]+)\/.*/\1/' || echo "0")"
INTRODUCED="$(echo "$OUTPUT" | grep -E '^PR violations introduced:' | head -1 | sed -E 's/.*: ([0-9]+)/\1/' || echo "0")"

if [ -n "${GITHUB_OUTPUT:-}" ]; then
  {
    echo "health-score=$HEALTH"
    echo "introduced-violations=$INTRODUCED"
    echo "report-path=$REPORT"
    echo "comment-path=$COMMENT"
  } >> "$GITHUB_OUTPUT"
fi

exit "$EXIT"
