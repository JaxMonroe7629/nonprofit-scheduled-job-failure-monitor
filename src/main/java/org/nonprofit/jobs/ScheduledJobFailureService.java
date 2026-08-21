package org.nonprofit.jobs;

import java.time.Instant;
import java.util.Locale;

public final class ScheduledJobFailureService {
    public enum JobKind {
        DONOR_RECEIPTS, VOLUNTEER_REMINDERS, CAMPAIGN_REPORTING
    }

    public enum JobStatus {
        SUCCEEDED, FAILED
    }

    public record JobRun(String executionId, JobKind kind, JobStatus status, Instant finishedAt,
                         String failureMessage) {
        public JobRun {
            if (executionId == null || executionId.isBlank()) {
                throw new IllegalArgumentException("executionId is required");
            }
            if (status == JobStatus.FAILED && (failureMessage == null || failureMessage.isBlank())) {
                throw new IllegalArgumentException("failureMessage is required for a failed job");
            }
        }
    }

    public enum AlertDecision {
        NO_ALERT, FAILURE_CAPTURED
    }

    private final ErrorReporter reporter;

    public ScheduledJobFailureService(ErrorReporter reporter) {
        this.reporter = reporter;
    }

    public AlertDecision record(JobRun run) throws Exception {
        if (run.status() == JobStatus.SUCCEEDED) {
            return AlertDecision.NO_ALERT;
        }
        String payload = "Scheduled nonprofit job failed"
                + "\njob=" + run.kind().name().toLowerCase(Locale.ROOT)
                + "\nexecution_id=" + run.executionId()
                + "\nfinished_at=" + run.finishedAt()
                + "\nmessage=" + run.failureMessage();
        reporter.capture(payload, "nonprofit-job-" + run.executionId());
        return AlertDecision.FAILURE_CAPTURED;
    }
}
