package com.in8.trainee.crawler.config;

import java.net.URI;
import java.util.Optional;

public record AiConfig(
        URI baseUrl,
        String model,
        Optional<String> apiKey
) {

    public static Optional<AiConfig> fromEnvironment() {
        String enabled = System.getenv("BOOKS_AI_ENABLED");
        if (!"true".equalsIgnoreCase(enabled)) {
            return Optional.empty();
        }

        String baseUrl = System.getenv("BOOKS_AI_BASE_URL");
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://127.0.0.1:11434/v1";
        }

        String model = System.getenv("BOOKS_AI_MODEL");
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("BOOKS_AI_MODEL must be provided when BOOKS_AI_ENABLED=true.");
        }

        String apiKey = System.getenv("BOOKS_AI_API_KEY");

        return Optional.of(new AiConfig(
                URI.create(baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl),
                model,
                Optional.ofNullable(apiKey).filter(value -> !value.isBlank())
        ));
    }
}
