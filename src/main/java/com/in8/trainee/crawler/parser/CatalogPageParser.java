package com.in8.trainee.crawler.parser;

import com.in8.trainee.crawler.model.BookSummary;
import com.in8.trainee.crawler.model.CatalogPage;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public final class CatalogPageParser {

    public CatalogPage parse(final URI pageUri, final String html) {
        Document document = Jsoup.parse(html, pageUri.toString());

        List<BookSummary> books = document.select("article.product_pod")
                .stream()
                .map(this::toBookSummary)
                .collect(Collectors.toList());

        Optional<URI> nextPage = document.select("li.next a")
                .stream()
                .findFirst()
                .map(anchor -> pageUri.resolve(anchor.attr("href")));

        return new CatalogPage(books, nextPage);
    }

    private BookSummary toBookSummary(final Element article) {
        Element anchor = article.selectFirst("h3 a");
        Element image = article.selectFirst(".image_container img");
        Element price = article.selectFirst(".price_color");
        Element availability = article.selectFirst(".instock.availability");
        Element rating = article.selectFirst(".star-rating");

        if (anchor == null || image == null || price == null || availability == null || rating == null) {
            throw new IllegalStateException("Catalog item is missing expected elements.");
        }

        String title = anchor.attr("title").trim();
        URI detailPageUrl = URI.create(anchor.absUrl("href"));
        URI imageUrl = URI.create(image.absUrl("src"));
        BigDecimal priceGbp = PriceParser.parse(price.text());
        String availabilityText = availability.text().trim();

        return new BookSummary(
                title,
                detailPageUrl,
                imageUrl,
                priceGbp,
                RatingParser.parse(rating.classNames()),
                availabilityText.toLowerCase().contains("in stock"),
                availabilityText
        );
    }
}
