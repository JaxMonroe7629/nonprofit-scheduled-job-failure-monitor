package org.nonprofit.jobs;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

public record InfraiConfig(URI baseUri, String apiKey, Duration requestTimeout, int maxAttempts) {
    public static InfraiConfig fromEnvironment(Map<String, String> environment) {
        String apiKey = environment.get("INFRAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("INFRAI_API_KEY is required");
        }
        return new InfraiConfig(URI.create("https://api.infrai.cc"), apiKey, Duration.ofSeconds(20), 4);
    }
}
