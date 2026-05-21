package com.in8.trainee.crawler.config;

import java.util.Optional;

public record DatabaseConfig(
        String jdbcUrl,
        String username,
        String password
) {

    public static Optional<DatabaseConfig> fromEnvironment() {
        String jdbcUrl = System.getenv("BOOKS_DB_URL");
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            return Optional.empty();
        }

        String username = System.getenv("BOOKS_DB_USER");
        String password = System.getenv("BOOKS_DB_PASSWORD");

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("BOOKS_DB_USER must be provided when BOOKS_DB_URL is set.");
        }
        if (password == null) {
            throw new IllegalArgumentException("BOOKS_DB_PASSWORD must be provided when BOOKS_DB_URL is set.");
        }

        return Optional.of(new DatabaseConfig(jdbcUrl, username, password));
    }
}
