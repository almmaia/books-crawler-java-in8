package com.in8.trainee.crawler.parser;

import java.util.Map;
import java.util.Set;

public final class RatingParser {

    private static final Map<String, Integer> VALUES = Map.of(
            "One", 1,
            "Two", 2,
            "Three", 3,
            "Four", 4,
            "Five", 5
    );

    private RatingParser() {
    }

    public static int parse(final Set<String> classNames) {
        return classNames.stream()
                .filter(VALUES::containsKey)
                .findFirst()
                .map(VALUES::get)
                .orElseThrow(() -> new IllegalStateException("Unknown rating classes: " + classNames));
    }
}
