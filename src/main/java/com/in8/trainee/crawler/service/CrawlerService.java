package com.in8.trainee.crawler.service;

import com.in8.trainee.crawler.ai.AiBookEnricher;
import com.in8.trainee.crawler.client.HtmlFetcher;
import com.in8.trainee.crawler.model.Book;
import com.in8.trainee.crawler.model.BookSummary;
import com.in8.trainee.crawler.model.CatalogPage;
import com.in8.trainee.crawler.parser.BookPageParser;
import com.in8.trainee.crawler.parser.CatalogPageParser;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CrawlerService {

    private final HtmlFetcher htmlFetcher;
    private final URI baseUrl;
    private final CatalogPageParser catalogPageParser;
    private final BookPageParser bookPageParser;
    private final Optional<AiBookEnricher> aiBookEnricher;

    public CrawlerService(
            final HtmlFetcher htmlFetcher,
            final URI baseUrl,
            final Optional<AiBookEnricher> aiBookEnricher
    ) {
        this.htmlFetcher = htmlFetcher;
        this.baseUrl = baseUrl;
        this.catalogPageParser = new CatalogPageParser();
        this.bookPageParser = new BookPageParser();
        this.aiBookEnricher = aiBookEnricher;
    }

    public List<Book> crawlAllBooks(final int maxBooks) {
        List<Book> books = new ArrayList<>();
        URI currentPage = baseUrl.resolve("catalogue/page-1.html");

        while (currentPage != null) {
            String catalogHtml = htmlFetcher.get(currentPage);
            CatalogPage catalogPage = catalogPageParser.parse(currentPage, catalogHtml);
            for (BookSummary summary : catalogPage.books()) {
                books.add(fetchBook(summary));
                if (books.size() >= maxBooks) {
                    return List.copyOf(books);
                }
            }
            currentPage = catalogPage.nextPageUrl().orElse(null);
        }

        return List.copyOf(books);
    }

    private Book fetchBook(final BookSummary summary) {
        String html = htmlFetcher.get(summary.detailPageUrl());
        Book book = bookPageParser.parse(summary, html);
        return aiBookEnricher.map(enricher -> enricher.enrich(book)).orElse(book);
    }
}
