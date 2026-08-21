#!/usr/bin/env sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
OUT="$ROOT/.build"
mkdir -p "$OUT"
javac -d "$OUT" $(find "$ROOT/src/main/java" "$ROOT/src/test/java" -name '*.java' -print)
java -cp "$OUT" org.nonprofit.jobs.ScheduledJobFailureServiceTest

if [ "${1:-}" = "capture" ]; then
  java -cp "$OUT" org.nonprofit.jobs.NonprofitJobMonitor \
    campaign_reporting campaign-2026-08-21 "ledger export rejected"
fi
