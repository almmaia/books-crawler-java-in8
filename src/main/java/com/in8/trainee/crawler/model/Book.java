package com.in8.trainee.crawler.model;

import java.math.BigDecimal;
import java.net.URI;

public record Book(
        String title,
        URI productPageUrl,
        URI imageUrl,
        String category,
        String upc,
        BigDecimal priceGbp,
        int rating,
        boolean inStock,
        String availability,
        String description,
        BookAiInsights aiInsights
) {
}
