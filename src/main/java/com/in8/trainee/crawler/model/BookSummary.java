package com.in8.trainee.crawler.model;

import java.math.BigDecimal;
import java.net.URI;

public record BookSummary(
        String title,
        URI detailPageUrl,
        URI imageUrl,
        BigDecimal priceGbp,
        int rating,
        boolean inStock,
        String availability
) {
}
