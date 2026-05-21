package com.in8.trainee.crawler.model;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public record CatalogPage(
        List<BookSummary> books,
        Optional<URI> nextPageUrl
) {
}
