package com.in8.trainee.crawler.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.in8.trainee.crawler.config.AppConfig;
import com.in8.trainee.crawler.service.CrawlOrchestrator;
import com.in8.trainee.crawler.service.CrawlResult;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ScraperHttpServer {

    private final CrawlOrchestrator orchestrator;
    private final AppConfig config;
    private final ObjectMapper objectMapper;

    public ScraperHttpServer(final CrawlOrchestrator orchestrator, final AppConfig config) {
        this.orchestrator = orchestrator;
        this.config = config;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(config.port()), 0);
        ExecutorService executor = Executors.newFixedThreadPool(4);
        server.setExecutor(executor);

        server.createContext("/health", exchange -> writeJson(exchange, 200, Map.of("status", "ok")));
        server.createContext("/run", this::handleRun);

        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop(0);
            executor.shutdown();
        }));
    }

    private void handleRun(final HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            writeJson(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }

        try {
            CrawlResult result = orchestrator.run();
            writeJson(exchange, 200, Map.of(
                    "books", result.books().size(),
                    "jsonPath", result.jsonPath().toString(),
                    "csvPath", result.csvPath().toString(),
                    "finishedAt", result.finishedAt().toString(),
                    "databasePersisted", result.databasePersisted(),
                    "aiEnrichmentEnabled", result.aiEnrichmentEnabled()
            ));
        } catch (RuntimeException exception) {
            writeJson(exchange, 500, Map.of("error", exception.getMessage()));
        }
    }

    private void writeJson(final HttpExchange exchange, final int statusCode, final Object payload)
            throws IOException {
        byte[] body = objectMapper.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, body.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(body);
        }
    }
}
