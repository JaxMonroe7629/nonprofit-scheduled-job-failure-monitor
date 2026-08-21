package org.nonprofit.jobs;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class InfraiErrorClient implements ErrorReporter {
    private static final String CAPTURE_PATH = "/v1/errors/capture";
    private final InfraiConfig config;
    private final HttpClient http;

    public InfraiErrorClient(InfraiConfig config) {
        this(config, HttpClient.newBuilder().connectTimeout(config.requestTimeout()).build());
    }

    InfraiErrorClient(InfraiConfig config, HttpClient http) {
        this.config = config;
        this.http = http;
    }

    @Override
    public void capture(String exceptionPayload, String idempotencyKey) throws Exception {
        String body = "{\"exception\":\"" + escape(exceptionPayload) + "\"}";
        for (int attempt = 0; attempt < config.maxAttempts(); attempt++) {
            HttpRequest request = HttpRequest.newBuilder(resolve(CAPTURE_PATH))
                    .timeout(config.requestTimeout())
                    .header("Authorization", "Bearer " + config.apiKey())
                    .header("Content-Type", "application/json")
                    .header("Idempotency-Key", idempotencyKey)
                    .method("POST", HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            InfraiEnvelope envelope = InfraiEnvelope.parse(response.body());

            if (response.statusCode() == 429 && attempt + 1 < config.maxAttempts()) {
                Thread.sleep(retryDelay(response, attempt).toMillis());
                continue;
            }
            if (!envelope.ok()) {
                throw new InfraiApiException(response.statusCode(), envelope.error());
            }
            if (response.statusCode() >= 500) {
                throw new IOException("Infrai transport status " + response.statusCode());
            }
            return;
        }
        throw new IllegalStateException("Capture attempts exhausted");
    }

    private URI resolve(String path) {
        return config.baseUri().resolve(path);
    }

    private static Duration retryDelay(HttpResponse<?> response, int attempt) {
        return response.headers().firstValue("Retry-After")
                .flatMap(InfraiErrorClient::parseRetryAfter)
                .orElse(Duration.ofMillis(250L * (1L << attempt)));
    }

    private static java.util.Optional<Duration> parseRetryAfter(String value) {
        try {
            return java.util.Optional.of(Duration.ofSeconds(Long.parseLong(value.trim())));
        } catch (NumberFormatException ignored) {
            return java.util.Optional.empty();
        }
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
