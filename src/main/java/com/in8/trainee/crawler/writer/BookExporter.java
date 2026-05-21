package com.in8.trainee.crawler.writer;

import com.in8.trainee.crawler.config.AppConfig;
import com.in8.trainee.crawler.model.Book;
import com.in8.trainee.crawler.persistence.DatabaseBookWriter;
import com.in8.trainee.crawler.service.CrawlResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class BookExporter {

    private static final DateTimeFormatter FILE_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);

    private final JsonBookWriter jsonBookWriter;
    private final CsvBookWriter csvBookWriter;
    private final DatabaseBookWriter databaseBookWriter;

    public BookExporter() {
        this.jsonBookWriter = new JsonBookWriter();
        this.csvBookWriter = new CsvBookWriter();
        this.databaseBookWriter = new DatabaseBookWriter();
    }

    public CrawlResult export(final List<Book> books, final AppConfig config, final Instant finishedAt) {
        try {
            Path outputDirectory = config.outputDirectory();
            Files.createDirectories(outputDirectory);
            String suffix = FILE_TIMESTAMP_FORMATTER.format(finishedAt);
            Path jsonPath = outputDirectory.resolve("books-" + suffix + ".json");
            Path csvPath = outputDirectory.resolve("books-" + suffix + ".csv");
            jsonBookWriter.write(books, jsonPath);
            csvBookWriter.write(books, csvPath);
            boolean databasePersisted = config.databaseConfig()
                    .map(databaseConfig -> {
                        databaseBookWriter.persist(books, databaseConfig);
                        return true;
                    })
                    .orElse(false);
            return new CrawlResult(
                    books,
                    jsonPath,
                    csvPath,
                    finishedAt,
                    databasePersisted,
                    config.aiConfig().isPresent()
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to export crawl result.", exception);
        }
    }
}
