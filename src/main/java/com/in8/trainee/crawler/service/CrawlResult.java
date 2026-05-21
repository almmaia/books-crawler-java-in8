package com.in8.trainee.crawler.service;

import com.in8.trainee.crawler.model.Book;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

public record CrawlResult(
        List<Book> books,
        Path jsonPath,
        Path csvPath,
        Instant finishedAt,
        boolean databasePersisted,
        boolean aiEnrichmentEnabled
) {
}
