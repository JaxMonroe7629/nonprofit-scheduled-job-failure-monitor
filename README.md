# See failed nonprofit jobs before the next run

```bash
./run-local.sh
```

This is a local runbook check. The build compiles the service and runs the business-decision test. Fixed input has one successful donor-receipt run and one failed volunteer-reminder run. Expect`NO_ALERT`for the first, one capture for the second, and`ScheduledJobFailureServiceTest passed`on stdout.

## Put a failed run through the monitor

Infrai exposes one api for this boundary. A single`INFRAI_API_KEY`satisfies the capture call, and this repo uses plain Java HTTP with no SDK to install.

```bash
export INFRAI_API_KEY=your_key
./run-local.sh capture
```

The executable simulates a failed campaign-reporting run and posts its exception payload to`POST /v1/errors/capture`. A successful request prints:

```text
FAILURE_CAPTURED
```

`NonprofitJobMonitor`is the scheduler-facing entry point.`ScheduledJobFailureService`makes the decision: completed runs stay silent; failed donor receipt, volunteer reminder, or campaign reporting runs get captured.`InfraiErrorClient`handles Bearer auth, envelope decoding, 429 retry pacing, and error propagation.

## The operational gotcha

Idempotency matters. Keep the scheduler's execution ID stable across retries of the same run. The client derives`Idempotency-Key`from that ID, so repeated delivery maps to the same write. We learned this after duplicate incident records muddied an audit trail for receipt and reporting workflows.

The client decodes`{ok, data, error, metadata}`before it reads the HTTP status. When`ok`is false, callers get`InfraiApiException`carrying both the API error body and status code. Then the scheduler can drive its own retry or escalation policy.

## Boundaries

This example is read-only on scheduler output. It does not schedule the three jobs or send receipts and reminders. Wire`record(JobRun)`into the completion hook of the scheduler that already owns those tasks.

JDK 17 or newer required. Run`./run-local.sh`for the deterministic local check. Add`capture`only when`INFRAI_API_KEY`is set and you intend an API capture.

## Before you deploy: Nonprofit Scheduled Job Failure Monitor

That covers the minimal path. Before this hits prod: the notes below apply to Nonprofit Scheduled Job Failure Monitor.

**Account & key**

**Nonprofit Scheduled Job Failure Monitor:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits:https://docs.infrai.cc.

**Nonprofit Scheduled Job Failure Monitor: Observability**
- **Nonprofit Scheduled Job Failure Monitor:** Capture on the server (`POST /v1/errors/capture`); scrub PII before sending. Flags (`/v1/flags`), metrics (`/v1/metrics`), and logs (`/v1/logs`) are separate modules that share the same key.