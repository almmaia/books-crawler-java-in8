package com.in8.trainee.crawler.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.in8.trainee.crawler.config.AiConfig;
import com.in8.trainee.crawler.model.Book;
import com.in8.trainee.crawler.model.BookAiInsights;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

public final class AiBookEnricher {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AiConfig aiConfig;

    public AiBookEnricher(final HttpClient httpClient, final AiConfig aiConfig) {
        this.httpClient = httpClient;
        this.aiConfig = aiConfig;
        this.objectMapper = new ObjectMapper();
    }

    public Book enrich(final Book book) {
        if (book.description().isBlank()) {
            return book;
        }

        try {
            String payload = objectMapper.writeValueAsString(buildRequestBody(book));
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(chatCompletionsUri())
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(300))
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8));

            aiConfig.apiKey().ifPresent(apiKey -> requestBuilder.header("Authorization", "Bearer " + apiKey));

            HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );

            if (response.statusCode() >= 400) {
                throw new IllegalStateException("AI enrichment failed with status " + response.statusCode());
            }

            BookAiInsights insights = parseInsights(response.body());
            return new Book(
                    book.title(),
                    book.productPageUrl(),
                    book.imageUrl(),
                    book.category(),
                    book.upc(),
                    book.priceGbp(),
                    book.rating(),
                    book.inStock(),
                    book.availability(),
                    book.description(),
                    insights
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to serialize AI request.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while enriching book with AI.", exception);
        }
    }

    private java.net.URI chatCompletionsUri() {
        String base = aiConfig.baseUrl().toString();
        if (base.endsWith("/")) {
            return java.net.URI.create(base + "chat/completions");
        }
        return java.net.URI.create(base + "/chat/completions");
    }

    private Object buildRequestBody(final Book book) {
        return java.util.Map.of(
                "model", aiConfig.model(),
                "temperature", 0.2,
                "max_tokens", 180,
                "response_format", java.util.Map.of("type", "json_object"),
                "messages", List.of(
                        java.util.Map.of(
                                "role", "system",
                                "content", """
                                        You extract structured book insights from free-text descriptions.
                                        Return valid JSON with exactly these fields:
                                        shortSummary (string),
                                        themes (array of strings),
                                        recommendedAudience (string),
                                        tone (string),
                                        contentWarnings (array of strings).
                                        Keep summary under 35 words.
                                        If a field is unclear, return the best concise inference.
                                        """
                        ),
                        java.util.Map.of(
                                "role", "user",
                                "content", """
                                        Title: %s
                                        Category: %s
                                        Description:
                                        %s
                                        """.formatted(book.title(), book.category(), truncate(book.description(), 1600))
                        )
                )
        );
    }

    private String truncate(final String text, final int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    private BookAiInsights parseInsights(final String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        String content = root.path("choices").path(0).path("message").path("content").asText();
        JsonNode contentNode = objectMapper.readTree(content);

        return new BookAiInsights(
                contentNode.path("shortSummary").asText(""),
                readStringList(contentNode.path("themes")),
                contentNode.path("recommendedAudience").asText(""),
                contentNode.path("tone").asText(""),
                readStringList(contentNode.path("contentWarnings"))
        );
    }

    private List<String> readStringList(final JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        return java.util.stream.StreamSupport.stream(node.spliterator(), false)
                .map(JsonNode::asText)
                .filter(value -> !value.isBlank())
                .toList();
    }
}
