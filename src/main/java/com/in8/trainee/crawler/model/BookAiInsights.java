package com.in8.trainee.crawler.model;

import java.util.List;

public record BookAiInsights(
        String shortSummary,
        List<String> themes,
        String recommendedAudience,
        String tone,
        List<String> contentWarnings
) {
}
