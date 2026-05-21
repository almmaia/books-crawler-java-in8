package com.in8.trainee.crawler.service;

import com.in8.trainee.crawler.config.AppConfig;
import com.in8.trainee.crawler.model.Book;
import com.in8.trainee.crawler.writer.BookExporter;
import java.time.Instant;
import java.util.List;

public final class CrawlOrchestrator {

    private final CrawlerService crawlerService;
    private final BookExporter exporter;
    private final AppConfig config;

    public CrawlOrchestrator(
            final CrawlerService crawlerService,
            final BookExporter exporter,
            final AppConfig config
    ) {
        this.crawlerService = crawlerService;
        this.exporter = exporter;
        this.config = config;
    }

    public CrawlResult run() {
        List<Book> books = crawlerService.crawlAllBooks(config.maxBooks().orElse(Integer.MAX_VALUE));
        return exporter.export(books, config, Instant.now());
    }
}
