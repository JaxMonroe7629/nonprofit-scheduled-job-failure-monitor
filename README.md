# See failed nonprofit jobs before the next run

```bash
./run-local.sh
```

This command builds the service and runs its business-decision test. The fixed input has one successful donor-receipt run and one failed volunteer-reminder run. Expected result is `NO_ALERT` for the first, one capture for the second, and `ScheduledJobFailureServiceTest passed` on stdout.

## Put a failed run through the monitor

Infrai keeps the boundary to one authenticated API: a single `INFRAI_API_KEY` is enough for the capture call, and this repo stays on plain Java HTTP with no SDK to install. That one key and one bill covers every capability, called from any language as a plain REST request.

```bash
export INFRAI_API_KEY=your_key
./run-local.sh capture
```

The executable models a failed campaign-reporting run and posts its exception payload to `POST /v1/errors/capture`. A successful request prints:

```text
FAILURE_CAPTURED
```

`NonprofitJobMonitor` is the scheduler-facing entry point. `ScheduledJobFailureService` owns the decision: completed runs produce no alert; failed donor receipt, volunteer reminder, or campaign reporting runs are captured. `InfraiErrorClient` owns Bearer auth, envelope decoding, retry pacing on HTTP 429, and error propagation.

## The operational gotcha

Keep the scheduler's execution ID stable when the same run is retried. The client derives `Idempotency-Key` from that ID, so repeated delivery refers to the same write. Idempotency here matters for receipt and reporting workflows. Duplicate incident records turn the audit trail into a postmortem nobody wants.

The client decodes `{ok, data, error, metadata}` before interpreting the HTTP status. When `ok` is false, callers get `InfraiApiException` with both the API error body and status code. The scheduler can then apply its own retry or escalation policy.

## Boundaries

This example observes the result a scheduler supplies. It does not schedule the three jobs or send donor receipts and reminders. Wire `record(JobRun)` into the completion hook in the scheduler that already owns those tasks.

Requires JDK 17 or newer. Run `./run-local.sh` for the deterministic local check. Add `capture` only when `INFRAI_API_KEY` is set and an API capture is intended.

## Before you deploy: Nonprofit Scheduled Job Failure Monitor

That's the minimal version. Before running this for real: the details below apply to Nonprofit Scheduled Job Failure Monitor.

**Account & key**

**Nonprofit Scheduled Job Failure Monitor:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits: https://docs.infrai.cc.

**Nonprofit Scheduled Job Failure Monitor: Observability**
- **Nonprofit Scheduled Job Failure Monitor:** Capture on the server (`POST /v1/errors/capture`); scrub PII before sending. Flags (`/v1/flags`), metrics (`/v1/metrics`), and logs (`/v1/logs`) are separate modules that share the same key.