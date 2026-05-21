package com.in8.trainee.crawler.cli;

import java.nio.file.Path;

public record AppOptions(
        boolean serverMode,
        String baseUrl,
        Path outputDirectory,
        long delayMillis,
        String userAgent,
        int port,
        Integer maxBooks
) {
}
