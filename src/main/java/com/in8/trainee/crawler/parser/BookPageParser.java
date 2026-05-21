package com.in8.trainee.crawler.parser;

import com.in8.trainee.crawler.model.Book;
import com.in8.trainee.crawler.model.BookSummary;
import java.net.URI;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public final class BookPageParser {

    public Book parse(final BookSummary summary, final String html) {
        Document document = Jsoup.parse(html, summary.detailPageUrl().toString());
        String category = extractCategory(document);
        String upc = requireCellValue(document, "UPC");
        String availability = requireCellValue(document, "Availability");
        boolean inStock = availability.toLowerCase().contains("in stock");
        String description = document.select("#product_description + p")
                .stream()
                .findFirst()
                .map(Element::text)
                .orElse("");
        URI imageUrl = document.select("#product_gallery img")
                .stream()
                .findFirst()
                .map(image -> summary.detailPageUrl().resolve(image.attr("src")))
                .orElse(summary.imageUrl());

        return new Book(
                summary.title(),
                summary.detailPageUrl(),
                normalizedImageUrl(summary.detailPageUrl(), imageUrl),
                category,
                upc,
                summary.priceGbp(),
                summary.rating(),
                inStock,
                availability,
                description,
                null
        );
    }

    private String extractCategory(final Document document) {
        return document.select("ul.breadcrumb li")
                .stream()
                .skip(2)
                .findFirst()
                .map(Element::text)
                .orElse("Unknown");
    }

    private String requireCellValue(final Document document, final String heading) {
        Element row = document.select("table.table.table-striped tr")
                .stream()
                .filter(element -> {
                    Element header = element.selectFirst("th");
                    return header != null && heading.equalsIgnoreCase(header.text());
                })
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing table row for " + heading));

        Element value = row.selectFirst("td");
        if (value == null) {
            throw new IllegalStateException("Missing table cell for " + heading);
        }
        return value.text().trim();
    }

    private URI normalizedImageUrl(final URI detailPageUrl, final URI imageUrl) {
        String rawUrl = imageUrl.toString();
        if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
            return imageUrl;
        }
        return detailPageUrl.resolve(rawUrl);
    }
}
