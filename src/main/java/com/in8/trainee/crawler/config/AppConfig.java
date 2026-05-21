package com.in8.trainee.crawler.config;

import com.in8.trainee.crawler.cli.AppOptions;
import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;

public record AppConfig(
        URI baseUrl,
        Path outputDirectory,
        long delayMillis,
        String userAgent,
        int port,
        Optional<DatabaseConfig> databaseConfig,
        Optional<Integer> maxBooks,
        Optional<AiConfig> aiConfig
) {

    public static AppConfig from(final AppOptions options) {
        Integer maxBooks = options.maxBooks() != null
                ? options.maxBooks()
                : readPositiveIntegerEnv("BOOKS_MAX_BOOKS").orElse(null);
        if (options.delayMillis() < 0) {
            throw new IllegalArgumentException("Delay cannot be negative.");
        }
        if (options.port() < 1 || options.port() > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535.");
        }
        if (maxBooks != null && maxBooks < 1) {
            throw new IllegalArgumentException("Max books must be greater than zero.");
        }

        return new AppConfig(
                URI.create(options.baseUrl()),
                options.outputDirectory(),
                options.delayMillis(),
                options.userAgent(),
                options.port(),
                DatabaseConfig.fromEnvironment(),
                Optional.ofNullable(maxBooks),
                AiConfig.fromEnvironment()
        );
    }

    private static Optional<Integer> readPositiveIntegerEnv(final String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(Integer.parseInt(value));
    }
}
