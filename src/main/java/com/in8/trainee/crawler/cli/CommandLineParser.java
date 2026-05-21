package com.in8.trainee.crawler.cli;

import java.nio.file.Path;

public final class CommandLineParser {

    private static final String DEFAULT_BASE_URL = "http://books.toscrape.com/";
    private static final Path DEFAULT_OUTPUT_DIRECTORY = Path.of("output");
    private static final long DEFAULT_DELAY_MILLIS = 400L;
    private static final String DEFAULT_USER_AGENT = "books-crawler-java/1.0 (+https://books.toscrape.com/)";
    private static final int DEFAULT_PORT = 8080;

    private CommandLineParser() {
    }

    public static AppOptions parse(final String[] args) {
        boolean serverMode = false;
        String baseUrl = DEFAULT_BASE_URL;
        Path outputDirectory = DEFAULT_OUTPUT_DIRECTORY;
        long delayMillis = DEFAULT_DELAY_MILLIS;
        String userAgent = DEFAULT_USER_AGENT;
        int port = DEFAULT_PORT;
        Integer maxBooks = null;

        for (int index = 0; index < args.length; index++) {
            String argument = args[index];
            switch (argument) {
                case "--server" -> serverMode = true;
                case "--base-url" -> baseUrl = requireValue(argument, args, ++index);
                case "--output-dir" -> outputDirectory = Path.of(requireValue(argument, args, ++index));
                case "--delay-ms" -> delayMillis = Long.parseLong(requireValue(argument, args, ++index));
                case "--user-agent" -> userAgent = requireValue(argument, args, ++index);
                case "--port" -> port = Integer.parseInt(requireValue(argument, args, ++index));
                case "--max-books" -> maxBooks = Integer.parseInt(requireValue(argument, args, ++index));
                default -> throw new IllegalArgumentException("Unknown argument: " + argument);
            }
        }

        return new AppOptions(serverMode, baseUrl, outputDirectory, delayMillis, userAgent, port, maxBooks);
    }

    private static String requireValue(final String argument, final String[] args, final int index) {
        if (index >= args.length) {
            throw new IllegalArgumentException("Missing value for argument: " + argument);
        }
        return args[index];
    }
}
