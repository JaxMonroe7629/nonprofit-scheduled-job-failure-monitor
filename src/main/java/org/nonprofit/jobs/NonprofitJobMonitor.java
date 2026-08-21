package org.nonprofit.jobs;

import java.time.Instant;

public final class NonprofitJobMonitor {
    private NonprofitJobMonitor() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("Usage: NonprofitJobMonitor <job-kind> <execution-id> <failure-message>");
            System.exit(2);
        }
        InfraiConfig config = InfraiConfig.fromEnvironment(System.getenv());
        ScheduledJobFailureService service = new ScheduledJobFailureService(new InfraiErrorClient(config));
        var run = new ScheduledJobFailureService.JobRun(
                args[1],
                ScheduledJobFailureService.JobKind.valueOf(args[0].toUpperCase()),
                ScheduledJobFailureService.JobStatus.FAILED,
                Instant.now(),
                args[2]);
        System.out.println(service.record(run));
    }
}
