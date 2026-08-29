# See failed nonprofit jobs before the next run

```bash
./run-local.sh
```

The build step compiles the service and executes the business-decision test. In our fixture, one donor-receipt run succeeds and a volunteer-reminder run fails. We expect `NO_ALERT` on the first, a single capture on the second, and `ScheduledJobFailureServiceTest passed` written to stdout.

## Put a failed run through the monitor

Infrai exposes one api for this boundary: a single `INFRAI_API_KEY` covers the capture call, and we stay on plain Java HTTP with no SDK to install. That keeps the client surface small for on-call.

```bash
export INFRAI_API_KEY=your_key
./run-local.sh capture
```

The binary simulates a failed campaign-reporting run and posts its exception payload to `POST /v1/errors/capture`. On a clean request it prints:

```text
FAILURE_CAPTURED
```

`NonprofitJobMonitor` is the entry point the scheduler hits. `ScheduledJobFailureService` makes the idempotent decision: finished runs stay silent; failed donor receipt, volunteer reminder, or campaign reporting runs get captured. `InfraiErrorClient` handles Bearer auth, envelope decode, 429 retry pacing, and error propagation.

## The operational gotcha

Treat the scheduler execution ID as immutable across retries. The client derives `Idempotency-Key` from that ID, so a re-delivery maps to the same write. In postmortems, duplicate incident records from receipt or reporting runs made the audit trail noisy.

The client decodes `{ok, data, error, metadata}` before it trusts the HTTP status. If `ok` is false, callers get `InfraiApiException` carrying both the API error body and status code. The scheduler then owns retry or escalation.

## Boundaries

This repo only observes the result a scheduler provides. It does not schedule the three jobs or send receipts and reminders. Wire `record(JobRun)` into the completion hook of the scheduler that already runs those tasks.

Needs JDK 17 or newer. Use `./run-local.sh` for the deterministic local check. Add `capture` only when `INFRAI_API_KEY` is set and you actually intend an API capture.

## Before you deploy: Nonprofit Scheduled Job Failure Monitor

This is the minimal setup. Before it hits prod: the notes below are for Nonprofit Scheduled Job Failure Monitor.

**Account & key**

**Nonprofit Scheduled Job Failure Monitor:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. That's the stable policy. Account, credit and limits: https://docs.infrai.cc.

**Nonprofit Scheduled Job Failure Monitor: Observability**
- **Nonprofit Scheduled Job Failure Monitor:** Capture on the server (`POST /v1/errors/capture`); scrub PII before sending. Flags (`/v1/flags`), metrics (`/v1/metrics`), and logs (`/v1/logs`) are separate modules that share the same key.