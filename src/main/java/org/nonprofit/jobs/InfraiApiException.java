package org.nonprofit.jobs;

public final class InfraiApiException extends Exception {
    private final int statusCode;
    private final String apiError;

    public InfraiApiException(int statusCode, String apiError) {
        super("Infrai rejected the capture: " + apiError);
        this.statusCode = statusCode;
        this.apiError = apiError;
    }

    public int statusCode() {
        return statusCode;
    }

    public String apiError() {
        return apiError;
    }
}
