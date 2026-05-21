package com.in8.trainee.crawler.client;

import java.io.IOException;
import java.net.URI;
import java.net.ConnectException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.nio.channels.UnresolvedAddressException;

public final class HtmlFetcher {

    private static final String BOOKS_TO_SCRAPE_HOST = "books.toscrape.com";
    private static final String BOOKS_TO_SCRAPE_IP = "35.211.122.109";

    private final HttpClient httpClient;
    private final String userAgent;
    private final long delayMillis;
    private final Clock clock;
    private Instant lastRequestAt;

    public HtmlFetcher(
            final HttpClient httpClient,
            final String userAgent,
            final long delayMillis,
            final Clock clock
    ) {
        this.httpClient = httpClient;
        this.userAgent = userAgent;
        this.delayMillis = delayMillis;
        this.clock = clock;
        this.lastRequestAt = Instant.EPOCH;
    }

    public synchronized String get(final URI uri) {
        waitIfNeeded();

        try {
            HttpResponse<byte[]> response = httpClient.send(buildRequest(uri, false), HttpResponse.BodyHandlers.ofByteArray());
            lastRequestAt = clock.instant();
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("Request failed with status " + response.statusCode() + " for " + uri);
            }
            return new String(response.body(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            if (shouldRetryWithHostFallback(uri, exception)) {
                return retryWithHostFallback(uri);
            }
            throw new IllegalStateException("I/O error fetching " + uri, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while fetching " + uri, exception);
        }
    }

    private void waitIfNeeded() {
        Duration elapsed = Duration.between(lastRequestAt, clock.instant());
        long remainingMillis = delayMillis - elapsed.toMillis();
        if (remainingMillis <= 0) {
            return;
        }

        try {
            Thread.sleep(remainingMillis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting between requests.", exception);
        }
    }

    private HttpRequest buildRequest(final URI uri, final boolean useHostFallback) {
        URI target = uri;
        HttpRequest.Builder builder;

        if (useHostFallback) {
            target = URI.create(uri.toString().replace(BOOKS_TO_SCRAPE_HOST, BOOKS_TO_SCRAPE_IP));
            builder = HttpRequest.newBuilder(target)
                    .header("Host", BOOKS_TO_SCRAPE_HOST);
        } else {
            builder = HttpRequest.newBuilder(target);
        }

        return builder
                .GET()
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", userAgent)
                .header("Accept", "text/html,application/xhtml+xml")
                .build();
    }

    private boolean shouldRetryWithHostFallback(final URI uri, final IOException exception) {
        if (!BOOKS_TO_SCRAPE_HOST.equalsIgnoreCase(uri.getHost())) {
            return false;
        }

        Throwable cause = exception.getCause();
        return exception instanceof ConnectException || cause instanceof UnresolvedAddressException;
    }

    private String retryWithHostFallback(final URI uri) {
        try {
            HttpResponse<byte[]> response = httpClient.send(buildRequest(uri, true), HttpResponse.BodyHandlers.ofByteArray());
            lastRequestAt = clock.instant();
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("Fallback request failed with status " + response.statusCode() + " for " + uri);
            }
            return new String(response.body(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("I/O error fetching " + uri + " with fallback host resolution.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while fetching " + uri + " with fallback host resolution.", exception);
        }
    }
}
