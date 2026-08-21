package org.nonprofit.jobs;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class ScheduledJobFailureServiceTest {
    public static void main(String[] args) throws Exception {
        List<String> captures = new ArrayList<>();
        ErrorReporter reporter = (payload, key) -> captures.add(key + "|" + payload);
        ScheduledJobFailureService service = new ScheduledJobFailureService(reporter);

        var success = new ScheduledJobFailureService.JobRun(
                "receipt-1041",
                ScheduledJobFailureService.JobKind.DONOR_RECEIPTS,
                ScheduledJobFailureService.JobStatus.SUCCEEDED,
                Instant.parse("2026-08-21T01:00:00Z"),
                null);
        assertEquals(ScheduledJobFailureService.AlertDecision.NO_ALERT, service.record(success));
        assertEquals(0, captures.size());

        var failure = new ScheduledJobFailureService.JobRun(
                "reminder-882",
                ScheduledJobFailureService.JobKind.VOLUNTEER_REMINDERS,
                ScheduledJobFailureService.JobStatus.FAILED,
                Instant.parse("2026-08-21T02:00:00Z"),
                "mail handoff rejected");
        assertEquals(ScheduledJobFailureService.AlertDecision.FAILURE_CAPTURED, service.record(failure));
        assertEquals(1, captures.size());
        assertTrue(captures.get(0).startsWith("nonprofit-job-reminder-882|"));
        assertTrue(captures.get(0).contains("job=volunteer_reminders"));
        System.out.println("ScheduledJobFailureServiceTest passed");
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("expected " + expected + " but got " + actual);
        }
    }

    private static void assertTrue(boolean condition) {
        if (!condition) {
            throw new AssertionError("condition was false");
        }
    }
}
