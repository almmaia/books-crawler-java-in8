package com.in8.trainee.crawler.parser;

import java.math.BigDecimal;

public final class PriceParser {

    private PriceParser() {
    }

    public static BigDecimal parse(final String rawPrice) {
        String sanitized = rawPrice.replaceAll("[^0-9.,]", "").replace(",", "").trim();
        return new BigDecimal(sanitized);
    }
}
