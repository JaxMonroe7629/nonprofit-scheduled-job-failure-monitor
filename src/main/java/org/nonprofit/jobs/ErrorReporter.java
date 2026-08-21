package org.nonprofit.jobs;

@FunctionalInterface
public interface ErrorReporter {
    void capture(String exceptionPayload, String idempotencyKey) throws Exception;
}
