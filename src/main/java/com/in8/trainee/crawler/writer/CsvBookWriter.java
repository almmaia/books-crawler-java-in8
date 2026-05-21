package com.in8.trainee.crawler.writer;

import com.in8.trainee.crawler.model.Book;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

public final class CsvBookWriter {

    public void write(final List<Book> books, final Path path) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader(
                        "title",
                        "product_page_url",
                        "image_url",
                        "category",
                        "upc",
                        "price_gbp",
                        "rating",
                        "in_stock",
                        "availability",
                        "description",
                        "ai_short_summary",
                        "ai_themes",
                        "ai_recommended_audience",
                        "ai_tone",
                        "ai_content_warnings"
                )
                .get();

        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
             CSVPrinter csvPrinter = new CSVPrinter(writer, format)) {
            for (Book book : books) {
                csvPrinter.printRecord(
                        book.title(),
                        book.productPageUrl(),
                        book.imageUrl(),
                        book.category(),
                        book.upc(),
                        book.priceGbp(),
                        book.rating(),
                        book.inStock(),
                        book.availability(),
                        book.description(),
                        book.aiInsights() == null ? "" : book.aiInsights().shortSummary(),
                        book.aiInsights() == null ? "" : String.join(" | ", book.aiInsights().themes()),
                        book.aiInsights() == null ? "" : book.aiInsights().recommendedAudience(),
                        book.aiInsights() == null ? "" : book.aiInsights().tone(),
                        book.aiInsights() == null ? "" : String.join(" | ", book.aiInsights().contentWarnings())
                );
            }
        }
    }
}
